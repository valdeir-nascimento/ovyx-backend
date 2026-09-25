package io.github.ovyx.farm.domain.model;

import io.github.ovyx.farm.domain.FarmErrorCode;
import io.github.ovyx.farm.domain.port.SectorRoster;
import io.github.ovyx.farm.domain.valueobject.Battery;
import io.github.ovyx.farm.domain.valueobject.BirdCount;
import io.github.ovyx.farm.domain.valueobject.CageNumber;
import io.github.ovyx.farm.domain.valueobject.SectorDescription;
import io.github.ovyx.farm.domain.valueobject.SectorName;
import io.github.ovyx.shared.domain.AggregateRoot;
import io.github.ovyx.shared.domain.DomainException;
import io.github.ovyx.shared.domain.Notification;
import io.github.ovyx.shared.domain.Violation;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Raiz de agregado do contexto farm: um setor da granja — um galpao, uma especie ou um lote
 * acompanhado separadamente (R-001, R-003).
 *
 * <p>Invariantes garantidas aqui (data-model.md):
 *
 * <ol>
 *   <li>Nome e descricao seguem as regras dos campos, e <strong>todas</strong> as violacoes voltam de
 *       uma vez (FR-017)
 *   <li>Nenhum outro setor ativo tem o mesmo nome, comparado sem maiusculas (FR-002)
 *   <li>Nenhuma outra gaiola ativa do setor tem a mesma bateria e o mesmo numero (FR-007)
 *   <li>O setor de uma gaiola nao muda: nao ha operacao que a mova (FR-009)
 *   <li>Setor inativo nao tem gaiola ativa: nao recebe gaiola nova, nao edita e nao reativa gaiola
 *       sozinho (FR-014, FR-015)
 *   <li>Nada e apagado: inativar o setor inativa junto as gaiolas ativas dele, e reativa-lo traz de
 *       volta exatamente essas (FR-012, FR-015, R-004)
 * </ol>
 *
 * <p>O setor contem as gaiolas (R-003): toda escrita numa gaiola passa por ele, e muda o instante da
 * ultima alteracao dele, e com isso a versao da linha. Duas escritas simultaneas em gaiolas do mesmo
 * setor nao passam as duas sem que uma veja a outra.
 *
 * <p>A segunda depende dos demais setores. Mesmo assim e decidida aqui, e nao no tratador (principio
 * II): o agregado pergunta ao {@link SectorRoster} e recusa. Duas requisicoes simultaneas podem fazer a
 * mesma pergunta antes de qualquer uma gravar; o indice unico parcial do banco recusa a segunda
 * gravacao, e o despachante repete o comando, que entao recebe daqui o conflito com o codigo da regra
 * (R-005).
 */
public final class Sector extends AggregateRoot<SectorId> {

    private static final String NAME_IN_USE = "Já existe um setor ativo com este nome.";
    private static final String CAGE_NOT_FOUND = "Gaiola não encontrada.";
    private static final String SECTOR_INACTIVE =
            "O setor está inativo. Reative o setor antes de mexer nas gaiolas dele.";

    private final Instant createdAt;
    private final List<Cage> cages;

    private SectorName name;
    private SectorDescription description;
    private Status status;
    private Instant updatedAt;

    private Sector(
            SectorId id,
            SectorName name,
            SectorDescription description,
            Status status,
            List<Cage> cages,
            Instant createdAt,
            Instant updatedAt) {
        super(id);
        this.name = name;
        this.description = description;
        this.status = status;
        this.cages = new ArrayList<>(cages);
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /**
     * Cadastra um setor, ativo e sem gaiolas (FR-001).
     *
     * <p>Primeiro todas as violacoes de campo, de uma vez; depois o conflito de nome, como na feature
     * 001: o que a pessoa digitou errado vem antes do que os outros setores impedem.
     *
     * @throws DomainException quando algum campo viola uma regra, ou quando outro setor ativo ja usa o
     *     nome
     */
    public static Sector register(String rawName, String rawDescription, SectorRoster roster, Clock clock) {
        Notification notification = new Notification();
        SectorName.validate(rawName, notification);
        SectorDescription.validate(rawDescription, notification);
        notification.throwIfAny(FarmErrorCode.VALIDATION_FAILED);

        SectorId id = SectorId.generate();
        SectorName name = SectorName.of(rawName);
        refuseIfNameInUse(name, id, roster);

        Instant now = clock.instant();
        return new Sector(
                id,
                name,
                SectorDescription.optionalOf(rawDescription).orElse(null),
                Status.ACTIVE,
                List.of(),
                now,
                now);
    }

    /**
     * Reconstroi o agregado a partir do que foi gravado.
     *
     * <p>Usada pelo adaptador de persistencia: os dados ja foram validados quando entraram, e
     * revalidar na leitura faria o sistema recusar registros legitimos apos uma mudanca de regra.
     *
     * @param description a descricao, ou {@code null} quando o setor nao tem
     * @param cages todas as gaiolas do setor, ativas e inativas
     */
    public static Sector restore(
            SectorId id,
            SectorName name,
            SectorDescription description,
            Status status,
            List<Cage> cages,
            Instant createdAt,
            Instant updatedAt) {
        return new Sector(id, name, description, status, cages, createdAt, updatedAt);
    }

    /**
     * Edita o nome e a descricao, com as mesmas regras do cadastro (FR-003). Recusada, a edicao nao
     * altera nada.
     *
     * @throws DomainException quando algum campo viola uma regra, ou quando outro setor ativo ja usa o
     *     nome
     */
    public void update(String rawName, String rawDescription, SectorRoster roster, Clock clock) {
        Notification notification = new Notification();
        SectorName.validate(rawName, notification);
        SectorDescription.validate(rawDescription, notification);
        notification.throwIfAny(FarmErrorCode.VALIDATION_FAILED);

        SectorName newName = SectorName.of(rawName);
        refuseIfNameInUse(newName, id(), roster);

        this.name = newName;
        this.description = SectorDescription.optionalOf(rawDescription).orElse(null);
        touch(clock);
    }

    /**
     * Cadastra uma gaiola no setor (FR-006, FR-007).
     *
     * <p>Primeiro as violacoes dos tres campos, de uma vez; depois o conflito com outra gaiola ativa.
     *
     * @return a identidade da gaiola nova, que as features seguintes usam
     * @throws DomainException quando o setor esta inativo, quando algum campo viola uma regra, ou quando
     *     outra gaiola ativa do setor ja tem a bateria e o numero
     */
    public CageId registerCage(String rawBattery, String rawNumber, String rawBirdCount, Clock clock) {
        refuseIfInactive();
        validateCage(rawBattery, rawNumber, rawBirdCount);
        Battery battery = Battery.of(rawBattery);
        CageNumber number = CageNumber.of(rawNumber);
        refuseIfCageExists(battery, number, null);

        Instant now = clock.instant();
        Cage cage = Cage.register(battery, number, BirdCount.of(rawBirdCount), now);
        cages.add(cage);
        this.updatedAt = now;
        return cage.id();
    }

    /**
     * Edita a bateria, o numero e as aves de uma gaiola, com as mesmas regras do cadastro (FR-009). O
     * setor da gaiola nao muda. Recusada, a edicao nao altera nada.
     *
     * <p>A unicidade so e conferida para a gaiola ativa: a inativa nao ocupa codigo, e a reativacao dela
     * confere de novo.
     *
     * @throws DomainException quando a gaiola nao e deste setor, quando algum campo viola uma regra, ou
     *     quando outra gaiola ativa ja tem a bateria e o numero
     */
    public void updateCage(CageId cageId, String rawBattery, String rawNumber, String rawBirdCount, Clock clock) {
        Cage cage = cageOrRefuse(cageId);
        refuseIfInactive();
        validateCage(rawBattery, rawNumber, rawBirdCount);
        Battery battery = Battery.of(rawBattery);
        CageNumber number = CageNumber.of(rawNumber);
        if (cage.isActive()) {
            refuseIfCageExists(battery, number, cageId);
        }

        Instant now = clock.instant();
        cage.update(battery, number, BirdCount.of(rawBirdCount), now);
        this.updatedAt = now;
    }

    /**
     * Inativa o setor e, junto, cada gaiola ativa dele, marcada como inativada com o setor (FR-015,
     * R-004). Nada e apagado. Inativar o que ja esta inativo nao muda nada, nem o instante da ultima
     * alteracao.
     *
     * @return se o setor mudou: o que ja estava inativo nao muda, e nao precisa ser gravado
     */
    public boolean deactivate(Clock clock) {
        if (!isActive()) {
            return false;
        }
        Instant now = clock.instant();
        this.status = Status.INACTIVE;
        cages.stream().filter(Cage::isActive).forEach(cage -> cage.deactivate(true, now));
        this.updatedAt = now;
        return true;
    }

    /**
     * Reativa o setor e exatamente as gaiolas que a inativacao dele levou; as que ja estavam inativas
     * antes continuam inativas (FR-015). As que voltam nao conflitam entre si nem com outras: enquanto
     * o setor esteve inativo, ele nao recebeu gaiola nova.
     *
     * @return se o setor mudou: o que ja estava ativo nao muda, e nao precisa ser gravado
     * @throws DomainException quando outro setor ativo tomou o nome enquanto este esteve inativo (FR-016)
     */
    public boolean reactivate(SectorRoster roster, Clock clock) {
        if (isActive()) {
            return false;
        }
        refuseIfNameInUse(name, id(), roster);
        Instant now = clock.instant();
        this.status = Status.ACTIVE;
        cages.stream().filter(Cage::deactivatedWithSector).forEach(cage -> cage.reactivate(now));
        this.updatedAt = now;
        return true;
    }

    /**
     * Inativa uma gaiola sozinha (FR-012): ela sai dos totais e continua consultavel. Inativar a que ja
     * esta inativa nao muda nada.
     *
     * @return se a gaiola mudou: a que ja estava inativa nao muda, e nao precisa ser gravada
     * @throws DomainException quando a gaiola nao e deste setor
     */
    public boolean deactivateCage(CageId cageId, Clock clock) {
        Cage cage = cageOrRefuse(cageId);
        if (!cage.isActive()) {
            return false;
        }
        Instant now = clock.instant();
        cage.deactivate(false, now);
        this.updatedAt = now;
        return true;
    }

    /**
     * Reativa uma gaiola sozinha. Reativar a que ja esta ativa nao muda nada.
     *
     * @throws DomainException quando a gaiola nao e deste setor, quando o setor esta inativo (FR-015), ou
     *     quando outra gaiola ativa tomou a bateria e o numero dela (FR-016)
     * @return se a gaiola mudou: a que ja estava ativa nao muda, e nao precisa ser gravada
     */
    public boolean reactivateCage(CageId cageId, Clock clock) {
        Cage cage = cageOrRefuse(cageId);
        refuseIfInactive();
        if (cage.isActive()) {
            return false;
        }
        refuseIfCageExists(cage.battery(), cage.number(), cageId);
        Instant now = clock.instant();
        cage.reactivate(now);
        this.updatedAt = now;
        return true;
    }

    /** A gaiola deste setor, ou nenhuma. */
    public Optional<Cage> cage(CageId cageId) {
        return cages.stream().filter(cage -> cage.id().equals(cageId)).findFirst();
    }

    /** Todas as gaiolas do setor, ativas e inativas, na ordem em que foram cadastradas. */
    public List<Cage> cages() {
        return Collections.unmodifiableList(cages);
    }

    /** Setor inativo nao tem gaiola ativa: nao recebe, nao edita e nao reativa gaiola (invariante 3). */
    private void refuseIfInactive() {
        if (!isActive()) {
            throw new DomainException(FarmErrorCode.SECTOR_INACTIVE, SECTOR_INACTIVE);
        }
    }

    private Cage cageOrRefuse(CageId cageId) {
        return cage(cageId)
                .orElseThrow(() -> new DomainException(FarmErrorCode.CAGE_NOT_FOUND, CAGE_NOT_FOUND));
    }

    private static void validateCage(String rawBattery, String rawNumber, String rawBirdCount) {
        Notification notification = new Notification();
        Battery.validate(rawBattery, notification);
        CageNumber.validate(rawNumber, notification);
        BirdCount.validate(rawBirdCount, notification);
        notification.throwIfAny(FarmErrorCode.VALIDATION_FAILED);
    }

    /**
     * Recusa com {@code CAGE_ALREADY_EXISTS} quando outra gaiola ativa do setor ocupa a bateria e o
     * numero (FR-007): a mesma frase na recusa e nos dois campos, que e onde a pessoa corrige.
     *
     * @param self a gaiola que se edita, que nao conflita consigo mesma; {@code null} no cadastro
     */
    private void refuseIfCageExists(Battery battery, CageNumber number, CageId self) {
        boolean taken = cages.stream()
                .filter(cage -> !cage.id().equals(self))
                .anyMatch(cage -> cage.occupies(battery, number));
        if (taken) {
            String message = "Já existe uma gaiola ativa " + Cage.codeOf(battery.value(), number.value())
                    + " neste setor.";
            throw new DomainException(
                    FarmErrorCode.CAGE_ALREADY_EXISTS,
                    message,
                    List.of(
                            new Violation("battery", FarmErrorCode.CAGE_ALREADY_EXISTS, message),
                            new Violation("number", FarmErrorCode.CAGE_ALREADY_EXISTS, message)));
        }
    }

    /**
     * Recusa com {@code SECTOR_NAME_IN_USE} quando outro setor ativo usa o nome (FR-002).
     *
     * <p>A mesma frase na recusa e no campo: e o que a pessoa le ao lado do nome.
     */
    private static void refuseIfNameInUse(SectorName name, SectorId self, SectorRoster roster) {
        if (roster.anotherActiveSectorNamed(name, self)) {
            throw new DomainException(
                    FarmErrorCode.SECTOR_NAME_IN_USE,
                    NAME_IN_USE,
                    List.of(new Violation("name", FarmErrorCode.SECTOR_NAME_IN_USE, NAME_IN_USE)));
        }
    }

    private void touch(Clock clock) {
        this.updatedAt = clock.instant();
    }

    public boolean isActive() {
        return status == Status.ACTIVE;
    }

    public SectorName name() {
        return name;
    }

    public Optional<SectorDescription> description() {
        return Optional.ofNullable(description);
    }

    public Status status() {
        return status;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }
}
