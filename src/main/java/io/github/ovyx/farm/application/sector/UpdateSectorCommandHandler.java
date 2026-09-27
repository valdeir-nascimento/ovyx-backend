package io.github.ovyx.farm.application.sector;

import io.github.ovyx.farm.application.common.FarmRefusals;
import io.github.ovyx.farm.domain.model.Sector;
import io.github.ovyx.farm.domain.model.SectorId;
import io.github.ovyx.farm.domain.port.SectorRepository;
import io.github.ovyx.shared.application.CommandHandler;
import io.github.ovyx.shared.application.Result;
import io.github.ovyx.shared.domain.DomainException;

import java.time.Clock;
import java.util.Optional;

/**
 * Edita o nome e a descricao de um setor (FR-003).
 */
public class UpdateSectorCommandHandler implements CommandHandler<UpdateSectorCommand, SectorId> {

    private final SectorRepository sectorRepository;
    private final Clock clock;

    public UpdateSectorCommandHandler(SectorRepository sectorRepository, Clock clock) {
        this.sectorRepository = sectorRepository;
        this.clock = clock;
    }

    @Override
    public Result<SectorId> handle(UpdateSectorCommand command) {
        Optional<Sector> found = SectorId.parse(command.sectorId()).flatMap(sectorRepository::findById);
        if (found.isEmpty()) {
            return Result.failure(FarmRefusals.sectorNotFound());
        }

        Sector sector = found.get();
        try {
            sector.update(command.name(), command.description(), sectorRepository, clock);
        } catch (DomainException refusal) {
            return Result.failure(FarmRefusals.from(refusal));
        }

        sectorRepository.save(sector);
        return Result.success(sector.id());
    }
}
