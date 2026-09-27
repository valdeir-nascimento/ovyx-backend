package io.github.ovyx.farm.application.weighing;

import io.github.ovyx.farm.application.common.FarmRefusals;
import io.github.ovyx.farm.domain.model.CageId;
import io.github.ovyx.farm.domain.model.Sector;
import io.github.ovyx.farm.domain.model.SectorId;
import io.github.ovyx.farm.domain.model.Weighing;
import io.github.ovyx.farm.domain.model.WeighingId;
import io.github.ovyx.farm.domain.port.SectorRepository;
import io.github.ovyx.farm.domain.port.WeighingRepository;
import io.github.ovyx.shared.application.ApplicationError;
import java.util.Optional;

/**
 * O setor e a pesagem que o endereco aponta, para a correcao e a anulacao (US4 da 005): o setor que existe, a
 * gaiola que e dele e a pesagem que e dela. Cada falta tem a sua resposta, e o identificador malformado recebe
 * a mesma do inexistente.
 */
final class WeighingLookup {

    /** O que foi encontrado, ou a recusa do que faltou. */
    record Found(Sector sector, Weighing weighing, ApplicationError refusal) {

        boolean refused() {
            return refusal != null;
        }
    }

    private WeighingLookup() {}

    static Found find(
            SectorRepository sectors,
            WeighingRepository weighings,
            String rawSectorId,
            String rawCageId,
            String rawWeighingId) {
        Optional<Sector> sector = SectorId.parse(rawSectorId).flatMap(sectors::findById);
        if (sector.isEmpty()) {
            return new Found(null, null, FarmRefusals.sectorNotFound());
        }
        Optional<CageId> cageId = CageId.parse(rawCageId).filter(id -> sector.get().cage(id).isPresent());
        if (cageId.isEmpty()) {
            return new Found(null, null, FarmRefusals.cageNotFound());
        }
        return WeighingId.parse(rawWeighingId)
                .flatMap(weighings::findById)
                .filter(weighing -> weighing.cageId().equals(cageId.get()))
                .map(weighing -> new Found(sector.get(), weighing, null))
                .orElseGet(() -> new Found(null, null, FarmRefusals.weighingNotFound()));
    }
}
