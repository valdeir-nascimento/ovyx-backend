package io.github.ovyx.farm.presentation.formula;

import io.github.ovyx.farm.application.common.StatusFilter;
import io.github.ovyx.farm.application.formula.DeactivateFeedFormulaCommand;
import io.github.ovyx.farm.application.formula.FeedFormulaSummary;
import io.github.ovyx.farm.application.formula.FindFeedFormulaQuery;
import io.github.ovyx.farm.application.formula.ListFeedFormulasQuery;
import io.github.ovyx.farm.application.formula.ReactivateFeedFormulaCommand;
import io.github.ovyx.farm.application.formula.RegisterFeedFormulaCommand;
import io.github.ovyx.farm.application.formula.UpdateFeedFormulaCommand;
import io.github.ovyx.farm.domain.model.FeedFormulaId;
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
@RequestMapping("/api/v1/feed-formulas")
public class FeedFormulaController implements FeedFormulaApi {

    private final Dispatcher dispatcher;
    private final ResultHttpMapper resultHttpMapper;

    public FeedFormulaController(Dispatcher dispatcher, ResultHttpMapper resultHttpMapper) {
        this.dispatcher = dispatcher;
        this.resultHttpMapper = resultHttpMapper;
    }

    @Override
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> listFeedFormulas(@RequestParam(required = false) StatusFilter status) {
        return resultHttpMapper.ok(dispatcher
                .ask(new ListFeedFormulasQuery(status))
                .map(formulas -> formulas.stream().map(FeedFormulaResponse::from).toList()));
    }

    @Override
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> registerFeedFormula(@RequestBody FeedFormulaRequest body) {
        // Sem @Valid: o corpo nao tem anotacoes, e todas as violacoes vem do dominio, de uma vez.
        Result<FeedFormulaId> registered = dispatcher.dispatch(new RegisterFeedFormulaCommand(
                body.name(), body.rawPricePerKg(), body.rawExpectedIntake(), body.description()));
        return resultHttpMapper.created(
                formulaOf(registered).map(FeedFormulaResponse::from),
                formula -> URI.create("/api/v1/feed-formulas/" + formula.id()));
    }

    @Override
    @GetMapping(path = "/{formulaId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> findFeedFormula(@PathVariable String formulaId) {
        return resultHttpMapper.ok(
                dispatcher.ask(new FindFeedFormulaQuery(formulaId)).map(FeedFormulaResponse::from));
    }

    @Override
    @PutMapping(
            path = "/{formulaId}",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> updateFeedFormula(
            @PathVariable String formulaId, @RequestBody FeedFormulaRequest body) {
        Result<FeedFormulaId> updated = dispatcher.dispatch(new UpdateFeedFormulaCommand(
                formulaId, body.name(), body.rawPricePerKg(), body.rawExpectedIntake(), body.description()));
        return resultHttpMapper.ok(formulaOf(updated).map(FeedFormulaResponse::from));
    }

    @Override
    @PostMapping(path = "/{formulaId}/deactivation", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> deactivateFeedFormula(@PathVariable String formulaId) {
        Result<FeedFormulaId> deactivated = dispatcher.dispatch(new DeactivateFeedFormulaCommand(formulaId));
        return resultHttpMapper.ok(formulaOf(deactivated).map(FeedFormulaResponse::from));
    }

    @Override
    @PostMapping(path = "/{formulaId}/reactivation", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> reactivateFeedFormula(@PathVariable String formulaId) {
        Result<FeedFormulaId> reactivated = dispatcher.dispatch(new ReactivateFeedFormulaCommand(formulaId));
        return resultHttpMapper.ok(formulaOf(reactivated).map(FeedFormulaResponse::from));
    }

    private Result<FeedFormulaSummary> formulaOf(Result<FeedFormulaId> written) {
        return written.flatMap(id -> dispatcher.ask(new FindFeedFormulaQuery(id.value().toString())));
    }
}
