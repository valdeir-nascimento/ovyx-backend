package io.github.ovyx.identity.domain.model;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

/**
 * Registro auditavel de uma tentativa de acesso ou de um encerramento de sessao (FR-006).
 *
 * <p>Somente inclusao: nunca alterado, nunca removido.
 *
 * <p><strong>A senha nao tem por onde entrar aqui.</strong> Nenhuma fabrica a recebe, e nenhum
 * campo a comporta — a invariante de FR-021 e estrutural, e nao uma verificacao que alguem possa
 * esquecer de fazer.
 *
 * @param id                  identidade do evento
 * @param attemptedIdentifier e-mail ou celular informado, como digitado
 * @param caretakerId         responsavel correspondente, ou {@code null} quando o identificador nao
 *                            corresponde a ninguem
 * @param outcome             causa real, visivel apenas na auditoria
 * @param origin              endereco de origem da requisicao
 * @param occurredAt          instante do evento
 */
public record AccessEvent(
    UUID id,
    String attemptedIdentifier,
    CaretakerId caretakerId,
    AccessOutcome outcome,
    String origin,
    Instant occurredAt
) {

    public AccessEvent {
        if (attemptedIdentifier == null || attemptedIdentifier.isBlank()) {
            throw new IllegalArgumentException("identificador tentado é obrigatório no evento de acesso");
        }
        if (origin == null || origin.isBlank()) {
            throw new IllegalArgumentException("origem é obrigatória no evento de acesso");
        }
        if (outcome == null || occurredAt == null) {
            throw new IllegalArgumentException("resultado e instante são obrigatórios no evento de acesso");
        }
    }

    public static AccessEvent granted(String identifier, CaretakerId caretakerId, String origin, Clock clock) {
        return create(identifier, caretakerId, AccessOutcome.GRANTED, origin, clock);
    }

    /**
     * Credencial recusada.
     *
     * @param caretakerId responsavel a quem o identificador corresponde, ou {@code null} quando nao
     *                    corresponde a ninguem. Preenche-lo quando a conta existe e o que permite investigar ataques
     *                    dirigidos a uma conta especifica.
     */
    public static AccessEvent invalidCredentials(
        String identifier, CaretakerId caretakerId, String origin, Clock clock) {
        return create(identifier, caretakerId, AccessOutcome.INVALID_CREDENTIALS, origin, clock);
    }

    public static AccessEvent inactiveCaretaker(
        String identifier, CaretakerId caretakerId, String origin, Clock clock) {
        return create(identifier, caretakerId, AccessOutcome.INACTIVE_CARETAKER, origin, clock);
    }

    /**
     * Tentativa recusada pela contencao. {@code caretakerId} segue a mesma regra de {@link #invalidCredentials}.
     */
    public static AccessEvent throttled(String identifier, CaretakerId caretakerId, String origin, Clock clock) {
        return create(identifier, caretakerId, AccessOutcome.THROTTLED, origin, clock);
    }

    public static AccessEvent signedOut(String identifier, CaretakerId caretakerId, String origin, Clock clock) {
        return create(identifier, caretakerId, AccessOutcome.SIGNED_OUT, origin, clock);
    }

    private static AccessEvent create(
        String identifier, CaretakerId caretakerId, AccessOutcome outcome, String origin, Clock clock) {
        return new AccessEvent(UUID.randomUUID(), identifier, caretakerId, outcome, origin, clock.instant());
    }
}
