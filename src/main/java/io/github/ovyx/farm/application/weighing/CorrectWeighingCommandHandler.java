package io.github.ovyx.farm.application.weighing;

import io.github.ovyx.farm.application.common.FarmRefusals;
import io.github.ovyx.farm.domain.model.WeighingId;
import io.github.ovyx.farm.domain.port.SectorRepository;
import io.github.ovyx.farm.domain.port.WeighingRepository;
import io.github.ovyx.shared.application.CommandHandler;
import io.github.ovyx.shared.application.FarmCalendar;
import io.github.ovyx.shared.application.Result;
import io.github.ovyx.shared.domain.DomainException;
import java.time.Clock;

/**
 * Corrige a pesagem de uma gaiola, de qualquer responsavel (US4 da 005; FR-005, FR-007, FR-008). A anulada
 * nao se corrige: responde como nao encontrada.
 */
public class CorrectWeighingCommandHandler implements CommandHandler<CorrectWeighingCommand, WeighingId> {

    private final SectorRepository sectorRepository;
    private final WeighingRepository weighingRepository;
    private final FarmCalendar calendar;
    private final Clock clock;

    public CorrectWeighingCommandHandler(
            SectorRepository sectorRepository,
            WeighingRepository weighingRepository,
            FarmCalendar calendar,
            Clock clock) {
        this.sectorRepository = sectorRepository;
        this.weighingRepository = weighingRepository;
        this.calendar = calendar;
        this.clock = clock;
    }

    @Override
    public Result<WeighingId> handle(CorrectWeighingCommand command) {
        WeighingLookup.Found found = WeighingLookup.find(
                sectorRepository, weighingRepository, command.sectorId(), command.cageId(), command.weighingId());
        if (found.refused()) {
            return Result.failure(found.refusal());
        }

        try {
            found.weighing().correct(
                    found.sector(),
                    command.weighedOn(),
                    command.averageWeight(),
                    calendar.today(),
                    weighingRepository,
                    command.actor(),
                    clock.instant());
        } catch (DomainException refusal) {
            return Result.failure(FarmRefusals.from(refusal));
        }

        weighingRepository.save(found.weighing());
        return Result.success(found.weighing().id());
    }
}
