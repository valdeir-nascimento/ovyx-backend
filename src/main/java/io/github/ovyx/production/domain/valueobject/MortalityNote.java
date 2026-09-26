package io.github.ovyx.production.domain.valueobject;

import io.github.ovyx.production.domain.ProductionErrorCode;
import io.github.ovyx.shared.domain.Notification;
import io.github.ovyx.shared.domain.Rule;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Observacao da mortalidade de uma gaiola: causa provavel, sintomas, destino das aves (FR-011).
 *
 * <p>Opcional. A vazia, ou so de espacos, vira ausente, como a observacao do relatorio.
 *
 * @param value observacao ja aparada, de 1 a 500 caracteres
 */
public record MortalityNote(String value) {

    private static final int MAXIMUM_LENGTH = 500;
    private static final String FIELD = "note";
    private static final List<Rule<String>> RULES = List.of(Rule.of(
            note -> note.codePointCount(0, note.length()) <= MAXIMUM_LENGTH,
            ProductionErrorCode.MORTALITY_NOTE_TOO_LONG,
            "A observação deve ter no máximo 500 caracteres."));

    /** Construtor canonico: so a garantia estrutural, para a reidratacao. */
    public MortalityNote {
        Objects.requireNonNull(value, "value");
    }

    /** Registra no {@link Notification} as violacoes do campo, sem lancar. A ausencia nao e violacao. */
    public static void validate(String raw, Notification notification) {
        if (raw != null && !raw.isBlank()) {
            notification.check(FIELD, raw.strip(), RULES);
        }
    }

    /**
     * A observacao, ou nenhuma quando veio ausente ou em branco.
     *
     * @throws io.github.ovyx.shared.domain.DomainException quando passa de 500 caracteres
     */
    public static Optional<MortalityNote> optionalOf(String raw) {
        Notification notification = new Notification();
        validate(raw, notification);
        notification.throwIfAny(ProductionErrorCode.VALIDATION_FAILED);
        return raw == null || raw.isBlank() ? Optional.empty() : Optional.of(new MortalityNote(raw.strip()));
    }

    @Override
    public String toString() {
        return value;
    }
}
