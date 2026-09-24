package io.github.ovyx.identity.presentation.caretaker;

import io.github.ovyx.identity.application.caretaker.CaretakerDetail;
import io.github.ovyx.identity.application.caretaker.DeactivateCaretakerCommand;
import io.github.ovyx.identity.application.caretaker.FindCaretakerByIdQuery;
import io.github.ovyx.identity.application.caretaker.RegisterCaretakerCommand;
import io.github.ovyx.identity.application.caretaker.SearchCaretakersQuery;
import io.github.ovyx.identity.application.caretaker.UpdateCaretakerCommand;
import io.github.ovyx.identity.domain.model.CaretakerId;
import io.github.ovyx.identity.domain.model.CaretakerStatus;
import io.github.ovyx.shared.application.Dispatcher;
import io.github.ovyx.shared.application.Result;
import io.github.ovyx.shared.presentation.ResultHttpMapper;
import java.net.URI;
import java.util.UUID;
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
 * Rotas da administracao de responsaveis. So rotas e traducao do {@code Result}: a documentacao
 * fica em {@link CaretakerApi}, e a autorizacao, na configuracao de seguranca.
 *
 * <p>Toda rota declara {@code produces}, e as que recebem corpo, {@code consumes}: assim a
 * negociacao recusa um formato indisponivel antes de o caso de uso rodar. Sem isso, o 406 vinha
 * depois do efeito — o responsavel era cadastrado e o cliente recebia erro, como aconteceu na
 * entrada (T252).
 *
 * <p>Os comandos devolvem so o identificador (principio V); o corpo da resposta vem da consulta,
 * feita em seguida.
 */
@RestController
@RequestMapping("/api/v1/caretakers")
public class CaretakerController implements CaretakerApi {

    private final Dispatcher dispatcher;
    private final ResultHttpMapper resultHttpMapper;

    public CaretakerController(Dispatcher dispatcher, ResultHttpMapper resultHttpMapper) {
        this.dispatcher = dispatcher;
        this.resultHttpMapper = resultHttpMapper;
    }

    @Override
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> register(@RequestBody RegisterCaretakerRequest body) {
        // Sem @Valid: o corpo nao tem anotacoes, e todas as violacoes vem do dominio, de uma vez.
        Result<CaretakerId> registered = dispatcher.dispatch(new RegisterCaretakerCommand(
                body.fullName(), body.cpf(), body.email(), body.mobilePhone(), body.password(), body.role()));
        return resultHttpMapper.created(
                detailOf(registered).map(CaretakerDetailResponse::from),
                detail -> URI.create("/api/v1/caretakers/" + detail.id()));
    }

    @Override
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> search(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) CaretakerStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return resultHttpMapper.ok(dispatcher
                .ask(new SearchCaretakersQuery(name, status, page, size))
                .map(CaretakerPageResponse::from));
    }

    @Override
    @GetMapping(path = "/{caretakerId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> find(@PathVariable UUID caretakerId) {
        return resultHttpMapper.ok(dispatcher
                .ask(new FindCaretakerByIdQuery(CaretakerId.of(caretakerId)))
                .map(CaretakerDetailResponse::from));
    }

    @Override
    @PutMapping(
            path = "/{caretakerId}",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> update(@PathVariable UUID caretakerId, @RequestBody UpdateCaretakerRequest body) {
        Result<CaretakerId> updated = dispatcher.dispatch(new UpdateCaretakerCommand(
                CaretakerId.of(caretakerId),
                body.fullName(),
                body.cpf(),
                body.email(),
                body.mobilePhone(),
                body.role()));
        return resultHttpMapper.ok(detailOf(updated).map(CaretakerDetailResponse::from));
    }

    @Override
    @PostMapping(path = "/{caretakerId}/deactivation", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> deactivate(@PathVariable UUID caretakerId) {
        Result<CaretakerId> deactivated =
                dispatcher.dispatch(new DeactivateCaretakerCommand(CaretakerId.of(caretakerId)));
        return resultHttpMapper.ok(detailOf(deactivated).map(CaretakerDetailResponse::from));
    }

    private Result<CaretakerDetail> detailOf(Result<CaretakerId> written) {
        return written.flatMap(id -> dispatcher.ask(new FindCaretakerByIdQuery(id)));
    }
}
