package io.github.ovyx.farm.application.weighing;

import io.github.ovyx.farm.application.common.FarmRefusals;
import io.github.ovyx.farm.domain.model.WeighingId;
import io.github.ovyx.farm.domain.port.SectorRepository;
import io.github.ovyx.farm.domain.port.WeighingRepository;
import io.github.ovyx.shared.application.CommandHandler;
import io.github.ovyx.shared.application.Result;
import io.github.ovyx.shared.domain.DomainException;
import java.time.Clock;

/**
 * Exclui a pesagem de uma gaiola, de qualquer responsavel, anulando-a: ela fica guardada com quem a anulou e
 * quando (US4 da 005; FR-006, FR-015). Anular de novo a ja anulada responde sucesso, sem gravar.
 */
public class VoidWeighingCommandHandler implements CommandHandler<VoidWeighingCommand, WeighingId> {

    private final SectorRepository sectorRepository;
    private final WeighingRepository weighingRepository;
    private final Clock clock;

    public VoidWeighingCommandHandler(
            SectorRepository sectorRepository, WeighingRepository weighingRepository, Clock clock) {
        this.sectorRepository = sectorRepository;
        this.weighingRepository = weighingRepository;
        this.clock = clock;
    }

    @Override
    public Result<WeighingId> handle(VoidWeighingCommand command) {
        WeighingLookup.Found found = WeighingLookup.find(
                sectorRepository, weighingRepository, command.sectorId(), command.cageId(), command.weighingId());
        if (found.refused()) {
            return Result.failure(found.refusal());
        }

        boolean changed;
        try {
            changed = found.weighing().voidBy(found.sector(), command.actor(), clock.instant());
        } catch (DomainException refusal) {
            return Result.failure(FarmRefusals.from(refusal));
        }

        if (changed) {
            weighingRepository.save(found.weighing());
        }
        return Result.success(found.weighing().id());
    }
}
