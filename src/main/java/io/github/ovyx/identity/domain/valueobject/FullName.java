package io.github.ovyx.identity.domain.valueobject;

import io.github.ovyx.identity.domain.Notification;

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
        Notification notification = new Notification();
        FullName fullName = of(raw, notification);
        notification.throwIfAny();
        return fullName;
    }

    /**
     * Valida escrevendo no {@link Notification} de quem chamou, em vez de lancar.
     *
     * <p>E o caminho do agregado, que precisa reunir as violacoes de todos os campos antes de
     * recusar uma vez so (FR-017).
     *
     * @return o nome, ou {@code null} quando alguma regra do campo foi violada
     */
    public static FullName of(String raw, Notification notification) {
        if (raw == null || raw.isBlank()) {
            notification.add(FIELD, "Informe o nome completo.");
            return null;
        }

        String trimmed = raw.trim();
        boolean rejected = false;

        if (trimmed.length() < MINIMUM_LENGTH) {
            notification.add(FIELD, "O nome deve ter ao menos 3 caracteres.");
            rejected = true;
        } else if (trimmed.length() > MAXIMUM_LENGTH) {
            notification.add(FIELD, "O nome deve ter no máximo 120 caracteres.");
            rejected = true;
        }
        if (trimmed.chars().noneMatch(Character::isLetter)) {
            notification.add(FIELD, "O nome deve conter ao menos uma letra.");
            rejected = true;
        }

        return rejected ? null : new FullName(trimmed);
    }

    @Override
    public String toString() {
        return value;
    }
}
