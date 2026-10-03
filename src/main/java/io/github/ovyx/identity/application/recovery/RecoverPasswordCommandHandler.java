package io.github.ovyx.identity.application.recovery;

import io.github.ovyx.identity.domain.IdentityErrorCode;
import io.github.ovyx.identity.domain.model.AccessEvent;
import io.github.ovyx.identity.domain.model.Caretaker;
import io.github.ovyx.identity.domain.model.CaretakerId;
import io.github.ovyx.identity.domain.port.AccessEventRecorder;
import io.github.ovyx.identity.domain.port.CaretakerRepository;
import io.github.ovyx.identity.domain.port.PasswordHasher;
import io.github.ovyx.identity.domain.port.RecoveryThrottle;
import io.github.ovyx.shared.application.ApplicationError;
import io.github.ovyx.shared.application.CommandHandler;
import io.github.ovyx.shared.application.DeferredCommands;
import io.github.ovyx.shared.application.ErrorType;
import io.github.ovyx.shared.application.Result;
import io.github.ovyx.shared.domain.DomainException;
import java.time.Clock;
import java.util.Optional;

/**
 * A redefinicao da senha pelo link de recuperacao (US2 da 012).
 *
 * <p>A regra e do agregado; aqui ficam a orquestracao (achar a conta do link, delegar, gravar), a auditoria e o aviso
 * por e-mail, adiado para depois da confirmacao.
 *
 * <p>Duas redefinicoes simultaneas com o mesmo link gravam a mesma linha: a versao recusa a segunda, o despachante a
 * repete numa transacao nova, e ela encontra o link ja gasto. A segunda recebe a recusa do link, e o aviso dela nunca
 * sai, porque os comandos adiados de uma transacao desfeita nao rodam.
 */
public class RecoverPasswordCommandHandler implements CommandHandler<RecoverPasswordCommand, CaretakerId> {

    private final CaretakerRepository caretakerRepository;
    private final PasswordHasher passwordHasher;
    private final RecoveryThrottle throttle;
    private final AccessEventRecorder accessEventRecorder;
    private final DeferredCommands deferred;
    private final Clock clock;

    public RecoverPasswordCommandHandler(
            CaretakerRepository caretakerRepository,
            PasswordHasher passwordHasher,
            RecoveryThrottle throttle,
            AccessEventRecorder accessEventRecorder,
            DeferredCommands deferred,
            Clock clock) {
        this.caretakerRepository = caretakerRepository;
        this.passwordHasher = passwordHasher;
        this.throttle = throttle;
        this.accessEventRecorder = accessEventRecorder;
        this.deferred = deferred;
        this.clock = clock;
    }

    @Override
    public Result<CaretakerId> handle(RecoverPasswordCommand command) {
        if (throttle.isBlocked(command.origin())) {
            accessEventRecorder.record(AccessEvent.recoveryThrottled(null, null, command.origin(), clock));
            return Result.failure(RecoveryLinks.invalidLink());
        }
        Optional<Caretaker> owner = RecoveryLinks.ownerOf(command.token(), caretakerRepository);
        if (owner.isEmpty()) {
            throttle.registerAttempt(command.origin());
            accessEventRecorder.record(AccessEvent.recoveryLinkRefused(null, null, command.origin(), clock));
            return Result.failure(RecoveryLinks.invalidLink());
        }

        Caretaker caretaker = owner.get();
        String email = caretaker.email().value();
        try {
            caretaker.recoverPassword(command.token(), command.newPassword(), passwordHasher, clock);
        } catch (DomainException refusal) {
            if (refusal.errorCode() == IdentityErrorCode.RECOVERY_LINK_INVALID) {
                throttle.registerAttempt(command.origin());
                accessEventRecorder.record(
                        AccessEvent.recoveryLinkRefused(email, caretaker.id(), command.origin(), clock));
                return Result.failure(RecoveryLinks.invalidLink());
            }
            // A politica da nova senha: recusa de preenchimento, no campo, e o link continua valendo.
            return Result.failure(ApplicationError.from(refusal, ErrorType.VALIDATION));
        }

        caretakerRepository.save(caretaker);
        accessEventRecorder.record(AccessEvent.passwordRecovered(email, caretaker.id(), command.origin(), clock));
        deferred.submit(new NotifyPasswordRecoveredCommand(caretaker.id(), clock.instant()));
        return Result.success(caretaker.id());
    }
}
