package io.github.ovyx.farm.domain.valueobject;

import io.github.ovyx.farm.domain.FarmErrorCode;
import io.github.ovyx.shared.domain.Notification;
import io.github.ovyx.shared.domain.Rule;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Bateria da gaiola: o agrupamento de gaiolas dentro do setor, identificado por uma letra ou um codigo
 * curto (FR-006; spec, Key Entities).
 *
 * <p>Gravada em maiusculas: "b" e "B" sao a mesma bateria, e o codigo da gaiola sai "B-07" (spec,
 * casos de borda). A mesma mensagem para a bateria ausente e para a fora do formato, porque ela diz o
 * que a pessoa precisa digitar nos dois casos.
 *
 * @param value bateria ja aparada e em maiusculas, de 1 a 3 letras ou digitos
 */
public record Battery(String value) {

    private static final String FIELD = "battery";
    private static final String MESSAGE = "Informe a bateria, com até 3 letras ou dígitos.";

    private static final List<Rule<String>> RULES = List.of(
            Rule.of(battery -> battery.matches("[A-Z0-9]{1,3}"), FarmErrorCode.CAGE_BATTERY_INVALID, MESSAGE));

    /** Construtor canonico: so a garantia estrutural, para a reidratacao. */
    public Battery {
        Objects.requireNonNull(value, "value");
    }

    /** Registra no {@link Notification} as violacoes do campo, sem lancar. */
    public static void validate(String raw, Notification notification) {
        if (notification.requirePresent(FIELD, raw, FarmErrorCode.CAGE_BATTERY_REQUIRED, MESSAGE)) {
            notification.check(FIELD, normalized(raw), RULES);
        }
    }

    /**
     * Cria a bateria, recusando na hora com as violacoes do campo.
     *
     * @throws io.github.ovyx.shared.domain.DomainException quando a bateria falta ou sai do formato
     */
    public static Battery of(String raw) {
        Notification notification = new Notification();
        validate(raw, notification);
        notification.throwIfAny(FarmErrorCode.VALIDATION_FAILED);
        return new Battery(normalized(raw));
    }

    private static String normalized(String raw) {
        return raw.strip().toUpperCase(Locale.ROOT);
    }

    @Override
    public String toString() {
        return value;
    }
}
