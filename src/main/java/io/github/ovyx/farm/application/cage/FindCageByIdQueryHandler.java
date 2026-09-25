package io.github.ovyx.farm.application.cage;

import io.github.ovyx.farm.application.common.FarmRefusals;
import io.github.ovyx.farm.domain.model.CageId;
import io.github.ovyx.farm.domain.model.SectorId;
import io.github.ovyx.shared.application.QueryHandler;
import io.github.ovyx.shared.application.Result;

import java.util.Optional;

/**
 * Consulta uma gaiola do setor, ativa ou inativa.
 *
 * <p>Setor que nao existe e "setor nao encontrado"; gaiola que nao existe, que e de outro setor ou com
 * identificador malformado e "gaiola nao encontrada" — a mesma resposta nos tres casos.
 */
public class FindCageByIdQueryHandler implements QueryHandler<FindCageByIdQuery, CageDetail> {

    private final CageDirectory cageDirectory;

    public FindCageByIdQueryHandler(CageDirectory cageDirectory) {
        this.cageDirectory = cageDirectory;
    }

    @Override
    public Result<CageDetail> handle(FindCageByIdQuery query) {
        Optional<SectorId> sectorId = SectorId.parse(query.sectorId()).filter(cageDirectory::sectorExists);
        if (sectorId.isEmpty()) {
            return Result.failure(FarmRefusals.sectorNotFound());
        }
        return CageId.parse(query.cageId())
            .flatMap(cageId -> cageDirectory.findDetail(sectorId.get(), cageId))
            .map(Result::success)
            .orElseGet(() -> Result.failure(FarmRefusals.cageNotFound()));
    }
}
