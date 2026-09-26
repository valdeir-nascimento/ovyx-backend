package io.github.ovyx.production.application.dailyreport;

import io.github.ovyx.production.domain.model.CageId;
import io.github.ovyx.production.domain.model.DailyReportId;
import io.github.ovyx.production.domain.model.SectorId;
import io.github.ovyx.shared.application.PageResponse;
import java.time.LocalDate;
import java.util.Optional;

/**
 * Porta de leitura dos relatorios (R-008): as consultas montam o que a tela precisa direto do banco, sem
 * carregar o agregado (principio V).
 */
public interface DailyReportDirectory {

    /** O setor, ativo ou inativo, ou nenhum se nao existe. */
    Optional<ReportingSector> sectorOf(SectorId sectorId);

    /**
     * Os relatorios do setor, do dia mais recente para o mais antigo, numa pagina.
     *
     * @param collectionDate so o relatorio deste dia; {@code null} nao filtra
     */
    PageResponse<DailyReportSummary> list(SectorId sectorId, LocalDate collectionDate, int page, int size);

    /** O relatorio, se for deste setor, com as gaiolas e os totais. */
    Optional<DailyReportDetail> findDetail(SectorId sectorId, DailyReportId reportId);

    /** Se o relatorio existe e e deste setor. */
    boolean reportExists(SectorId sectorId, DailyReportId reportId);

    /** A gaiola do relatorio, se o relatorio for deste setor e a gaiola for dele, com os lancamentos. */
    Optional<ReportCageDetail> findCage(SectorId sectorId, DailyReportId reportId, CageId cageId);

    /** O relatorio mais recente do setor, pela data da coleta. */
    Optional<LatestDailyReport> latestOf(SectorId sectorId);

    /** A soma das aves das gaiolas ativas do setor, para o primeiro relatorio. */
    int activeBirdsOf(SectorId sectorId);
}
