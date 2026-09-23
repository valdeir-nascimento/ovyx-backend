package io.github.ovyx.identity.domain.model;

/**
 * Situacao do responsavel.
 *
 * <p>Nao existe no legado, que excluia fisicamente. A inativacao substitui a exclusao porque
 * registros de outras areas referenciam o responsavel, e o historico precisa permanecer integro
 * (FR-018).
 */
public enum CaretakerStatus {
    ACTIVE,
    INACTIVE
}
