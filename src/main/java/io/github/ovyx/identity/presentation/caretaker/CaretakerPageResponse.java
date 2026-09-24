package io.github.ovyx.identity.presentation.caretaker;

import io.github.ovyx.identity.application.caretaker.CaretakerSummary;
import io.github.ovyx.shared.application.PageResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * Pagina de responsaveis, no formato do contrato: os metadados ao lado do conteudo, e nao aninhados.
 */
@Schema(description = "Página de responsáveis")
public record CaretakerPageResponse(
    List<CaretakerSummaryResponse> content,
    @Schema(example = "0") int page,
    @Schema(example = "20") int size,
    @Schema(description = "Quantos responsáveis atendem à pesquisa, em todas as páginas", example = "2")
    long totalElements,
    @Schema(example = "1") int totalPages) {

    public static CaretakerPageResponse from(PageResponse<CaretakerSummary> page) {
        return new CaretakerPageResponse(
            page.content().stream().map(CaretakerSummaryResponse::from).toList(),
            page.metadata().page(),
            page.metadata().size(),
            page.metadata().totalElements(),
            page.metadata().totalPages());
    }
}
