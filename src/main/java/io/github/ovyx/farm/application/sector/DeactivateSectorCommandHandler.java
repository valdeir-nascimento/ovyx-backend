package io.github.ovyx.farm.application.sector;

import io.github.ovyx.farm.application.common.FarmRefusals;
import io.github.ovyx.farm.domain.model.Sector;
import io.github.ovyx.farm.domain.model.SectorId;
import io.github.ovyx.farm.domain.port.SectorRepository;
import io.github.ovyx.shared.application.CommandHandler;
import io.github.ovyx.shared.application.Result;

import java.time.Clock;
import java.util.Optional;

/**
 * Inativa um setor e, junto, as gaiolas ativas dele, numa operacao so (FR-015). Nada e apagado
 * (FR-012), e a inativacao nao tem recusa: o setor inativo so continua inativo, e nao e gravado de
 * novo — gravar sem mudar subiria a versao e faria uma escrita simultanea legitima se repetir a toa.
 */
public class DeactivateSectorCommandHandler implements CommandHandler<DeactivateSectorCommand, SectorId> {

    private final SectorRepository sectorRepository;
    private final Clock clock;

    public DeactivateSectorCommandHandler(SectorRepository sectorRepository, Clock clock) {
        this.sectorRepository = sectorRepository;
        this.clock = clock;
    }

    @Override
    public Result<SectorId> handle(DeactivateSectorCommand command) {
        Optional<Sector> found = SectorId.parse(command.sectorId()).flatMap(sectorRepository::findById);
        if (found.isEmpty()) {
            return Result.failure(FarmRefusals.sectorNotFound());
        }

        Sector sector = found.get();
        if (sector.deactivate(clock)) {
            sectorRepository.save(sector);
        }
        return Result.success(sector.id());
    }
}
