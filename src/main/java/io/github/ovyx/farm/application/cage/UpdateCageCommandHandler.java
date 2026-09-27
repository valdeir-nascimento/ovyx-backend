package io.github.ovyx.farm.application.cage;

import io.github.ovyx.farm.application.common.FarmRefusals;
import io.github.ovyx.farm.domain.model.CageId;
import io.github.ovyx.farm.domain.model.Sector;
import io.github.ovyx.farm.domain.model.SectorId;
import io.github.ovyx.farm.domain.port.SectorRepository;
import io.github.ovyx.shared.application.CommandHandler;
import io.github.ovyx.shared.application.Result;
import io.github.ovyx.shared.domain.DomainException;

import java.time.Clock;
import java.util.Optional;

/**
 * Edita a bateria, o numero e as aves de uma gaiola (FR-009).
 */
public class UpdateCageCommandHandler implements CommandHandler<UpdateCageCommand, CageId> {

    private final SectorRepository sectorRepository;
    private final Clock clock;

    public UpdateCageCommandHandler(SectorRepository sectorRepository, Clock clock) {
        this.sectorRepository = sectorRepository;
        this.clock = clock;
    }

    @Override
    public Result<CageId> handle(UpdateCageCommand command) {
        Optional<Sector> found = SectorId.parse(command.sectorId()).flatMap(sectorRepository::findById);
        if (found.isEmpty()) {
            return Result.failure(FarmRefusals.sectorNotFound());
        }
        Optional<CageId> cageId = CageId.parse(command.cageId());
        if (cageId.isEmpty()) {
            return Result.failure(FarmRefusals.cageNotFound());
        }

        Sector sector = found.get();
        try {
            sector.updateCage(cageId.get(), command.battery(), command.number(), command.birdCount(), clock);
        } catch (DomainException refusal) {
            return Result.failure(FarmRefusals.from(refusal));
        }

        sectorRepository.save(sector);
        return Result.success(cageId.get());
    }
}
