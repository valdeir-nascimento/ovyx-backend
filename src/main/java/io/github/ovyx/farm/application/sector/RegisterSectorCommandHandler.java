package io.github.ovyx.farm.application.sector;

import io.github.ovyx.farm.application.common.FarmRefusals;
import io.github.ovyx.farm.domain.model.Sector;
import io.github.ovyx.farm.domain.model.SectorId;
import io.github.ovyx.farm.domain.port.SectorRepository;
import io.github.ovyx.shared.application.CommandHandler;
import io.github.ovyx.shared.application.Result;
import io.github.ovyx.shared.domain.DomainException;
import java.time.Clock;

/**
 * Cadastra um setor (FR-001, FR-002).
 *
 * <p>As regras — campos e nome unico — sao do agregado. O tratador entrega a ele o repositorio, como
 * o quadro dos demais setores, grava o resultado e traduz a recusa em {@code Failure} (principio IV).
 */
public class RegisterSectorCommandHandler implements CommandHandler<RegisterSectorCommand, SectorId> {

    private final SectorRepository sectorRepository;
    private final Clock clock;

    public RegisterSectorCommandHandler(SectorRepository sectorRepository, Clock clock) {
        this.sectorRepository = sectorRepository;
        this.clock = clock;
    }

    @Override
    public Result<SectorId> handle(RegisterSectorCommand command) {
        Sector sector;
        try {
            sector = Sector.register(command.name(), command.description(), sectorRepository, clock);
        } catch (DomainException refusal) {
            return Result.failure(FarmRefusals.from(refusal));
        }

        sectorRepository.save(sector);
        return Result.success(sector.id());
    }
}
