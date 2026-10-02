package io.github.ovyx.farm.application.cage;

import java.time.DayOfWeek;
import java.time.LocalDate;
import io.github.ovyx.shared.application.spreadsheet.Sheet;
import io.github.ovyx.shared.application.spreadsheet.Column;
import io.github.ovyx.shared.application.spreadsheet.Cell;
import io.github.ovyx.farm.domain.model.CageId;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.ovyx.farm.application.common.StatusFilter;
import io.github.ovyx.farm.application.sector.SectorDetail;
import io.github.ovyx.farm.application.sector.SectorDirectory;
import io.github.ovyx.farm.application.sector.SectorSummary;
import io.github.ovyx.farm.domain.model.SectorId;
import io.github.ovyx.farm.domain.model.Status;
import io.github.ovyx.shared.application.ErrorType;
import io.github.ovyx.shared.application.FarmCalendar;
import io.github.ovyx.shared.application.Result;
import io.github.ovyx.shared.application.spreadsheet.Spreadsheet;
import io.github.ovyx.shared.application.spreadsheet.SpreadsheetFile;
import io.github.ovyx.shared.domain.FixedClock;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * A exportação das gaiolas de um setor (US3 da 007; FR-022 a FR-024): os filtros da pesquisa, sem página, e o
 * arquivo pedido à porta com a data e a hora da granja.
 */
@DisplayName("ExportCagesQueryHandler")
class ExportCagesQueryHandlerTest {

    private static final byte[] CONTENT = "PK planilha".getBytes(StandardCharsets.UTF_8);

    private final SectorId sectorId = SectorId.generate();
    private final SectorDetail sector = new SectorDetail(
            sectorId,
            "Codornas — Galpão 1",
            null,
            Status.ACTIVE,
            4,
            200,
            List.of("A"),
            Instant.parse("2026-09-01T10:00:00Z"),
            Instant.parse("2026-09-01T10:00:00Z"),
            null, new BigDecimal("85.0"), null);
    private final RecordingCageDirectory cages = new RecordingCageDirectory().withSector(sectorId);
    private final SectorDirectory sectors = new SectorDirectory() {
        @Override
        public List<SectorSummary> list(StatusFilter status) {
            return List.of();
        }

        @Override
        public Optional<SectorDetail> findDetail(SectorId id) {
            return id.equals(sectorId) ? Optional.of(sector) : Optional.empty();
        }
    };
    /** 28/09/2026 às 21:40 na granja. */
    private final FarmCalendar calendar =
            new FarmCalendar(FixedClock.at("2026-09-29T00:40:00Z"), ZoneId.of("America/Sao_Paulo"));
    private Spreadsheet written;
    private final ExportCagesQueryHandler handler = new ExportCagesQueryHandler(
            cages,
            sectors,
            spreadsheet -> {
                written = spreadsheet;
                return CONTENT;
            },
            calendar);

    @Test
    @DisplayName("asks for every cage of the filters, the active ones when no status is asked, the battery in capitals")
    void givenFiltersAndNoStatus_whenExporting_thenAskForTheActiveCagesOfTheFilters() {
        // given
        ExportCagesQuery query = new ExportCagesQuery(sectorId.toString(), " b-0 ", " b ", null, null);

        // when
        Result<SpreadsheetFile> result = handler.handle(query);

        // then
        assertThat(result.value()).isEqualTo(new SpreadsheetFile("gaiolas-codornas-galpao-1.xlsx", CONTENT));
        assertThat(cages.everyCageAsked())
                .containsExactly(new RecordingCageDirectory.All(sectorId, "b-0", "B", StatusFilter.ACTIVE, null));
        assertThat(written.sheets().getFirst().heading())
                .contains("Filtros: busca \"b-0\", bateria B, só as ativas", "Gerada em 28/09/2026 às 21:40 (horário da granja)");
    }

    @Test
    @DisplayName("leaves a blank code and a blank battery out, and keeps the status asked")
    void givenBlankFiltersAndEveryStatus_whenExporting_thenAskWithoutThem() {
        // given
        ExportCagesQuery query = new ExportCagesQuery(sectorId.toString(), " ", "", StatusFilter.ALL, null);

        // when
        handler.handle(query);

        // then
        assertThat(cages.everyCageAsked())
                .containsExactly(new RecordingCageDirectory.All(sectorId, null, null, StatusFilter.ALL, null));
    }

    @ParameterizedTest(name = "\"{0}\"")
    @ValueSource(strings = {"3f6c2b1a-8d4e-4c7f-9a2b-1e5d7c9f0a11", "galpao-9"})
    @DisplayName("fails as sector not found for a sector that does not exist or a malformed identifier")
    void givenUnknownOrMalformedSector_whenExporting_thenFailAsSectorNotFound(String sector) {
        // given
        ExportCagesQuery query = new ExportCagesQuery(sector, null, null, null, null);

        // when
        Result<SpreadsheetFile> result = handler.handle(query);

        // then
        assertThat(result.error().type()).isEqualTo(ErrorType.NOT_FOUND);
        assertThat(result.error().code()).isEqualTo("SECTOR_NOT_FOUND");
        assertThat(written).isNull();
        assertThat(cages.everyCageAsked()).isEmpty();
    }

    // ---------------------------------------------------------------- agenda de pesagem (010)

    @Test
    @DisplayName("asks for the pending cages since 7 days ago in a sector without a weighing day, and says the filter")
    void givenPendingFilterInSectorWithoutWeighingDay_whenExporting_thenAskSinceSevenDaysAgo() {
        // given
        ExportCagesQuery query = new ExportCagesQuery(sectorId.toString(), null, null, null, WeighingFilter.PENDING);

        // when
        handler.handle(query);

        // then
        assertThat(cages.everyCageAsked())
                .containsExactly(new RecordingCageDirectory.All(
                        sectorId, null, null, StatusFilter.ACTIVE, LocalDate.of(2026, 9, 21)));
        assertThat(written.sheets().getFirst().heading()).contains("Filtros: pesagem pendente, só as ativas");
    }

    @Test
    @DisplayName("writes the standing of each active cage in the schedule of the sector")
    void givenCagesWeighedAndNot_whenExporting_thenWriteTheStandingOfEach() {
        // given
        cages.answering(List.of(
                new CageSummary(CageId.generate(), sectorId, "A-01", "A", 1, 50, Status.ACTIVE,
                        new CageLastWeighing(LocalDate.of(2026, 9, 25), new BigDecimal("160.0")), null),
                new CageSummary(CageId.generate(), sectorId, "B-07", "B", 7, 50, Status.ACTIVE,
                        new CageLastWeighing(LocalDate.of(2026, 9, 18), new BigDecimal("160.0")), null)));

        // when
        handler.handle(new ExportCagesQuery(sectorId.toString(), null, null, null, null));

        // then
        Sheet sheet = written.sheets().getFirst();
        int column = sheet.columns().stream().map(Column::title).toList().indexOf("Pesagem");
        assertThat(sheet.rows()).extracting(row -> row.get(column)).containsExactly(Cell.text("Em dia"), Cell.text("Atrasada"));
    }

    @Test
    @DisplayName("exports an empty sheet with the pending filter in an inactive sector, without asking for cages")
    void givenInactiveSectorAndPendingFilter_whenExporting_thenWriteAnEmptySheetWithoutAskingForCages() {
        // given
        SectorDetail inactive = new SectorDetail(
                sector.id(),
                sector.name(),
                null,
                Status.INACTIVE,
                0,
                0,
                List.of("A"),
                sector.createdAt(),
                sector.updatedAt(),
                null,
                new BigDecimal("85.0"),
                DayOfWeek.FRIDAY);
        ExportCagesQueryHandler onInactive = new ExportCagesQueryHandler(
                cages,
                new SectorDirectory() {
                    @Override
                    public List<SectorSummary> list(StatusFilter status) {
                        return List.of();
                    }

                    @Override
                    public Optional<SectorDetail> findDetail(SectorId id) {
                        return Optional.of(inactive);
                    }
                },
                spreadsheet -> {
                    written = spreadsheet;
                    return CONTENT;
                },
                calendar);

        // when
        onInactive.handle(new ExportCagesQuery(sectorId.toString(), null, null, null, WeighingFilter.PENDING));

        // then
        assertThat(cages.everyCageAsked()).isEmpty();
        assertThat(written.sheets().getFirst().rows()).isEmpty();
        assertThat(written.sheets().getFirst().heading()).contains("Filtros: pesagem pendente, só as ativas");
    }
}
