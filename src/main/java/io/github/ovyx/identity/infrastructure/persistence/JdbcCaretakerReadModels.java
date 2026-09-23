package io.github.ovyx.identity.infrastructure.persistence;

import io.github.ovyx.identity.application.authentication.AuthenticatedCaretaker;
import io.github.ovyx.identity.application.authentication.CaretakerReadModels;
import io.github.ovyx.identity.domain.model.CaretakerId;
import io.github.ovyx.identity.domain.model.Role;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Adaptador da porta de leitura do contexto Identity.
 *
 * <p>Monta o modelo de leitura direto da consulta, sem passar por agregado nem pelo ORM
 * (principio V). A consulta seleciona apenas as colunas que a tela usa — em particular, nao lista
 * {@code password_hash}, entao nao ha como o hash chegar a borda HTTP por este caminho.
 */
@Repository
public class JdbcCaretakerReadModels implements CaretakerReadModels {

    private final JdbcClient jdbcClient;

    JdbcCaretakerReadModels(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public Optional<AuthenticatedCaretaker> findAuthenticatedById(CaretakerId id) {
        return jdbcClient
                .sql(
                        """
                        select id, full_name, role, must_change_password
                        from caretaker
                        where id = :id and status = 'ACTIVE'
                        """)
                .param("id", id.value())
                .query((rs, rowNumber) -> new AuthenticatedCaretaker(
                        CaretakerId.of(rs.getObject("id", java.util.UUID.class)),
                        rs.getString("full_name"),
                        Role.valueOf(rs.getString("role")),
                        rs.getBoolean("must_change_password")))
                .optional();
    }
}
