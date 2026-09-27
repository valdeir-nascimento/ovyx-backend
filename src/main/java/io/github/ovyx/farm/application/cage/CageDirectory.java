package io.github.ovyx.farm.application.cage;

import io.github.ovyx.farm.application.common.StatusFilter;
import io.github.ovyx.farm.domain.model.CageId;
import io.github.ovyx.farm.domain.model.SectorId;
import io.github.ovyx.shared.application.PageResponse;

import java.util.Optional;

/**
 * Porta de leitura das gaiolas: monta os modelos de leitura sem carregar o agregado (R-006).
 */
public interface CageDirectory {

    /**
     * Se o setor existe, ativo ou inativo: a pesquisa num setor que nao existe e "setor nao encontrado".
     */
    boolean sectorExists(SectorId sectorId);

    /**
     * As gaiolas do setor, por bateria e numero, numa pagina.
     *
     * @param code    trecho do codigo, sem distinguir maiusculas; {@code null} nao filtra
     * @param battery a bateria exata, em maiusculas; {@code null} nao filtra
     */
    PageResponse<CageSummary> search(SectorId sectorId, String code, String battery, StatusFilter status, int page, int size);

    /**
     * A gaiola, se for deste setor.
     */
    Optional<CageDetail> findDetail(SectorId sectorId, CageId cageId);
}
