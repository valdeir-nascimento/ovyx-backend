package io.github.ovyx.farm.application.weighing;

import io.github.ovyx.farm.application.common.FarmRefusals;
import io.github.ovyx.farm.domain.model.CageId;
import io.github.ovyx.farm.domain.model.Sector;
import io.github.ovyx.farm.domain.model.SectorId;
import io.github.ovyx.farm.domain.model.Weighing;
import io.github.ovyx.farm.domain.model.WeighingId;
import io.github.ovyx.farm.domain.port.SectorRepository;
import io.github.ovyx.farm.domain.port.WeighingRepository;
import io.github.ovyx.shared.application.CommandHandler;
import io.github.ovyx.shared.application.FarmCalendar;
import io.github.ovyx.shared.application.Result;
import io.github.ovyx.shared.domain.DomainException;
import java.time.Clock;
import java.util.Optional;

/**
 * Registra a pesagem de uma gaiola, de qualquer responsavel (US1 da 005; FR-003, FR-004, FR-007, FR-008).
 *
 * <p>O setor e lido para o agregado saber se a gaiola e dele e se os dois estao ativos, e nunca e gravado:
 * cada transacao modifica um agregado so (principio II).
 */
public class RecordWeighingCommandHandler implements CommandHandler<RecordWeighingCommand, WeighingId> {

    private final SectorRepository sectorRepository;
    private final WeighingRepository weighingRepository;
    private final FarmCalendar calendar;
    private final Clock clock;

    public RecordWeighingCommandHandler(
        SectorRepository sectorRepository,
        WeighingRepository weighingRepository,
        FarmCalendar calendar,
        Clock clock
    ) {
        this.sectorRepository = sectorRepository;
        this.weighingRepository = weighingRepository;
        this.calendar = calendar;
        this.clock = clock;
    }

    @Override
    public Result<WeighingId> handle(RecordWeighingCommand command) {
        Optional<Sector> sector = SectorId.parse(command.sectorId()).flatMap(sectorRepository::findById);
        if (sector.isEmpty()) {
            return Result.failure(FarmRefusals.sectorNotFound());
        }
        Optional<CageId> cageId = CageId.parse(command.cageId());
        if (cageId.isEmpty()) {
            return Result.failure(FarmRefusals.cageNotFound());
        }

        Weighing weighing;
        try {
            weighing = Weighing.record(
                sector.get(),
                cageId.get(),
                command.weighedOn(),
                command.averageWeight(),
                calendar.today(),
                weighingRepository,
                command.actor(),
                clock.instant()
            );
        } catch (DomainException refusal) {
            return Result.failure(FarmRefusals.from(refusal));
        }

        weighingRepository.save(weighing);
        return Result.success(weighing.id());
    }
}
