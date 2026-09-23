package io.github.ovyx.identity.application.authentication;

import io.github.ovyx.identity.domain.model.AccessEvent;
import io.github.ovyx.identity.domain.model.CaretakerId;
import io.github.ovyx.identity.domain.port.AccessEventRecorder;
import io.github.ovyx.identity.domain.port.CaretakerRepository;
import io.github.ovyx.identity.domain.IdentityErrorCode;
import io.github.ovyx.shared.application.ApplicationError;
import io.github.ovyx.shared.application.CommandHandler;
import io.github.ovyx.shared.application.ErrorType;
import io.github.ovyx.shared.application.Result;

import java.time.Clock;
import java.util.Map;

/**
 * Encerramento de sessao (FR-004).
 *
 * <p>A destruicao da sessao em si cabe a apresentacao, que conhece o HTTP, e acontece mesmo que
 * este caso de uso falhe: sair nao pode ficar refem da auditoria. Aqui fica o registro do evento.
 */
public class SignOutCommandHandler implements CommandHandler<SignOutCommand, CaretakerId> {

    private final CaretakerRepository caretakerRepository;
    private final AccessEventRecorder accessEventRecorder;
    private final Clock clock;

    public SignOutCommandHandler(CaretakerRepository caretakerRepository, AccessEventRecorder accessEventRecorder, Clock clock) {
        this.caretakerRepository = caretakerRepository;
        this.accessEventRecorder = accessEventRecorder;
        this.clock = clock;
    }

    @Override
    public Result<CaretakerId> handle(SignOutCommand command) {
        CaretakerId caretakerId = command.caretakerId();

        if (caretakerId == null) {
            return Result.failure(new ApplicationError(
                ErrorType.VALIDATION,
                IdentityErrorCode.VALIDATION_FAILED.code(),
                "Não há responsável autenticado para encerrar a sessão.",
                Map.of("caretaker", "Não há responsável autenticado para encerrar a sessão.")));
        }

        // No encerramento nao ha identificador digitado. Registra-se o e-mail, como nos demais
        // eventos, para que a auditoria seja consultada de um jeito so. Se o cadastro nao for mais
        // encontrado, fica a identidade: sair nao pode depender de o responsavel ainda existir.
        String identifier = caretakerRepository
            .findById(caretakerId)
            .map(caretaker -> caretaker.email().value())
            .orElseGet(caretakerId::toString);

        accessEventRecorder.record(AccessEvent.signedOut(identifier, caretakerId, command.origin(), clock));

        return Result.success(caretakerId);
    }
}
