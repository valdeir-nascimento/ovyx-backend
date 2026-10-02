package io.github.ovyx.farm.application.cage;

import java.util.List;
import java.time.LocalDate;
import io.github.ovyx.shared.application.FarmCalendar;
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
 *
 * <p>Cada gaiola ativa de setor ativo sai com a situacao na agenda de pesagem, e o filtro "Pesagem pendente" vira
 * a data a partir da qual uma pesagem conta para a semana, a mesma da situacao (feature 010).
 */
public class SearchCagesQueryHandler implements QueryHandler<SearchCagesQuery, PageResponse<CageSummary>> {

    private static final int MAXIMUM_SIZE = 100;

    private final CageDirectory cageDirectory;
    private final FarmCalendar calendar;

    public SearchCagesQueryHandler(CageDirectory cageDirectory, FarmCalendar calendar) {
        this.cageDirectory = cageDirectory;
        this.calendar = calendar;
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

        Optional<SectorId> sectorId = SectorId.parse(query.sectorId());
        Optional<SectorSchedule> schedule = sectorId.flatMap(cageDirectory::scheduleOf);
        if (schedule.isEmpty()) {
            return Result.failure(FarmRefusals.sectorNotFound());
        }
        LocalDate today = calendar.today();
        boolean pending = query.weighing() == WeighingFilter.PENDING;
        if (pending && !schedule.get().isActive()) {
            // Setor inativo nao tem gaiola a pesar.
            return Result.success(PageResponse.of(List.of(), query.page(), query.size(), 0));
        }

        String battery = blankToNull(query.battery());
        return Result.success(cageDirectory.search(
                sectorId.get(),
                new CageFilter(
                        blankToNull(query.code()),
                        battery == null ? null : battery.toUpperCase(Locale.ROOT),
                        query.status() == null ? StatusFilter.ACTIVE : query.status(),
                        pending ? schedule.get().schedule().requiredSince(today) : null),
                query.page(),
                query.size())
                .map(cage -> schedule.get().standingOf(cage, today)));
    }

    private static String blankToNull(String text) {
        return text == null || text.isBlank() ? null : text.strip();
    }
}
