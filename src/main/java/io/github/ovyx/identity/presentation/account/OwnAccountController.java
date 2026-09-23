package io.github.ovyx.identity.presentation.account;

import io.github.ovyx.identity.application.account.ChangeOwnPasswordCommand;
import io.github.ovyx.identity.domain.IdentityErrorCode;
import io.github.ovyx.identity.domain.model.CaretakerId;
import io.github.ovyx.identity.presentation.security.SessionAuthenticator;
import io.github.ovyx.shared.application.Dispatcher;
import io.github.ovyx.shared.application.Result;
import io.github.ovyx.shared.presentation.AuthenticatedUser;
import io.github.ovyx.shared.presentation.ResultHttpMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me")
public class OwnAccountController implements OwnAccountApi {

    private final Dispatcher dispatcher;
    private final SessionAuthenticator sessionAuthenticator;
    private final ResultHttpMapper resultHttpMapper;

    public OwnAccountController(Dispatcher dispatcher, SessionAuthenticator sessionAuthenticator, ResultHttpMapper resultHttpMapper) {
        this.dispatcher = dispatcher;
        this.sessionAuthenticator = sessionAuthenticator;
        this.resultHttpMapper = resultHttpMapper;
    }

    @Override
    @PutMapping(path = "/password", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> changeOwnPassword(@RequestBody ChangePasswordRequest body, HttpServletRequest request, HttpServletResponse response) {
        // Sem @Valid: o corpo nao tem anotacoes, e as regras dos dois campos vem todas do dominio,
        // de uma vez so (ver ChangePasswordRequest).
        AuthenticatedUser user = SessionAuthenticator.currentUser();

        Result<CaretakerId> result = dispatcher.dispatch(new ChangeOwnPasswordCommand(
            CaretakerId.of(user.id()),
            body.currentPassword(),
            body.newPassword())
        );

        if (!result.isSuccess()) {
            if (IdentityErrorCode.CARETAKER_UNAVAILABLE.code().equals(result.error().code())) {
                // Mesma regra do /me: a sessao sobreviveu ao responsavel e e encerrada, nao usada.
                sessionAuthenticator.invalidate(request);
            }
            return resultHttpMapper.problem(result.error());
        }

        // Encerra a obrigacao de trocar sem obrigar a pessoa a entrar de novo.
        sessionAuthenticator.refresh(user.withPasswordChanged(), request, response);

        return ResponseEntity.noContent().build();
    }
}
