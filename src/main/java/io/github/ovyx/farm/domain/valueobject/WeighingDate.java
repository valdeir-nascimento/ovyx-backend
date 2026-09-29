package io.github.ovyx.farm.domain.valueobject;

import io.github.ovyx.farm.domain.FarmErrorCode;
import io.github.ovyx.shared.domain.Notification;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Objects;
import java.util.Optional;

/**
 * O dia da pesagem (FR-003 da 005): uma data ISO, nao futura no fuso da granja. Datas passadas valem, para
 * registrar uma pesagem esquecida.
 *
 * @param value o dia
 */
public record WeighingDate(LocalDate value) {

    private static final String FIELD = "weighedOn";

    /** Construtor canonico: so a garantia estrutural, para a reidratacao. */
    public WeighingDate {
        Objects.requireNonNull(value, "value");
    }

    /** Registra no {@link Notification} a violacao do campo, sem lancar: a primeira que se aplica. */
    public static void validate(String raw, LocalDate today, Notification notification) {
        if (!notification.requirePresent(FIELD, raw, FarmErrorCode.WEIGHED_ON_REQUIRED, "Informe a data da pesagem.")) {
            return;
        }
        Optional<LocalDate> day = parse(raw);
        if (day.isEmpty()) {
            notification.add(
                    FIELD, FarmErrorCode.WEIGHED_ON_INVALID, "Informe a data da pesagem no formato AAAA-MM-DD.");
        } else if (day.get().isAfter(today)) {
            notification.add(FIELD, FarmErrorCode.WEIGHED_ON_IN_FUTURE, "A data da pesagem não pode ser futura.");
        }
    }

    /**
     * Cria o dia, recusando na hora com a violacao do campo.
     *
     * @throws io.github.ovyx.shared.domain.DomainException quando a data falta, e invalida ou e futura
     */
    public static WeighingDate of(String raw, LocalDate today) {
        Notification notification = new Notification();
        validate(raw, today, notification);
        notification.throwIfAny(FarmErrorCode.VALIDATION_FAILED);
        return new WeighingDate(parse(raw).orElseThrow());
    }

    private static Optional<LocalDate> parse(String raw) {
        try {
            // ISO estrito: "2026-02-30" e "2026-9-24" nao sao datas.
            return Optional.of(LocalDate.parse(raw.strip(), DateTimeFormatter.ISO_LOCAL_DATE));
        } catch (DateTimeParseException invalid) {
            return Optional.empty();
        }
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
