package io.github.ovyx.production.presentation.dashboard;

import io.github.ovyx.production.application.dashboard.DashboardPeriod;
import io.github.ovyx.production.application.dashboard.ExportSectorDashboardQuery;
import io.github.ovyx.production.application.dashboard.GetDashboardOverviewQuery;
import io.github.ovyx.production.application.dashboard.ExportFarmDashboardQuery;
import io.github.ovyx.production.application.dashboard.GetFarmDashboardQuery;
import io.github.ovyx.production.application.dashboard.GetSectorDashboardQuery;
import io.github.ovyx.shared.application.Dispatcher;
import io.github.ovyx.shared.application.Result;
import io.github.ovyx.shared.application.spreadsheet.SpreadsheetFile;
import io.github.ovyx.shared.presentation.ResultHttpMapper;
import io.github.ovyx.shared.presentation.SpreadsheetResponses;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * As consultas do painel (feature 006): o cabecalho e o painel de um setor. O periodo fora dos tres e recusado
 * na leitura do parametro, como o filtro de data da lista de relatorios.
 */
@RestController
public class DashboardController implements DashboardApi {

    private final Dispatcher dispatcher;
    private final ResultHttpMapper resultHttpMapper;

    public DashboardController(Dispatcher dispatcher, ResultHttpMapper resultHttpMapper) {
        this.dispatcher = dispatcher;
        this.resultHttpMapper = resultHttpMapper;
    }

    @Override
    @GetMapping(path = "/api/v1/dashboard", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> getDashboardOverview() {
        return resultHttpMapper.ok(
                dispatcher.ask(new GetDashboardOverviewQuery()).map(DashboardOverviewResponse::from));
    }

    @Override
    @GetMapping(path = "/api/v1/dashboard/farm", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> getFarmDashboard(@RequestParam(required = false) DashboardPeriod period) {
        return resultHttpMapper.ok(
                dispatcher.ask(new GetFarmDashboardQuery(period)).map(FarmDashboardResponse::from));
    }

    /** A planilha da granja toda no periodo (US4 da 009): o arquivo no sucesso, o Problem Details na recusa. */
    @Override
    @GetMapping(path = "/api/v1/dashboard/farm/export")
    public ResponseEntity<?> exportFarmDashboard(@RequestParam(required = false) DashboardPeriod period) {
        Result<SpreadsheetFile> exported = dispatcher.ask(new ExportFarmDashboardQuery(period));
        return exported.isSuccess()
                ? SpreadsheetResponses.of(exported.value())
                : resultHttpMapper.problem(exported.error());
    }

    @Override
    @GetMapping(path = "/api/v1/sectors/{sectorId}/dashboard", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> getSectorDashboard(
            @PathVariable String sectorId, @RequestParam(required = false) DashboardPeriod period) {
        return resultHttpMapper.ok(dispatcher
                .ask(new GetSectorDashboardQuery(sectorId, period))
                .map(SectorDashboardResponse::from));
    }

    /** A planilha do painel do setor no periodo (US2 da 007): o arquivo no sucesso, o Problem Details na recusa. */
    @Override
    @GetMapping(path = "/api/v1/sectors/{sectorId}/dashboard/export")
    public ResponseEntity<?> exportSectorDashboard(
            @PathVariable String sectorId, @RequestParam(required = false) DashboardPeriod period) {
        Result<SpreadsheetFile> exported = dispatcher.ask(new ExportSectorDashboardQuery(sectorId, period));
        return exported.isSuccess()
                ? SpreadsheetResponses.of(exported.value())
                : resultHttpMapper.problem(exported.error());
    }
}
