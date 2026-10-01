package io.github.ovyx.production.application.dashboard;

import io.github.ovyx.production.application.dailyreport.ReportingSector;
import io.github.ovyx.production.domain.model.SectorId;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Porta de leitura do painel (R-004 da 006): os numeros crus, somados no banco. As contas sao do
 * {@link SectorDashboard}, para ficarem iguais as do relatorio e testaveis sem banco.
 */
public interface DashboardDirectory {

    /** Os setores ativos, os completos no dia e as abas. */
    DashboardSectors overview(LocalDate today);

    /** O setor do painel, ativo ou inativo; vazio se nao existe. */
    Optional<ReportingSector> sector(SectorId sectorId);

    /**
     * A meta de produtividade do setor, que o administrador define no cadastro dele (feature 008). Lida so depois
     * de {@link #sector} achar o setor: setor nao e apagado, e todo setor tem a meta.
     */
    LayingRateTarget layingRateTarget(SectorId sectorId);

    /**
     * Os setores ativos, com ou sem relatorio, com a meta, na ordem das abas: pelo nome, sem maiusculas (feature
     * 009).
     */
    List<ActiveSector> activeSectors();

    /**
     * Os relatorios dos setores ativos de {@code from} a {@code to}, inclusive, agrupados pelo setor, cada lista pela
     * data (feature 009). O setor sem relatorio no intervalo nao aparece.
     */
    Map<UUID, List<ReportDay>> activeReportDays(LocalDate from, LocalDate to);

    /** Um {@link ReportDay} por relatorio do setor de {@code from} a {@code to}, inclusive, pela data. */
    List<ReportDay> reportDays(SectorId sectorId, LocalDate from, LocalDate to);

    /** As gaiolas ativas do setor com os numeros dos alertas de hoje, a faixa de peso e a mortalidade da semana. */
    CageWatchReading cageWatch(SectorId sectorId, LocalDate today);

    /** Os {@code limit} relatorios mais recentes do setor, do mais novo para o mais antigo. */
    List<LatestReport> latestReports(SectorId sectorId, int limit);
}
