package io.github.ovyx.production.application.dashboard;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.Objects;

/**
 * A meta de produtividade do setor, como o painel a le do farm (R-008 da 008). Ate a feature 008, era a
 * constante de 85% para todos os setores; agora e a que o administrador define no cadastro do setor.
 *
 * <p>O grafico da produtividade diaria a marca, o selo compara o ultimo dia com ela, e o alerta de baixa
 * postura a usa. A regra de "atingiu a meta" fica aqui, e nao em cada conta, para o selo e o alerta nunca
 * discordarem.
 *
 * @param value a meta, em porcentagem, com duas casas como a produtividade
 */
public record LayingRateTarget(BigDecimal value) {

    private static final int SCALE = 2;

    public LayingRateTarget {
        Objects.requireNonNull(value, "value");
        value = value.setScale(SCALE, RoundingMode.HALF_UP);
    }

    /** Se a produtividade atingiu a meta: a igualdade conta como atingida (FR-006 da 008, FR-010 da 006). */
    public boolean isMetBy(BigDecimal layingRate) {
        return layingRate.compareTo(value) >= 0;
    }

    /** A meta sem zeros a direita, com virgula, como o texto do alerta a escreve: "85", "82,5". */
    public String label() {
        DecimalFormat format = new DecimalFormat("0.##", DecimalFormatSymbols.getInstance(Locale.of("pt", "BR")));
        return format.format(value);
    }
}
