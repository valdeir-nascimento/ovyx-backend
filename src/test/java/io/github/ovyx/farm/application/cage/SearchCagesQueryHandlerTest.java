package io.github.ovyx.farm.application.cage;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.ovyx.farm.application.common.StatusFilter;
import io.github.ovyx.farm.domain.model.SectorId;
import io.github.ovyx.shared.application.ErrorType;
import io.github.ovyx.shared.application.PageResponse;
import io.github.ovyx.shared.application.Result;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Pesquisa das gaiolas de um setor, com busca pelo código, filtros e páginas (FR-010). */
@DisplayName("SearchCagesQueryHandler")
class SearchCagesQueryHandlerTest {

    private final SectorId sectorId = SectorId.generate();
    private final RecordingCageDirectory directory = new RecordingCageDirectory().withSector(sectorId);
    private final SearchCagesQueryHandler handler = new SearchCagesQueryHandler(directory);

    @Test
    @DisplayName("asks for the active cages when no status is asked, with the battery in capitals")
    void givenNoStatusAndBatteryInLowerCase_whenSearching_thenAskForTheActiveCagesOfTheBatteryInCapitals() {
        // given
        SearchCagesQuery query = new SearchCagesQuery(sectorId.toString(), " b-0 ", " b ", null, 0, 20);

        // when
        Result<PageResponse<CageSummary>> result = handler.handle(query);

        // then
        assertThat(result.isSuccess()).isTrue();
        assertThat(directory.searches())
                .containsExactly(new RecordingCageDirectory.Search(sectorId, "b-0", "B", StatusFilter.ACTIVE, 0, 20));
    }

    @Test
    @DisplayName("leaves a blank code and a blank battery out of the search")
    void givenBlankCodeAndBattery_whenSearching_thenSearchWithoutThem() {
        // given
        SearchCagesQuery query = new SearchCagesQuery(sectorId.toString(), "  ", "", StatusFilter.ALL, 1, 10);

        // when
        handler.handle(query);

        // then
        assertThat(directory.searches())
                .containsExactly(new RecordingCageDirectory.Search(sectorId, null, null, StatusFilter.ALL, 1, 10));
    }

    @Test
    @DisplayName("refuses a negative page and a size outside 1 to 100 at once")
    void givenNegativePageAndSizeAbove100_whenSearching_thenFailAsValidationWithBoth() {
        // given
        SearchCagesQuery query = new SearchCagesQuery(sectorId.toString(), null, null, null, -1, 101);

        // when
        Result<PageResponse<CageSummary>> result = handler.handle(query);

        // then
        assertThat(result.error().type()).isEqualTo(ErrorType.VALIDATION);
        assertThat(result.error().code()).isEqualTo("VALIDATION_FAILED");
        assertThat(result.error().message()).isEqualTo("A requisição contém campos inválidos.");
        assertThat(result.error().details())
                .containsExactly(
                        Map.entry("page", "A página começa em 0."),
                        Map.entry("size", "O tamanho da página deve estar entre 1 e 100."));
        assertThat(directory.searches()).isEmpty();
    }

    @Test
    @DisplayName("fails as not found for a sector that does not exist or a malformed identifier")
    void givenUnknownSector_whenSearching_thenFailAsSectorNotFound() {
        // given
        SearchCagesQuery unknown = new SearchCagesQuery(SectorId.generate().toString(), null, null, null, 0, 20);
        SearchCagesQuery malformed = new SearchCagesQuery("galpao-9", null, null, null, 0, 20);

        // when
        Result<PageResponse<CageSummary>> unknownResult = handler.handle(unknown);
        Result<PageResponse<CageSummary>> malformedResult = handler.handle(malformed);

        // then
        assertThat(unknownResult.error().code()).isEqualTo("SECTOR_NOT_FOUND");
        assertThat(malformedResult.error().code()).isEqualTo("SECTOR_NOT_FOUND");
        assertThat(directory.searches()).isEmpty();
    }
}
