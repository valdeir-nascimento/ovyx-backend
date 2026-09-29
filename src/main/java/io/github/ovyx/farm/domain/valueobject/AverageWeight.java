package io.github.ovyx.farm.domain.valueobject;

import io.github.ovyx.farm.domain.FarmErrorCode;
import io.github.ovyx.shared.domain.Notification;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * O peso medio da amostra de aves de uma gaiola, em gramas, de 1 a 10.000, com ate uma casa decimal
 * (FR-003 e R-007 da 005).
 *
 * <p>Chega como foi digitado, com virgula ou ponto: a media de uma amostra raramente e inteira, e o que nao
 * e peso e recusado no proprio campo, junto das demais falhas.
 *
 * @param value o peso, com uma casa decimal
 */
public record AverageWeight(BigDecimal value) {

    private static final String FIELD = "averageWeight";
    private static final BigDecimal MINIMUM = BigDecimal.ONE;
    private static final BigDecimal MAXIMUM = new BigDecimal("10000");
    private static final int SCALE = 1;

    /** Inteiro, ou com uma casa depois da virgula ou do ponto; com sinal, para cair na faixa. */
    private static final Pattern GRAMS = Pattern.compile("[+-]?\\d+([.,]\\d)?");

    /** Construtor canonico: so a garantia estrutural, para a reidratacao. */
    public AverageWeight {
        Objects.requireNonNull(value, "value");
    }

    /**
     * Registra no {@link Notification} a violacao do campo, sem lancar: ausente, fora do formato ou fora da
     * faixa, uma so, a primeira que se aplica.
     */
    public static void validate(String raw, Notification notification) {
        if (!notification.requirePresent(FIELD, raw, FarmErrorCode.WEIGHT_REQUIRED, "Informe o peso médio em gramas.")) {
            return;
        }
        Optional<BigDecimal> grams = parse(raw);
        if (grams.isEmpty()) {
            notification.add(
                    FIELD, FarmErrorCode.WEIGHT_INVALID, "Informe o peso em gramas, com até uma casa decimal.");
        } else if (grams.get().compareTo(MINIMUM) < 0 || grams.get().compareTo(MAXIMUM) > 0) {
            notification.add(
                    FIELD, FarmErrorCode.WEIGHT_OUT_OF_RANGE, "O peso médio deve ficar entre 1 e 10.000 gramas.");
        }
    }

    /**
     * Cria o peso, recusando na hora com a violacao do campo.
     *
     * @throws io.github.ovyx.shared.domain.DomainException quando o peso falta, esta fora do formato ou sai
     *     da faixa
     */
    public static AverageWeight of(String raw) {
        Notification notification = new Notification();
        validate(raw, notification);
        notification.throwIfAny(FarmErrorCode.VALIDATION_FAILED);
        return new AverageWeight(parse(raw).orElseThrow());
    }

    /** O peso lido do texto, com uma casa, ou nenhum quando o texto nao e um peso em gramas. */
    private static Optional<BigDecimal> parse(String raw) {
        String text = raw.strip();
        if (!GRAMS.matcher(text).matches()) {
            return Optional.empty();
        }
        return Optional.of(new BigDecimal(text.replace(',', '.')).setScale(SCALE, RoundingMode.UNNECESSARY));
    }

    @Override
    public String toString() {
        return value.toPlainString();
    }
}
