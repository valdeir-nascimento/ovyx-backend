package io.github.ovyx.farm.application.sector;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.ovyx.farm.application.common.StatusFilter;
import io.github.ovyx.farm.domain.model.SectorId;
import io.github.ovyx.farm.domain.model.Status;
import io.github.ovyx.shared.application.Result;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/** Lista de setores com os totais (FR-004, FR-005). */
@DisplayName("ListSectorsQueryHandler")
class ListSectorsQueryHandlerTest {

    private final RecordingSectorDirectory directory = new RecordingSectorDirectory();
    private final ListSectorsQueryHandler handler = new ListSectorsQueryHandler(directory);

    @Test
    @DisplayName("lists only the active sectors when no status is asked")
    void givenNoStatus_whenListing_thenAskForTheActiveSectors() {
        // given
        SectorSummary galpao = new SectorSummary(
                SectorId.generate(), "Codornas — Galpão 1", null, Status.ACTIVE, 48, 2400);
        directory.listing(galpao);

        // when
        Result<List<SectorSummary>> result = handler.handle(new ListSectorsQuery(null));

        // then
        assertThat(result.value()).containsExactly(galpao);
        assertThat(directory.askedFilters()).containsExactly(StatusFilter.ACTIVE);
    }

    @ParameterizedTest
    @EnumSource(StatusFilter.class)
    @DisplayName("passes the asked status on")
    void givenStatus_whenListing_thenAskForThatStatus(StatusFilter status) {
        // given
        ListSectorsQuery query = new ListSectorsQuery(status);

        // when
        handler.handle(query);

        // then
        assertThat(directory.askedFilters()).containsExactly(status);
    }
}
