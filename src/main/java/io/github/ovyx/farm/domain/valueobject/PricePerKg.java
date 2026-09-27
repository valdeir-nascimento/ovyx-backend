package io.github.ovyx.farm.domain.valueobject;

import io.github.ovyx.farm.domain.FarmErrorCode;
import io.github.ovyx.shared.domain.Notification;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Preco por quilo da formula, em reais: de 0,01 a 1.000,00, com ate duas casas (R-011 da 004).
 *
 * <p>Chega como texto cru, como as quantidades: "2,85" e "2.85" sao o mesmo preco, e o numero do JSON
 * chega como o texto dele. O separador de milhar nao e aceito ("1.000,00"): com ele, "1.000" seria mil
 * reais ou um real, e a pessoa corrige o campo em vez de o sistema adivinhar.
 *
 * @param value preco com exatamente duas casas decimais
 */
public record PricePerKg(BigDecimal value) {

    private static final String FIELD = "pricePerKg";
    private static final BigDecimal MINIMUM = new BigDecimal("0.01");
    private static final BigDecimal MAXIMUM = new BigDecimal("1000.00");
    private static final int SCALE = 2;

    /** Inteiro, ou com uma ou duas casas depois da virgula ou do ponto; com sinal, para cair na faixa. */
    private static final Pattern REAIS = Pattern.compile("[+-]?\\d+([.,]\\d{1,2})?");

    /** Construtor canonico: so a garantia estrutural, para a reidratacao. */
    public PricePerKg {
        Objects.requireNonNull(value, "value");
    }

    /**
     * Registra no {@link Notification} a violacao do campo, sem lancar: ausente, fora do formato ou fora
     * da faixa, uma so, a primeira que se aplica.
     */
    public static void validate(String raw, Notification notification) {
        if (!notification.requirePresent(FIELD, raw, FarmErrorCode.PRICE_REQUIRED, "Informe o preço por quilo.")) {
            return;
        }
        Optional<BigDecimal> price = parse(raw);
        if (price.isEmpty()) {
            notification.add(
                    FIELD, FarmErrorCode.PRICE_INVALID, "Informe o preço em reais, com até duas casas decimais.");
        } else if (price.get().compareTo(MINIMUM) < 0 || price.get().compareTo(MAXIMUM) > 0) {
            notification.add(
                    FIELD,
                    FarmErrorCode.PRICE_OUT_OF_RANGE,
                    "O preço deve ficar entre R$ 0,01 e R$ 1.000,00 o quilo.");
        }
    }

    /**
     * Cria o preco, recusando na hora com a violacao do campo.
     *
     * @throws io.github.ovyx.shared.domain.DomainException quando o preco falta, esta fora do formato ou
     *     sai da faixa
     */
    public static PricePerKg of(String raw) {
        Notification notification = new Notification();
        validate(raw, notification);
        notification.throwIfAny(FarmErrorCode.VALIDATION_FAILED);
        return new PricePerKg(parse(raw).orElseThrow());
    }

    /** O preco lido do texto, com duas casas, ou nenhum quando o texto nao e um valor em reais. */
    private static Optional<BigDecimal> parse(String raw) {
        String text = raw.strip();
        if (!REAIS.matcher(text).matches()) {
            return Optional.empty();
        }
        return Optional.of(new BigDecimal(text.replace(',', '.')).setScale(SCALE, RoundingMode.UNNECESSARY));
    }

    @Override
    public String toString() {
        return value.toPlainString();
    }
}
