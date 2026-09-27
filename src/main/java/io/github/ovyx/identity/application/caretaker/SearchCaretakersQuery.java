package io.github.ovyx.identity.application.caretaker;

import io.github.ovyx.identity.domain.model.CaretakerStatus;
import io.github.ovyx.shared.application.PageResponse;
import io.github.ovyx.shared.application.Query;

/**
 * Pesquisa de responsaveis por trecho do nome (FR-014, cenario 5 da Historia 2).
 *
 * @param name trecho do nome como foi digitado; vazio ou ausente, nao filtra
 * @param status situacao; ausente, traz as duas
 */
public record SearchCaretakersQuery(String name, CaretakerStatus status, int page, int size)
        implements Query<PageResponse<CaretakerSummary>> {}
