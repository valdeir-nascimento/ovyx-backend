package io.github.ovyx.farm.presentation.weighing;

import io.github.ovyx.farm.application.weighing.CorrectWeighingCommand;
import io.github.ovyx.farm.application.weighing.FindWeighingQuery;
import io.github.ovyx.farm.application.weighing.GetWeighingOverviewQuery;
import io.github.ovyx.farm.application.weighing.RecordWeighingCommand;
import io.github.ovyx.farm.application.weighing.VoidWeighingCommand;
import io.github.ovyx.farm.application.weighing.WeighingDetail;
import io.github.ovyx.farm.domain.model.Actor;
import io.github.ovyx.farm.domain.model.WeighingId;
import io.github.ovyx.shared.application.Dispatcher;
import io.github.ovyx.shared.application.Result;
import io.github.ovyx.shared.presentation.AuthenticatedUser;
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
import org.springframework.web.bind.annotation.RestController;

/**
 * As pesagens de uma gaiola (feature 005). So as rotas, a leitura da entrada e a traducao do {@link Result};
 * a documentacao fica em {@link WeighingApi}.
 */
@RestController
@RequestMapping("/api/v1/sectors/{sectorId}/cages/{cageId}/weighings")
public class WeighingController implements WeighingApi {

    private final Dispatcher dispatcher;
    private final ResultHttpMapper resultHttpMapper;

    public WeighingController(Dispatcher dispatcher, ResultHttpMapper resultHttpMapper) {
        this.dispatcher = dispatcher;
        this.resultHttpMapper = resultHttpMapper;
    }

    @Override
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> getWeighingOverview(@PathVariable String sectorId, @PathVariable String cageId) {
        return resultHttpMapper.ok(
                dispatcher.ask(new GetWeighingOverviewQuery(sectorId, cageId)).map(WeighingOverviewResponse::from));
    }

    @Override
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> recordWeighing(
            @PathVariable String sectorId,
            @PathVariable String cageId,
            @RequestBody WeighingRequest body,
            AuthenticatedUser user) {
        // Sem @Valid: o corpo nao tem anotacoes, e todas as violacoes vem do dominio, de uma vez.
        Result<WeighingId> recorded = dispatcher.dispatch(new RecordWeighingCommand(
                sectorId, cageId, body.weighedOn(), body.rawAverageWeight(), actorOf(user)));
        return resultHttpMapper.created(
                weighingOf(sectorId, cageId, recorded).map(WeighingResponse::from),
                weighing -> URI.create(
                        "/api/v1/sectors/" + sectorId + "/cages/" + cageId + "/weighings/" + weighing.id()));
    }

    @Override
    @GetMapping(path = "/{weighingId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> findWeighing(
            @PathVariable String sectorId, @PathVariable String cageId, @PathVariable String weighingId) {
        return resultHttpMapper.ok(dispatcher
                .ask(new FindWeighingQuery(sectorId, cageId, weighingId))
                .map(WeighingResponse::from));
    }

    @Override
    @PutMapping(
            path = "/{weighingId}",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> correctWeighing(
            @PathVariable String sectorId,
            @PathVariable String cageId,
            @PathVariable String weighingId,
            @RequestBody WeighingRequest body,
            AuthenticatedUser user) {
        Result<WeighingId> corrected = dispatcher.dispatch(new CorrectWeighingCommand(
                sectorId, cageId, weighingId, body.weighedOn(), body.rawAverageWeight(), actorOf(user)));
        return resultHttpMapper.ok(weighingOf(sectorId, cageId, corrected).map(WeighingResponse::from));
    }

    @Override
    @PostMapping(path = "/{weighingId}/voiding")
    public ResponseEntity<Object> voidWeighing(
            @PathVariable String sectorId,
            @PathVariable String cageId,
            @PathVariable String weighingId,
            AuthenticatedUser user) {
        return resultHttpMapper.noContent(
                dispatcher.dispatch(new VoidWeighingCommand(sectorId, cageId, weighingId, actorOf(user))));
    }

    private Result<WeighingDetail> weighingOf(String sectorId, String cageId, Result<WeighingId> written) {
        return written.flatMap(id -> dispatcher.ask(new FindWeighingQuery(sectorId, cageId, id.toString())));
    }

    private static Actor actorOf(AuthenticatedUser user) {
        return new Actor(user.id(), user.fullName());
    }
}
