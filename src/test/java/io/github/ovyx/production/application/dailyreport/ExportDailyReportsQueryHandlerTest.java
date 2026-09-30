package io.github.ovyx.production.application.dailyreport;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.ovyx.production.domain.model.Actor;
import io.github.ovyx.production.domain.model.MortalityStatus;
import io.github.ovyx.production.domain.model.ProductionStatus;
import io.github.ovyx.production.domain.model.SectorId;
import io.github.ovyx.shared.application.ErrorType;
import io.github.ovyx.shared.application.FarmCalendar;
import io.github.ovyx.shared.application.Result;
import io.github.ovyx.shared.application.spreadsheet.Spreadsheet;
import io.github.ovyx.shared.application.spreadsheet.SpreadsheetFile;
import io.github.ovyx.shared.domain.FixedClock;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * A exportação dos relatórios de um intervalo (US1 da 007; FR-012, FR-013, R-006): as regras do intervalo, com
 * todas as falhas de uma vez, o setor, e o arquivo pedido à porta com a data e a hora da granja.
 */
@DisplayName("ExportDailyReportsQueryHandler")
class ExportDailyReportsQueryHandlerTest {

    private static final byte[] CONTENT = "PK planilha".getBytes(StandardCharsets.UTF_8);
    private static final LocalDate FROM = LocalDate.of(2026, 9, 1);
    private static final LocalDate TO = LocalDate.of(2026, 9, 28);

    private final RecordingDailyReportDirectory directory = new RecordingDailyReportDirectory();
    /** 28/09/2026 às 21:40 na granja, 00:40 do dia 29 em UTC. */
    private final FarmCalendar calendar =
            new FarmCalendar(new FixedClock(Instant.parse("2026-09-29T00:40:00Z")), ZoneId.of("America/Sao_Paulo"));
    private Spreadsheet written;
    private final ExportDailyReportsQueryHandler handler =
            new ExportDailyReportsQueryHandler(directory, spreadsheet -> {
                written = spreadsheet;
                return CONTENT;
            }, calendar);
    private final UUID sectorId = UUID.randomUUID();

    private ReportingSector sector(String status) {
        return new ReportingSector(sectorId, "Codornas — Galpão 1", status);
    }

    private DailyReportDetail reportOn(LocalDate date) {
        return new DailyReportDetail(
                UUID.randomUUID(),
                sector("ACTIVE"),
                date,
                LocalTime.of(6, 30),
                100,
                20,
                null,
                true,
                new Actor(UUID.randomUUID(), "Marina Alves"),
                Instant.parse("2026-09-01T09:31:40Z"),
                null,
                null,
                new ProductionTotals(ProductionStatus.COMPLETE, 0, 0, 0, 0, BigDecimal.ZERO),
                new MortalityTotals(MortalityStatus.RECORDED, 0, 0, BigDecimal.ZERO, 100),
                DailyReportTotals.feed(List.of(), 0),
                List.of());
    }

    private Result<SpreadsheetFile> export(LocalDate from, LocalDate to) {
        return handler.handle(new ExportDailyReportsQuery(sectorId.toString(), from, to));
    }

    private static Map<String, String> detailsOf(Result<SpreadsheetFile> result) {
        assertThat(result.isFailure()).isTrue();
        assertThat(result.error().type()).isEqualTo(ErrorType.VALIDATION);
        assertThat(result.error().code()).isEqualTo("VALIDATION_FAILED");
        return result.error().details();
    }

    // ---------------------------------------------------------------- o intervalo

    @Test
    @DisplayName("refuses without both dates, telling each of them at once")
    void givenNoDates_whenExporting_thenRefuseBothDatesAtOnce() {
        // given
        directory.knowSector(sector("ACTIVE"));

        // when
        Result<SpreadsheetFile> result = export(null, null);

        // then
        assertThat(detailsOf(result))
                .containsExactly(
                        Map.entry("from", "Informe a data inicial."), Map.entry("to", "Informe a data final."));
        assertThat(written).isNull();
    }

    @Test
    @DisplayName("refuses the last day before the first one")
    void givenLastDayBeforeTheFirst_whenExporting_thenRefuseTheLastDay() {
        // given
        directory.knowSector(sector("ACTIVE"));

        // when
        Result<SpreadsheetFile> result = export(TO, FROM);

        // then
        assertThat(detailsOf(result))
                .containsExactly(Map.entry("to", "A data final deve ser igual ou posterior à inicial."));
    }

    @Test
    @DisplayName("accepts 366 days, counting both ends")
    void givenIntervalOf366Days_whenExporting_thenExport() {
        // given
        directory.knowSector(sector("ACTIVE"));
        LocalDate from = LocalDate.of(2025, 9, 28);

        // when
        Result<SpreadsheetFile> result = export(from, from.plusDays(365));

        // then
        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    @DisplayName("refuses 367 days, counting both ends")
    void givenIntervalOf367Days_whenExporting_thenRefuseTheLastDay() {
        // given
        directory.knowSector(sector("ACTIVE"));
        LocalDate from = LocalDate.of(2025, 9, 27);

        // when
        Result<SpreadsheetFile> result = export(from, from.plusDays(366));

        // then
        assertThat(detailsOf(result)).containsExactly(Map.entry("to", "O intervalo deve ter no máximo 366 dias."));
    }

    @Test
    @DisplayName("accepts a single day")
    void givenSameFirstAndLastDay_whenExporting_thenExport() {
        // given
        directory.knowSector(sector("ACTIVE"));

        // when
        Result<SpreadsheetFile> result = export(TO, TO);

        // then
        assertThat(result.isSuccess()).isTrue();
        assertThat(directory.betweenFrom).isEqualTo(TO);
        assertThat(directory.betweenTo).isEqualTo(TO);
    }

    @Test
    @DisplayName("refuses the interval before asking for the sector")
    void givenUnknownSectorAndNoDates_whenExporting_thenRefuseTheDates() {
        // given
        ExportDailyReportsQuery query = new ExportDailyReportsQuery("galpao-9", null, TO);

        // when
        Result<SpreadsheetFile> result = handler.handle(query);

        // then
        assertThat(detailsOf(result)).containsOnlyKeys("from");
    }

    // ---------------------------------------------------------------- o setor

    @ParameterizedTest(name = "\"{0}\"")
    @ValueSource(strings = {"3f6c2b1a-8d4e-4c7f-9a2b-1e5d7c9f0a11", "galpao-9"})
    @DisplayName("fails as sector not found for a sector that does not exist or a malformed identifier")
    void givenUnknownOrMalformedSector_whenExporting_thenFailAsSectorNotFound(String sector) {
        // given
        ExportDailyReportsQuery query = new ExportDailyReportsQuery(sector, FROM, TO);

        // when
        Result<SpreadsheetFile> result = handler.handle(query);

        // then
        assertThat(result.error().type()).isEqualTo(ErrorType.NOT_FOUND);
        assertThat(result.error().code()).isEqualTo("SECTOR_NOT_FOUND");
        assertThat(written).isNull();
    }

    @Test
    @DisplayName("exports the reports of an inactive sector, which stay open to reading")
    void givenInactiveSector_whenExporting_thenExport() {
        // given
        directory.knowSector(sector("INACTIVE"));

        // when
        Result<SpreadsheetFile> result = export(FROM, TO);

        // then
        assertThat(result.isSuccess()).isTrue();
    }

    // ---------------------------------------------------------------- o arquivo

    @Test
    @DisplayName("asks the port for the interval and hands the file of the writer back")
    void givenReportsInTheInterval_whenExporting_thenAnswerTheFileOfTheWriter() {
        // given
        directory.knowSector(sector("ACTIVE"));
        directory.knowDetail(reportOn(LocalDate.of(2026, 9, 2)));
        directory.knowDetail(reportOn(LocalDate.of(2026, 9, 1)));
        directory.knowDetail(reportOn(LocalDate.of(2026, 8, 31)));

        // when
        Result<SpreadsheetFile> result = export(FROM, TO);

        // then
        assertThat(result.value())
                .isEqualTo(new SpreadsheetFile("relatorios-codornas-galpao-1-01-09-2026-a-28-09-2026.xlsx", CONTENT));
        assertThat(directory.betweenSector).isEqualTo(SectorId.of(sectorId));
        assertThat(directory.betweenFrom).isEqualTo(FROM);
        assertThat(directory.betweenTo).isEqualTo(TO);
        assertThat(written.sheets().getFirst().rows()).hasSize(2);
    }

    @Test
    @DisplayName("heads the spreadsheet with the day and the time of the farm")
    void givenLateEveningAtTheFarm_whenExporting_thenUseTheDayOfTheFarm() {
        // given
        directory.knowSector(sector("ACTIVE"));

        // when
        export(FROM, TO);

        // then
        assertThat(written.sheets().getFirst().heading())
                .last()
                .isEqualTo("Gerada em 28/09/2026 às 21:40 (horário da granja)");
    }

    // ---------------------------------------------------------------- mutação (T051)

    @Test
    @DisplayName("refuses the last day one day before the first one")
    void givenLastDayOneDayBeforeTheFirst_whenExporting_thenRefuseTheLastDay() {
        // given
        directory.knowSector(sector("ACTIVE"));

        // when
        Result<SpreadsheetFile> result = export(FROM, FROM.minusDays(1));

        // then
        assertThat(detailsOf(result))
                .containsExactly(Map.entry("to", "A data final deve ser igual ou posterior à inicial."));
    }
}
