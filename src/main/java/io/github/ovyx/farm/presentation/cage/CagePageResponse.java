package io.github.ovyx.farm.presentation.cage;

import io.github.ovyx.farm.application.cage.CageSummary;
import io.github.ovyx.shared.application.PageResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/** Uma pagina da pesquisa de gaiolas, no formato do contrato: os totais ao lado do conteudo. */
@Schema(description = "Uma página da pesquisa de gaiolas, com os totais de todas as páginas")
public record CagePageResponse(
        List<CageSummaryResponse> content,
        @Schema(example = "0") int page,
        @Schema(example = "20") int size,
        @Schema(description = "Quantas gaiolas atendem à pesquisa, em todas as páginas", example = "48")
        long totalElements,
        @Schema(example = "3") int totalPages) {

    public static CagePageResponse from(PageResponse<CageSummary> page) {
        return new CagePageResponse(
                page.content().stream().map(CageSummaryResponse::from).toList(),
                page.metadata().page(),
                page.metadata().size(),
                page.metadata().totalElements(),
                page.metadata().totalPages());
    }
}
