package io.github.ovyx.identity.domain.valueobject;

import io.github.ovyx.identity.domain.Violations;

/**
 * Nome completo do responsavel.
 *
 * <p>A fabrica avalia <strong>todas</strong> as regras do campo antes de recusar: um nome curto
 * demais e sem letra recusa com as duas mensagens, e nao so com a primeira.
 *
 * @param value nome ja aparado, entre 3 e 120 caracteres
 */
public record FullName(String value) {

    private static final int MINIMUM_LENGTH = 3;
    private static final int MAXIMUM_LENGTH = 120;
    private static final String FIELD = "fullName";

    /**
     * Cria o nome.
     *
     * @param raw texto como digitado
     * @throws io.github.ovyx.shared.domain.DomainException quando alguma regra do campo e violada
     */
    public static FullName of(String raw) {
        if (raw == null || raw.isBlank()) {
            throw Violations.of(FIELD, "Informe o nome completo.");
        }

        String trimmed = raw.trim();
        Violations violations = new Violations();

        if (trimmed.length() < MINIMUM_LENGTH) {
            violations.add(FIELD, "O nome deve ter ao menos 3 caracteres.");
        } else if (trimmed.length() > MAXIMUM_LENGTH) {
            violations.add(FIELD, "O nome deve ter no máximo 120 caracteres.");
        }
        if (trimmed.chars().noneMatch(Character::isLetter)) {
            violations.add(FIELD, "O nome deve conter ao menos uma letra.");
        }
        violations.throwIfAny();

        return new FullName(trimmed);
    }

    @Override
    public String toString() {
        return value;
    }
}
