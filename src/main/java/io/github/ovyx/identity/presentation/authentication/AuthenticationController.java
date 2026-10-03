package io.github.ovyx.identity.presentation.authentication;

import io.github.ovyx.identity.application.authentication.AuthenticatedCaretaker;
import io.github.ovyx.identity.application.authentication.GetAuthenticatedCaretakerQuery;
import io.github.ovyx.identity.application.authentication.SignInCommand;
import io.github.ovyx.identity.application.authentication.SignOutCommand;
import io.github.ovyx.identity.application.authentication.SignedIn;
import io.github.ovyx.identity.domain.IdentityErrorCode;
import io.github.ovyx.identity.domain.model.CaretakerId;
import io.github.ovyx.identity.presentation.security.RequestOrigin;
import io.github.ovyx.identity.presentation.security.SessionAuthenticator;
import io.github.ovyx.shared.application.ApplicationError;
import io.github.ovyx.shared.application.Dispatcher;
import io.github.ovyx.shared.application.ErrorType;
import io.github.ovyx.shared.application.Result;
import io.github.ovyx.shared.presentation.AuthenticatedUser;
import io.github.ovyx.shared.presentation.ResultHttpMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class AuthenticationController implements AuthenticationApi {

    private static final Logger log = LoggerFactory.getLogger(AuthenticationController.class);


    private final Dispatcher dispatcher;
    private final SessionAuthenticator sessionAuthenticator;
    private final ResultHttpMapper resultHttpMapper;

    public AuthenticationController(
        Dispatcher dispatcher, SessionAuthenticator sessionAuthenticator, ResultHttpMapper resultHttpMapper) {
        this.dispatcher = dispatcher;
        this.sessionAuthenticator = sessionAuthenticator;
        this.resultHttpMapper = resultHttpMapper;
    }

    @Override
    // O produces faz a negociacao recusar o formato de resposta antes de o tratador rodar. Sem ele, o
    // 406 vinha depois da entrada concedida: sessao aberta, acesso auditado, e o cliente recebendo erro.
    @PostMapping(
            path = "/auth/sign-in",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> signIn(
        @Valid @RequestBody SignInRequest body, HttpServletRequest request, HttpServletResponse response) {

        Result<SignedIn> signedIn =
            dispatcher.dispatch(new SignInCommand(body.identifier(), body.password(), originOf(request)));

        if (!signedIn.isSuccess()) {
            // A resposta de falha e montada aqui, sempre igual, sem repassar o erro do tratador:
            // defesa em profundidade para FR-002.
            return invalidCredentials();
        }

        // O comando devolve so a identidade; nome e perfil vem da consulta (principio V).
        // A geracao conferida na entrada vai na consulta: se a senha foi redefinida no intervalo, a consulta recusa e a
        // sessao nao abre com a geracao nova (R-022 da 012).
        Result<AuthenticatedCaretaker> identity = dispatcher.ask(new GetAuthenticatedCaretakerQuery(
            signedIn.value().caretakerId(), signedIn.value().sessionGeneration()));

        if (!identity.isSuccess()) {
            // O responsavel foi inativado entre a autenticacao e a consulta. Falha fechada, e com a
            // mesma resposta das demais falhas de credencial.
            return invalidCredentials();
        }

        sessionAuthenticator.authenticate(userOf(identity.value()), request, response);

        return ResponseEntity.ok(AuthenticatedCaretakerResponse.from(identity.value()));
    }

    @Override
    @PostMapping("/auth/sign-out")
    public ResponseEntity<Object> signOut(HttpServletRequest request) {
        AuthenticatedUser user = SessionAuthenticator.currentUser();

        Result<CaretakerId> result = dispatcher.dispatch(new SignOutCommand(user == null ? null : CaretakerId.of(user.id()), originOf(request)));

        if (!result.isSuccess()) {
            // Sair nunca fica refem da auditoria: a sessao e encerrada de qualquer forma. A falha,
            // porem, e inspecionada e deixada registrada, e nao descartada em silencio.
            log.warn("Encerramento de sessão sem registro de auditoria: {}", result.error().code());
        }

        sessionAuthenticator.invalidate(request);
        return ResponseEntity.noContent().build();
    }

    @Override
    @GetMapping(path = "/auth/me", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> me(HttpServletRequest request) {
        AuthenticatedUser user = SessionAuthenticator.currentUser();

        Result<AuthenticatedCaretaker> result =
            dispatcher.ask(new GetAuthenticatedCaretakerQuery(CaretakerId.of(user.id())));

        if (!result.isSuccess()) {
            // A sessao sobreviveu ao responsavel: ele foi inativado enquanto ela estava aberta.
            // Encerrar a sessao aqui evita que ela continue valendo sem dono.
            sessionAuthenticator.invalidate(request);
            return resultHttpMapper.problem(result.error());
        }

        return ResponseEntity.ok(AuthenticatedCaretakerResponse.from(result.value()));
    }

    /**
     * Identidade da sessao, montada do modelo de leitura do responsavel autenticado.
     */
    private static AuthenticatedUser userOf(AuthenticatedCaretaker caretaker) {
        return new AuthenticatedUser(
            caretaker.id().value(),
            caretaker.fullName(),
            caretaker.role().name(),
            caretaker.mustChangePassword(),
            caretaker.sessionGeneration()
        );
    }

    private ResponseEntity<Object> invalidCredentials() {
        return resultHttpMapper.problem(ApplicationError.of(
            ErrorType.UNAUTHENTICATED,
            IdentityErrorCode.INVALID_CREDENTIALS.code(),
            "E-mail, celular ou senha inválidos."));
    }

    /** A origem da requisicao, pela regra unica do {@link RequestOrigin}. */
    private static String originOf(HttpServletRequest request) {
        return RequestOrigin.of(request);
    }
}
