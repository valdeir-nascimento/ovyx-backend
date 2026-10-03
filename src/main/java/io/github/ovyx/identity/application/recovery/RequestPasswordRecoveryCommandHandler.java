package io.github.ovyx.identity.application.recovery;

import io.github.ovyx.identity.domain.IdentityErrorCode;
import io.github.ovyx.identity.domain.model.AccessEvent;
import io.github.ovyx.identity.domain.port.AccessEventRecorder;
import io.github.ovyx.identity.domain.port.RecoveryThrottle;
import io.github.ovyx.identity.domain.valueobject.Email;
import io.github.ovyx.shared.application.ApplicationError;
import io.github.ovyx.shared.application.CommandHandler;
import io.github.ovyx.shared.application.DeferredCommands;
import io.github.ovyx.shared.application.ErrorType;
import io.github.ovyx.shared.application.Result;
import io.github.ovyx.shared.domain.DomainException;
import io.github.ovyx.shared.domain.Notification;
import java.time.Clock;

/**
 * O pedido do link de recuperacao, na requisicao (R-003 da 012).
 *
 * <p>Confere so o formato do e-mail, que nao depende de nenhuma conta, e adia todo o resto. Este tratador nao recebe
 * o repositorio de responsaveis, de proposito: sem consulta a conta, a resposta e o tempo dela nao tem como depender
 * de a conta existir, estar ativa ou ter atingido o limite (FR-002, SC-003).
 *
 * <p>A contencao por origem (FR-015) conta a tentativa antes de tudo; a origem bloqueada recebe a mesma resposta, e o
 * bloqueio fica so na auditoria.
 */
public class RequestPasswordRecoveryCommandHandler implements CommandHandler<RequestPasswordRecoveryCommand, Void> {

    private final RecoveryThrottle throttle;
    private final AccessEventRecorder accessEventRecorder;
    private final DeferredCommands deferred;
    private final Clock clock;

    public RequestPasswordRecoveryCommandHandler(
            RecoveryThrottle throttle, AccessEventRecorder accessEventRecorder, DeferredCommands deferred, Clock clock) {
        this.throttle = throttle;
        this.accessEventRecorder = accessEventRecorder;
        this.deferred = deferred;
        this.clock = clock;
    }

    @Override
    public Result<Void> handle(RequestPasswordRecoveryCommand command) {
        Email email;
        try {
            Notification notification = new Notification();
            Email.validate(command.email(), notification);
            notification.throwIfAny(IdentityErrorCode.VALIDATION_FAILED);
            email = Email.of(command.email());
        } catch (DomainException refusal) {
            return Result.failure(ApplicationError.from(refusal, ErrorType.VALIDATION));
        }

        throttle.registerAttempt(command.origin());
        if (throttle.isBlocked(command.origin())) {
            accessEventRecorder.record(
                    AccessEvent.recoveryThrottled(email.value(), null, command.origin(), clock));
            return Result.success(null);
        }
        deferred.submit(new IssuePasswordRecoveryCommand(email, command.origin()));
        return Result.success(null);
    }
}
