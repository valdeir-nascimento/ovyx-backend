package io.github.ovyx.identity.application.caretaker;

import io.github.ovyx.identity.domain.model.Caretaker;
import io.github.ovyx.identity.domain.model.CaretakerId;
import io.github.ovyx.identity.domain.port.CaretakerRepository;
import io.github.ovyx.shared.application.CommandHandler;
import io.github.ovyx.shared.application.Result;
import io.github.ovyx.shared.domain.DomainException;
import java.time.Clock;
import java.util.Optional;

/** Edita os dados cadastrais e o perfil de um responsavel (FR-014, FR-019). */
public class UpdateCaretakerCommandHandler implements CommandHandler<UpdateCaretakerCommand, CaretakerId> {

    private final CaretakerRepository caretakerRepository;
    private final Clock clock;

    public UpdateCaretakerCommandHandler(CaretakerRepository caretakerRepository, Clock clock) {
        this.caretakerRepository = caretakerRepository;
        this.clock = clock;
    }

    @Override
    public Result<CaretakerId> handle(UpdateCaretakerCommand command) {
        Optional<Caretaker> found = caretakerRepository.findById(command.caretakerId());
        if (found.isEmpty()) {
            return Result.failure(CaretakerRefusals.notFound());
        }

        Caretaker caretaker = found.get();
        try {
            caretaker.update(
                    command.fullName(),
                    command.cpf(),
                    command.email(),
                    command.mobilePhone(),
                    command.role(),
                    caretakerRepository,
                    clock);
        } catch (DomainException refusal) {
            return Result.failure(CaretakerRefusals.from(refusal));
        }

        caretakerRepository.save(caretaker);
        return Result.success(caretaker.id());
    }
}
