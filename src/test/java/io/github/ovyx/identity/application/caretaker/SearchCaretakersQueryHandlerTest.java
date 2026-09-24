package io.github.ovyx.identity.application.caretaker;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.ovyx.identity.domain.IdentityErrorCode;
import io.github.ovyx.identity.domain.model.CaretakerId;
import io.github.ovyx.identity.domain.model.CaretakerStatus;
import io.github.ovyx.identity.domain.model.Role;
import io.github.ovyx.shared.application.ErrorType;
import io.github.ovyx.shared.application.PageResponse;
import io.github.ovyx.shared.application.Result;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Testes da pesquisa de responsaveis (FR-014, cenario 5 da Historia 2).
 *
 * <p>O tratador valida a pagina pedida e normaliza o trecho do nome; a pesquisa em si e do
 * adaptador, e esta provada contra o PostgreSQL em {@code CaretakerDirectoryIT}.
 */
@DisplayName("SearchCaretakersQueryHandler")
class SearchCaretakersQueryHandlerTest {

    private static final CaretakerSummary JOAO = new CaretakerSummary(
            CaretakerId.generate(),
            "João Pereira de Souza",
            "joao.pereira@ovyx.com.br",
            "91991234567",
            Role.USER,
            CaretakerStatus.ACTIVE);

    private final RecordingCaretakerDirectory directory =
            new RecordingCaretakerDirectory().answering(PageResponse.of(List.of(JOAO), 0, 20, 1));
    private final SearchCaretakersQueryHandler handler = new SearchCaretakersQueryHandler(directory);

    @Test
    @DisplayName("returns the page the directory finds")
    void givenValidQuery_whenSearching_thenReturnTheDirectoryPage() {
        // given
        SearchCaretakersQuery query = new SearchCaretakersQuery("pereira", CaretakerStatus.ACTIVE, 0, 20);

        // when
        Result<PageResponse<CaretakerSummary>> result = handler.handle(query);

        // then
        assertThat(result.value().content()).containsExactly(JOAO);
        assertThat(directory.lastSearch())
                .isEqualTo(new RecordingCaretakerDirectory.Search("pereira", CaretakerStatus.ACTIVE, 0, 20));
    }

    @Test
    @DisplayName("searches by the trimmed name fragment")
    void givenNameWithSurroundingSpaces_whenSearching_thenSearchByTheTrimmedFragment() {
        // given
        SearchCaretakersQuery query = new SearchCaretakersQuery("  Pereira ", null, 0, 20);

        // when
        handler.handle(query);

        // then
        assertThat(directory.lastSearch().nameFragment()).isEqualTo("Pereira");
    }

    @ParameterizedTest(name = "name [{0}]")
    @NullSource
    @ValueSource(strings = {"", "   "})
    @DisplayName("a missing or blank name means no name filter")
    void givenMissingOrBlankName_whenSearching_thenSearchWithoutNameFilter(String name) {
        // given
        SearchCaretakersQuery query = new SearchCaretakersQuery(name, null, 0, 20);

        // when
        handler.handle(query);

        // then
        assertThat(directory.lastSearch().nameFragment()).isNull();
    }

    @ParameterizedTest(name = "page {0}, size {1} -> {2}")
    @CsvSource({"-1, 20, page", "0, 0, size", "0, 101, size"})
    @DisplayName("fails as a validation failure for a page outside the contract, without searching")
    void givenPageOutsideTheContract_whenSearching_thenFailAsValidationWithoutSearching(
            int page, int size, String field) {
        // given
        SearchCaretakersQuery query = new SearchCaretakersQuery(null, null, page, size);

        // when
        Result<PageResponse<CaretakerSummary>> result = handler.handle(query);

        // then
        assertThat(result.error().type()).isEqualTo(ErrorType.VALIDATION);
        assertThat(result.error().code()).isEqualTo(IdentityErrorCode.VALIDATION_FAILED.code());
        assertThat(result.error().details()).containsOnlyKeys(field);
        assertThat(directory.lastSearch()).isNull();
    }

    @Test
    @DisplayName("reports the page and the size together when both are wrong")
    void givenPageAndSizeBothOutsideTheContract_whenSearching_thenReportBothTogether() {
        // given
        SearchCaretakersQuery query = new SearchCaretakersQuery(null, null, -1, 0);

        // when
        Result<PageResponse<CaretakerSummary>> result = handler.handle(query);

        // then
        assertThat(result.error().details().keySet()).containsExactly("page", "size");
    }
}
