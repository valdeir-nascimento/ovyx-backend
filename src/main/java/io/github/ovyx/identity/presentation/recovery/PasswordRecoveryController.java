package io.github.ovyx.identity.presentation.recovery;

import io.github.ovyx.identity.application.recovery.RecoverPasswordCommand;
import io.github.ovyx.identity.application.recovery.RequestPasswordRecoveryCommand;
import io.github.ovyx.identity.application.recovery.VerifyRecoveryLinkCommand;
import io.github.ovyx.identity.domain.model.CaretakerId;
import io.github.ovyx.identity.presentation.security.RequestOrigin;
import io.github.ovyx.shared.application.Dispatcher;
import io.github.ovyx.shared.application.Result;
import io.github.ovyx.shared.presentation.ResultHttpMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * A recuperacao da senha pelo e-mail (feature 012): rotas publicas, sem sessao, com o CSRF de toda escrita.
 */
@RestController
@RequestMapping("/api/v1/auth")
public class PasswordRecoveryController implements PasswordRecoveryApi {

    private final Dispatcher dispatcher;
    private final ResultHttpMapper resultHttpMapper;

    public PasswordRecoveryController(Dispatcher dispatcher, ResultHttpMapper resultHttpMapper) {
        this.dispatcher = dispatcher;
        this.resultHttpMapper = resultHttpMapper;
    }

    @Override
    @PostMapping(path = "/password-recovery", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> requestPasswordRecovery(
            @RequestBody PasswordRecoveryRequest body, HttpServletRequest request) {
        Result<Void> result =
                dispatcher.dispatch(new RequestPasswordRecoveryCommand(body.email(), RequestOrigin.of(request)));
        if (!result.isSuccess()) {
            return resultHttpMapper.problem(result.error());
        }
        // Sempre o mesmo 202, sem corpo, qualquer que seja a conta: o trabalho acontece depois da resposta (FR-002).
        return ResponseEntity.accepted().build();
    }

    @Override
    @PostMapping(path = "/password-recovery/verification", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> verifyRecoveryLink(
            @RequestBody RecoveryLinkVerificationRequest body, HttpServletRequest request) {
        Result<Void> result =
                dispatcher.dispatch(new VerifyRecoveryLinkCommand(body.token(), RequestOrigin.of(request)));
        if (!result.isSuccess()) {
            return resultHttpMapper.problem(result.error());
        }
        return ResponseEntity.noContent().build();
    }

    @Override
    @PostMapping(path = "/password-reset", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> resetPassword(@RequestBody PasswordResetRequest body, HttpServletRequest request) {
        // A sessao que por acaso esteja no navegador nao entra aqui: a redefinicao e da conta do link (caso-limite
        // da spec), e uma sessao de outra conta segue como estava.
        Result<CaretakerId> result = dispatcher.dispatch(
                new RecoverPasswordCommand(body.token(), body.newPassword(), RequestOrigin.of(request)));
        if (!result.isSuccess()) {
            return resultHttpMapper.problem(result.error());
        }
        return ResponseEntity.noContent().build();
    }
}
