package io.github.ovyx.identity.domain.model;

import io.github.ovyx.identity.domain.IdentityErrorCode;
import io.github.ovyx.shared.domain.Notification;
import java.util.Arrays;

/**
 * Perfil do responsavel (FR-007).
 *
 * <p>Dois valores, herdados do legado, onde eram gravados como os caracteres {@code A} e {@code U}
 * na coluna {@code perfil}. O sistema novo grava o nome por extenso: um caractere solto em banco
 * obriga quem le a consultar documentacao para saber o que significa.
 */
public enum Role {

    /** Acesso total, incluindo a gestao de responsaveis. */
    ADMINISTRATOR,

    /** Acesso as demais areas do sistema. */
    USER;

    private static final String FIELD = "role";

    /**
     * Registra no {@link Notification} o perfil ausente ou fora da lista, sem lancar.
     *
     * <p>O perfil chega como texto, e nao como enum, de proposito: convertido na borda, um valor
     * desconhecido tornava o corpo inteiro ilegivel e escondia as demais violacoes (FR-017). So o
     * nome exato vale, como o contrato publica.
     */
    public static void validate(String raw, Notification notification) {
        if (notification.requirePresent(FIELD, raw, IdentityErrorCode.ROLE_REQUIRED, "Informe o perfil.")
                && Arrays.stream(values()).noneMatch(role -> role.name().equals(raw))) {
            notification.add(FIELD, IdentityErrorCode.ROLE_INVALID, "Perfil inválido.");
        }
    }

    /**
     * O perfil com este nome, recusando na hora o que {@link #validate} recusaria.
     *
     * @throws io.github.ovyx.shared.domain.DomainException quando o perfil e ausente ou fora da lista
     */
    public static Role of(String raw) {
        Notification notification = new Notification();
        validate(raw, notification);
        notification.throwIfAny(IdentityErrorCode.VALIDATION_FAILED);
        return valueOf(raw);
    }
}
