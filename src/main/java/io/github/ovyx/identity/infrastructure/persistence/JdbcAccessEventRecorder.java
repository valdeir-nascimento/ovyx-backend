package io.github.ovyx.identity.infrastructure.persistence;

import io.github.ovyx.identity.domain.model.AccessEvent;
import io.github.ovyx.identity.domain.port.AccessEventRecorder;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Adaptador da trilha de auditoria de acesso (FR-006).
 *
 * <p>Usa {@code REQUIRES_NEW}: o registro de auditoria precisa sobreviver mesmo que a transacao que
 * o disparou seja desfeita. Uma tentativa de acesso recusada nao deve desaparecer da trilha porque
 * a operacao que a produziu falhou — e justamente a tentativa recusada que interessa investigar.
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
    @Transactional(propagation = Propagation.REQUIRES_NEW)
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
