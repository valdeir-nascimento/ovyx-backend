package io.github.ovyx.farm.application.sector;

import io.github.ovyx.farm.application.common.FarmRefusals;
import io.github.ovyx.farm.domain.model.SectorId;
import io.github.ovyx.shared.application.QueryHandler;
import io.github.ovyx.shared.application.Result;

/** Consulta um setor, ativo ou inativo, com os totais. */
public class FindSectorByIdQueryHandler implements QueryHandler<FindSectorByIdQuery, SectorDetail> {

    private final SectorDirectory sectorDirectory;

    public FindSectorByIdQueryHandler(SectorDirectory sectorDirectory) {
        this.sectorDirectory = sectorDirectory;
    }

    @Override
    public Result<SectorDetail> handle(FindSectorByIdQuery query) {
        return SectorId.parse(query.sectorId())
                .flatMap(sectorDirectory::findDetail)
                .map(Result::success)
                .orElseGet(() -> Result.failure(FarmRefusals.sectorNotFound()));
    }
}
