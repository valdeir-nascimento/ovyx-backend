package io.github.ovyx.production.infrastructure.persistence;

import io.github.ovyx.production.application.dailyreport.ReportingSector;
import io.github.ovyx.production.application.dashboard.CageWatch;
import io.github.ovyx.production.application.dashboard.CageWatchReading;
import io.github.ovyx.production.application.dashboard.DashboardDirectory;
import io.github.ovyx.production.application.dashboard.DashboardSector;
import io.github.ovyx.production.application.dashboard.DashboardSectors;
import io.github.ovyx.production.application.dashboard.LatestReport;
import io.github.ovyx.production.application.dashboard.MortalityBaseline;
import io.github.ovyx.production.application.dashboard.ReferenceWeight;
import io.github.ovyx.production.application.dashboard.ReportDay;
import io.github.ovyx.production.domain.model.Actor;
import io.github.ovyx.production.domain.model.FeedStatus;
import io.github.ovyx.production.domain.model.MortalityStatus;
import io.github.ovyx.production.domain.model.ProductionStatus;
import io.github.ovyx.production.domain.model.SectorId;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Adaptador da porta de leitura do painel (R-004 da 006).
 *
 * <p>Soma as gaiolas de cada relatorio na propria consulta: um setor de 300 gaiolas em 14 dias vira 14 linhas.
 * As contas ficam na aplicacao. As tabelas {@code sector} e {@code cage} do farm sao lidas so aqui, como na
 * {@code JdbcFarmStructure}: o codigo dos dois contextos continua independente (R-001).
 */
@Repository
public class JdbcDashboardDirectory implements DashboardDirectory {

    /**
     * Os relatorios mais recentes em que a baixa postura procura os 3 de cada gaiola. Uma janela limitada deixa
     * a leitura presa ao indice por setor e data, em vez de varrer todas as gaiolas de todos os relatorios.
     */
    private static final int REPORTS_SEARCHED_FOR_LAYING = 10;

    private final JdbcClient jdbcClient;

    JdbcDashboardDirectory(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public DashboardSectors overview(LocalDate today) {
        int activeSectors = jdbcClient
                .sql("select count(*) from sector where status = 'ACTIVE'")
                .query(Integer.class)
                .single();
        // O banco so soma as gaiolas; o "completo" e decidido pelas mesmas situacoes do relatorio (FR-022).
        long completeToday = jdbcClient
                .sql("""
                        select r.no_mortality_confirmed,
                               count(c.cage_id) as cages,
                               count(c.eggs) as cages_with_production,
                               count(c.feed_consumption) as cages_with_feed,
                               coalesce(sum(c.deaths + c.culls), 0) as removed_birds
                          from daily_report r
                          join sector s on s.id = r.sector_id
                          left join report_cage c on c.report_id = r.id
                         where s.status = 'ACTIVE' and r.collection_date = :today
                         group by r.id, r.no_mortality_confirmed
                        """)
                .param("today", today)
                .query((row, index) -> {
                    int cages = row.getInt("cages");
                    return ProductionStatus.of(cages - row.getInt("cages_with_production")) == ProductionStatus.COMPLETE
                            && FeedStatus.of(cages - row.getInt("cages_with_feed")) == FeedStatus.COMPLETE
                            && MortalityStatus.of(row.getBoolean("no_mortality_confirmed"), row.getInt("removed_birds"))
                                    == MortalityStatus.RECORDED;
                })
                .list()
                .stream()
                .filter(Boolean::booleanValue)
                .count();
        List<DashboardSector> sectors = jdbcClient
                .sql("""
                        select s.id, s.name from sector s
                         where s.status = 'ACTIVE'
                           and exists (select 1 from daily_report r where r.sector_id = s.id)
                         order by lower(s.name), s.id
                        """)
                .query((row, index) -> new DashboardSector(row.getObject("id", UUID.class), row.getString("name")))
                .list();
        return new DashboardSectors(activeSectors, (int) completeToday, sectors);
    }

    @Override
    public Optional<ReportingSector> sector(SectorId sectorId) {
        return jdbcClient
                .sql("select id, name, status from sector where id = :id")
                .param("id", sectorId.value())
                .query((row, index) -> new ReportingSector(
                        row.getObject("id", UUID.class), row.getString("name"), row.getString("status")))
                .optional();
    }

    @Override
    public List<ReportDay> reportDays(SectorId sectorId, LocalDate from, LocalDate to) {
        return jdbcClient
                .sql("""
                        select r.id, r.collection_date, r.opening_bird_count, r.no_mortality_confirmed,
                               count(c.cage_id) as cages,
                               count(c.eggs) as cages_with_production,
                               count(c.feed_consumption) as cages_with_feed,
                               coalesce(sum(c.eggs), 0) as eggs,
                               coalesce(sum(c.small), 0) as small,
                               coalesce(sum(c.jumbo), 0) as jumbo,
                               coalesce(sum(c.dirty), 0) as dirty,
                               coalesce(sum(c.cracked), 0) as cracked,
                               coalesce(sum(c.blood_spot), 0) as blood_spot,
                               coalesce(sum(c.abnormal), 0) as abnormal,
                               coalesce(sum(c.feed_consumption * c.feed_price_per_kg), 0) as exact_feed_cost,
                               coalesce(sum(c.deaths + c.culls), 0) as removed_birds
                          from daily_report r
                          left join report_cage c on c.report_id = r.id
                         where r.sector_id = :sectorId and r.collection_date between :from and :to
                         group by r.id
                         order by r.collection_date
                        """)
                .param("sectorId", sectorId.value())
                .param("from", from)
                .param("to", to)
                .query((row, index) -> new ReportDay(
                        row.getObject("id", UUID.class),
                        row.getObject("collection_date", LocalDate.class),
                        row.getInt("opening_bird_count"),
                        row.getInt("cages"),
                        row.getInt("cages_with_production"),
                        row.getInt("cages_with_feed"),
                        row.getInt("eggs"),
                        row.getInt("small"),
                        row.getInt("jumbo"),
                        row.getInt("dirty"),
                        row.getInt("cracked"),
                        row.getInt("blood_spot"),
                        row.getInt("abnormal"),
                        row.getBigDecimal("exact_feed_cost"),
                        row.getInt("removed_birds"),
                        row.getBoolean("no_mortality_confirmed")))
                .list();
    }

    @Override
    public CageWatchReading cageWatch(SectorId sectorId, LocalDate today) {
        Map<UUID, Integer> removedToday = removedToday(sectorId, today);
        Map<UUID, int[]> recent = recentProduction(sectorId, today);
        List<CageWatch> cages = jdbcClient
                .sql("""
                        select c.id, c.battery || '-' || lpad(c.number::text, 2, '0') as code,
                               last.weighed_on, last.average_weight
                          from cage c
                          left join lateral (select w.weighed_on, w.average_weight from weighing w
                                              where w.cage_id = c.id and w.status = 'VALID'
                                              order by w.weighed_on desc limit 1) last on true
                         where c.sector_id = :sectorId and c.status = 'ACTIVE'
                         order by c.battery, c.number
                        """)
                .param("sectorId", sectorId.value())
                .query((row, index) -> {
                    UUID cageId = row.getObject("id", UUID.class);
                    int[] production = recent.getOrDefault(cageId, new int[3]);
                    return new CageWatch(
                            cageId,
                            row.getString("code"),
                            removedToday.getOrDefault(cageId, 0),
                            production[0],
                            production[1],
                            production[2],
                            row.getObject("weighed_on", LocalDate.class),
                            row.getBigDecimal("average_weight"));
                })
                .list();
        return new CageWatchReading(cages, range(sectorId), baseline(sectorId, today));
    }

    /** As aves removidas hoje em cada gaiola do relatorio de hoje. */
    private Map<UUID, Integer> removedToday(SectorId sectorId, LocalDate today) {
        Map<UUID, Integer> removed = new HashMap<>();
        jdbcClient
                .sql("""
                        select c.cage_id, coalesce(c.deaths + c.culls, 0) as removed
                          from report_cage c join daily_report r on r.id = c.report_id
                         where r.sector_id = :sectorId and r.collection_date = :today
                        """)
                .param("sectorId", sectorId.value())
                .param("today", today)
                .query((row, index) -> removed.put(row.getObject("cage_id", UUID.class), row.getInt("removed")))
                .list();
        return removed;
    }

    /**
     * Os ovos, as aves e a quantidade dos ultimos 3 relatorios com a producao de cada gaiola lancada, entre os
     * relatorios mais recentes do setor ate hoje.
     */
    private Map<UUID, int[]> recentProduction(SectorId sectorId, LocalDate today) {
        Map<UUID, int[]> recent = new HashMap<>();
        jdbcClient
                .sql("""
                        with recent_reports as (
                            select id, collection_date from daily_report
                             where sector_id = :sectorId and collection_date <= :today
                             order by collection_date desc
                             limit :reports),
                        ranked as (
                            select c.cage_id, c.eggs, c.bird_count,
                                   row_number() over (partition by c.cage_id order by r.collection_date desc) as position
                              from report_cage c join recent_reports r on r.id = c.report_id
                             where c.eggs is not null)
                        select cage_id, sum(eggs) as eggs, sum(bird_count) as birds, count(*) as reports
                          from ranked where position <= 3
                         group by cage_id
                        """)
                .param("sectorId", sectorId.value())
                .param("today", today)
                .param("reports", REPORTS_SEARCHED_FOR_LAYING)
                .query((row, index) -> recent.put(
                        row.getObject("cage_id", UUID.class),
                        new int[] {row.getInt("eggs"), row.getInt("birds"), row.getInt("reports")}))
                .list();
        return recent;
    }

    /** A faixa de peso de referencia do setor; {@code null} sem faixa. */
    private ReferenceWeight range(SectorId sectorId) {
        return jdbcClient
                .sql("select reference_weight_min, reference_weight_max from sector"
                        + " where id = :id and reference_weight_min is not null")
                .param("id", sectorId.value())
                .query((row, index) ->
                        new ReferenceWeight(row.getInt("reference_weight_min"), row.getInt("reference_weight_max")))
                .optional()
                .orElse(null);
    }

    @Override
    public List<LatestReport> latestReports(SectorId sectorId, int limit) {
        // Os relatorios sao escolhidos antes de somar as gaiolas: a soma fica so com os que aparecem.
        return jdbcClient
                .sql("""
                        with latest as (
                            select id, collection_date, collection_time, opened_by_id, opened_by_name,
                                   no_mortality_confirmed
                              from daily_report
                             where sector_id = :sectorId
                             order by collection_date desc
                             limit :limit)
                        select r.id, r.collection_date, r.collection_time, r.opened_by_id, r.opened_by_name,
                               r.no_mortality_confirmed,
                               count(c.cage_id) as cages,
                               count(c.eggs) as cages_with_production,
                               count(c.feed_consumption) as cages_with_feed,
                               coalesce(sum(c.eggs), 0) as eggs,
                               coalesce(sum(c.deaths + c.culls), 0) as removed_birds
                          from latest r
                          left join report_cage c on c.report_id = r.id
                         group by r.id, r.collection_date, r.collection_time, r.opened_by_id, r.opened_by_name,
                                  r.no_mortality_confirmed
                         order by r.collection_date desc
                        """)
                .param("sectorId", sectorId.value())
                .param("limit", limit)
                .query((row, index) -> {
                    int cages = row.getInt("cages");
                    int removed = row.getInt("removed_birds");
                    return new LatestReport(
                            row.getObject("id", UUID.class),
                            row.getObject("collection_date", LocalDate.class),
                            row.getObject("collection_time", LocalTime.class),
                            new Actor(row.getObject("opened_by_id", UUID.class), row.getString("opened_by_name")),
                            row.getInt("eggs"),
                            removed,
                            ProductionStatus.of(cages - row.getInt("cages_with_production")),
                            FeedStatus.of(cages - row.getInt("cages_with_feed")),
                            MortalityStatus.of(row.getBoolean("no_mortality_confirmed"), removed));
                })
                .list();
    }

    /** A mortalidade dos relatorios dos 7 dias antes de hoje: as aves removidas e as gaiolas, dia a dia. */
    private MortalityBaseline baseline(SectorId sectorId, LocalDate today) {
        return jdbcClient
                .sql("""
                        select coalesce(sum(c.deaths + c.culls), 0) as removed, count(c.cage_id) as cage_days
                          from daily_report r join report_cage c on c.report_id = r.id
                         where r.sector_id = :sectorId and r.collection_date between :from and :to
                        """)
                .param("sectorId", sectorId.value())
                .param("from", today.minusDays(7))
                .param("to", today.minusDays(1))
                .query((row, index) -> new MortalityBaseline(row.getInt("removed"), row.getInt("cage_days")))
                .single();
    }
}
