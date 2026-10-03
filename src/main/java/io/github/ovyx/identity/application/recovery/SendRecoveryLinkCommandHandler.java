package io.github.ovyx.identity.application.recovery;

import io.github.ovyx.identity.domain.model.AccessEvent;
import io.github.ovyx.identity.domain.model.Caretaker;
import io.github.ovyx.identity.domain.port.AccessEventRecorder;
import io.github.ovyx.identity.domain.port.CaretakerRepository;
import io.github.ovyx.shared.application.CommandHandler;
import io.github.ovyx.shared.application.Result;
import java.time.Clock;
import java.util.Optional;

/**
 * O envio do e-mail do link de recuperacao (US1 da 012) e o registro do resultado na auditoria.
 *
 * <p>O servidor de envio fora do ar nao muda nada para a pessoa: a resposta ja foi dada, o link continua emitido, e
 * o evento {@code RECOVERY_DELIVERY_FAILED} conta o que houve (FR-020).
 *
 * <p>So o link ainda pendente sai: se ele foi anulado (troca de senha, inativacao) ou substituido por um pedido mais
 * novo entre a emissao e o envio, o e-mail nao sai, porque traria um link que ja nao vale.
 */
public class SendRecoveryLinkCommandHandler implements CommandHandler<SendRecoveryLinkCommand, Void> {

    private final CaretakerRepository caretakerRepository;
    private final IdentityMailer mailer;
    private final AccessEventRecorder accessEventRecorder;
    private final Clock clock;

    public SendRecoveryLinkCommandHandler(
            CaretakerRepository caretakerRepository,
            IdentityMailer mailer,
            AccessEventRecorder accessEventRecorder,
            Clock clock) {
        this.caretakerRepository = caretakerRepository;
        this.mailer = mailer;
        this.accessEventRecorder = accessEventRecorder;
        this.clock = clock;
    }

    @Override
    public Result<Void> handle(SendRecoveryLinkCommand command) {
        Optional<Caretaker> found = caretakerRepository.findById(command.caretakerId());
        if (found.isEmpty() || !found.get().holdsPendingRecovery(command.token())) {
            return Result.success(null);
        }

        Caretaker caretaker = found.get();
        String email = caretaker.email().value();
        boolean accepted = mailer.sendRecoveryLink(new RecoveryLinkMail(
                email,
                caretaker.fullName().value(),
                command.token(),
                caretaker.passwordRecovery().expiresAt()));
        accessEventRecorder.record(
                accepted
                        ? AccessEvent.recoveryLinkSent(email, caretaker.id(), command.origin(), clock)
                        : AccessEvent.recoveryDeliveryFailed(email, caretaker.id(), command.origin(), clock));
        return Result.success(null);
    }
}
