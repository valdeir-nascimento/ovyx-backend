package io.github.ovyx.farm.application.weighing;

import io.github.ovyx.farm.application.common.FarmRefusals;
import io.github.ovyx.farm.domain.model.CageId;
import io.github.ovyx.farm.domain.model.SectorId;
import io.github.ovyx.shared.application.QueryHandler;
import io.github.ovyx.shared.application.Result;
import java.util.Optional;

/**
 * O acompanhamento do peso de uma gaiola, ativa ou inativa (US1 e US3 da 005; FR-009 a FR-013). E leitura: nao
 * passa pelo agregado (principio V).
 */
public class GetWeighingOverviewQueryHandler implements QueryHandler<GetWeighingOverviewQuery, WeighingOverview> {

    private final WeighingDirectory directory;

    public GetWeighingOverviewQueryHandler(WeighingDirectory directory) {
        this.directory = directory;
    }

    @Override
    public Result<WeighingOverview> handle(GetWeighingOverviewQuery query) {
        Optional<SectorId> sectorId = SectorId.parse(query.sectorId());
        Optional<WeighedSector> sector = sectorId.flatMap(directory::sectorOf);
        if (sector.isEmpty()) {
            return Result.failure(FarmRefusals.sectorNotFound());
        }
        Optional<WeighedCage> cage = CageId.parse(query.cageId()).flatMap(id -> directory.cageOf(sectorId.get(), id));
        if (cage.isEmpty()) {
            return Result.failure(FarmRefusals.cageNotFound());
        }
        return Result.success(WeighingOverview.of(cage.get(), sector.get(), directory.weighingsOf(cage.get().id())));
    }
}
