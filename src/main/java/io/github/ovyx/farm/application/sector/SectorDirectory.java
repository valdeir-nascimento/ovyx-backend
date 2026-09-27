package io.github.ovyx.farm.application.sector;

import io.github.ovyx.farm.application.common.StatusFilter;
import io.github.ovyx.farm.domain.model.SectorId;

import java.util.List;
import java.util.Optional;

/**
 * Porta de leitura dos setores: monta os modelos de leitura sem carregar o agregado (R-006).
 *
 * <p>Fica na aplicacao, e nao no dominio, porque serve as consultas, e nao as regras.
 */
public interface SectorDirectory {

    /**
     * Os setores da situacao pedida, por nome, sem distinguir maiusculas.
     */
    List<SectorSummary> list(StatusFilter status);

    Optional<SectorDetail> findDetail(SectorId id);
}
