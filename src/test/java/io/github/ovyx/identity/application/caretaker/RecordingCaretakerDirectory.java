package io.github.ovyx.identity.application.caretaker;

import io.github.ovyx.identity.domain.model.CaretakerId;
import io.github.ovyx.identity.domain.model.CaretakerStatus;
import io.github.ovyx.shared.application.PageResponse;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Dublê da porta de leitura de responsaveis, para os testes das consultas.
 *
 * <p>Grava o que a consulta pediu e devolve o que o teste preparou. A pesquisa de verdade — sem
 * distincao entre maiusculas e minusculas, paginada — e do adaptador, e esta provada contra o
 * PostgreSQL em {@code CaretakerDirectoryIT}; aqui so importa o que o tratador pede a porta.
 */
final class RecordingCaretakerDirectory implements CaretakerDirectory {

    /** O que a ultima pesquisa pediu; {@code null} enquanto ninguem pesquisou. */
    record Search(String nameFragment, CaretakerStatus status, int page, int size) {}

    private final Map<CaretakerId, CaretakerDetail> details = new LinkedHashMap<>();
    private PageResponse<CaretakerSummary> page = PageResponse.of(List.of(), 0, 20, 0);
    private Search lastSearch;

    RecordingCaretakerDirectory answering(PageResponse<CaretakerSummary> page) {
        this.page = page;
        return this;
    }

    RecordingCaretakerDirectory holding(CaretakerDetail detail) {
        details.put(detail.id(), detail);
        return this;
    }

    Search lastSearch() {
        return lastSearch;
    }

    @Override
    public PageResponse<CaretakerSummary> search(String nameFragment, CaretakerStatus status, int page, int size) {
        lastSearch = new Search(nameFragment, status, page, size);
        return this.page;
    }

    @Override
    public Optional<CaretakerDetail> findDetail(CaretakerId id) {
        return Optional.ofNullable(details.get(id));
    }
}
