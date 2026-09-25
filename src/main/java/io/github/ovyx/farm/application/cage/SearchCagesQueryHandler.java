package io.github.ovyx.farm.application.cage;

import io.github.ovyx.farm.application.common.FarmRefusals;
import io.github.ovyx.farm.application.common.StatusFilter;
import io.github.ovyx.farm.domain.FarmErrorCode;
import io.github.ovyx.farm.domain.model.SectorId;
import io.github.ovyx.shared.application.ApplicationError;
import io.github.ovyx.shared.application.ErrorType;
import io.github.ovyx.shared.application.PageResponse;
import io.github.ovyx.shared.application.QueryHandler;
import io.github.ovyx.shared.application.Result;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Pesquisa as gaiolas de um setor, com busca pelo codigo, filtros e paginas (FR-010).
 *
 * <p>Valida a pagina pedida contra o contrato — pagina a partir de 0, tamanho de 1 a 100 — e devolve
 * as duas violacoes juntas, com as mesmas mensagens da pesquisa de responsaveis. O trecho do codigo e a
 * bateria chegam aparados ao adaptador, a bateria em maiusculas, e vazios viram "sem filtro".
 */
public class SearchCagesQueryHandler implements QueryHandler<SearchCagesQuery, PageResponse<CageSummary>> {

    private static final int MAXIMUM_SIZE = 100;

    private final CageDirectory cageDirectory;

    public SearchCagesQueryHandler(CageDirectory cageDirectory) {
        this.cageDirectory = cageDirectory;
    }

    @Override
    public Result<PageResponse<CageSummary>> handle(SearchCagesQuery query) {
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
                    FarmErrorCode.VALIDATION_FAILED.code(),
                    "A requisição contém campos inválidos.",
                    violations));
        }

        Optional<SectorId> sectorId = SectorId.parse(query.sectorId()).filter(cageDirectory::sectorExists);
        if (sectorId.isEmpty()) {
            return Result.failure(FarmRefusals.sectorNotFound());
        }

        String battery = blankToNull(query.battery());
        return Result.success(cageDirectory.search(
                sectorId.get(),
                blankToNull(query.code()),
                battery == null ? null : battery.toUpperCase(Locale.ROOT),
                query.status() == null ? StatusFilter.ACTIVE : query.status(),
                query.page(),
                query.size()));
    }

    private static String blankToNull(String text) {
        return text == null || text.isBlank() ? null : text.strip();
    }
}
