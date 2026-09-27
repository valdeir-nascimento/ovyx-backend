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
 * Reativa um setor e exatamente as gaiolas que a inativacao dele levou (FR-015). Recusa quando outro
 * setor ativo tomou o nome no meio tempo (FR-016).
 */
public class ReactivateSectorCommandHandler implements CommandHandler<ReactivateSectorCommand, SectorId> {

    private final SectorRepository sectorRepository;
    private final Clock clock;

    public ReactivateSectorCommandHandler(SectorRepository sectorRepository, Clock clock) {
        this.sectorRepository = sectorRepository;
        this.clock = clock;
    }

    @Override
    public Result<SectorId> handle(ReactivateSectorCommand command) {
        Optional<Sector> found = SectorId.parse(command.sectorId()).flatMap(sectorRepository::findById);
        if (found.isEmpty()) {
            return Result.failure(FarmRefusals.sectorNotFound());
        }

        Sector sector = found.get();
        boolean changed;
        try {
            changed = sector.reactivate(sectorRepository, clock);
        } catch (DomainException refusal) {
            return Result.failure(FarmRefusals.from(refusal));
        }

        // O que ja estava ativo nao muda, e gravar de novo so subiria a versao.
        if (changed) {
            sectorRepository.save(sector);
        }
        return Result.success(sector.id());
    }
}
