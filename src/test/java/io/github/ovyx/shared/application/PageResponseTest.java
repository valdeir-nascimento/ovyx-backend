package io.github.ovyx.shared.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Testes da pagina devolvida por uma consulta.
 *
 * <p>A conta do total de paginas e o classico erro de um a mais: com 21 itens e paginas de 10, sao
 * tres paginas, e nao duas — a terceira tem um item so, e sem ela a interface esconde um registro.
 */
@DisplayName("PageResponse")
class PageResponseTest {

    @ParameterizedTest(name = "given {0} items in pages of {1} then there are {2} pages")
    @CsvSource({"0, 10, 0", "1, 10, 1", "10, 10, 1", "11, 10, 2", "21, 10, 3"})
    @DisplayName("Counts the pages the filter produces, never hiding the last one")
    void givenTotalOfItems_whenBuildingThePage_thenCountEveryPageIncludingAPartialLastOne(
            long totalElements, int size, int expectedPages) {
        // given
        List<String> content = List.of("primeiro");

        // when
        PageResponse<String> page = PageResponse.of(content, 0, size, totalElements);

        // then
        assertThat(page.metadata().totalPages()).isEqualTo(expectedPages);
    }

    @Test
    @DisplayName("Keeps the filter total, not the size of the current page")
    void givenPageSmallerThanTheTotal_whenBuildingThePage_thenKeepTheTotalOfTheFilter() {
        // given
        List<String> content = List.of("primeiro", "segundo");

        // when
        PageResponse<String> page = PageResponse.of(content, 1, 2, 7);

        // then
        assertThat(page.metadata().totalElements()).isEqualTo(7);
        assertThat(page.metadata().page()).isEqualTo(1);
    }

    @Test
    @DisplayName("Converts the items without losing the pagination metadata")
    void givenPageOfDomainItems_whenMappingToResponses_thenKeepTheSameMetadata() {
        // given
        PageResponse<String> page = PageResponse.of(List.of("maria", "joão"), 0, 2, 5);

        // when
        PageResponse<Integer> mapped = page.map(String::length);

        // then
        assertThat(mapped.content()).containsExactly(5, 4);
        assertThat(mapped.metadata()).isEqualTo(page.metadata());
    }

    @Test
    @DisplayName("Answers zero pages when the page size is zero, instead of dividing by it")
    void givenPageSizeOfZero_whenBuildingThePage_thenAnswerZeroPages() {
        // given
        List<String> content = List.of();

        // when
        PageResponse<String> page = PageResponse.of(content, 0, 0, 12);

        // then
        assertThat(page.metadata().totalPages()).isZero();
    }
}
