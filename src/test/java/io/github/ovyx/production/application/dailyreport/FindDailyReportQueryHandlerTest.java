package io.github.ovyx.production.application.dailyreport;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.ovyx.production.domain.model.Actor;
import io.github.ovyx.production.domain.model.DailyReportId;
import io.github.ovyx.production.domain.model.MortalityStatus;
import io.github.ovyx.production.domain.model.ProductionStatus;
import io.github.ovyx.production.domain.model.SectorId;
import io.github.ovyx.shared.application.ErrorType;
import io.github.ovyx.shared.application.Result;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** O detalhe do relatório (US1 a US4; FR-009, FR-014). */
@DisplayName("FindDailyReportQueryHandler")
class FindDailyReportQueryHandlerTest {

    private final RecordingDailyReportDirectory directory = new RecordingDailyReportDirectory();
    private final FindDailyReportQueryHandler handler = new FindDailyReportQueryHandler(directory);
    private final UUID sectorId = UUID.randomUUID();
    private final UUID reportId = UUID.randomUUID();

    private DailyReportDetail detailOf(UUID sector) {
        return new DailyReportDetail(
                reportId,
                new ReportingSector(sector, "Codornas — Galpão 4", "ACTIVE"),
                LocalDate.of(2026, 9, 24),
                LocalTime.of(6, 30),
                98,
                20,
                null,
                false,
                new Actor(UUID.randomUUID(), "Marina Alves"),
                Instant.parse("2026-09-24T09:31:40Z"),
                null,
                null,
                new ProductionTotals(ProductionStatus.PENDING, 2, 0, 0, 0, BigDecimal.ZERO),
                new MortalityTotals(MortalityStatus.PENDING, 0, 0, BigDecimal.ZERO, 98),
                List.of());
    }

    @Test
    @DisplayName("finds the report of the sector")
    void givenReportOfTheSector_whenFinding_thenReturnIt() {
        // given
        directory.knowSector(new ReportingSector(sectorId, "Codornas — Galpão 4", "ACTIVE"));
        directory.knowDetail(detailOf(sectorId));

        // when
        Result<DailyReportDetail> result =
                handler.handle(new FindDailyReportQuery(sectorId.toString(), reportId.toString()));

        // then
        assertThat(result.value().id()).isEqualTo(reportId);
        assertThat(directory.detailSector).isEqualTo(SectorId.of(sectorId));
        assertThat(directory.detailReport).isEqualTo(DailyReportId.of(reportId));
    }

    @ParameterizedTest(name = "\"{0}\"")
    @ValueSource(strings = {"3f6c2b1a-8d4e-4c7f-9a2b-1e5d7c9f0a11", "galpao-9"})
    @DisplayName("fails as sector not found for a sector that does not exist or a malformed identifier")
    void givenUnknownOrMalformedSector_whenFinding_thenFailAsSectorNotFound(String sector) {
        // given
        FindDailyReportQuery query = new FindDailyReportQuery(sector, reportId.toString());

        // when
        Result<DailyReportDetail> result = handler.handle(query);

        // then
        assertThat(result.error().type()).isEqualTo(ErrorType.NOT_FOUND);
        assertThat(result.error().code()).isEqualTo("SECTOR_NOT_FOUND");
    }

    @ParameterizedTest(name = "\"{0}\"")
    @ValueSource(strings = {"6b1d3f5a-7c9e-4a2b-8d4f-1e3a5c7b9d55", "24-09"})
    @DisplayName("fails as report not found for a report that does not exist or a malformed identifier")
    void givenUnknownOrMalformedReport_whenFinding_thenFailAsReportNotFound(String report) {
        // given
        directory.knowSector(new ReportingSector(sectorId, "Codornas — Galpão 4", "ACTIVE"));

        // when
        Result<DailyReportDetail> result = handler.handle(new FindDailyReportQuery(sectorId.toString(), report));

        // then
        assertThat(result.error().type()).isEqualTo(ErrorType.NOT_FOUND);
        assertThat(result.error().code()).isEqualTo("DAILY_REPORT_NOT_FOUND");
    }

    @Test
    @DisplayName("fails as report not found for a report of another sector")
    void givenReportOfAnotherSector_whenFinding_thenFailAsReportNotFound() {
        // given
        UUID otherSector = UUID.randomUUID();
        directory.knowSector(new ReportingSector(sectorId, "Codornas — Galpão 4", "ACTIVE"));
        directory.knowDetail(detailOf(otherSector));

        // when
        Result<DailyReportDetail> result =
                handler.handle(new FindDailyReportQuery(sectorId.toString(), reportId.toString()));

        // then
        assertThat(result.error().code()).isEqualTo("DAILY_REPORT_NOT_FOUND");
    }
}
