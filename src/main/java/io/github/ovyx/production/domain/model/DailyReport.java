package io.github.ovyx.production.domain.model;

import io.github.ovyx.production.domain.ProductionErrorCode;
import io.github.ovyx.production.domain.port.DailyReportRoster;
import io.github.ovyx.production.domain.valueobject.CollectionDate;
import io.github.ovyx.production.domain.valueobject.CollectionTime;
import io.github.ovyx.production.domain.valueobject.EggGrades;
import io.github.ovyx.production.domain.valueobject.FlockAge;
import io.github.ovyx.production.domain.valueobject.MortalityEntry;
import io.github.ovyx.production.domain.valueobject.OpeningBirdCount;
import io.github.ovyx.production.domain.valueobject.ProductionEntry;
import io.github.ovyx.production.domain.valueobject.ReportNote;
import io.github.ovyx.shared.domain.AggregateRoot;
import io.github.ovyx.shared.domain.DomainException;
import io.github.ovyx.shared.domain.Notification;
import io.github.ovyx.shared.domain.Violation;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * Raiz de agregado do contexto production: o relatorio de um dia de coleta de um setor (R-001, R-003).
 *
 * <p>Invariantes garantidas aqui (data-model.md):
 *
 * <ol>
 *   <li>Um relatorio por setor e data (FR-002, R-005)
 *   <li>A data da coleta nao e posterior a hoje no fuso da granja (FR-001, R-006)
 *   <li>Num setor inativo, nenhuma escrita (FR-020)
 *   <li>Setor sem gaiola ativa nao abre relatorio (R-015)
 *   <li>As gaiolas sao fixadas na abertura: nada as acrescenta nem remove depois (FR-005)
 *   <li>A classificacao dos ovos de uma gaiola nao soma mais que os ovos coletados (FR-008), garantida
 *       pelo {@link ProductionEntry}
 *   <li>Mortes e descartes nao passam das aves da gaiola, e os do dia nao passam das aves do inicio do dia
 *       (FR-012)
 *   <li>A confirmacao de dia sem ocorrencia so vale sem morte nem descarte lancado (FR-013)
 *   <li>Nada e apagado (FR-021)
 * </ol>
 *
 * <p>A primeira depende dos demais relatorios do setor. Mesmo assim e decidida aqui, e nao no tratador
 * (principio II): o agregado pergunta ao {@link DailyReportRoster} e recusa. Duas aberturas simultaneas
 * podem fazer a mesma pergunta antes de qualquer uma gravar; o indice unico do banco recusa a segunda
 * gravacao, e o despachante repete o comando, que entao recebe daqui o conflito com o codigo da regra.
 *
 * <p>A situacao do setor vem de fora, pela porta {@link io.github.ovyx.production.domain.port.FarmStructure}
 * (R-004), trazida pelo tratador; e o agregado que recusa.
 */
public final class DailyReport extends AggregateRoot<DailyReportId> {

    private static final String SECTOR_INACTIVE = "O setor está inativo; os relatórios dele são só para consulta.";
    private static final String CAGE_NOT_FOUND = "Gaiola não encontrada neste relatório.";
    private static final String MORTALITY_ALREADY_RECORDED = "O relatório já tem mortes ou descartes lançados.";
    private static final Locale PORTUGUESE = Locale.forLanguageTag("pt-BR");
    private static final String WITHOUT_ACTIVE_CAGES =
            "O setor não tem gaiola ativa. Cadastre as gaiolas antes de abrir o relatório.";
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Comparator<FarmCage> BY_BATTERY_AND_NUMBER =
            Comparator.comparing(FarmCage::battery).thenComparingInt(FarmCage::number);

    private final SectorId sectorId;
    private final List<ReportCage> cages;
    private final Actor openedBy;
    private final Instant openedAt;
    private CollectionDate collectionDate;
    private CollectionTime collectionTime;
    private OpeningBirdCount openingBirdCount;
    private FlockAge flockAge;
    private ReportNote note;
    private boolean noMortalityConfirmed;
    private Actor lastCorrectedBy;
    private Instant lastCorrectedAt;

    private DailyReport(
            DailyReportId id,
            SectorId sectorId,
            CollectionDate collectionDate,
            CollectionTime collectionTime,
            OpeningBirdCount openingBirdCount,
            FlockAge flockAge,
            ReportNote note,
            boolean noMortalityConfirmed,
            List<ReportCage> cages,
            Actor openedBy,
            Instant openedAt,
            Actor lastCorrectedBy,
            Instant lastCorrectedAt) {
        super(id);
        this.sectorId = sectorId;
        this.collectionDate = collectionDate;
        this.collectionTime = collectionTime;
        this.openingBirdCount = openingBirdCount;
        this.flockAge = flockAge;
        this.note = note;
        this.noMortalityConfirmed = noMortalityConfirmed;
        this.cages = new ArrayList<>(cages);
        this.openedBy = openedBy;
        this.openedAt = openedAt;
        this.lastCorrectedBy = lastCorrectedBy;
        this.lastCorrectedAt = lastCorrectedAt;
    }

    /**
     * Abre o relatorio de um dia de coleta do setor (FR-001 a FR-005).
     *
     * <p>Primeiro o setor: inativo ou sem gaiola ativa, nao ha o que preencher. Depois todas as violacoes
     * de campo, de uma vez; por fim o conflito de data, como no farm: o que a pessoa digitou errado vem
     * antes do que os outros relatorios impedem.
     *
     * @param sector o setor como a porta da estrutura da granja o descreve
     * @param today hoje no fuso da granja
     * @param now o instante da abertura
     * @throws DomainException quando o setor esta inativo ou sem gaiola ativa, quando algum campo viola uma
     *     regra, ou quando outro relatorio do setor ja tem a data
     */
    public static DailyReport open(
            FarmSector sector,
            String rawDate,
            String rawTime,
            String rawBirds,
            String rawAge,
            String rawNote,
            Actor actor,
            LocalDate today,
            DailyReportRoster roster,
            Instant now) {
        refuseIfInactive(sector);
        if (sector.activeCages().isEmpty()) {
            throw new DomainException(ProductionErrorCode.SECTOR_WITHOUT_ACTIVE_CAGES, WITHOUT_ACTIVE_CAGES);
        }
        validateGeneralData(rawDate, rawTime, rawBirds, rawAge, rawNote, today);
        DailyReportId id = DailyReportId.generate();
        CollectionDate date = CollectionDate.of(rawDate, today);
        refuseIfDateTaken(sector.id(), date, id, roster);
        List<ReportCage> cages = sector.activeCages().stream()
                .sorted(BY_BATTERY_AND_NUMBER)
                .map(ReportCage::fixed)
                .toList();
        return new DailyReport(
                id,
                sector.id(),
                date,
                CollectionTime.of(rawTime),
                OpeningBirdCount.of(rawBirds),
                FlockAge.of(rawAge),
                ReportNote.optionalOf(rawNote).orElse(null),
                false,
                cages,
                Objects.requireNonNull(actor, "actor"),
                now,
                null,
                null);
    }

    /**
     * Reconstroi o agregado a partir do que foi gravado, sem revalidar: os dados ja foram validados quando
     * entraram.
     *
     * @param note a observacao, ou {@code null}
     * @param lastCorrectedBy quem corrigiu por ultimo, ou {@code null} se ninguem corrigiu
     * @param lastCorrectedAt quando, ou {@code null}
     */
    public static DailyReport restore(
            DailyReportId id,
            SectorId sectorId,
            CollectionDate collectionDate,
            CollectionTime collectionTime,
            OpeningBirdCount openingBirdCount,
            FlockAge flockAge,
            ReportNote note,
            boolean noMortalityConfirmed,
            List<ReportCage> cages,
            Actor openedBy,
            Instant openedAt,
            Actor lastCorrectedBy,
            Instant lastCorrectedAt) {
        return new DailyReport(
                id,
                sectorId,
                collectionDate,
                collectionTime,
                openingBirdCount,
                flockAge,
                note,
                noMortalityConfirmed,
                cages,
                openedBy,
                openedAt,
                lastCorrectedBy,
                lastCorrectedAt);
    }

    public SectorId sectorId() {
        return sectorId;
    }

    public CollectionDate collectionDate() {
        return collectionDate;
    }

    public CollectionTime collectionTime() {
        return collectionTime;
    }

    public OpeningBirdCount openingBirdCount() {
        return openingBirdCount;
    }

    public FlockAge flockAge() {
        return flockAge;
    }

    public Optional<ReportNote> note() {
        return Optional.ofNullable(note);
    }

    public boolean noMortalityConfirmed() {
        return noMortalityConfirmed;
    }

    /** As gaiolas do relatorio, por bateria e numero. */
    public List<ReportCage> cages() {
        return Collections.unmodifiableList(cages);
    }

    public Optional<ReportCage> cage(CageId cageId) {
        return cages.stream().filter(cage -> cage.cageId().equals(cageId)).findFirst();
    }

    public Actor openedBy() {
        return openedBy;
    }

    public Instant openedAt() {
        return openedAt;
    }

    public Optional<Actor> lastCorrectedBy() {
        return Optional.ofNullable(lastCorrectedBy);
    }

    public Optional<Instant> lastCorrectedAt() {
        return Optional.ofNullable(lastCorrectedAt);
    }

    /**
     * Corrige os dados gerais do relatorio (FR-003), com as mesmas regras da abertura, e marca a correcao
     * com quem corrigiu e quando. As gaiolas e os lancamentos nao mudam; quem abriu tambem nao.
     *
     * <p>Primeiro o setor, que precisa estar ativo. Depois todas as violacoes de campo, de uma vez, com a
     * das aves do inicio do dia abaixo do que o relatorio ja removeu (invariante 5). Por fim o conflito de
     * data, como na abertura; manter a propria data nao e conflito.
     *
     * @param sector o setor do relatorio, como a porta da estrutura da granja o descreve
     * @param today hoje no fuso da granja
     * @param now o instante da correcao
     * @throws DomainException quando o setor esta inativo, quando algum campo viola uma regra, ou quando
     *     outro relatorio do setor ja tem a data
     */
    public void correct(
            FarmSector sector,
            String rawDate,
            String rawTime,
            String rawBirds,
            String rawAge,
            String rawNote,
            Actor actor,
            LocalDate today,
            DailyReportRoster roster,
            Instant now) {
        refuseIfNotOf(sector);
        refuseIfInactive(sector);
        Notification notification = new Notification();
        CollectionDate.validate(rawDate, today, notification);
        CollectionTime.validate(rawTime, notification);
        int beforeBirds = notification.violations().size();
        OpeningBirdCount.validate(rawBirds, notification);
        if (notification.violations().size() == beforeBirds) {
            checkOpeningBirds(OpeningBirdCount.of(rawBirds), notification);
        }
        FlockAge.validate(rawAge, notification);
        ReportNote.validate(rawNote, notification);
        notification.throwIfAny(ProductionErrorCode.VALIDATION_FAILED);
        CollectionDate date = CollectionDate.of(rawDate, today);
        refuseIfDateTaken(sectorId, date, id(), roster);
        this.collectionDate = date;
        this.collectionTime = CollectionTime.of(rawTime);
        this.openingBirdCount = OpeningBirdCount.of(rawBirds);
        this.flockAge = FlockAge.of(rawAge);
        this.note = ReportNote.optionalOf(rawNote).orElse(null);
        markCorrection(actor, now);
    }

    /**
     * Lanca ou corrige a producao de uma gaiola do relatorio (FR-007 a FR-010), e marca a correcao do
     * relatorio com quem lancou e quando.
     *
     * <p>Primeiro a gaiola: fora do relatorio, nao ha o que lancar. Depois o setor, que precisa estar ativo;
     * por fim os campos, todos de uma vez, com a soma da classificacao.
     *
     * @param sector o setor do relatorio, como a porta da estrutura da granja o descreve
     * @param now o instante do lancamento
     * @throws DomainException quando a gaiola nao e do relatorio, quando o setor esta inativo, ou quando
     *     algum campo viola uma regra
     */
    public void recordProduction(
            FarmSector sector, CageId cageId, String rawEggs, EggGrades.Raw rawGrades, Actor actor, Instant now) {
        ReportCage cage = cageOrRefuse(cageId);
        refuseIfNotOf(sector);
        refuseIfInactive(sector);
        ProductionEntry entry = ProductionEntry.of(rawEggs, rawGrades);
        cage.recordProduction(entry);
        markCorrection(actor, now);
    }

    /**
     * Lanca ou corrige a mortalidade de uma gaiola do relatorio (FR-011, FR-012), e marca a correcao do
     * relatorio com quem lancou e quando. As aves da gaiola nao mudam (FR-015).
     *
     * <p>Primeiro a gaiola, depois o setor, por fim os campos, todos de uma vez. Com os campos validos,
     * os limites: mortes e descartes nao passam das aves da gaiola, e os do dia — o lancamento antigo desta
     * gaiola fora da soma — nao passam das aves do inicio do dia.
     *
     * @param sector o setor do relatorio, como a porta da estrutura da granja o descreve
     * @param now o instante do lancamento
     * @throws DomainException quando a gaiola nao e do relatorio, quando o setor esta inativo, quando algum
     *     campo viola uma regra, ou quando mortes e descartes passam das aves
     */
    public void recordMortality(
            FarmSector sector,
            CageId cageId,
            String rawDeaths,
            String rawCulls,
            String rawNote,
            Actor actor,
            Instant now) {
        ReportCage cage = cageOrRefuse(cageId);
        refuseIfNotOf(sector);
        refuseIfInactive(sector);
        Notification notification = new Notification();
        if (MortalityEntry.validate(rawDeaths, rawCulls, rawNote, notification)) {
            checkRemovals(cage, MortalityEntry.of(rawDeaths, rawCulls, rawNote).removals(), notification);
        }
        notification.throwIfAny(ProductionErrorCode.VALIDATION_FAILED);
        cage.recordMortality(MortalityEntry.of(rawDeaths, rawCulls, rawNote));
        markCorrection(actor, now);
    }

    /**
     * Confirma que o dia nao teve mortes nem descartes no setor (FR-013). Confirmar de novo nao muda nada.
     *
     * @return se mudou: so entao ha o que gravar
     * @throws DomainException quando o setor esta inativo, ou quando ha morte ou descarte lancado
     */
    public boolean confirmNoMortality(FarmSector sector, Actor actor, Instant now) {
        refuseIfNotOf(sector);
        refuseIfInactive(sector);
        if (cages.stream().anyMatch(ReportCage::hasOccurrence)) {
            throw new DomainException(ProductionErrorCode.MORTALITY_ALREADY_RECORDED, MORTALITY_ALREADY_RECORDED);
        }
        if (noMortalityConfirmed) {
            return false;
        }
        noMortalityConfirmed = true;
        markCorrection(actor, now);
        return true;
    }

    /** {@code COMPLETE} quando toda gaiola do relatorio tem a producao lancada. */
    public ProductionStatus productionStatus() {
        return ProductionStatus.of(pendingCages());
    }

    /** Quantas gaiolas ainda estao sem producao lancada. */
    public int pendingCages() {
        return (int) cages.stream().filter(cage -> !cage.hasProduction()).count();
    }

    /** {@code RECORDED} quando ha ocorrencia lancada ou a confirmacao de dia sem ocorrencia. */
    public MortalityStatus mortalityStatus() {
        return MortalityStatus.of(noMortalityConfirmed, cages.stream().mapToInt(ReportCage::removals).sum());
    }

    private ReportCage cageOrRefuse(CageId cageId) {
        return cage(cageId)
                .orElseThrow(() -> new DomainException(ProductionErrorCode.CAGE_NOT_FOUND, CAGE_NOT_FOUND));
    }

    /** Na correcao, as aves do inicio do dia nao ficam abaixo do que o relatorio ja removeu (invariante 5). */
    private void checkOpeningBirds(OpeningBirdCount birds, Notification notification) {
        int removed = cages.stream().mapToInt(ReportCage::removals).sum();
        if (birds.value() < removed) {
            String removedBirds = removed == 1 ? "1 ave removida" : countOf(removed) + " aves removidas";
            notification.add(
                    "openingBirdCount",
                    ProductionErrorCode.REMOVALS_EXCEED_OPENING_BIRDS,
                    "O relatório já tem " + removedBirds
                            + "; as aves do início do dia não podem ficar abaixo disso.");
        }
    }

    /** Os limites da invariante 5, com as mensagens que nomeiam os numeros (R-013). */
    private void checkRemovals(ReportCage cage, int removals, Notification notification) {
        if (removals > cage.birdCount()) {
            notification.add(
                    "deaths",
                    ProductionErrorCode.REMOVALS_EXCEED_CAGE_BIRDS,
                    "A gaiola tem " + birdsOf(cage.birdCount()) + "; mortes e descartes somam " + countOf(removals)
                            + ".");
            return;
        }
        int ofTheDay = removals
                + cages.stream().filter(other -> !other.equals(cage)).mapToInt(ReportCage::removals).sum();
        int opening = openingBirdCount.value();
        if (ofTheDay > opening) {
            String birds = opening == 1 ? "a 1 ave" : "as " + countOf(opening) + " aves";
            notification.add(
                    "deaths",
                    ProductionErrorCode.REMOVALS_EXCEED_OPENING_BIRDS,
                    "Mortes e descartes do dia somariam " + countOf(ofTheDay) + ", mais que " + birds
                            + " do início do dia.");
        }
    }

    private static String birdsOf(int birds) {
        return birds == 1 ? "1 ave" : countOf(birds) + " aves";
    }

    private static String countOf(int value) {
        return String.format(PORTUGUESE, "%,d", value);
    }

    /** O setor trazido pelo tratador e o do relatorio; outro seria erro de programacao, e nao recusa. */
    private void refuseIfNotOf(FarmSector sector) {
        if (!sector.id().equals(sectorId)) {
            throw new IllegalArgumentException("o setor " + sector.id() + " não é o do relatório " + id());
        }
    }

    private void markCorrection(Actor actor, Instant now) {
        this.lastCorrectedBy = Objects.requireNonNull(actor, "actor");
        this.lastCorrectedAt = Objects.requireNonNull(now, "now");
    }

    private static void refuseIfInactive(FarmSector sector) {
        if (!sector.active()) {
            throw new DomainException(ProductionErrorCode.SECTOR_INACTIVE, SECTOR_INACTIVE);
        }
    }

    private static void validateGeneralData(
            String rawDate, String rawTime, String rawBirds, String rawAge, String rawNote, LocalDate today) {
        Notification notification = new Notification();
        CollectionDate.validate(rawDate, today, notification);
        CollectionTime.validate(rawTime, notification);
        OpeningBirdCount.validate(rawBirds, notification);
        FlockAge.validate(rawAge, notification);
        ReportNote.validate(rawNote, notification);
        notification.throwIfAny(ProductionErrorCode.VALIDATION_FAILED);
    }

    private static void refuseIfDateTaken(
            SectorId sectorId, CollectionDate date, DailyReportId self, DailyReportRoster roster) {
        if (roster.anotherReportOn(sectorId, date.value(), self)) {
            String message = "Já existe o relatório de " + DAY.format(date.value()) + " neste setor.";
            throw new DomainException(
                    ProductionErrorCode.DAILY_REPORT_ALREADY_EXISTS,
                    message,
                    List.of(new Violation("collectionDate", ProductionErrorCode.DAILY_REPORT_ALREADY_EXISTS, message)));
        }
    }
}
