package io.github.ovyx.production.domain.valueobject;

import io.github.ovyx.production.domain.ProductionErrorCode;
import io.github.ovyx.shared.domain.Notification;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Objects;
import java.util.Optional;

/**
 * Data da coleta: o dia da granja a que o relatorio se refere (FR-001, R-006).
 *
 * <p>Nao pode ser posterior a hoje no fuso da granja. "Hoje" chega de fora, calculado pelo tratador a
 * partir do relogio e do fuso: o dominio nao le relogio.
 *
 * @param value a data, no formato ISO ({@code AAAA-MM-DD}) na entrada
 */
public record CollectionDate(LocalDate value) {

    private static final String FIELD = "collectionDate";

    /** Construtor canonico: so a garantia estrutural, para a reidratacao. */
    public CollectionDate {
        Objects.requireNonNull(value, "value");
    }

    /** Registra no {@link Notification} a violacao do campo, sem lancar: a primeira que se aplica. */
    public static void validate(String raw, LocalDate today, Notification notification) {
        if (!notification.requirePresent(
                FIELD, raw, ProductionErrorCode.COLLECTION_DATE_REQUIRED, "Informe a data da coleta.")) {
            return;
        }
        Optional<LocalDate> date = parse(raw);
        if (date.isEmpty()) {
            notification.add(
                    FIELD,
                    ProductionErrorCode.COLLECTION_DATE_INVALID,
                    "Informe a data da coleta no formato AAAA-MM-DD.");
        } else if (date.get().isAfter(today)) {
            notification.add(
                    FIELD, ProductionErrorCode.COLLECTION_DATE_IN_FUTURE, "A data da coleta não pode ser futura.");
        }
    }

    /**
     * Cria a data, recusando na hora com a violacao do campo.
     *
     * @throws io.github.ovyx.shared.domain.DomainException quando a data falta, e invalida ou e futura
     */
    public static CollectionDate of(String raw, LocalDate today) {
        Notification notification = new Notification();
        validate(raw, today, notification);
        notification.throwIfAny(ProductionErrorCode.VALIDATION_FAILED);
        return new CollectionDate(parse(raw).orElseThrow());
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
