package io.github.ovyx.production.application.dashboard;

import io.github.ovyx.production.application.dailyreport.ReportingSector;
import io.github.ovyx.production.domain.model.SectorId;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Porta de leitura do painel (R-004 da 006): os numeros crus, somados no banco. As contas sao do
 * {@link SectorDashboard}, para ficarem iguais as do relatorio e testaveis sem banco.
 */
public interface DashboardDirectory {

    /** Os setores ativos, os completos no dia e as abas. */
    DashboardSectors overview(LocalDate today);

    /** O setor do painel, ativo ou inativo; vazio se nao existe. */
    Optional<ReportingSector> sector(SectorId sectorId);

    /** Um {@link ReportDay} por relatorio do setor de {@code from} a {@code to}, inclusive, pela data. */
    List<ReportDay> reportDays(SectorId sectorId, LocalDate from, LocalDate to);

    /** As gaiolas ativas do setor com os numeros dos alertas de hoje, a faixa de peso e a mortalidade da semana. */
    CageWatchReading cageWatch(SectorId sectorId, LocalDate today);

    /** Os {@code limit} relatorios mais recentes do setor, do mais novo para o mais antigo. */
    List<LatestReport> latestReports(SectorId sectorId, int limit);
}
