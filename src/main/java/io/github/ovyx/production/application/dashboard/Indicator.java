package io.github.ovyx.production.application.dashboard;

import java.math.BigDecimal;

/**
 * Um indicador do periodo, comparado com o periodo anterior (FR-005, FR-006 da 006). Sem dado, o valor
 * fica ausente, e nao zero (FR-009).
 *
 * @param value o valor do periodo; {@code null} sem dado
 * @param previous o valor do periodo anterior; {@code null} sem dado
 * @param change a variacao sobre o anterior; {@code null} sem os dois valores ou com o anterior zero
 * @param incompleteDays os dias do periodo com o lancamento pendente
 */
public record Indicator(
        BigDecimal value, BigDecimal previous, BigDecimal change, GoodDirection goodDirection, int incompleteDays) {}
