package io.github.ovyx.farm.application.formula;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.ovyx.farm.application.common.StatusFilter;
import io.github.ovyx.farm.domain.model.FeedFormulaId;
import io.github.ovyx.farm.domain.model.Status;
import io.github.ovyx.shared.application.Result;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/** Lista de fórmulas, com o custo por ave ao dia (US1 da 004; FR-005). */
@DisplayName("ListFeedFormulasQueryHandler")
class ListFeedFormulasQueryHandlerTest {

    private final RecordingFeedFormulaDirectory directory = new RecordingFeedFormulaDirectory();
    private final ListFeedFormulasQueryHandler handler = new ListFeedFormulasQueryHandler(directory);

    @Test
    @DisplayName("lists only the active formulas when no status is asked")
    void givenNoStatus_whenListing_thenAskForTheActiveFormulas() {
        // given
        FeedFormulaSummary posturaPlus = new FeedFormulaSummary(
                FeedFormulaId.generate(),
                "Postura Plus",
                null,
                new BigDecimal("2.85"),
                28,
                new BigDecimal("0.080"),
                Status.ACTIVE,
                Instant.parse("2026-09-20T10:15:00Z"),
                Instant.parse("2026-09-24T17:40:12Z"));
        directory.listing(posturaPlus);

        // when
        Result<List<FeedFormulaSummary>> result = handler.handle(new ListFeedFormulasQuery(null));

        // then
        assertThat(result.value()).containsExactly(posturaPlus);
        assertThat(directory.askedFilters()).containsExactly(StatusFilter.ACTIVE);
    }

    @ParameterizedTest
    @EnumSource(StatusFilter.class)
    @DisplayName("passes the asked status on")
    void givenStatus_whenListing_thenAskForThatStatus(StatusFilter status) {
        // given
        ListFeedFormulasQuery query = new ListFeedFormulasQuery(status);

        // when
        handler.handle(query);

        // then
        assertThat(directory.askedFilters()).containsExactly(status);
    }
}
