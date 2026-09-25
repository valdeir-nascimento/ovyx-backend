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
 * Reativacao de uma gaiola sozinha (FR-015, FR-016). A gaiola e escrita pelo setor, que guarda as regras entre as gaiolas dele (R-003).
 */
public class ReactivateCageCommandHandler implements CommandHandler<ReactivateCageCommand, CageId> {

    private final SectorRepository sectorRepository;
    private final Clock clock;

    public ReactivateCageCommandHandler(SectorRepository sectorRepository, Clock clock) {
        this.sectorRepository = sectorRepository;
        this.clock = clock;
    }

    @Override
    public Result<CageId> handle(ReactivateCageCommand command) {
        Optional<Sector> found = SectorId.parse(command.sectorId()).flatMap(sectorRepository::findById);
        if (found.isEmpty()) {
            return Result.failure(FarmRefusals.sectorNotFound());
        }
        Optional<CageId> cageId = CageId.parse(command.cageId());
        if (cageId.isEmpty()) {
            return Result.failure(FarmRefusals.cageNotFound());
        }

        Sector sector = found.get();
        boolean changed;
        try {
            changed = sector.reactivateCage(cageId.get(), clock);
        } catch (DomainException refusal) {
            return Result.failure(FarmRefusals.from(refusal));
        }

        // O que ja estava na situacao pedida nao muda, e gravar de novo so subiria a versao do setor.
        if (changed) {
            sectorRepository.save(sector);
        }
        return Result.success(cageId.get());
    }
}
