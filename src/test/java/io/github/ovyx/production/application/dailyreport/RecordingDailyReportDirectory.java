package io.github.ovyx.production.application.dailyreport;

import io.github.ovyx.production.domain.model.CageId;
import io.github.ovyx.production.domain.model.DailyReportId;
import io.github.ovyx.production.domain.model.SectorId;
import io.github.ovyx.shared.application.PageResponse;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Dublê da porta de leitura dos relatórios: guarda o que os tratadores pediram e devolve o que o teste
 * montou.
 */
final class RecordingDailyReportDirectory implements DailyReportDirectory {

    private final Map<SectorId, ReportingSector> sectors = new HashMap<>();
    private final Map<DailyReportId, DailyReportDetail> details = new HashMap<>();
    private final Map<SectorId, LatestDailyReport> latest = new HashMap<>();
    private final Map<SectorId, Integer> activeBirds = new HashMap<>();

    SectorId listedSector;
    LocalDate listedDate;
    int listedPage = -1;
    int listedSize = -1;
    SectorId detailSector;
    DailyReportId detailReport;
    SectorId cageSector;
    DailyReportId cageReport;
    CageId cageAsked;

    void knowSector(ReportingSector sector) {
        sectors.put(SectorId.of(sector.id()), sector);
    }

    void knowDetail(DailyReportDetail detail) {
        details.put(DailyReportId.of(detail.id()), detail);
    }

    void knowLatest(SectorId sectorId, LatestDailyReport report) {
        latest.put(sectorId, report);
    }

    void knowActiveBirds(SectorId sectorId, int birds) {
        activeBirds.put(sectorId, birds);
    }

    @Override
    public Optional<ReportingSector> sectorOf(SectorId sectorId) {
        return Optional.ofNullable(sectors.get(sectorId));
    }

    @Override
    public PageResponse<DailyReportSummary> list(SectorId sectorId, LocalDate collectionDate, int page, int size) {
        this.listedSector = sectorId;
        this.listedDate = collectionDate;
        this.listedPage = page;
        this.listedSize = size;
        return PageResponse.of(List.of(), page, size, 0);
    }

    @Override
    public Optional<DailyReportDetail> findDetail(SectorId sectorId, DailyReportId reportId) {
        this.detailSector = sectorId;
        this.detailReport = reportId;
        return Optional.ofNullable(details.get(reportId))
                .filter(detail -> detail.sector().id().equals(sectorId.value()));
    }

    @Override
    public boolean reportExists(SectorId sectorId, DailyReportId reportId) {
        return Optional.ofNullable(details.get(reportId))
                .filter(detail -> detail.sector().id().equals(sectorId.value()))
                .isPresent();
    }

    @Override
    public Optional<ReportCageDetail> findCage(SectorId sectorId, DailyReportId reportId, CageId cageId) {
        this.cageSector = sectorId;
        this.cageReport = reportId;
        this.cageAsked = cageId;
        return Optional.ofNullable(details.get(reportId))
                .filter(detail -> detail.sector().id().equals(sectorId.value()))
                .flatMap(detail -> detail.cages().stream()
                        .filter(cage -> cage.cageId().equals(cageId.value()))
                        .findFirst());
    }

    @Override
    public Optional<LatestDailyReport> latestOf(SectorId sectorId) {
        return Optional.ofNullable(latest.get(sectorId));
    }

    @Override
    public int activeBirdsOf(SectorId sectorId) {
        return activeBirds.getOrDefault(sectorId, 0);
    }
}
