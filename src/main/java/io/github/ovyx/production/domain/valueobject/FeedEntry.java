package io.github.ovyx.production.domain.valueobject;

import io.github.ovyx.production.domain.ProductionErrorCode;
import io.github.ovyx.production.domain.model.CatalogFormula;
import io.github.ovyx.production.domain.model.FeedFormulaId;
import io.github.ovyx.shared.domain.Notification;
import io.github.ovyx.shared.domain.WholeNumber;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * A racao de uma gaiola num dia (feature 004): a formula, o preco por quilo e o consumo esperado dela como
 * estavam no lancamento (R-005), e o consumo do dia, em gramas, inteiro de 0 a 50.000.
 *
 * <p>O preco e o esperado sao guardados, e nao lidos da formula a cada vez: mudar a formula depois nao muda
 * o custo nem o desvio de um lancamento ja feito (SC-003). Zero grama e lancamento, como zero ovo.
 *
 * @param formulaId a formula do farm
 * @param pricePerKg o preco por quilo da formula quando o lancamento foi feito
 * @param expectedIntake o consumo esperado por ave ao dia da formula quando o lancamento foi feito
 * @param consumption o consumo da gaiola no dia, em gramas
 */
public record FeedEntry(FeedFormulaId formulaId, BigDecimal pricePerKg, int expectedIntake, int consumption) {

    private static final String CONSUMPTION_FIELD = "consumption";
    private static final String FORMULA_FIELD = "formulaId";
    private static final int MINIMUM = 0;
    private static final int MAXIMUM = 50_000;

    public FeedEntry {
        Objects.requireNonNull(formulaId, FORMULA_FIELD);
        Objects.requireNonNull(pricePerKg, "pricePerKg");
    }

    /**
     * Registra no {@link Notification} a violacao do consumo, sem lancar: ausente, nao inteiro ou fora da
     * faixa, uma so, a primeira que se aplica.
     */
    public static void validateConsumption(String raw, Notification notification) {
        if (!notification.requirePresent(
                CONSUMPTION_FIELD, raw, ProductionErrorCode.CONSUMPTION_REQUIRED, "Informe o consumo em gramas.")) {
            return;
        }
        if (!WholeNumber.isInteger(raw)) {
            notification.add(
                    CONSUMPTION_FIELD,
                    ProductionErrorCode.CONSUMPTION_NOT_INTEGER,
                    "O consumo deve ser um número inteiro de gramas.");
        } else if (!WholeNumber.within(raw, MINIMUM, MAXIMUM)) {
            notification.add(
                    CONSUMPTION_FIELD,
                    ProductionErrorCode.CONSUMPTION_OUT_OF_RANGE,
                    "O consumo deve ficar entre 0 e 50.000 gramas.");
        }
    }

    /**
     * O lancamento com a formula dada, guardando o preco e o esperado atuais dela.
     *
     * @throws io.github.ovyx.shared.domain.DomainException quando o consumo falta, nao e inteiro ou sai da
     *     faixa
     */
    public static FeedEntry of(CatalogFormula formula, String rawConsumption) {
        Notification notification = new Notification();
        validateConsumption(rawConsumption, notification);
        notification.throwIfAny(ProductionErrorCode.VALIDATION_FAILED);
        return new FeedEntry(
                formula.id(),
                formula.pricePerKg(),
                formula.expectedIntake(),
                WholeNumber.valueOf(rawConsumption).orElseThrow());
    }

    /**
     * O mesmo lancamento com outro consumo: a formula, o preco e o esperado guardados continuam (R-005).
     *
     * @throws io.github.ovyx.shared.domain.DomainException quando o consumo falta, nao e inteiro ou sai da
     *     faixa
     */
    public FeedEntry withConsumption(String rawConsumption) {
        Notification notification = new Notification();
        validateConsumption(rawConsumption, notification);
        notification.throwIfAny(ProductionErrorCode.VALIDATION_FAILED);
        return new FeedEntry(formulaId, pricePerKg, expectedIntake, WholeNumber.valueOf(rawConsumption).orElseThrow());
    }

    /** A proposta da sugestao para uma gaiola: as aves dela vezes o consumo esperado da formula (FR-009). */
    public static FeedEntry suggestedFor(CatalogFormula formula, int birdCount) {
        return new FeedEntry(
                formula.id(), formula.pricePerKg(), formula.expectedIntake(), birdCount * formula.expectedIntake());
    }

    /**
     * Registra, no campo da formula, a proposta que passaria do consumo maximo: as faixas das aves e do
     * esperado permitem mais que o consumo, e o lancamento nao pode sair da regra do campo (research.md da
     * 004, decisoes da implementacao).
     *
     * @param cageCode a gaiola da proposta, como as telas a escrevem
     */
    public static void validateSuggestion(FeedEntry proposal, String cageCode, Notification notification) {
        if (!proposal.withinLimits()) {
            notification.add(
                    FORMULA_FIELD,
                    ProductionErrorCode.CONSUMPTION_OUT_OF_RANGE,
                    "A proposta da gaiola " + cageCode
                            + " passaria de 50.000 g. Lance a ração dela pela própria gaiola.");
        }
    }

    /** Se o consumo fica dentro da faixa do campo. */
    public boolean withinLimits() {
        return consumption >= MINIMUM && consumption <= MAXIMUM;
    }
}
