package io.github.ovyx.production.presentation.dashboard;

import io.github.ovyx.production.application.dashboard.DashboardPeriod;
import io.github.ovyx.production.application.dashboard.GetDashboardOverviewQuery;
import io.github.ovyx.production.application.dashboard.GetSectorDashboardQuery;
import io.github.ovyx.shared.application.Dispatcher;
import io.github.ovyx.shared.presentation.ResultHttpMapper;
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
    @GetMapping(path = "/api/v1/sectors/{sectorId}/dashboard", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> getSectorDashboard(
            @PathVariable String sectorId, @RequestParam(required = false) DashboardPeriod period) {
        return resultHttpMapper.ok(dispatcher
                .ask(new GetSectorDashboardQuery(sectorId, period))
                .map(SectorDashboardResponse::from));
    }
}
