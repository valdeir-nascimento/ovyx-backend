package io.github.ovyx.identity.infrastructure.persistence;

import io.github.ovyx.identity.application.caretaker.CaretakerDetail;
import io.github.ovyx.identity.application.caretaker.CaretakerDirectory;
import io.github.ovyx.identity.application.caretaker.CaretakerSummary;
import io.github.ovyx.identity.domain.model.CaretakerId;
import io.github.ovyx.identity.domain.model.CaretakerStatus;
import io.github.ovyx.identity.domain.model.Role;
import io.github.ovyx.shared.application.PageResponse;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Adaptador da porta de leitura da administracao de responsaveis.
 *
 * <p>Monta os modelos de leitura direto da consulta, sem passar por agregado nem pelo ORM
 * (principio V). As consultas nao selecionam {@code password_hash}: nao ha como o hash chegar a
 * borda HTTP por este caminho.
 */
@Repository
public class JdbcCaretakerDirectory implements CaretakerDirectory {

    private static final String SUMMARY_COLUMNS = "id, full_name, email, mobile_phone, role, status";

    private final JdbcClient jdbcClient;

    JdbcCaretakerDirectory(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public PageResponse<CaretakerSummary> search(String nameFragment, CaretakerStatus status, int page, int size) {
        // So entram na consulta os filtros informados. Um parametro nulo em "(:x is null or ...)"
        // chega ao PostgreSQL sem tipo, e ele recusa a consulta.
        List<String> conditions = new ArrayList<>();
        Map<String, Object> parameters = new LinkedHashMap<>();
        if (nameFragment != null) {
            conditions.add("lower(full_name) like lower(:pattern) escape '\\'");
            parameters.put("pattern", "%" + literal(nameFragment) + "%");
        }
        if (status != null) {
            conditions.add("status = :status");
            parameters.put("status", status.name());
        }
        String where = conditions.isEmpty() ? "" : " where " + String.join(" and ", conditions);

        long total = jdbcClient
                .sql("select count(*) from caretaker" + where)
                .params(parameters)
                .query(Long.class)
                .single();

        List<CaretakerSummary> content = jdbcClient
                .sql("select " + SUMMARY_COLUMNS + " from caretaker" + where
                        // O id desempata nomes iguais: sem ele, a mesma pessoa podia aparecer em duas
                        // paginas, ou em nenhuma.
                        + " order by lower(full_name), id limit :size offset :offset")
                .params(parameters)
                .param("size", size)
                .param("offset", (long) page * size)
                .query((rs, rowNumber) -> summaryOf(rs))
                .list();

        return PageResponse.of(content, page, size, total);
    }

    @Override
    public Optional<CaretakerDetail> findDetail(CaretakerId id) {
        return jdbcClient
                .sql(
                        """
                        select id, full_name, cpf, email, mobile_phone, role, status, created_at, updated_at
                        from caretaker
                        where id = :id
                        """)
                .param("id", id.value())
                .query((rs, rowNumber) -> new CaretakerDetail(
                        CaretakerId.of(rs.getObject("id", UUID.class)),
                        rs.getString("full_name"),
                        rs.getString("cpf"),
                        rs.getString("email"),
                        rs.getString("mobile_phone"),
                        Role.valueOf(rs.getString("role")),
                        CaretakerStatus.valueOf(rs.getString("status")),
                        rs.getObject("created_at", OffsetDateTime.class).toInstant(),
                        rs.getObject("updated_at", OffsetDateTime.class).toInstant()))
                .optional();
    }

    private static CaretakerSummary summaryOf(ResultSet rs) throws SQLException {
        return new CaretakerSummary(
                CaretakerId.of(rs.getObject("id", UUID.class)),
                rs.getString("full_name"),
                rs.getString("email"),
                rs.getString("mobile_phone"),
                Role.valueOf(rs.getString("role")),
                CaretakerStatus.valueOf(rs.getString("status")));
    }

    /** O trecho como texto literal: {@code %} e {@code _} digitados sao procurados, e nao curingas. */
    private static String literal(String fragment) {
        return fragment.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
