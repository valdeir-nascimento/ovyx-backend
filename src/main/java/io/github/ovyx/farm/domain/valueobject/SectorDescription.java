package io.github.ovyx.farm.domain.valueobject;

import io.github.ovyx.farm.domain.FarmErrorCode;
import io.github.ovyx.shared.domain.Notification;
import io.github.ovyx.shared.domain.Rule;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Descricao do setor: especie, linhagem, galpao ou o que ajude a reconhece-lo (FR-001).
 *
 * <p>Opcional. A descricao vazia, ou so de espacos, vira ausente: guardar um texto em branco
 * obrigaria toda tela a distinguir "sem descricao" de "descricao vazia".
 *
 * @param value descricao ja aparada, de 1 a 500 caracteres
 */
public record SectorDescription(String value) {

    private static final int MAXIMUM_LENGTH = 500;
    private static final String FIELD = "description";

    private static final List<Rule<String>> RULES = List.of(Rule.of(
            description -> description.codePointCount(0, description.length()) <= MAXIMUM_LENGTH,
            FarmErrorCode.SECTOR_DESCRIPTION_TOO_LONG,
            "A descrição deve ter no máximo 500 caracteres."));

    /** Construtor canonico: so a garantia estrutural, para a reidratacao. */
    public SectorDescription {
        Objects.requireNonNull(value, "value");
    }

    /** Registra no {@link Notification} as violacoes do campo, sem lancar. A ausencia nao e violacao. */
    public static void validate(String raw, Notification notification) {
        if (raw != null && !raw.isBlank()) {
            notification.check(FIELD, raw.strip(), RULES);
        }
    }

    /**
     * A descricao, ou nenhuma quando veio ausente ou em branco.
     *
     * @throws io.github.ovyx.shared.domain.DomainException quando passa de 500 caracteres
     */
    public static Optional<SectorDescription> optionalOf(String raw) {
        Notification notification = new Notification();
        validate(raw, notification);
        notification.throwIfAny(FarmErrorCode.VALIDATION_FAILED);
        return raw == null || raw.isBlank() ? Optional.empty() : Optional.of(new SectorDescription(raw.strip()));
    }

    @Override
    public String toString() {
        return value;
    }
}
