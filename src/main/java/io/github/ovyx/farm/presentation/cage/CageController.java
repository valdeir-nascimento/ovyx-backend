package io.github.ovyx.farm.presentation.cage;

import io.github.ovyx.farm.application.common.StatusFilter;
import io.github.ovyx.farm.application.cage.CageDetail;
import io.github.ovyx.farm.application.cage.DeactivateCageCommand;
import io.github.ovyx.farm.application.cage.FindCageByIdQuery;
import io.github.ovyx.farm.application.cage.ReactivateCageCommand;
import io.github.ovyx.farm.application.cage.RegisterCageCommand;
import io.github.ovyx.farm.application.cage.SearchCagesQuery;
import io.github.ovyx.farm.application.cage.UpdateCageCommand;
import io.github.ovyx.farm.domain.model.CageId;
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

/**
 * Rotas das gaiolas de um setor. So rotas e traducao do {@code Result}: a documentacao fica em
 * {@link CageApi}, e a autorizacao, em {@code FarmRouteAuthorization}.
 *
 * <p>Os identificadores chegam como texto, como nos setores: o malformado e "nao encontrado", e nao 400
 * de parametro. Os comandos devolvem so o identificador (principio V); o corpo da resposta vem da
 * consulta, feita em seguida.
 */
@RestController
@RequestMapping("/api/v1/sectors/{sectorId}/cages")
public class CageController implements CageApi {

    private final Dispatcher dispatcher;
    private final ResultHttpMapper resultHttpMapper;

    public CageController(Dispatcher dispatcher, ResultHttpMapper resultHttpMapper) {
        this.dispatcher = dispatcher;
        this.resultHttpMapper = resultHttpMapper;
    }

    @Override
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> searchCages(
            @PathVariable String sectorId,
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String battery,
            @RequestParam(required = false) StatusFilter status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return resultHttpMapper.ok(dispatcher
                .ask(new SearchCagesQuery(sectorId, code, battery, status, page, size))
                .map(CagePageResponse::from));
    }

    @Override
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> registerCage(@PathVariable String sectorId, @RequestBody CageRequest body) {
        // Sem @Valid: o corpo nao tem anotacoes, e todas as violacoes vem do dominio, de uma vez.
        Result<CageId> registered = dispatcher.dispatch(
                new RegisterCageCommand(sectorId, body.battery(), body.rawNumber(), body.rawBirdCount()));
        return resultHttpMapper.created(
                detailOf(sectorId, registered).map(CageDetailResponse::from),
                detail -> URI.create("/api/v1/sectors/" + detail.sectorId() + "/cages/" + detail.id()));
    }

    @Override
    @GetMapping(path = "/{cageId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> findCage(@PathVariable String sectorId, @PathVariable String cageId) {
        return resultHttpMapper.ok(
                dispatcher.ask(new FindCageByIdQuery(sectorId, cageId)).map(CageDetailResponse::from));
    }

    @Override
    @PutMapping(
            path = "/{cageId}",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> updateCage(
            @PathVariable String sectorId, @PathVariable String cageId, @RequestBody CageRequest body) {
        Result<CageId> updated = dispatcher.dispatch(new UpdateCageCommand(
                sectorId, cageId, body.battery(), body.rawNumber(), body.rawBirdCount()));
        return resultHttpMapper.ok(detailOf(sectorId, updated).map(CageDetailResponse::from));
    }

    @Override
    @PostMapping(path = "/{cageId}/deactivation", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> deactivateCage(@PathVariable String sectorId, @PathVariable String cageId) {
        Result<CageId> deactivated = dispatcher.dispatch(new DeactivateCageCommand(sectorId, cageId));
        return resultHttpMapper.ok(detailOf(sectorId, deactivated).map(CageDetailResponse::from));
    }

    @Override
    @PostMapping(path = "/{cageId}/reactivation", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> reactivateCage(@PathVariable String sectorId, @PathVariable String cageId) {
        Result<CageId> reactivated = dispatcher.dispatch(new ReactivateCageCommand(sectorId, cageId));
        return resultHttpMapper.ok(detailOf(sectorId, reactivated).map(CageDetailResponse::from));
    }

    private Result<CageDetail> detailOf(String sectorId, Result<CageId> written) {
        return written.flatMap(id -> dispatcher.ask(new FindCageByIdQuery(sectorId, id.toString())));
    }
}
