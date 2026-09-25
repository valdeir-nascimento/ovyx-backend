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
 * Cadastra uma gaiola (FR-006, FR-007).
 *
 * <p>A gaiola e escrita pelo setor, que guarda as regras entre as gaiolas dele (R-003): o tratador
 * carrega o setor, pede a ele o cadastro e grava o setor inteiro.
 */
public class RegisterCageCommandHandler implements CommandHandler<RegisterCageCommand, CageId> {

    private final SectorRepository sectorRepository;
    private final Clock clock;

    public RegisterCageCommandHandler(SectorRepository sectorRepository, Clock clock) {
        this.sectorRepository = sectorRepository;
        this.clock = clock;
    }

    @Override
    public Result<CageId> handle(RegisterCageCommand command) {
        Optional<Sector> found = SectorId.parse(command.sectorId()).flatMap(sectorRepository::findById);
        if (found.isEmpty()) {
            return Result.failure(FarmRefusals.sectorNotFound());
        }

        Sector sector = found.get();
        CageId cageId;
        try {
            cageId = sector.registerCage(command.battery(), command.number(), command.birdCount(), clock);
        } catch (DomainException refusal) {
            return Result.failure(FarmRefusals.from(refusal));
        }

        sectorRepository.save(sector);
        return Result.success(cageId);
    }
}
