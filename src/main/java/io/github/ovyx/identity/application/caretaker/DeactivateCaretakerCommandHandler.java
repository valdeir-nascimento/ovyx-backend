package io.github.ovyx.identity.application.caretaker;

import io.github.ovyx.identity.domain.model.Caretaker;
import io.github.ovyx.identity.domain.model.CaretakerId;
import io.github.ovyx.identity.domain.port.CaretakerRepository;
import io.github.ovyx.shared.application.CommandHandler;
import io.github.ovyx.shared.application.Result;
import io.github.ovyx.shared.domain.DomainException;
import java.time.Clock;
import java.util.Optional;

/**
 * Inativa um responsavel (FR-014, FR-018, FR-019).
 *
 * <p>Nao remove nada: a linha fica, e o historico continua consultavel.
 */
public class DeactivateCaretakerCommandHandler implements CommandHandler<DeactivateCaretakerCommand, CaretakerId> {

    private final CaretakerRepository caretakerRepository;
    private final Clock clock;

    public DeactivateCaretakerCommandHandler(CaretakerRepository caretakerRepository, Clock clock) {
        this.caretakerRepository = caretakerRepository;
        this.clock = clock;
    }

    @Override
    public Result<CaretakerId> handle(DeactivateCaretakerCommand command) {
        Optional<Caretaker> found = caretakerRepository.findById(command.caretakerId());
        if (found.isEmpty()) {
            return Result.failure(CaretakerRefusals.notFound());
        }

        Caretaker caretaker = found.get();
        try {
            caretaker.deactivate(caretakerRepository, clock);
        } catch (DomainException refusal) {
            return Result.failure(CaretakerRefusals.from(refusal));
        }

        caretakerRepository.save(caretaker);
        return Result.success(caretaker.id());
    }
}
