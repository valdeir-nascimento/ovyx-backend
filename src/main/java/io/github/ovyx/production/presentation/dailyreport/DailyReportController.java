package io.github.ovyx.production.presentation.dailyreport;

import io.github.ovyx.production.application.dailyreport.ConfirmNoMortalityCommand;
import io.github.ovyx.production.application.dailyreport.CorrectDailyReportCommand;
import io.github.ovyx.production.application.dailyreport.DailyReportDetail;
import io.github.ovyx.production.application.dailyreport.FindDailyReportQuery;
import io.github.ovyx.production.application.dailyreport.FindReportCageQuery;
import io.github.ovyx.production.application.dailyreport.ListDailyReportsQuery;
import io.github.ovyx.production.application.dailyreport.OpenDailyReportCommand;
import io.github.ovyx.production.application.dailyreport.RecordMortalityCommand;
import io.github.ovyx.production.application.dailyreport.RecordProductionCommand;
import io.github.ovyx.production.application.dailyreport.SuggestDailyReportQuery;
import io.github.ovyx.production.domain.model.Actor;
import io.github.ovyx.production.domain.model.CageId;
import io.github.ovyx.production.domain.model.DailyReportId;
import io.github.ovyx.shared.application.Dispatcher;
import io.github.ovyx.shared.application.Result;
import io.github.ovyx.shared.presentation.AuthenticatedUser;
import io.github.ovyx.shared.presentation.ResultHttpMapper;

import java.net.URI;
import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Rotas dos relatorios diarios de um setor. So rotas e traducao do {@code Result}: a documentacao fica em
 * {@link DailyReportApi}, e a autorizacao, em {@code ProductionRouteAuthorization}.
 *
 * <p>Os identificadores chegam como texto: o malformado e "nao encontrado", e nao 400 de parametro. Quem
 * abre e quem corrige vem da sessao ({@link AuthenticatedUser}), e nao do corpo (R-007). Os comandos
 * devolvem so o identificador (principio V); o corpo da resposta vem da consulta, feita em seguida.
 */
@RestController
@RequestMapping("/api/v1/sectors/{sectorId}/daily-reports")
public class DailyReportController implements DailyReportApi {

    private final Dispatcher dispatcher;
    private final ResultHttpMapper resultHttpMapper;

    public DailyReportController(Dispatcher dispatcher, ResultHttpMapper resultHttpMapper) {
        this.dispatcher = dispatcher;
        this.resultHttpMapper = resultHttpMapper;
    }

    @Override
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> listDailyReports(
        @PathVariable String sectorId,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate collectionDate,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size) {
        return resultHttpMapper.ok(dispatcher
            .ask(new ListDailyReportsQuery(sectorId, collectionDate, page, size))
            .map(DailyReportPageResponse::from));
    }

    @Override
    @GetMapping(path = "/suggestion", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> suggestDailyReport(@PathVariable String sectorId) {
        return resultHttpMapper.ok(
            dispatcher.ask(new SuggestDailyReportQuery(sectorId)).map(DailyReportSuggestionResponse::from));
    }

    @Override
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> openDailyReport(
        @PathVariable String sectorId, @RequestBody DailyReportRequest body, AuthenticatedUser user) {
        // Sem @Valid: o corpo nao tem anotacoes, e todas as violacoes vem do dominio, de uma vez.
        Result<DailyReportId> opened = dispatcher.dispatch(new OpenDailyReportCommand(
            sectorId,
            body.collectionDate(),
            body.collectionTime(),
            body.rawOpeningBirdCount(),
            body.rawFlockAge(),
            body.note(),
            actorOf(user)));
        return resultHttpMapper.created(
            detailOf(sectorId, opened).map(DailyReportDetailResponse::from),
            detail -> URI.create(
                "/api/v1/sectors/" + detail.sector().id() + "/daily-reports/" + detail.id()));
    }

    @Override
    @GetMapping(path = "/{reportId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> findDailyReport(@PathVariable String sectorId, @PathVariable String reportId) {
        return resultHttpMapper.ok(
            dispatcher.ask(new FindDailyReportQuery(sectorId, reportId)).map(DailyReportDetailResponse::from));
    }

    @Override
    @PutMapping(
        path = "/{reportId}",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> correctDailyReport(
        @PathVariable String sectorId,
        @PathVariable String reportId,
        @RequestBody DailyReportRequest body,
        AuthenticatedUser user) {
        Result<DailyReportId> corrected = dispatcher.dispatch(new CorrectDailyReportCommand(
            sectorId,
            reportId,
            body.collectionDate(),
            body.collectionTime(),
            body.rawOpeningBirdCount(),
            body.rawFlockAge(),
            body.note(),
            actorOf(user)));
        return resultHttpMapper.ok(detailOf(sectorId, corrected).map(DailyReportDetailResponse::from));
    }

    @Override
    @GetMapping(path = "/{reportId}/cages/{cageId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> findReportCage(
        @PathVariable String sectorId, @PathVariable String reportId, @PathVariable String cageId) {
        return resultHttpMapper.ok(dispatcher
            .ask(new FindReportCageQuery(sectorId, reportId, cageId))
            .map(ReportCageDetailResponse::from));
    }

    @Override
    @PutMapping(
        path = "/{reportId}/cages/{cageId}/production",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> recordProduction(
        @PathVariable String sectorId,
        @PathVariable String reportId,
        @PathVariable String cageId,
        @RequestBody ProductionRequest body,
        AuthenticatedUser user) {
        Result<CageId> recorded = dispatcher.dispatch(new RecordProductionCommand(
            sectorId,
            reportId,
            cageId,
            body.rawEggs(),
            body.rawSmall(),
            body.rawJumbo(),
            body.rawDirty(),
            body.rawCracked(),
            body.rawBloodSpot(),
            body.rawAbnormal(),
            actorOf(user)));
        return resultHttpMapper.ok(recorded
            .flatMap(id -> dispatcher.ask(new FindReportCageQuery(sectorId, reportId, id.toString())))
            .map(ReportCageDetailResponse::from));
    }

    @Override
    @PutMapping(
        path = "/{reportId}/cages/{cageId}/mortality",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> recordMortality(
        @PathVariable String sectorId,
        @PathVariable String reportId,
        @PathVariable String cageId,
        @RequestBody MortalityRequest body,
        AuthenticatedUser user) {
        Result<CageId> recorded = dispatcher.dispatch(new RecordMortalityCommand(
            sectorId, reportId, cageId, body.rawDeaths(), body.rawCulls(), body.note(), actorOf(user)));
        return resultHttpMapper.ok(recorded
            .flatMap(id -> dispatcher.ask(new FindReportCageQuery(sectorId, reportId, id.toString())))
            .map(ReportCageDetailResponse::from));
    }

    @Override
    @PostMapping(path = "/{reportId}/mortality-confirmation", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> confirmNoMortality(
        @PathVariable String sectorId, @PathVariable String reportId, AuthenticatedUser user) {
        Result<DailyReportId> confirmed =
            dispatcher.dispatch(new ConfirmNoMortalityCommand(sectorId, reportId, actorOf(user)));
        return resultHttpMapper.ok(detailOf(sectorId, confirmed).map(DailyReportDetailResponse::from));
    }

    private Result<DailyReportDetail> detailOf(String sectorId, Result<DailyReportId> written) {
        return written.flatMap(id -> dispatcher.ask(new FindDailyReportQuery(sectorId, id.toString())));
    }

    private static Actor actorOf(AuthenticatedUser user) {
        return new Actor(user.id(), user.fullName());
    }
}
