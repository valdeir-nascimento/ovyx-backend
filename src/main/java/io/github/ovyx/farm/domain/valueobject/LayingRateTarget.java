package io.github.ovyx.farm.domain.valueobject;

import io.github.ovyx.farm.domain.FarmErrorCode;
import io.github.ovyx.shared.domain.Notification;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Meta de produtividade do setor: a porcentagem de ovos coletados sobre as aves alojadas que a granja espera por
 * dia, de 1 a 100, com ate uma casa decimal (FR-001 e R-003 da 008). Obrigatoria em todo setor.
 *
 * <p>Chega como texto cru, como o preco da formula: "82,5" e "82.5" sao a mesma meta, e o numero do JSON chega
 * como o texto dele. O separador de milhar e o simbolo "%" nao sao aceitos: a pessoa corrige o campo em vez de o
 * sistema adivinhar.
 *
 * @param value a meta, em porcentagem, com exatamente uma casa decimal
 */
public record LayingRateTarget(BigDecimal value) {

    private static final String FIELD = "layingRateTarget";
    private static final BigDecimal MINIMUM = BigDecimal.ONE;
    private static final BigDecimal MAXIMUM = new BigDecimal("100");
    private static final int SCALE = 1;

    /** Inteiro, ou com uma casa depois da virgula ou do ponto; com sinal, para cair na faixa. */
    private static final Pattern PERCENT = Pattern.compile("[+-]?\\d+([.,]\\d)?");

    /**
     * Construtor canonico: so a garantia estrutural, para a reidratacao. Guarda a meta com uma casa, para a
     * igualdade nao depender da escala em que ela chegou (72 e 72.0 sao a mesma meta).
     *
     * @throws ArithmeticException quando a meta tem mais de uma casa decimal que nao seja zero
     */
    public LayingRateTarget {
        Objects.requireNonNull(value, "value");
        value = value.setScale(SCALE, RoundingMode.UNNECESSARY);
    }

    /**
     * Registra no {@link Notification} a violacao do campo, sem lancar: ausente, fora do formato ou fora da
     * faixa, uma so, a primeira que se aplica.
     */
    public static void validate(String raw, Notification notification) {
        if (!notification.requirePresent(
                FIELD,
                raw,
                FarmErrorCode.LAYING_RATE_TARGET_REQUIRED,
                "Informe a meta de produtividade, de 1 a 100%.")) {
            return;
        }
        Optional<BigDecimal> target = parse(raw);
        if (target.isEmpty()) {
            notification.add(
                    FIELD,
                    FarmErrorCode.LAYING_RATE_TARGET_INVALID,
                    "Informe a meta em porcentagem, com até uma casa decimal.");
        } else if (target.get().compareTo(MINIMUM) < 0 || target.get().compareTo(MAXIMUM) > 0) {
            notification.add(
                    FIELD, FarmErrorCode.LAYING_RATE_TARGET_OUT_OF_RANGE, "A meta deve ficar entre 1% e 100%.");
        }
    }

    /**
     * Cria a meta, recusando na hora com a violacao do campo.
     *
     * @throws io.github.ovyx.shared.domain.DomainException quando a meta falta, esta fora do formato ou sai da
     *     faixa
     */
    public static LayingRateTarget of(String raw) {
        Notification notification = new Notification();
        validate(raw, notification);
        notification.throwIfAny(FarmErrorCode.VALIDATION_FAILED);
        return new LayingRateTarget(parse(raw).orElseThrow());
    }

    /** A meta lida do texto, com uma casa, ou nenhuma quando o texto nao e uma porcentagem. */
    private static Optional<BigDecimal> parse(String raw) {
        String text = raw.strip();
        if (!PERCENT.matcher(text).matches()) {
            return Optional.empty();
        }
        return Optional.of(new BigDecimal(text.replace(',', '.')).setScale(SCALE, RoundingMode.UNNECESSARY));
    }

    @Override
    public String toString() {
        return value.toPlainString();
    }
}
