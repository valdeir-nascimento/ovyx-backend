package io.github.ovyx.identity.application.recovery;

import io.github.ovyx.identity.domain.model.Caretaker;
import io.github.ovyx.identity.domain.port.CaretakerRepository;
import io.github.ovyx.shared.application.CommandHandler;
import io.github.ovyx.shared.application.Result;
import java.util.Optional;

/**
 * O aviso por e-mail de que a senha foi redefinida (FR-012 da 012). Se o servidor de envio falhar, o carteiro ja
 * registra no log; a senha continua redefinida.
 */
public class NotifyPasswordRecoveredCommandHandler implements CommandHandler<NotifyPasswordRecoveredCommand, Void> {

    private final CaretakerRepository caretakerRepository;
    private final IdentityMailer mailer;

    public NotifyPasswordRecoveredCommandHandler(CaretakerRepository caretakerRepository, IdentityMailer mailer) {
        this.caretakerRepository = caretakerRepository;
        this.mailer = mailer;
    }

    @Override
    public Result<Void> handle(NotifyPasswordRecoveredCommand command) {
        Optional<Caretaker> found = caretakerRepository.findById(command.caretakerId());
        found.ifPresent(caretaker -> mailer.sendPasswordRecoveredNotice(new PasswordRecoveredMail(
                caretaker.email().value(), caretaker.fullName().value(), command.recoveredAt())));
        return Result.success(null);
    }
}
