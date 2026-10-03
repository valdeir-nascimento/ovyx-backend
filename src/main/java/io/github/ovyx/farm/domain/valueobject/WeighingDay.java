package io.github.ovyx.farm.domain.valueobject;

import io.github.ovyx.farm.domain.FarmErrorCode;
import io.github.ovyx.shared.domain.Notification;
import java.time.DayOfWeek;
import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * Dia da pesagem do setor: o dia da semana em que as aves dele sao pesadas (FR-001 e R-003 da 010).
 *
 * <p>Opcional. Ausente, vazio ou so de espacos, o setor fica sem dia fixo e segue o prazo de 7 dias desde a
 * ultima pesagem de cada gaiola. Chega como texto cru, o nome do dia em ingles ({@code MONDAY} a
 * {@code SUNDAY}), sem distinguir maiusculas: o valor errado e recusado no campo, junto das demais violacoes do
 * setor, e nao como erro de leitura do corpo (FR-003).
 *
 * @param value o dia da semana
 */
public record WeighingDay(DayOfWeek value) {

    private static final String FIELD = "weighingDay";

    /** Construtor canonico: so a garantia estrutural, para a reidratacao. */
    public WeighingDay {
        Objects.requireNonNull(value, "value");
    }

    /** Registra no {@link Notification} a violacao do campo, sem lancar. A ausencia nao e violacao. */
    public static void validate(String raw, Notification notification) {
        if (raw != null && !raw.isBlank() && parse(raw).isEmpty()) {
            notification.add(
                    FIELD, FarmErrorCode.WEIGHING_DAY_INVALID, "Escolha um dia da semana, de segunda a domingo.");
        }
    }

    /**
     * O dia, ou nenhum quando veio ausente ou em branco.
     *
     * @throws io.github.ovyx.shared.domain.DomainException quando o texto nao e um dia da semana
     */
    public static Optional<WeighingDay> optionalOf(String raw) {
        Notification notification = new Notification();
        validate(raw, notification);
        notification.throwIfAny(FarmErrorCode.VALIDATION_FAILED);
        return raw == null || raw.isBlank() ? Optional.empty() : parse(raw).map(WeighingDay::new);
    }

    /** O dia pelo nome, sem distinguir maiusculas; o numero ou a abreviacao nao valem. */
    private static Optional<DayOfWeek> parse(String raw) {
        String name = raw.strip().toUpperCase(Locale.ROOT);
        return Arrays.stream(DayOfWeek.values()).filter(day -> day.name().equals(name)).findFirst();
    }

    @Override
    public String toString() {
        return value.name();
    }
}
