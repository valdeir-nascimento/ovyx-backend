package io.github.ovyx.identity.infrastructure.persistence;

import io.github.ovyx.identity.domain.model.AccessEvent;
import io.github.ovyx.identity.domain.port.AccessEventRecorder;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Adaptador da trilha de auditoria de acesso (FR-006).
 *
 * <p>Grava na transacao do comando que registra o acesso. A tentativa recusada fica na trilha porque
 * o despachante confirma a transacao tambem quando o {@code Result} e falha — e justamente a
 * tentativa recusada que interessa investigar. So um defeito tecnico, que desfaz o comando inteiro,
 * leva o registro junto.
 *
 * <p>Ja usou transacao propria ({@code REQUIRES_NEW}). Com a transacao por comando, cada entrada
 * passou a segurar duas conexoes, e 10 entradas simultaneas esgotavam o pool: todas esperavam a
 * segunda conexao ate o tempo limite, e a API inteira parava.
 *
 * <p>Somente inclusao: nao ha update nem delete nesta classe.
 */
@Repository
public class JdbcAccessEventRecorder implements AccessEventRecorder {

    private final JdbcClient jdbcClient;

    JdbcAccessEventRecorder(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    @Transactional
    public void record(AccessEvent event) {
        jdbcClient
                .sql(
                        """
                        insert into access_event
                            (id, attempted_identifier, caretaker_id, outcome, origin, occurred_at)
                        values
                            (:id, :attemptedIdentifier, :caretakerId, :outcome, :origin, :occurredAt)
                        """)
                .param("id", event.id())
                .param("attemptedIdentifier", event.attemptedIdentifier())
                .param("caretakerId", event.caretakerId() == null ? null : event.caretakerId().value())
                .param("outcome", event.outcome().name())
                .param("origin", event.origin())
                .param("occurredAt", java.sql.Timestamp.from(event.occurredAt()))
                .update();
    }
}
