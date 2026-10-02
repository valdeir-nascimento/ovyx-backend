package io.github.ovyx.farm.infrastructure.persistence;

import io.github.ovyx.farm.application.cage.SectorSchedule;
import io.github.ovyx.farm.application.cage.CageFilter;
import io.github.ovyx.farm.application.cage.CageDetail;
import io.github.ovyx.farm.application.cage.CageDirectory;
import io.github.ovyx.farm.application.cage.CageLastWeighing;
import io.github.ovyx.farm.application.cage.CageSummary;
import io.github.ovyx.farm.application.common.StatusFilter;
import io.github.ovyx.farm.domain.model.Cage;
import io.github.ovyx.farm.domain.model.CageId;
import io.github.ovyx.farm.domain.model.SectorId;
import io.github.ovyx.farm.domain.model.Status;
import io.github.ovyx.shared.application.PageResponse;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
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
 * Adaptador da porta de leitura das gaiolas (R-006).
 *
 * <p>A busca compara com o codigo como a tela o mostra — bateria, hifen e numero com pelo menos dois
 * digitos —, por trecho e sem distinguir maiusculas: "b-0" acha B-01 a B-09, e "07" acha A-07 e B-07.
 * O numero e completado com um zero so abaixo de 10: o {@code lpad} do PostgreSQL corta o texto maior
 * que o tamanho pedido, e 120 viraria "12".
 *
 * <p>A busca compara tambem com o numero sem o zero, porque "07" e "7" sao o mesmo numero (spec, casos
 * de borda): quem digita "B-7" acha a B-07.
 */
@Repository
public class JdbcCageDirectory implements CageDirectory {

    private static final String CODE =
            "battery || '-' || case when number < 10 then '0' || number::text else number::text end";

    private static final String CODE_WITHOUT_ZERO = "battery || '-' || number::text";

    private static final String COLUMNS =
            "id, sector_id, battery, number, bird_count, status, created_at, updated_at";

    /**
     * A ultima pesagem valida de cada gaiola da pagina, na mesma consulta (R-009 da 005): o indice parcial
     * {@code ux_weighing_cage_day} atende a busca da mais recente.
     */
    private static final String LAST_WEIGHING =
            " left join lateral (select w.weighed_on as last_weighed_on, w.average_weight as last_average_weight"
                    + " from weighing w where w.cage_id = cage.id and w.status = 'VALID'"
                    + " order by w.weighed_on desc limit 1) last on true";

    /** A ordem da lista: bateria e numero, e o id desempata as gaiolas de mesmo codigo. */
    private static final String ORDER = " order by battery, number, id";

    private final JdbcClient jdbcClient;

    JdbcCageDirectory(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public Optional<SectorSchedule> scheduleOf(SectorId sectorId) {
        return jdbcClient
                .sql("select status, weighing_day from sector where id = :id")
                .param("id", sectorId.value())
                .query((rs, rowNumber) -> new SectorSchedule(
                        Status.valueOf(rs.getString("status")), JdbcSectorDirectory.weighingDayOf(rs)))
                .optional();
    }

    @Override
    public PageResponse<CageSummary> search(SectorId sectorId, CageFilter cageFilter, int page, int size) {
        Filter filter = filterOf(sectorId, cageFilter);

        long total = jdbcClient
                .sql("select count(*) from cage" + filter.where())
                .params(filter.parameters())
                .query(Long.class)
                .single();

        List<CageSummary> content = jdbcClient
                .sql("select " + COLUMNS + ", last.last_weighed_on, last.last_average_weight from cage"
                        + LAST_WEIGHING
                        + filter.where()
                        // O id desempata a gaiola ativa e as inativas de mesmo codigo: sem ele, a mesma
                        // gaiola podia aparecer em duas paginas, ou em nenhuma.
                        + ORDER
                        + " limit :size offset :offset")
                .params(filter.parameters())
                .param("size", size)
                .param("offset", (long) page * size)
                .query((rs, rowNumber) -> summaryOf(rs, lastWeighingOf(rs)))
                .list();

        return PageResponse.of(content, page, size, total);
    }

    /** As gaiolas dos mesmos filtros e da mesma ordem da pesquisa, sem pagina (R-009 da 007). */
    @Override
    public List<CageSummary> searchAll(SectorId sectorId, CageFilter cageFilter) {
        Filter filter = filterOf(sectorId, cageFilter);
        return jdbcClient
                .sql("select " + COLUMNS + ", last.last_weighed_on, last.last_average_weight from cage"
                        + LAST_WEIGHING
                        + filter.where()
                        + ORDER)
                .params(filter.parameters())
                .query((rs, rowNumber) -> summaryOf(rs, lastWeighingOf(rs)))
                .list();
    }

    /** O trecho {@code where} e os parametros de uma pesquisa, iguais na pagina e na exportacao. */
    private record Filter(String where, Map<String, Object> parameters) {}

    private static Filter filterOf(SectorId sectorId, CageFilter cageFilter) {
        String code = cageFilter.code();
        String battery = cageFilter.battery();
        StatusFilter status = cageFilter.status();
        // So entram na consulta os filtros informados: um parametro nulo chega ao PostgreSQL sem tipo.
        List<String> conditions = new ArrayList<>(List.of("sector_id = :sectorId"));
        Map<String, Object> parameters = new LinkedHashMap<>(Map.of("sectorId", sectorId.value()));
        if (status != StatusFilter.ALL) {
            conditions.add("status = :status");
            parameters.put("status", status.name());
        }
        if (battery != null) {
            conditions.add("battery = :battery");
            parameters.put("battery", battery);
        }
        if (code != null) {
            conditions.add("((" + CODE + ") ilike :pattern escape '\\' or (" + CODE_WITHOUT_ZERO
                    + ") ilike :pattern escape '\\')");
            parameters.put("pattern", "%" + literal(code) + "%");
        }
        if (cageFilter.pendingSince() != null) {
            // "Pesagem pendente" (feature 010): a gaiola ativa sem pesagem valida desde o inicio da semana. O
            // indice parcial ux_weighing_cage_day atende a procura, e a condicao vale tambem na contagem.
            conditions.add("status = 'ACTIVE' and not exists (select 1 from weighing w where w.cage_id = cage.id"
                    + " and w.status = 'VALID' and w.weighed_on >= :pendingSince)");
            parameters.put("pendingSince", cageFilter.pendingSince());
        }
        return new Filter(" where " + String.join(" and ", conditions), parameters);
    }

    @Override
    public Optional<CageDetail> findDetail(SectorId sectorId, CageId cageId) {
        return jdbcClient
                .sql("select " + COLUMNS + " from cage where id = :id and sector_id = :sectorId")
                .param("id", cageId.value())
                .param("sectorId", sectorId.value())
                .query((rs, rowNumber) -> {
                    CageSummary summary = summaryOf(rs, null);
                    return new CageDetail(
                            summary.id(),
                            summary.sectorId(),
                            summary.code(),
                            summary.battery(),
                            summary.number(),
                            summary.birdCount(),
                            summary.status(),
                            rs.getObject("created_at", OffsetDateTime.class).toInstant(),
                            rs.getObject("updated_at", OffsetDateTime.class).toInstant());
                })
                .optional();
    }

    private static CageLastWeighing lastWeighingOf(ResultSet rs) throws SQLException {
        LocalDate day = rs.getObject("last_weighed_on", LocalDate.class);
        return day == null ? null : new CageLastWeighing(day, rs.getBigDecimal("last_average_weight"));
    }

    private static CageSummary summaryOf(ResultSet rs, CageLastWeighing lastWeighing) throws SQLException {
        String battery = rs.getString("battery");
        int number = rs.getInt("number");
        return new CageSummary(
                CageId.of(rs.getObject("id", UUID.class)),
                SectorId.of(rs.getObject("sector_id", UUID.class)),
                Cage.codeOf(battery, number),
                battery,
                number,
                rs.getInt("bird_count"),
                Status.valueOf(rs.getString("status")),
                lastWeighing, null);
    }

    /** O trecho como texto literal: {@code %} e {@code _} digitados sao procurados, e nao curingas. */
    private static String literal(String fragment) {
        return fragment.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
