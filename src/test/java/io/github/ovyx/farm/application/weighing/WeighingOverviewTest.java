package io.github.ovyx.farm.application.weighing;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.ovyx.farm.domain.model.Actor;
import io.github.ovyx.farm.domain.model.CageId;
import io.github.ovyx.farm.domain.model.SectorId;
import io.github.ovyx.farm.domain.model.Status;
import io.github.ovyx.farm.domain.model.WeighingId;
import io.github.ovyx.farm.domain.model.WeightRangeStatus;
import io.github.ovyx.farm.domain.valueobject.ReferenceWeight;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * O cálculo do acompanhamento do peso (R-008 da 005): a última pesagem e o histórico, com a variação de
 * cada pesagem sobre a anterior. Puro, sem banco.
 */
@DisplayName("WeighingOverview")
class WeighingOverviewTest {

    private static final Actor MARINA = new Actor(UUID.fromString("5e7a9c1e-3b5d-4f7a-9c1e-3b5d7f9a1c22"), "Marina Alves");

    private static final WeighedCage A01 = new WeighedCage(
            CageId.of(UUID.fromString("2a4c6e8a-0b1d-4f3a-9c5e-7a9b1d3f5a66")), "A-01", "A", 1, 48, Status.ACTIVE);
    private static final WeighedSector GALPAO_1 = new WeighedSector(
            SectorId.of(UUID.fromString("3f6c2b1a-8d4e-4c7f-9a2b-1e5d7c9f0a11")), "Codornas — Galpão 1", Status.ACTIVE, null);

    private static WeighingEntry entry(String day, String weight) {
        return new WeighingEntry(WeighingId.generate(), LocalDate.parse(day), new BigDecimal(weight), MARINA, null);
    }

    @Test
    @DisplayName("lists the history from the most recent, with the change over the previous weighing")
    void givenWeighings_whenBuildingTheOverview_thenListTheHistoryWithTheChanges() {
        // given
        List<WeighingEntry> entries =
                List.of(entry("2026-09-10", "156.0"), entry("2026-09-17", "158.0"), entry("2026-09-24", "161.4"));

        // when
        WeighingOverview overview = WeighingOverview.of(A01, GALPAO_1, entries);

        // then
        assertThat(overview.history())
                .extracting(WeighingHistoryEntry::weighedOn)
                .containsExactly(
                        LocalDate.parse("2026-09-24"), LocalDate.parse("2026-09-17"), LocalDate.parse("2026-09-10"));
        assertThat(overview.history().get(0).change()).isEqualByComparingTo("3.4");
        assertThat(overview.history().get(1).change()).isEqualByComparingTo("2.0");
        assertThat(overview.history().get(2).change()).isNull();
    }

    @Test
    @DisplayName("orders the weighings by day, whatever order they come in")
    void givenWeighingsOutOfOrder_whenBuildingTheOverview_thenOrderThemByDay() {
        // given
        List<WeighingEntry> entries = List.of(entry("2026-09-24", "161.0"), entry("2026-09-10", "150.0"));

        // when
        WeighingOverview overview = WeighingOverview.of(A01, GALPAO_1, entries);

        // then
        assertThat(overview.latest().weighedOn()).isEqualTo(LocalDate.parse("2026-09-24"));
        assertThat(overview.history().get(0).change()).isEqualByComparingTo("11.0");
    }

    @Test
    @DisplayName("keeps a negative change, when the cage lost weight")
    void givenWeightLost_whenBuildingTheOverview_thenKeepTheNegativeChange() {
        // given
        List<WeighingEntry> entries = List.of(entry("2026-09-17", "158.0"), entry("2026-09-24", "156.5"));

        // when
        WeighingOverview overview = WeighingOverview.of(A01, GALPAO_1, entries);

        // then
        assertThat(overview.history().get(0).change()).isEqualByComparingTo("-1.5");
    }

    @Test
    @DisplayName("takes the most recent weighing as the latest, with the cage and the sector")
    void givenWeighings_whenBuildingTheOverview_thenTakeTheMostRecentAsTheLatest() {
        // given
        WeighingEntry latest = entry("2026-09-24", "161.4");
        List<WeighingEntry> entries = List.of(entry("2026-09-17", "158.0"), latest);

        // when
        WeighingOverview overview = WeighingOverview.of(A01, GALPAO_1, entries);

        // then
        assertThat(overview.cage()).isEqualTo(A01);
        assertThat(overview.sector()).isEqualTo(GALPAO_1);
        assertThat(overview.latest().id()).isEqualTo(latest.id());
        assertThat(overview.latest().averageWeight()).isEqualByComparingTo("161.4");
    }

    @Test
    @DisplayName("has no latest weighing and an empty history before the first weighing")
    void givenNoWeighing_whenBuildingTheOverview_thenHaveNoLatestAndAnEmptyHistory() {
        // given
        List<WeighingEntry> none = List.of();

        // when
        WeighingOverview overview = WeighingOverview.of(A01, GALPAO_1, none);

        // then
        assertThat(overview.latest()).isNull();
        assertThat(overview.history()).isEmpty();
    }

    // ---------------------------------------------------------------- acompanhamento (US3)

    private static final WeighedSector GALPAO_1_WITH_RANGE = new WeighedSector(
            GALPAO_1.id(), GALPAO_1.name(), Status.ACTIVE, new ReferenceWeight(155, 175));

    private static WeighingOverview weeklyOverview(WeighedSector sector, String... weights) {
        List<WeighingEntry> entries = new ArrayList<>();
        LocalDate day = LocalDate.parse("2026-08-27");
        for (String weight : weights) {
            entries.add(entry(day.toString(), weight));
            day = day.plusDays(7);
        }
        return WeighingOverview.of(A01, sector, entries);
    }

    @Test
    @DisplayName("changes over four weeks: the latest minus the most recent weighing up to 28 days before it")
    void givenFiveWeeklyWeighings_whenBuildingTheOverview_thenChangeOverFourWeeks() {
        // given
        String[] weights = {"150", "153", "156", "158", "161.4"};

        // when
        WeighingOverview overview = weeklyOverview(GALPAO_1, weights);

        // then
        assertThat(overview.fourWeekChange().change()).isEqualByComparingTo("11.4");
        assertThat(overview.fourWeekChange().since()).isEqualTo(LocalDate.parse("2026-08-27"));
    }

    @Test
    @DisplayName("counts a weighing exactly 28 days before the latest, and not one 27 days before")
    void givenWeighingsTwentySevenAndTwentyEightDaysBefore_whenBuildingTheOverview_thenTakeTheTwentyEighth() {
        // given
        List<WeighingEntry> entries = List.of(
                entry("2026-08-27", "150"), entry("2026-08-28", "152"), entry("2026-09-24", "161"));

        // when
        WeighingOverview overview = WeighingOverview.of(A01, GALPAO_1, entries);

        // then
        assertThat(overview.fourWeekChange().since()).isEqualTo(LocalDate.parse("2026-08-27"));
        assertThat(overview.fourWeekChange().change()).isEqualByComparingTo("11.0");
    }

    @Test
    @DisplayName("takes the most recent of the weighings made up to 28 days before the latest, not the oldest")
    void givenWeighingsFiveAndFourWeeksBefore_whenBuildingTheOverview_thenTakeTheFourWeeksOne() {
        // given
        List<WeighingEntry> entries = List.of(
                entry("2026-08-20", "140"), entry("2026-08-27", "150"), entry("2026-09-24", "161"));

        // when
        WeighingOverview overview = WeighingOverview.of(A01, GALPAO_1, entries);

        // then
        assertThat(overview.fourWeekChange().since()).isEqualTo(LocalDate.parse("2026-08-27"));
        assertThat(overview.fourWeekChange().change()).isEqualByComparingTo("11.0");
    }

    @Test
    @DisplayName("has no change over four weeks without a weighing up to 28 days before the latest")
    void givenWeighingsOfLessThanFourWeeks_whenBuildingTheOverview_thenHaveNoFourWeekChange() {
        // given
        List<WeighingEntry> entries = List.of(entry("2026-08-28", "152"), entry("2026-09-24", "161"));

        // when
        WeighingOverview overview = WeighingOverview.of(A01, GALPAO_1, entries);

        // then
        assertThat(overview.fourWeekChange()).isNull();
    }

    @ParameterizedTest(name = "{0} g is {1}")
    @CsvSource({"155, WITHIN", "175, WITHIN", "161.4, WITHIN", "154.9, OUTSIDE", "175.1, OUTSIDE"})
    @DisplayName("places the latest weighing within the range of the sector, the limits included, or outside it")
    void givenRange_whenBuildingTheOverview_thenPlaceTheLatestWeighing(String weight, WeightRangeStatus expected) {
        // given
        String latest = weight;

        // when
        WeighingOverview overview = weeklyOverview(GALPAO_1_WITH_RANGE, "150", latest);

        // then
        assertThat(overview.rangeStatus()).isEqualTo(expected);
    }

    @Test
    @DisplayName("says there is no range when the sector has none, and nothing before the first weighing")
    void givenNoRangeOrNoWeighing_whenBuildingTheOverview_thenSayNoRangeOrNothing() {
        // given
        WeighingOverview withoutRange = weeklyOverview(GALPAO_1, "150");

        // when
        WeighingOverview withoutWeighing = WeighingOverview.of(A01, GALPAO_1_WITH_RANGE, List.of());

        // then
        assertThat(withoutRange.rangeStatus()).isEqualTo(WeightRangeStatus.NO_RANGE);
        assertThat(withoutWeighing.rangeStatus()).isNull();
        assertThat(withoutWeighing.fourWeekChange()).isNull();
        assertThat(withoutWeighing.chart()).isEmpty();
    }

    @Test
    @DisplayName("charts the last 12 weighings, from the oldest to the most recent")
    void givenThirteenWeighings_whenBuildingTheOverview_thenChartTheLastTwelve() {
        // given
        String[] weights = {"140", "141", "142", "143", "144", "145", "146", "147", "148", "149", "150", "151", "152"};

        // when
        WeighingOverview overview = weeklyOverview(GALPAO_1, weights);

        // then
        assertThat(overview.chart()).hasSize(12);
        assertThat(overview.chart().get(0).averageWeight()).isEqualByComparingTo("141");
        assertThat(overview.chart().get(11).averageWeight()).isEqualByComparingTo("152");
        assertThat(overview.chart().get(0).weighedOn()).isBefore(overview.chart().get(11).weighedOn());
    }
}
