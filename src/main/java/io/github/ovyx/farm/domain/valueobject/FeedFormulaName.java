package io.github.ovyx.farm.domain.valueobject;

import io.github.ovyx.farm.domain.FarmErrorCode;
import io.github.ovyx.shared.domain.Notification;
import io.github.ovyx.shared.domain.Rule;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Nome da formula de racao (FR-001 e R-011 da 004).
 *
 * <p>O tamanho e contado em caracteres, e nao em unidades UTF-16, como o {@code varchar(80)} do banco.
 *
 * @param value nome ja aparado, de 2 a 80 caracteres
 */
public record FeedFormulaName(String value) {

    private static final int MINIMUM_LENGTH = 2;
    private static final int MAXIMUM_LENGTH = 80;
    private static final String FIELD = "name";

    private static final List<Rule<String>> RULES = List.of(
            Rule.of(
                    name -> charactersOf(name) >= MINIMUM_LENGTH,
                    FarmErrorCode.FEED_FORMULA_NAME_TOO_SHORT,
                    "O nome da fórmula deve ter ao menos 2 caracteres."),
            Rule.of(
                    name -> charactersOf(name) <= MAXIMUM_LENGTH,
                    FarmErrorCode.FEED_FORMULA_NAME_TOO_LONG,
                    "O nome da fórmula deve ter no máximo 80 caracteres."));

    /** Construtor canonico: so a garantia estrutural, para a reidratacao. */
    public FeedFormulaName {
        Objects.requireNonNull(value, "value");
    }

    /** Registra no {@link Notification} as violacoes do campo, sem lancar. */
    public static void validate(String raw, Notification notification) {
        if (notification.requirePresent(
                FIELD, raw, FarmErrorCode.FEED_FORMULA_NAME_REQUIRED, "Informe o nome da fórmula.")) {
            notification.check(FIELD, raw.strip(), RULES);
        }
    }

    /**
     * Cria o nome, recusando na hora com as violacoes do campo.
     *
     * @throws io.github.ovyx.shared.domain.DomainException quando alguma regra do campo e violada
     */
    public static FeedFormulaName of(String raw) {
        Notification notification = new Notification();
        validate(raw, notification);
        notification.throwIfAny(FarmErrorCode.VALIDATION_FAILED);
        return new FeedFormulaName(raw.strip());
    }

    /** Se os dois nomes sao o mesmo para a regra de unicidade: sem distinguir maiusculas. */
    public boolean sameAs(FeedFormulaName other) {
        return comparisonKey().equals(other.comparisonKey());
    }

    /** A forma do nome que a unicidade compara, a mesma do indice {@code lower(name)} do banco. */
    public String comparisonKey() {
        return value.toLowerCase(Locale.ROOT);
    }

    private static int charactersOf(String text) {
        return text.codePointCount(0, text.length());
    }

    @Override
    public String toString() {
        return value;
    }
}
