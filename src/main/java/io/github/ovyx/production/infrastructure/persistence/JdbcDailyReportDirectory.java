package io.github.ovyx.production.infrastructure.persistence;

import io.github.ovyx.production.application.dailyreport.CageFeed;
import io.github.ovyx.production.application.dailyreport.CageMortality;
import io.github.ovyx.production.application.dailyreport.CageProduction;
import io.github.ovyx.production.application.dailyreport.DailyReportDetail;
import io.github.ovyx.production.application.dailyreport.DailyReportDirectory;
import io.github.ovyx.production.application.dailyreport.DailyReportSummary;
import io.github.ovyx.production.application.dailyreport.DailyReportTotals;
import io.github.ovyx.production.application.dailyreport.LatestDailyReport;
import io.github.ovyx.production.application.dailyreport.ProductionTotals;
import io.github.ovyx.production.application.dailyreport.ReportCageDetail;
import io.github.ovyx.production.application.dailyreport.ReportingSector;
import io.github.ovyx.production.domain.model.Actor;
import io.github.ovyx.production.domain.model.CageId;
import io.github.ovyx.production.domain.model.DailyReportId;
import io.github.ovyx.production.domain.model.FeedStatus;
import io.github.ovyx.production.domain.model.MortalityStatus;
import io.github.ovyx.production.domain.model.ProductionStatus;
import io.github.ovyx.production.domain.model.SectorId;
import io.github.ovyx.shared.application.PageResponse;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Adaptador da porta de leitura dos relatorios (R-008).
 *
 * <p>A lista soma as gaiolas de cada relatorio na propria consulta; o detalhe traz as gaiolas e calcula
 * os totais com {@link DailyReportTotals}; a gaiola do relatorio sai da mesma leitura de linha que o
 * detalhe usa, e por isso as duas nao divergem. O setor vem da tabela {@code sector}, e o nome atual da
 * formula de cada racao, da {@code feed_formula}, na mesma camada anticorrupcao da
 * {@code JdbcFarmStructure} (R-004; R-005 da 004).
 */
@Repository
public class JdbcDailyReportDirectory implements DailyReportDirectory {

    private static final String SUMMARY_COLUMNS = """
            r.id, r.collection_date, r.collection_time, r.opened_by_name, r.flock_age, r.note,
            r.opening_bird_count, r.no_mortality_confirmed,
            coalesce(sum(c.eggs), 0) as collected_eggs,
            coalesce(sum(c.deaths + c.culls), 0) as removed_birds,
            count(c.cage_id) filter (where c.eggs is null) as pending_cages,
            count(c.cage_id) filter (where c.feed_consumption is null) as feed_pending_cages
            """;

    /** As colunas da gaiola do relatorio, com o nome atual da formula da racao. */
    private static final String CAGE_COLUMNS = """
            select c.*, f.name as feed_formula_name
              from report_cage c left join feed_formula f on f.id = c.feed_formula_id
            """;

    private final JdbcClient jdbcClient;

    JdbcDailyReportDirectory(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public Optional<ReportingSector> sectorOf(SectorId sectorId) {
        return jdbcClient
                .sql("select id, name, status from sector where id = :id")
                .param("id", sectorId.value())
                .query((row, index) -> new ReportingSector(
                        row.getObject("id", UUID.class), row.getString("name"), row.getString("status")))
                .optional();
    }

    @Override
    public PageResponse<DailyReportSummary> list(SectorId sectorId, LocalDate collectionDate, int page, int size) {
        String filter = collectionDate == null ? "" : " and r.collection_date = :collectionDate";
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("sectorId", sectorId.value());
        if (collectionDate != null) {
            params.put("collectionDate", collectionDate);
        }
        long total = jdbcClient
                .sql("select count(*) from daily_report r where r.sector_id = :sectorId" + filter)
                .params(params)
                .query(Long.class)
                .single();
        List<DailyReportSummary> content = jdbcClient
                .sql("select " + SUMMARY_COLUMNS
                        + " from daily_report r left join report_cage c on c.report_id = r.id"
                        + " where r.sector_id = :sectorId" + filter
                        + " group by r.id order by r.collection_date desc limit :size offset :offset")
                .params(params)
                .param("size", size)
                .param("offset", (long) page * size)
                .query((row, index) -> summaryOf(row))
                .list();
        return PageResponse.of(content, page, size, total);
    }

    @Override
    public List<DailyReportDetail> detailsBetween(SectorId sectorId, LocalDate from, LocalDate to) {
        Optional<ReportingSector> sector = sectorOf(sectorId);
        if (sector.isEmpty()) {
            return List.of();
        }
        // Duas consultas, e nao uma por relatorio (R-006 da 007): as gaiolas de todos os relatorios do intervalo,
        // e depois os relatorios. As gaiolas vem antes, como no findDetail, para nao abrir uma consulta dentro
        // da outra; e os totais saem do DailyReportTotals, como no detalhe.
        Map<UUID, List<ReportCageDetail>> cages = new HashMap<>();
        jdbcClient
                .sql(CAGE_COLUMNS
                        + " join daily_report r on r.id = c.report_id"
                        + " where r.sector_id = :sectorId and r.collection_date between :from and :to"
                        + " order by c.battery, c.number")
                .param("sectorId", sectorId.value())
                .param("from", from)
                .param("to", to)
                .query((RowCallbackHandler) row -> cages
                        .computeIfAbsent(row.getObject("report_id", UUID.class), report -> new ArrayList<>())
                        .add(cageOf(row)));
        return jdbcClient
                .sql("select * from daily_report"
                        + " where sector_id = :sectorId and collection_date between :from and :to"
                        + " order by collection_date")
                .param("sectorId", sectorId.value())
                .param("from", from)
                .param("to", to)
                .query((row, index) -> detailOf(
                        row, sector.get(), cages.getOrDefault(row.getObject("id", UUID.class), List.of())))
                .list();
    }

    @Override
    public Optional<DailyReportDetail> findDetail(SectorId sectorId, DailyReportId reportId) {
        Optional<ReportingSector> sector = sectorOf(sectorId);
        if (sector.isEmpty()) {
            return Optional.empty();
        }
        // As gaiolas antes do relatorio, e nao dentro da leitura dele: aninhada, a consulta das gaiolas
        // pedia uma segunda conexao com a primeira aberta, e mais leituras que o pool travavam a API
        // (revisao da T110, a mesma falha do T285 da 002). As gaiolas de outro setor nao saem daqui: sem o
        // relatorio deste setor, o detalhe e vazio.
        List<ReportCageDetail> cages = cagesOf(reportId);
        return jdbcClient
                .sql("select * from daily_report where id = :id and sector_id = :sectorId")
                .param("id", reportId.value())
                .param("sectorId", sectorId.value())
                .query((row, index) -> detailOf(row, sector.get(), cages))
                .optional();
    }

    @Override
    public boolean reportExists(SectorId sectorId, DailyReportId reportId) {
        return jdbcClient
                .sql("select exists (select 1 from daily_report where id = :id and sector_id = :sectorId)")
                .param("id", reportId.value())
                .param("sectorId", sectorId.value())
                .query(Boolean.class)
                .single();
    }

    @Override
    public Optional<ReportCageDetail> findCage(SectorId sectorId, DailyReportId reportId, CageId cageId) {
        return jdbcClient
                .sql(CAGE_COLUMNS
                        + " join daily_report r on r.id = c.report_id"
                        + " where c.report_id = :reportId and r.sector_id = :sectorId and c.cage_id = :cageId")
                .param("reportId", reportId.value())
                .param("sectorId", sectorId.value())
                .param("cageId", cageId.value())
                .query((row, index) -> cageOf(row))
                .optional();
    }

    @Override
    public Optional<LatestDailyReport> latestOf(SectorId sectorId) {
        return jdbcClient
                .sql("""
                        select r.collection_date, r.flock_age,
                               r.opening_bird_count - coalesce(sum(c.deaths + c.culls), 0) as closing_bird_count
                          from daily_report r left join report_cage c on c.report_id = r.id
                         where r.sector_id = :sectorId
                         group by r.id
                         order by r.collection_date desc
                         limit 1
                        """)
                .param("sectorId", sectorId.value())
                .query((row, index) -> new LatestDailyReport(
                        row.getObject("collection_date", LocalDate.class),
                        row.getInt("flock_age"),
                        row.getInt("closing_bird_count")))
                .optional();
    }

    @Override
    public int activeBirdsOf(SectorId sectorId) {
        return jdbcClient
                .sql("select coalesce(sum(bird_count), 0) from cage where sector_id = :sectorId and status = 'ACTIVE'")
                .param("sectorId", sectorId.value())
                .query(Integer.class)
                .single();
    }

    private List<ReportCageDetail> cagesOf(DailyReportId reportId) {
        return jdbcClient
                .sql(CAGE_COLUMNS + " where c.report_id = :id order by c.battery, c.number")
                .param("id", reportId.value())
                .query((row, index) -> cageOf(row))
                .list();
    }

    private static DailyReportSummary summaryOf(ResultSet row) throws SQLException {
        int opening = row.getInt("opening_bird_count");
        int removed = row.getInt("removed_birds");
        int pending = row.getInt("pending_cages");
        int feedPending = row.getInt("feed_pending_cages");
        return new DailyReportSummary(
                row.getObject("id", UUID.class),
                row.getObject("collection_date", LocalDate.class),
                row.getObject("collection_time", LocalTime.class),
                row.getString("opened_by_name"),
                row.getInt("flock_age"),
                row.getInt("collected_eggs"),
                removed,
                opening - removed,
                row.getString("note"),
                ProductionStatus.of(pending),
                pending,
                MortalityStatus.of(row.getBoolean("no_mortality_confirmed"), removed),
                FeedStatus.of(feedPending),
                feedPending);
    }

    private static DailyReportDetail detailOf(ResultSet row, ReportingSector sector, List<ReportCageDetail> cages)
            throws SQLException {
        int opening = row.getInt("opening_bird_count");
        boolean confirmed = row.getBoolean("no_mortality_confirmed");
        UUID correctedById = row.getObject("last_corrected_by_id", UUID.class);
        ProductionTotals production = DailyReportTotals.production(opening, cages);
        return new DailyReportDetail(
                row.getObject("id", UUID.class),
                sector,
                row.getObject("collection_date", LocalDate.class),
                row.getObject("collection_time", LocalTime.class),
                opening,
                row.getInt("flock_age"),
                row.getString("note"),
                confirmed,
                new Actor(row.getObject("opened_by_id", UUID.class), row.getString("opened_by_name")),
                instantOf(row, "opened_at"),
                correctedById == null ? null : new Actor(correctedById, row.getString("last_corrected_by_name")),
                instantOf(row, "last_corrected_at"),
                production,
                DailyReportTotals.mortality(opening, confirmed, cages),
                DailyReportTotals.feed(cages, production.collectedEggs()),
                cages);
    }

    private static ReportCageDetail cageOf(ResultSet row) throws SQLException {
        String battery = row.getString("battery");
        int number = row.getInt("number");
        Integer eggs = row.getObject("eggs", Integer.class);
        Integer deaths = row.getObject("deaths", Integer.class);
        Integer consumption = row.getObject("feed_consumption", Integer.class);
        int birdCount = row.getInt("bird_count");
        return new ReportCageDetail(
                row.getObject("cage_id", UUID.class),
                ReportCageDetail.codeOf(battery, number),
                battery,
                number,
                birdCount,
                eggs == null
                        ? null
                        : new CageProduction(
                                eggs,
                                row.getInt("small"),
                                row.getInt("jumbo"),
                                row.getInt("dirty"),
                                row.getInt("cracked"),
                                row.getInt("blood_spot"),
                                row.getInt("abnormal")),
                deaths == null
                        ? null
                        : new CageMortality(deaths, row.getInt("culls"), row.getString("mortality_note")),
                consumption == null
                        ? null
                        : CageFeed.of(
                                row.getObject("feed_formula_id", UUID.class),
                                row.getString("feed_formula_name"),
                                row.getBigDecimal("feed_price_per_kg"),
                                row.getInt("feed_expected_intake"),
                                consumption,
                                birdCount));
    }

    private static Instant instantOf(ResultSet row, String column) throws SQLException {
        OffsetDateTime value = row.getObject(column, OffsetDateTime.class);
        return value == null ? null : value.toInstant();
    }
}
