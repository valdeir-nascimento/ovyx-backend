package io.github.ovyx.identity.domain.model;

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
    USER
}
