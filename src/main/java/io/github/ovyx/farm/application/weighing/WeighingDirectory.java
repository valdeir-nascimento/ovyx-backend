package io.github.ovyx.farm.application.weighing;

import io.github.ovyx.farm.domain.model.CageId;
import io.github.ovyx.farm.domain.model.SectorId;
import io.github.ovyx.farm.domain.model.WeighingId;
import java.util.List;
import java.util.Optional;

/**
 * A leitura das pesagens, direto do banco (principio V; R-008 da 005): o acompanhamento nao passa pelo
 * agregado.
 */
public interface WeighingDirectory {

    /** O setor, ativo ou inativo, ou nenhum, se nao existe. */
    Optional<WeighedSector> sectorOf(SectorId sectorId);

    /** A gaiola do setor, ativa ou inativa, ou nenhuma, se nao existe ou e de outro setor. */
    Optional<WeighedCage> cageOf(SectorId sectorId, CageId cageId);

    /** As pesagens validas da gaiola, em qualquer ordem; as anuladas ficam de fora. */
    List<WeighingEntry> weighingsOf(CageId cageId);

    /** A pesagem valida da gaiola, ou nenhuma: a anulada e a de outra gaiola ficam de fora. */
    Optional<WeighingDetail> findWeighing(CageId cageId, WeighingId weighingId);
}
