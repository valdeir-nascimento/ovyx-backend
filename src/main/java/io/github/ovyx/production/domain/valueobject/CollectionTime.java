package io.github.ovyx.production.domain.valueobject;

import io.github.ovyx.production.domain.ProductionErrorCode;
import io.github.ovyx.shared.domain.Notification;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Hora da coleta, em horas e minutos (FR-001, R-006).
 *
 * @param value a hora, sem segundos
 */
public record CollectionTime(LocalTime value) {

    private static final String FIELD = "collectionTime";
    private static final Pattern HOURS_AND_MINUTES = Pattern.compile("\\d{2}:\\d{2}");

    /** Construtor canonico: so a garantia estrutural, para a reidratacao. */
    public CollectionTime {
        Objects.requireNonNull(value, "value");
    }

    /** Registra no {@link Notification} a violacao do campo, sem lancar. */
    public static void validate(String raw, Notification notification) {
        if (!notification.requirePresent(
                FIELD, raw, ProductionErrorCode.COLLECTION_TIME_REQUIRED, "Informe a hora da coleta.")) {
            return;
        }
        if (parse(raw).isEmpty()) {
            notification.add(
                    FIELD, ProductionErrorCode.COLLECTION_TIME_INVALID, "Informe a hora da coleta no formato HH:mm.");
        }
    }

    /**
     * Cria a hora, recusando na hora com a violacao do campo.
     *
     * @throws io.github.ovyx.shared.domain.DomainException quando a hora falta ou nao e {@code HH:mm}
     */
    public static CollectionTime of(String raw) {
        Notification notification = new Notification();
        validate(raw, notification);
        notification.throwIfAny(ProductionErrorCode.VALIDATION_FAILED);
        return new CollectionTime(parse(raw).orElseThrow());
    }

    private static Optional<LocalTime> parse(String raw) {
        String text = raw.strip();
        if (!HOURS_AND_MINUTES.matcher(text).matches()) {
            return Optional.empty();
        }
        try {
            return Optional.of(LocalTime.parse(text));
        } catch (DateTimeParseException invalid) {
            return Optional.empty();
        }
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
