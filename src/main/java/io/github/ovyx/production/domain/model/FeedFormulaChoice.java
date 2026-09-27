package io.github.ovyx.production.domain.model;

import io.github.ovyx.production.domain.ProductionErrorCode;
import io.github.ovyx.production.domain.port.FeedCatalog;
import io.github.ovyx.shared.domain.Notification;
import java.util.Optional;

/**
 * A formula escolhida num lancamento de racao, como o catalogo a encontrou: nenhuma escolhida, uma que nao
 * existe, ou a formula.
 *
 * <p>A ausencia e a inexistencia sao recusas do campo, e nao "nao encontrado": vem junto das demais falhas do
 * lancamento, de uma vez (FR-020 da 004). Por isso a escolha chega ao agregado, que decide, em vez de o
 * tratador recusar antes dele.
 */
public final class FeedFormulaChoice {

    private static final String FIELD = "formulaId";

    private final boolean informed;
    private final CatalogFormula formula;

    private FeedFormulaChoice(boolean informed, CatalogFormula formula) {
        this.informed = informed;
        this.formula = formula;
    }

    /** A formula encontrada no catalogo. */
    public static FeedFormulaChoice of(CatalogFormula formula) {
        return new FeedFormulaChoice(true, formula);
    }

    /** Nenhuma formula escolhida. */
    public static FeedFormulaChoice absent() {
        return new FeedFormulaChoice(false, null);
    }

    /** Um identificador que nao aponta formula nenhuma, malformado ou inexistente. */
    public static FeedFormulaChoice unknown() {
        return new FeedFormulaChoice(true, null);
    }

    /** A escolha do identificador como veio, procurado no catalogo. */
    public static FeedFormulaChoice lookup(String raw, FeedCatalog catalog) {
        if (raw == null || raw.isBlank()) {
            return absent();
        }
        return FeedFormulaId.parse(raw)
                .flatMap(catalog::formulaOf)
                .map(FeedFormulaChoice::of)
                .orElseGet(FeedFormulaChoice::unknown);
    }

    /**
     * Registra no {@link Notification} a formula ausente ou inexistente, no campo da formula, e devolve a
     * formula quando ha uma. Se ela esta ativa e regra de quem lanca: a correcao aceita a que a gaiola ja usa.
     */
    public Optional<CatalogFormula> validate(Notification notification) {
        if (!informed) {
            notification.add(FIELD, ProductionErrorCode.FORMULA_REQUIRED, "Escolha a fórmula.");
            return Optional.empty();
        }
        if (formula == null) {
            notification.add(FIELD, ProductionErrorCode.FORMULA_NOT_FOUND, "Fórmula não encontrada.");
            return Optional.empty();
        }
        return Optional.of(formula);
    }
}
