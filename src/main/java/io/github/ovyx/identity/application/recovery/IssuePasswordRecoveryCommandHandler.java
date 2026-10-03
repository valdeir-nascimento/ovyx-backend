package io.github.ovyx.identity.application.recovery;

import io.github.ovyx.identity.domain.IdentityErrorCode;
import io.github.ovyx.identity.domain.model.AccessEvent;
import io.github.ovyx.identity.domain.model.Caretaker;
import io.github.ovyx.identity.domain.port.AccessEventRecorder;
import io.github.ovyx.identity.domain.port.CaretakerRepository;
import io.github.ovyx.identity.domain.port.RecoveryTokens;
import io.github.ovyx.identity.domain.valueobject.RecoveryToken;
import io.github.ovyx.shared.application.CommandHandler;
import io.github.ovyx.shared.application.DeferredCommands;
import io.github.ovyx.shared.application.Result;
import io.github.ovyx.shared.domain.DomainException;
import java.time.Clock;
import java.util.Optional;

/**
 * A emissao do link de recuperacao, depois da resposta ao pedido (R-003 da 012).
 *
 * <p>Procura a conta do e-mail, emite o link no agregado e grava. O e-mail sai por outro comando adiado, que so roda
 * depois que esta transacao confirmar: o link ja vale quando a pessoa o recebe, e a emissao repetida pelo despachante
 * nao manda dois e-mails.
 *
 * <p>O resultado e sempre sucesso: a resposta ja foi dada, igual para todos, e a causa real fica so na auditoria.
 */
public class IssuePasswordRecoveryCommandHandler implements CommandHandler<IssuePasswordRecoveryCommand, Void> {

    private final CaretakerRepository caretakerRepository;
    private final RecoveryTokens recoveryTokens;
    private final AccessEventRecorder accessEventRecorder;
    private final DeferredCommands deferred;
    private final Clock clock;

    public IssuePasswordRecoveryCommandHandler(
            CaretakerRepository caretakerRepository,
            RecoveryTokens recoveryTokens,
            AccessEventRecorder accessEventRecorder,
            DeferredCommands deferred,
            Clock clock) {
        this.caretakerRepository = caretakerRepository;
        this.recoveryTokens = recoveryTokens;
        this.accessEventRecorder = accessEventRecorder;
        this.deferred = deferred;
        this.clock = clock;
    }

    @Override
    public Result<Void> handle(IssuePasswordRecoveryCommand command) {
        String email = command.email().value();
        Optional<Caretaker> found = caretakerRepository.findByEmailOrMobilePhone(email);
        if (found.isEmpty()) {
            accessEventRecorder.record(AccessEvent.recoveryUnknownEmail(email, null, command.origin(), clock));
            return Result.success(null);
        }

        Caretaker caretaker = found.get();
        RecoveryToken token = recoveryTokens.issue();
        try {
            caretaker.issuePasswordRecovery(token, clock);
        } catch (DomainException refusal) {
            // O agregado recusa o inativo e o quarto link da hora; a recusa nao volta a ninguem, so fica na auditoria.
            accessEventRecorder.record(
                    refusal.errorCode() == IdentityErrorCode.RECOVERY_LIMIT_REACHED
                            ? AccessEvent.recoveryLimited(email, caretaker.id(), command.origin(), clock)
                            : AccessEvent.recoveryInactive(email, caretaker.id(), command.origin(), clock));
            return Result.success(null);
        }
        caretakerRepository.save(caretaker);
        deferred.submit(new SendRecoveryLinkCommand(caretaker.id(), token, command.origin()));
        return Result.success(null);
    }
}
