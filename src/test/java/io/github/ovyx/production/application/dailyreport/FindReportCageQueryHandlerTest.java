package io.github.ovyx.production.application.dailyreport;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.ovyx.production.domain.model.Actor;
import io.github.ovyx.production.domain.model.CageId;
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

/** Uma gaiola do relatório, para o diálogo de lançamento (US2 e US3). */
@DisplayName("FindReportCageQueryHandler")
class FindReportCageQueryHandlerTest {

    private final RecordingDailyReportDirectory directory = new RecordingDailyReportDirectory();
    private final FindReportCageQueryHandler handler = new FindReportCageQueryHandler(directory);
    private final UUID sectorId = UUID.randomUUID();
    private final UUID reportId = UUID.randomUUID();
    private final UUID cageId = UUID.fromString("9d2e4f6a-1b3c-4d5e-8f7a-2b4c6d8e0f44");
    private final ReportCageDetail b07 = new ReportCageDetail(
            cageId, "B-07", "B", 7, 50, new CageProduction(45, 0, 1, 1, 2, 1, 0), null);

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
                new ProductionTotals(ProductionStatus.COMPLETE, 0, 45, 40, 3, new BigDecimal("45.92")),
                new MortalityTotals(MortalityStatus.PENDING, 0, 0, BigDecimal.ZERO, 98),
                List.of(b07));
    }

    private void knowTheReport() {
        directory.knowSector(new ReportingSector(sectorId, "Codornas — Galpão 4", "ACTIVE"));
        directory.knowDetail(detailOf(sectorId));
    }

    @Test
    @DisplayName("finds the cage of the report, asking for the sector, the report and the cage of the address")
    void givenCageOfTheReport_whenFinding_thenReturnIt() {
        // given
        knowTheReport();

        // when
        Result<ReportCageDetail> result = handler.handle(
                new FindReportCageQuery(sectorId.toString(), reportId.toString(), cageId.toString()));

        // then
        assertThat(result.value()).isEqualTo(b07);
        assertThat(directory.cageSector).isEqualTo(SectorId.of(sectorId));
        assertThat(directory.cageReport).isEqualTo(DailyReportId.of(reportId));
        assertThat(directory.cageAsked).isEqualTo(CageId.of(cageId));
    }

    @ParameterizedTest(name = "\"{0}\"")
    @ValueSource(strings = {"3f6c2b1a-8d4e-4c7f-9a2b-1e5d7c9f0a11", "galpao-9"})
    @DisplayName("fails as sector not found for a sector that does not exist or a malformed identifier")
    void givenUnknownOrMalformedSector_whenFinding_thenFailAsSectorNotFound(String sector) {
        // given
        knowTheReport();

        // when
        Result<ReportCageDetail> result =
                handler.handle(new FindReportCageQuery(sector, reportId.toString(), cageId.toString()));

        // then
        assertThat(result.error().type()).isEqualTo(ErrorType.NOT_FOUND);
        assertThat(result.error().code()).isEqualTo("SECTOR_NOT_FOUND");
    }

    @ParameterizedTest(name = "\"{0}\"")
    @ValueSource(strings = {"6b1d3f5a-7c9e-4a2b-8d4f-1e3a5c7b9d55", "24-09"})
    @DisplayName("fails as report not found for a report that does not exist or a malformed identifier")
    void givenUnknownOrMalformedReport_whenFinding_thenFailAsReportNotFound(String report) {
        // given
        knowTheReport();

        // when
        Result<ReportCageDetail> result =
                handler.handle(new FindReportCageQuery(sectorId.toString(), report, cageId.toString()));

        // then
        assertThat(result.error().type()).isEqualTo(ErrorType.NOT_FOUND);
        assertThat(result.error().code()).isEqualTo("DAILY_REPORT_NOT_FOUND");
    }

    @Test
    @DisplayName("fails as report not found for a report of another sector")
    void givenReportOfAnotherSector_whenFinding_thenFailAsReportNotFound() {
        // given
        directory.knowSector(new ReportingSector(sectorId, "Codornas — Galpão 4", "ACTIVE"));
        directory.knowDetail(detailOf(UUID.randomUUID()));

        // when
        Result<ReportCageDetail> result = handler.handle(
                new FindReportCageQuery(sectorId.toString(), reportId.toString(), cageId.toString()));

        // then
        assertThat(result.error().code()).isEqualTo("DAILY_REPORT_NOT_FOUND");
    }

    @ParameterizedTest(name = "\"{0}\"")
    @ValueSource(strings = {"7c9e1a3b-5d7f-4a1c-8e3b-5d7f9a1c3e22", "B-07"})
    @DisplayName("fails as cage not found for a cage out of the report or a malformed identifier")
    void givenUnknownOrMalformedCage_whenFinding_thenFailAsCageNotFound(String cage) {
        // given
        knowTheReport();

        // when
        Result<ReportCageDetail> result =
                handler.handle(new FindReportCageQuery(sectorId.toString(), reportId.toString(), cage));

        // then
        assertThat(result.error().type()).isEqualTo(ErrorType.NOT_FOUND);
        assertThat(result.error().code()).isEqualTo("CAGE_NOT_FOUND");
    }
}
