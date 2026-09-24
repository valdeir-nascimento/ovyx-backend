package io.github.ovyx.identity.application.caretaker;

import io.github.ovyx.identity.domain.IdentityErrorCode;
import io.github.ovyx.shared.application.ApplicationError;
import io.github.ovyx.shared.application.ErrorType;
import io.github.ovyx.shared.application.PageResponse;
import io.github.ovyx.shared.application.QueryHandler;
import io.github.ovyx.shared.application.Result;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Pesquisa responsaveis por trecho do nome (FR-014, cenario 5 da Historia 2).
 *
 * <p>Valida a pagina pedida contra o contrato — pagina a partir de 0, tamanho de 1 a 100 — e
 * devolve as duas violacoes juntas, se houver. O trecho do nome chega aparado ao adaptador, e
 * vazio vira "sem filtro".
 */
public class SearchCaretakersQueryHandler
        implements QueryHandler<SearchCaretakersQuery, PageResponse<CaretakerSummary>> {

    private static final int MAXIMUM_SIZE = 100;

    private final CaretakerDirectory caretakerDirectory;

    public SearchCaretakersQueryHandler(CaretakerDirectory caretakerDirectory) {
        this.caretakerDirectory = caretakerDirectory;
    }

    @Override
    public Result<PageResponse<CaretakerSummary>> handle(SearchCaretakersQuery query) {
        Map<String, String> violations = new LinkedHashMap<>();
        if (query.page() < 0) {
            violations.put("page", "A página começa em 0.");
        }
        if (query.size() < 1 || query.size() > MAXIMUM_SIZE) {
            violations.put("size", "O tamanho da página deve estar entre 1 e 100.");
        }
        if (!violations.isEmpty()) {
            return Result.failure(new ApplicationError(
                    ErrorType.VALIDATION,
                    IdentityErrorCode.VALIDATION_FAILED.code(),
                    "A requisição contém campos inválidos.",
                    violations));
        }

        String fragment =
                query.name() == null || query.name().isBlank() ? null : query.name().trim();
        return Result.success(caretakerDirectory.search(fragment, query.status(), query.page(), query.size()));
    }
}
