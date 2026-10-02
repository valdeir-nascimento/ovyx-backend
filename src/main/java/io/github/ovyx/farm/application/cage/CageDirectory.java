package io.github.ovyx.farm.application.cage;

import io.github.ovyx.farm.domain.model.CageId;
import io.github.ovyx.farm.domain.model.SectorId;
import io.github.ovyx.shared.application.PageResponse;

import java.util.List;
import java.util.Optional;

/**
 * Porta de leitura das gaiolas: monta os modelos de leitura sem carregar o agregado (R-006).
 */
public interface CageDirectory {

    /**
     * A agenda de pesagem do setor, ativo ou inativo: a situacao e o dia da pesagem (feature 010). Vazia quando o
     * setor nao existe, e a pesquisa nele e "setor nao encontrado".
     */
    Optional<SectorSchedule> scheduleOf(SectorId sectorId);

    /** As gaiolas do setor dos filtros, por bateria e numero, numa pagina. A situacao na agenda fica nula. */
    PageResponse<CageSummary> search(SectorId sectorId, CageFilter filter, int page, int size);

    /**
     * Todas as gaiolas do setor com os mesmos filtros e a mesma ordem da {@link #search}, sem pagina, com a
     * ultima pesagem valida de cada uma (R-009 da 007).
     */
    List<CageSummary> searchAll(SectorId sectorId, CageFilter filter);

    /**
     * A gaiola, se for deste setor.
     */
    Optional<CageDetail> findDetail(SectorId sectorId, CageId cageId);
}
