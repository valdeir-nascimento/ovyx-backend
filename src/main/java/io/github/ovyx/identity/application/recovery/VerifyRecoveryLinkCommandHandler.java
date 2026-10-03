package io.github.ovyx.identity.application.recovery;

import io.github.ovyx.identity.domain.model.AccessEvent;
import io.github.ovyx.identity.domain.model.Caretaker;
import io.github.ovyx.identity.domain.port.AccessEventRecorder;
import io.github.ovyx.identity.domain.port.CaretakerRepository;
import io.github.ovyx.identity.domain.port.RecoveryThrottle;
import io.github.ovyx.shared.application.CommandHandler;
import io.github.ovyx.shared.application.Result;
import io.github.ovyx.shared.domain.DomainException;
import java.time.Clock;
import java.util.Optional;

/**
 * A conferencia do link de recuperacao (US2 da 012): diz se ele vale, sem gasta-lo.
 *
 * <p>O link que nao vale recebe sempre a mesma recusa; a auditoria registra a recusa, com a conta quando o link a
 * identifica. A origem que passou do limite de tentativas (FR-015) recebe a mesma recusa, ate para o link que vale; cada
 * link recusado conta uma tentativa dela.
 */
public class VerifyRecoveryLinkCommandHandler implements CommandHandler<VerifyRecoveryLinkCommand, Void> {

    private final CaretakerRepository caretakerRepository;
    private final RecoveryThrottle throttle;
    private final AccessEventRecorder accessEventRecorder;
    private final Clock clock;

    public VerifyRecoveryLinkCommandHandler(
            CaretakerRepository caretakerRepository,
            RecoveryThrottle throttle,
            AccessEventRecorder accessEventRecorder,
            Clock clock) {
        this.caretakerRepository = caretakerRepository;
        this.throttle = throttle;
        this.accessEventRecorder = accessEventRecorder;
        this.clock = clock;
    }

    @Override
    public Result<Void> handle(VerifyRecoveryLinkCommand command) {
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
        try {
            caretaker.checkRecovery(command.token(), clock);
        } catch (DomainException refusal) {
            throttle.registerAttempt(command.origin());
            accessEventRecorder.record(AccessEvent.recoveryLinkRefused(
                    caretaker.email().value(), caretaker.id(), command.origin(), clock));
            return Result.failure(RecoveryLinks.invalidLink());
        }
        return Result.success(null);
    }
}
