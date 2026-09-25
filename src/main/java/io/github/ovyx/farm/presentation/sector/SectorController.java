package io.github.ovyx.farm.presentation.sector;

import io.github.ovyx.farm.application.common.StatusFilter;
import io.github.ovyx.farm.application.sector.DeactivateSectorCommand;
import io.github.ovyx.farm.application.sector.FindSectorByIdQuery;
import io.github.ovyx.farm.application.sector.ListSectorsQuery;
import io.github.ovyx.farm.application.sector.ReactivateSectorCommand;
import io.github.ovyx.farm.application.sector.RegisterSectorCommand;
import io.github.ovyx.farm.application.sector.SectorDetail;
import io.github.ovyx.farm.application.sector.UpdateSectorCommand;
import io.github.ovyx.farm.domain.model.SectorId;
import io.github.ovyx.shared.application.Dispatcher;
import io.github.ovyx.shared.application.Result;
import io.github.ovyx.shared.presentation.ResultHttpMapper;

import java.net.URI;

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

@RestController
@RequestMapping("/api/v1/sectors")
public class SectorController implements SectorApi {

    private final Dispatcher dispatcher;
    private final ResultHttpMapper resultHttpMapper;

    public SectorController(Dispatcher dispatcher, ResultHttpMapper resultHttpMapper) {
        this.dispatcher = dispatcher;
        this.resultHttpMapper = resultHttpMapper;
    }

    @Override
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> listSectors(@RequestParam(required = false) StatusFilter status) {
        return resultHttpMapper.ok(dispatcher
            .ask(new ListSectorsQuery(status))
            .map(sectors -> sectors.stream().map(SectorSummaryResponse::from).toList()));
    }

    @Override
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> registerSector(@RequestBody SectorRequest body) {
        // Sem @Valid: o corpo nao tem anotacoes, e todas as violacoes vem do dominio, de uma vez.
        Result<SectorId> registered = dispatcher.dispatch(new RegisterSectorCommand(body.name(), body.description()));
        return resultHttpMapper.created(
            detailOf(registered).map(SectorDetailResponse::from),
            detail -> URI.create("/api/v1/sectors/" + detail.id()));
    }

    @Override
    @GetMapping(path = "/{sectorId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> findSector(@PathVariable String sectorId) {
        return resultHttpMapper.ok(dispatcher.ask(new FindSectorByIdQuery(sectorId)).map(SectorDetailResponse::from));
    }

    @Override
    @PutMapping(
        path = "/{sectorId}",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> updateSector(@PathVariable String sectorId, @RequestBody SectorRequest body) {
        Result<SectorId> updated = dispatcher.dispatch(new UpdateSectorCommand(sectorId, body.name(), body.description()));
        return resultHttpMapper.ok(detailOf(updated).map(SectorDetailResponse::from));
    }

    @Override
    @PostMapping(path = "/{sectorId}/deactivation", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> deactivateSector(@PathVariable String sectorId) {
        Result<SectorId> deactivated = dispatcher.dispatch(new DeactivateSectorCommand(sectorId));
        return resultHttpMapper.ok(detailOf(deactivated).map(SectorDetailResponse::from));
    }

    @Override
    @PostMapping(path = "/{sectorId}/reactivation", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> reactivateSector(@PathVariable String sectorId) {
        Result<SectorId> reactivated = dispatcher.dispatch(new ReactivateSectorCommand(sectorId));
        return resultHttpMapper.ok(detailOf(reactivated).map(SectorDetailResponse::from));
    }

    private Result<SectorDetail> detailOf(Result<SectorId> written) {
        return written.flatMap(id -> dispatcher.ask(new FindSectorByIdQuery(id.toString())));
    }
}
