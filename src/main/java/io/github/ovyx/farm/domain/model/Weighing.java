package io.github.ovyx.farm.domain.model;

import io.github.ovyx.farm.domain.FarmErrorCode;
import io.github.ovyx.farm.domain.port.WeighingRoster;
import io.github.ovyx.farm.domain.valueobject.AverageWeight;
import io.github.ovyx.farm.domain.valueobject.WeighingDate;
import io.github.ovyx.shared.domain.AggregateRoot;
import io.github.ovyx.shared.domain.DomainException;
import io.github.ovyx.shared.domain.Notification;
import io.github.ovyx.shared.domain.Violation;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * A pesagem de uma gaiola: o peso medio de uma amostra de aves num dia (feature 005). Raiz de agregado do
 * contexto farm, independente do {@link Sector} (R-003 da 005): pesar nao muda o setor, e o setor nao
 * carrega as pesagens.
 *
 * <p>As regras moram aqui:
 * <ul>
 *   <li>a gaiola precisa ser do setor, e os dois ativos: numa gaiola ou num setor inativos, as pesagens so
 *       se consultam (FR-008);
 *   <li>a data nao e futura no fuso da granja, e o peso fica de 1 a 10.000 g, com ate uma casa (FR-003);
 *   <li>a gaiola tem no maximo uma pesagem valida por dia (FR-004);
 *   <li>nada e apagado: a pesagem excluida fica anulada, com quem a anulou e quando, e nao se corrige nem
 *       volta (FR-006, FR-015).
 * </ul>
 *
 * <p>O setor chega ao agregado so para ser lido, e nunca e gravado por ele.
 */
public final class Weighing extends AggregateRoot<WeighingId> {

    private static final String CAGE_NOT_FOUND = "Gaiola não encontrada.";
    private static final String SECTOR_INACTIVE = "O setor está inativo; as pesagens dele são só para consulta.";
    private static final String CAGE_INACTIVE = "A gaiola está inativa; as pesagens dela são só para consulta.";
    private static final String NOT_FOUND = "Pesagem não encontrada.";
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final SectorId sectorId;
    private final CageId cageId;
    private final Actor recordedBy;
    private final Instant recordedAt;

    private WeighingDate weighedOn;
    private AverageWeight averageWeight;
    private WeighingStatus status;
    private Actor lastCorrectedBy;
    private Instant lastCorrectedAt;
    private Actor voidedBy;
    private Instant voidedAt;

    private Weighing(
            WeighingId id,
            SectorId sectorId,
            CageId cageId,
            WeighingDate weighedOn,
            AverageWeight averageWeight,
            WeighingStatus status,
            Actor recordedBy,
            Instant recordedAt) {
        super(id);
        this.sectorId = Objects.requireNonNull(sectorId, "sectorId");
        this.cageId = Objects.requireNonNull(cageId, "cageId");
        this.weighedOn = Objects.requireNonNull(weighedOn, "weighedOn");
        this.averageWeight = Objects.requireNonNull(averageWeight, "averageWeight");
        this.status = Objects.requireNonNull(status, "status");
        this.recordedBy = Objects.requireNonNull(recordedBy, "recordedBy");
        this.recordedAt = Objects.requireNonNull(recordedAt, "recordedAt");
    }

    /**
     * Registra a pesagem de uma gaiola do setor, valida (FR-003).
     *
     * <p>Primeiro a gaiola, depois a situacao dela e do setor, depois a data e o peso, de uma vez; por fim, o
     * dia ja pesado: o que a pessoa digitou errado vem antes do que as outras pesagens impedem.
     *
     * @param today o dia de hoje na granja
     * @throws DomainException quando a gaiola nao e do setor, quando a gaiola ou o setor estao inativos,
     *     quando a data ou o peso violam uma regra, ou quando a gaiola ja tem pesagem no dia
     */
    public static Weighing record(
            Sector sector,
            CageId cageId,
            String rawWeighedOn,
            String rawWeight,
            LocalDate today,
            WeighingRoster roster,
            Actor actor,
            Instant now) {
        refuseIfNotWritable(sector, cageId);
        validate(rawWeighedOn, rawWeight, today);

        WeighingId id = WeighingId.generate();
        WeighingDate day = WeighingDate.of(rawWeighedOn, today);
        refuseIfDayTaken(cageId, day, id, roster);
        return new Weighing(
                id, sector.id(), cageId, day, AverageWeight.of(rawWeight), WeighingStatus.VALID, actor, now);
    }

    /**
     * Reconstroi o agregado a partir do que foi gravado, sem revalidar.
     *
     * @param lastCorrectedBy quem corrigiu por ultimo, ou {@code null} sem correcao
     * @param voidedBy quem anulou, ou {@code null} na valida
     */
    public static Weighing restore(
            WeighingId id,
            SectorId sectorId,
            CageId cageId,
            WeighingDate weighedOn,
            AverageWeight averageWeight,
            WeighingStatus status,
            Actor recordedBy,
            Instant recordedAt,
            Actor lastCorrectedBy,
            Instant lastCorrectedAt,
            Actor voidedBy,
            Instant voidedAt) {
        Weighing weighing =
                new Weighing(id, sectorId, cageId, weighedOn, averageWeight, status, recordedBy, recordedAt);
        weighing.lastCorrectedBy = lastCorrectedBy;
        weighing.lastCorrectedAt = lastCorrectedAt;
        weighing.voidedBy = voidedBy;
        weighing.voidedAt = voidedAt;
        return weighing;
    }

    /**
     * Corrige o dia e o peso, com as regras do registro, e marca quem corrigiu e quando (FR-005). Recusada, a
     * correcao nao altera nada.
     *
     * @param sector o setor da pesagem, lido para a situacao dele e da gaiola
     * @param today o dia de hoje na granja
     * @throws DomainException quando a pesagem esta anulada, quando a gaiola ou o setor estao inativos,
     *     quando a data ou o peso violam uma regra, ou quando outra pesagem valida da gaiola ja tem o dia
     */
    public void correct(
            Sector sector,
            String rawWeighedOn,
            String rawWeight,
            LocalDate today,
            WeighingRoster roster,
            Actor actor,
            Instant now) {
        if (!isValid()) {
            throw new DomainException(FarmErrorCode.WEIGHING_NOT_FOUND, NOT_FOUND);
        }
        refuseIfNotWritable(sector, cageId);
        validate(rawWeighedOn, rawWeight, today);

        WeighingDate day = WeighingDate.of(rawWeighedOn, today);
        refuseIfDayTaken(cageId, day, id(), roster);

        this.weighedOn = day;
        this.averageWeight = AverageWeight.of(rawWeight);
        this.lastCorrectedBy = actor;
        this.lastCorrectedAt = now;
    }

    /**
     * Anula a pesagem: ela sai das leituras, e fica guardada com quem a anulou e quando (FR-006). Nada e
     * apagado (FR-015). Anular de novo a ja anulada nao muda nada.
     *
     * @return se a pesagem mudou; {@code false} quando ja estava anulada, e entao nao ha o que gravar
     * @throws DomainException quando a gaiola ou o setor estao inativos
     */
    public boolean voidBy(Sector sector, Actor actor, Instant now) {
        if (!isValid()) {
            return false;
        }
        refuseIfNotWritable(sector, cageId);
        this.status = WeighingStatus.VOIDED;
        this.voidedBy = actor;
        this.voidedAt = now;
        return true;
    }

    /** A gaiola precisa ser do setor, e os dois ativos (FR-008). */
    private static void refuseIfNotWritable(Sector sector, CageId cageId) {
        Cage cage = sector.cage(cageId)
                .orElseThrow(() -> new DomainException(FarmErrorCode.CAGE_NOT_FOUND, CAGE_NOT_FOUND));
        if (!sector.isActive()) {
            throw new DomainException(FarmErrorCode.SECTOR_INACTIVE, SECTOR_INACTIVE);
        }
        if (!cage.isActive()) {
            throw new DomainException(FarmErrorCode.CAGE_INACTIVE, CAGE_INACTIVE);
        }
    }

    /** A data e o peso, as falhas dos dois de uma vez (FR-014). */
    private static void validate(String rawWeighedOn, String rawWeight, LocalDate today) {
        Notification notification = new Notification();
        WeighingDate.validate(rawWeighedOn, today, notification);
        AverageWeight.validate(rawWeight, notification);
        notification.throwIfAny(FarmErrorCode.VALIDATION_FAILED);
    }

    /** Uma pesagem valida por gaiola e dia (FR-004): a do dia se corrige, e nao se registra outra. */
    private static void refuseIfDayTaken(CageId cageId, WeighingDate day, WeighingId self, WeighingRoster roster) {
        if (roster.isDayTaken(cageId, day.value(), self)) {
            String message = "A gaiola já tem pesagem em " + DAY.format(day.value()) + ". Corrija a pesagem desse dia.";
            throw new DomainException(
                    FarmErrorCode.WEIGHING_DATE_IN_USE,
                    message,
                    List.of(new Violation("weighedOn", FarmErrorCode.WEIGHING_DATE_IN_USE, message)));
        }
    }

    /** Se a pesagem vale: a anulada fica guardada, mas sai das leituras. */
    public boolean isValid() {
        return status == WeighingStatus.VALID;
    }

    public SectorId sectorId() {
        return sectorId;
    }

    public CageId cageId() {
        return cageId;
    }

    public WeighingDate weighedOn() {
        return weighedOn;
    }

    public AverageWeight averageWeight() {
        return averageWeight;
    }

    public WeighingStatus status() {
        return status;
    }

    public Actor recordedBy() {
        return recordedBy;
    }

    public Instant recordedAt() {
        return recordedAt;
    }

    public Optional<Actor> lastCorrectedBy() {
        return Optional.ofNullable(lastCorrectedBy);
    }

    public Optional<Instant> lastCorrectedAt() {
        return Optional.ofNullable(lastCorrectedAt);
    }

    public Optional<Actor> voidedBy() {
        return Optional.ofNullable(voidedBy);
    }

    public Optional<Instant> voidedAt() {
        return Optional.ofNullable(voidedAt);
    }
}
