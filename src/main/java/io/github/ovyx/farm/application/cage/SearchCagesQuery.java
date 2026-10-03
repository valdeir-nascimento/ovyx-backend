package io.github.ovyx.farm.application.cage;

import io.github.ovyx.farm.application.common.StatusFilter;
import io.github.ovyx.shared.application.PageResponse;
import io.github.ovyx.shared.application.Query;

/**
 * Pesquisa das gaiolas de um setor (FR-010).
 *
 * @param sectorId o identificador como veio no endereco
 * @param code trecho do codigo como foi digitado; vazio ou ausente, nao filtra
 * @param battery bateria como foi digitada; vazia ou ausente, nao filtra
 * @param status situacao; ausente, so as ativas
 * @param weighing o filtro de pesagem; ausente, nao filtra (feature 010)
 */
public record SearchCagesQuery(String sectorId, String code, String battery, StatusFilter status, int page, int size, WeighingFilter weighing)
        implements Query<PageResponse<CageSummary>> {}
