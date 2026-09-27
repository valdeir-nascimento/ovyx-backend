package io.github.ovyx.production.domain.model;

import io.github.ovyx.production.domain.ProductionErrorCode;
import io.github.ovyx.shared.domain.Notification;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * Uma formula de racao como o production precisa dela (R-004 da 004): o nome, se esta ativa, e o preco e o
 * consumo esperado atuais. E a resposta da porta {@code FeedCatalog}, com as palavras do production, e nao o
 * agregado do farm.
 *
 * @param pricePerKg o preco por quilo atual, em reais
 * @param expectedIntake o consumo esperado atual, em gramas por ave ao dia
 */
public record CatalogFormula(
        FeedFormulaId id, String name, boolean active, BigDecimal pricePerKg, int expectedIntake) {

    public CatalogFormula {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(pricePerKg, "pricePerKg");
    }

    /**
     * Registra, no campo da formula, a formula inativa: cada lancamento novo usa formula ativa (invariante
     * 10 da 004). A mensagem nomeia a formula: ela pode ter sido inativada com a tela da pessoa aberta.
     */
    public void validateActive(Notification notification) {
        if (!active) {
            notification.add(
                    "formulaId",
                    ProductionErrorCode.FORMULA_INACTIVE,
                    "A fórmula " + name + " está inativa. Escolha uma fórmula ativa.");
        }
    }
}
