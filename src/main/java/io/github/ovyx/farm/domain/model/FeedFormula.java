package io.github.ovyx.farm.domain.model;

import io.github.ovyx.farm.domain.FarmErrorCode;
import io.github.ovyx.farm.domain.port.FeedFormulaRoster;
import io.github.ovyx.farm.domain.valueobject.ExpectedIntake;
import io.github.ovyx.farm.domain.valueobject.FeedFormulaDescription;
import io.github.ovyx.farm.domain.valueobject.FeedFormulaName;
import io.github.ovyx.farm.domain.valueobject.PricePerKg;
import io.github.ovyx.shared.domain.AggregateRoot;
import io.github.ovyx.shared.domain.DomainException;
import io.github.ovyx.shared.domain.Notification;
import io.github.ovyx.shared.domain.Violation;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Raiz de agregado do contexto farm: uma formula de racao que a granja compra, com o preco por quilo e o
 * consumo esperado por ave ao dia (R-001 da 004).
 *
 * <p>Invariantes garantidas aqui (data-model.md da 004):
 *
 * <ol>
 *   <li>Os campos seguem as regras deles, e <strong>todas</strong> as violacoes voltam de uma vez
 *       (FR-020)
 *   <li>Nenhuma outra formula, ativa ou inativa, tem o mesmo nome, comparado sem maiusculas (R-011)
 *   <li>Nada e apagado: a formula se inativa e se reativa
 * </ol>
 *
 * <p>A unicidade depende das demais formulas. Mesmo assim e decidida aqui: o agregado pergunta ao
 * {@link FeedFormulaRoster} e recusa. Dois cadastros simultaneos podem fazer a mesma pergunta antes de
 * qualquer um gravar; o indice unico do banco recusa a segunda gravacao, e o despachante repete o
 * comando, que entao recebe daqui o conflito, como no setor.
 *
 * <p>A formula nao guarda os lancamentos que a usam: cada lancamento de racao guarda o preco e o
 * consumo esperado da epoca dele (R-005), e por isso editar a formula nunca muda o passado.
 */
public final class FeedFormula extends AggregateRoot<FeedFormulaId> {

    private static final String NAME_IN_USE = "Já existe uma fórmula com este nome.";
    private static final String NAME_IN_USE_ON_THE_FIELD =
            "Já existe uma fórmula com este nome. Se ela está inativa, reative-a.";

    private final Instant createdAt;

    private FeedFormulaName name;
    private PricePerKg pricePerKg;
    private ExpectedIntake expectedIntake;
    private FeedFormulaDescription description;
    private Status status;
    private Instant updatedAt;

    private FeedFormula(
            FeedFormulaId id,
            FeedFormulaName name,
            PricePerKg pricePerKg,
            ExpectedIntake expectedIntake,
            FeedFormulaDescription description,
            Status status,
            Instant createdAt,
            Instant updatedAt) {
        super(id);
        this.name = name;
        this.pricePerKg = pricePerKg;
        this.expectedIntake = expectedIntake;
        this.description = description;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /**
     * Cadastra uma formula, ativa (FR-001).
     *
     * <p>Primeiro todas as violacoes de campo, de uma vez; depois o conflito de nome: o que a pessoa
     * digitou errado vem antes do que as outras formulas impedem.
     *
     * @throws DomainException quando algum campo viola uma regra, ou quando outra formula ja usa o nome
     */
    public static FeedFormula register(
            String rawName,
            String rawPrice,
            String rawIntake,
            String rawDescription,
            FeedFormulaRoster roster,
            Clock clock) {
        validate(rawName, rawPrice, rawIntake, rawDescription);

        FeedFormulaId id = FeedFormulaId.generate();
        FeedFormulaName name = FeedFormulaName.of(rawName);
        refuseIfNameInUse(name, id, roster);

        Instant now = clock.instant();
        return new FeedFormula(
                id,
                name,
                PricePerKg.of(rawPrice),
                ExpectedIntake.of(rawIntake),
                FeedFormulaDescription.optionalOf(rawDescription).orElse(null),
                Status.ACTIVE,
                now,
                now);
    }

    /**
     * Reconstroi o agregado a partir do que foi gravado, sem revalidar.
     *
     * @param description a descricao, ou {@code null} quando a formula nao tem
     */
    public static FeedFormula restore(
            FeedFormulaId id,
            FeedFormulaName name,
            PricePerKg pricePerKg,
            ExpectedIntake expectedIntake,
            FeedFormulaDescription description,
            Status status,
            Instant createdAt,
            Instant updatedAt) {
        return new FeedFormula(id, name, pricePerKg, expectedIntake, description, status, createdAt, updatedAt);
    }

    /**
     * Edita os campos, com as mesmas regras do cadastro (FR-003). Vale tambem para a formula inativa, que
     * continua inativa. Recusada, a edicao nao altera nada.
     *
     * @throws DomainException quando algum campo viola uma regra, ou quando outra formula ja usa o nome
     */
    public void update(
            String rawName,
            String rawPrice,
            String rawIntake,
            String rawDescription,
            FeedFormulaRoster roster,
            Clock clock) {
        validate(rawName, rawPrice, rawIntake, rawDescription);

        FeedFormulaName newName = FeedFormulaName.of(rawName);
        refuseIfNameInUse(newName, id(), roster);

        this.name = newName;
        this.pricePerKg = PricePerKg.of(rawPrice);
        this.expectedIntake = ExpectedIntake.of(rawIntake);
        this.description = FeedFormulaDescription.optionalOf(rawDescription).orElse(null);
        this.updatedAt = clock.instant();
    }

    /**
     * Inativa a formula: ela deixa de ser oferecida nos lancamentos novos (FR-004). Inativar o que ja
     * esta inativo nao muda nada, nem o instante da ultima alteracao.
     *
     * @return se a formula mudou: a que ja estava inativa nao precisa ser gravada
     */
    public boolean deactivate(Clock clock) {
        if (!isActive()) {
            return false;
        }
        this.status = Status.INACTIVE;
        this.updatedAt = clock.instant();
        return true;
    }

    /**
     * Reativa a formula. O nome dela nao pode ter sido tomado enquanto esteve inativa: a unicidade vale
     * entre todas as formulas.
     *
     * @return se a formula mudou: a que ja estava ativa nao precisa ser gravada
     */
    public boolean reactivate(Clock clock) {
        if (isActive()) {
            return false;
        }
        this.status = Status.ACTIVE;
        this.updatedAt = clock.instant();
        return true;
    }

    private static void validate(String rawName, String rawPrice, String rawIntake, String rawDescription) {
        Notification notification = new Notification();
        FeedFormulaName.validate(rawName, notification);
        PricePerKg.validate(rawPrice, notification);
        ExpectedIntake.validate(rawIntake, notification);
        FeedFormulaDescription.validate(rawDescription, notification);
        notification.throwIfAny(FarmErrorCode.VALIDATION_FAILED);
    }

    /**
     * Recusa com {@code FEED_FORMULA_NAME_IN_USE} quando outra formula usa o nome. A mensagem do campo
     * lembra a reativacao, porque a outra pode estar inativa.
     */
    private static void refuseIfNameInUse(FeedFormulaName name, FeedFormulaId self, FeedFormulaRoster roster) {
        if (roster.nameInUse(name, self)) {
            throw new DomainException(
                    FarmErrorCode.FEED_FORMULA_NAME_IN_USE,
                    NAME_IN_USE,
                    List.of(new Violation("name", FarmErrorCode.FEED_FORMULA_NAME_IN_USE, NAME_IN_USE_ON_THE_FIELD)));
        }
    }

    public boolean isActive() {
        return status == Status.ACTIVE;
    }

    public FeedFormulaName name() {
        return name;
    }

    public PricePerKg pricePerKg() {
        return pricePerKg;
    }

    public ExpectedIntake expectedIntake() {
        return expectedIntake;
    }

    public Optional<FeedFormulaDescription> description() {
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
