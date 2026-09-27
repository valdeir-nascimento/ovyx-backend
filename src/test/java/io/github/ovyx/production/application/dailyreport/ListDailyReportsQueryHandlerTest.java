package io.github.ovyx.production.application.dailyreport;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.ovyx.production.domain.model.SectorId;
import io.github.ovyx.shared.application.ErrorType;
import io.github.ovyx.shared.application.Result;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** A lista de relatórios do setor (US1 e US4; FR-016, SC-005). */
@DisplayName("ListDailyReportsQueryHandler")
class ListDailyReportsQueryHandlerTest {

    private final RecordingDailyReportDirectory directory = new RecordingDailyReportDirectory();
    private final ListDailyReportsQueryHandler handler = new ListDailyReportsQueryHandler(directory);
    private final UUID sectorId = UUID.randomUUID();

    private void sectorKnown() {
        directory.knowSector(new ReportingSector(sectorId, "Codornas — Galpão 4", "ACTIVE"));
    }

    @Test
    @DisplayName("lists the page asked for, with the sector")
    void givenKnownSector_whenListing_thenAskForThePageAndBringTheSector() {
        // given
        sectorKnown();

        // when
        Result<DailyReportPage> result =
                handler.handle(new ListDailyReportsQuery(sectorId.toString(), null, 1, 10));

        // then
        assertThat(result.value().sector().name()).isEqualTo("Codornas — Galpão 4");
        assertThat(directory.listedSector).isEqualTo(SectorId.of(sectorId));
        assertThat(directory.listedPage).isEqualTo(1);
        assertThat(directory.listedSize).isEqualTo(10);
        assertThat(directory.listedDate).isNull();
    }

    @Test
    @DisplayName("passes the date of the filter on")
    void givenDateFilter_whenListing_thenAskForThatDate() {
        // given
        sectorKnown();

        // when
        handler.handle(new ListDailyReportsQuery(sectorId.toString(), LocalDate.of(2026, 9, 24), 0, 20));

        // then
        assertThat(directory.listedDate).isEqualTo(LocalDate.of(2026, 9, 24));
    }

    @Test
    @DisplayName("refuses a negative page and a size outside 1 to 100 at once, without listing")
    void givenNegativePageAndSizeAbove100_whenListing_thenFailAsValidationWithBoth() {
        // given
        sectorKnown();

        // when
        Result<DailyReportPage> result = handler.handle(new ListDailyReportsQuery(sectorId.toString(), null, -1, 101));

        // then
        assertThat(result.error().type()).isEqualTo(ErrorType.VALIDATION);
        assertThat(result.error().code()).isEqualTo("VALIDATION_FAILED");
        assertThat(result.error().details())
                .containsExactly(
                        Map.entry("page", "A página começa em 0."),
                        Map.entry("size", "O tamanho da página deve estar entre 1 e 100."));
        assertThat(directory.listedSector).isNull();
    }

    @ParameterizedTest(name = "size {0}")
    @ValueSource(ints = {0, 101})
    @DisplayName("fails as validation for a page size outside 1 to 100")
    void givenPageSizeOutOfRange_whenListing_thenFailAsValidation(int size) {
        // given
        sectorKnown();

        // when
        Result<DailyReportPage> result = handler.handle(new ListDailyReportsQuery(sectorId.toString(), null, 0, size));

        // then
        assertThat(result.error().type()).isEqualTo(ErrorType.VALIDATION);
        assertThat(result.error().details())
                .containsExactly(Map.entry("size", "O tamanho da página deve estar entre 1 e 100."));
    }

    @ParameterizedTest(name = "\"{0}\"")
    @ValueSource(strings = {"3f6c2b1a-8d4e-4c7f-9a2b-1e5d7c9f0a11", "galpao-9"})
    @DisplayName("fails as not found for a sector that does not exist or a malformed identifier")
    void givenUnknownOrMalformedSector_whenListing_thenFailAsSectorNotFound(String sector) {
        // given
        ListDailyReportsQuery query = new ListDailyReportsQuery(sector, null, 0, 20);

        // when
        Result<DailyReportPage> result = handler.handle(query);

        // then
        assertThat(result.error().type()).isEqualTo(ErrorType.NOT_FOUND);
        assertThat(result.error().code()).isEqualTo("SECTOR_NOT_FOUND");
    }
}
