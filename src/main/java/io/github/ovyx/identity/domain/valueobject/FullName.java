package io.github.ovyx.identity.domain.valueobject;

import io.github.ovyx.identity.domain.IdentityErrorCode;
import io.github.ovyx.shared.domain.Notification;
import io.github.ovyx.shared.domain.Rule;
import java.util.List;

/**
 * Nome completo do responsavel.
 *
 * <p>Avalia <strong>todas</strong> as regras do campo antes de recusar: um nome curto demais e sem
 * letra recusa com as duas violacoes, e nao so com a primeira.
 *
 * @param value nome ja aparado, entre 3 e 120 caracteres
 */
public record FullName(String value) {

    private static final int MINIMUM_LENGTH = 3;
    private static final int MAXIMUM_LENGTH = 120;
    private static final String FIELD = "fullName";

    private static final List<Rule<String>> RULES = List.of(
            Rule.of(
                    name -> name.length() >= MINIMUM_LENGTH,
                    IdentityErrorCode.FULL_NAME_TOO_SHORT,
                    "O nome deve ter ao menos 3 caracteres."),
            Rule.of(
                    name -> name.length() <= MAXIMUM_LENGTH,
                    IdentityErrorCode.FULL_NAME_TOO_LONG,
                    "O nome deve ter no máximo 120 caracteres."),
            Rule.of(
                    name -> name.chars().anyMatch(Character::isLetter),
                    IdentityErrorCode.FULL_NAME_WITHOUT_LETTER,
                    "O nome deve conter ao menos uma letra."));

    /**
     * Registra no {@link Notification} as violacoes do campo, sem lancar.
     *
     * <p>E o caminho do agregado, que reune as violacoes de todos os campos antes de recusar (FR-017).
     */
    public static void validate(String raw, Notification notification) {
        if (notification.requirePresent(
                FIELD, raw, IdentityErrorCode.FULL_NAME_REQUIRED, "Informe o nome completo.")) {
            notification.check(FIELD, raw.trim(), RULES);
        }
    }

    /**
     * Cria o nome, recusando na hora com as violacoes do campo.
     *
     * @param raw texto como digitado
     * @throws io.github.ovyx.shared.domain.DomainException quando alguma regra do campo e violada
     */
    public static FullName of(String raw) {
        Notification notification = new Notification();
        validate(raw, notification);
        notification.throwIfAny(IdentityErrorCode.VALIDATION_FAILED);
        return new FullName(raw.trim());
    }

    @Override
    public String toString() {
        return value;
    }
}
