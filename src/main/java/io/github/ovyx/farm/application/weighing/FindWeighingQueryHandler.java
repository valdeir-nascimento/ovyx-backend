package io.github.ovyx.farm.application.weighing;

import io.github.ovyx.farm.application.common.FarmRefusals;
import io.github.ovyx.farm.domain.model.CageId;
import io.github.ovyx.farm.domain.model.SectorId;
import io.github.ovyx.farm.domain.model.WeighingId;
import io.github.ovyx.shared.application.QueryHandler;
import io.github.ovyx.shared.application.Result;
import java.util.Optional;

/**
 * Uma pesagem valida da gaiola (US1 e US4 da 005). A anulada, a de outra gaiola, a inexistente e o
 * identificador malformado recebem a mesma resposta: pesagem nao encontrada.
 */
public class FindWeighingQueryHandler implements QueryHandler<FindWeighingQuery, WeighingDetail> {

    private final WeighingDirectory directory;

    public FindWeighingQueryHandler(WeighingDirectory directory) {
        this.directory = directory;
    }

    @Override
    public Result<WeighingDetail> handle(FindWeighingQuery query) {
        Optional<SectorId> sectorId = SectorId.parse(query.sectorId());
        if (sectorId.flatMap(directory::sectorOf).isEmpty()) {
            return Result.failure(FarmRefusals.sectorNotFound());
        }
        Optional<CageId> cageId = CageId.parse(query.cageId()).filter(id -> directory.cageOf(sectorId.get(), id).isPresent());
        if (cageId.isEmpty()) {
            return Result.failure(FarmRefusals.cageNotFound());
        }
        return WeighingId.parse(query.weighingId())
            .flatMap(id -> directory.findWeighing(cageId.get(), id))
            .map(Result::success)
            .orElseGet(() -> Result.failure(FarmRefusals.weighingNotFound()));
    }
}
