package io.github.ovyx.production.application.dashboard;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Os alertas e as pendências de hoje (US3 da 006; FR-015 a FR-018, R-007): cada um com os números que o
 * explicam e o destino da tela onde se resolve, derivados a cada consulta.
 */
@DisplayName("DashboardAlerts")
class DashboardAlertsTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 9, 24);
    private static final UUID REPORT = UUID.fromString("6b1d3f5a-7c9e-4a2b-8d4f-1e3a5c7b9d55");
    private static final ReferenceWeight RANGE = new ReferenceWeight(155, 175);
    private static final MortalityBaseline QUIET_WEEK = new MortalityBaseline(0, 28);

    /** O relatório de hoje com 2.000 aves e 1.740 ovos, com as gaiolas lançadas como dito. */
    private static ReportDay today(int cages, int withProduction, int withFeed, int removed, boolean confirmed) {
        return new ReportDay(
                REPORT,
                TODAY,
                2000,
                cages,
                withProduction,
                withFeed,
                1740,
                0,
                0,
                0,
                0,
                0,
                0,
                new BigDecimal("159600.00"),
                removed,
                confirmed);
    }

    private static ReportDay completeToday() {
        return today(4, 4, 4, 0, true);
    }

    /** Uma gaiola sem nada de anormal: 3 relatórios a 90%, nenhuma ave removida hoje e sem pesagem. */
    private static CageWatch quietCage(String code) {
        return new CageWatch(UUID.randomUUID(), code, 0, 135, 150, 3, null, null);
    }

    private static List<DashboardAlert> alerts(ReportDay today, List<CageWatch> cages, ReferenceWeight range,
            MortalityBaseline baseline) {
        return DashboardAlerts.of(TODAY, today, new CageWatchReading(cages, range, baseline));
    }

    // ---------------------------------------------------------------- pendências do relatório de hoje

    @Test
    @DisplayName("asks to open the report of today when it was not opened, and says nothing about the entries")
    void givenNoReportToday_whenListingTheAlerts_thenAskToOpenIt() {
        // given
        List<CageWatch> cages = List.of(quietCage("A-01"));

        // when
        List<DashboardAlert> alerts = alerts(null, cages, RANGE, QUIET_WEEK);

        // then
        assertThat(alerts).extracting(DashboardAlert::kind).containsExactly(AlertKind.REPORT_NOT_OPENED);
        DashboardAlert alert = alerts.get(0);
        assertThat(alert.tone()).isEqualTo(AlertTone.WARNING);
        assertThat(alert.title()).isEqualTo("Relatório de hoje não aberto");
        assertThat(alert.detail()).isEqualTo("Abra o relatório de 24/09 para lançar a produção, a ração e a mortalidade.");
        assertThat(alert.target().reportId()).isNull();
    }

    @Test
    @DisplayName("lists each pending entry of the report of today, in order, with how many cages are missing")
    void givenReportWithEverythingPending_whenListingTheAlerts_thenListProductionFeedAndMortality() {
        // given
        ReportDay pending = today(4, 3, 1, 0, false);

        // when
        List<DashboardAlert> alerts = alerts(pending, List.of(), RANGE, QUIET_WEEK);

        // then
        assertThat(alerts).extracting(DashboardAlert::kind).containsExactly(
                AlertKind.PRODUCTION_PENDING, AlertKind.FEED_PENDING, AlertKind.MORTALITY_PENDING);
        assertThat(alerts).extracting(DashboardAlert::tone)
                .containsExactly(AlertTone.WARNING, AlertTone.INFO, AlertTone.INFO);
        assertThat(alerts.get(0).detail()).isEqualTo("Falta lançar a produção de 1 gaiola.");
        assertThat(alerts.get(1).detail()).isEqualTo("Lance a ração de 3 gaiolas para calcular o custo por ovo.");
        assertThat(alerts).allSatisfy(alert -> assertThat(alert.target().reportId()).isEqualTo(REPORT));
    }

    @Test
    @DisplayName("says nothing when the report of today is complete and no cage stands out")
    void givenCompleteReportAndQuietCages_whenListingTheAlerts_thenListNothing() {
        // given
        List<CageWatch> cages = List.of(quietCage("A-01"), quietCage("B-07"));

        // when
        List<DashboardAlert> alerts = alerts(completeToday(), cages, RANGE, QUIET_WEEK);

        // then
        assertThat(alerts).isEmpty();
    }

    // ---------------------------------------------------------------- mortalidade acima da média

    @ParameterizedTest(name = "{0} birds today, {1} removed in {2} cage-days: alert {3}")
    @CsvSource({
        "3, 7, 28, true",
        "2, 28, 28, false",
        "1, 0, 28, false",
        "2, 0, 0, true",
        "3, 42, 28, false"
    })
    @DisplayName("warns about a cage with more than twice the daily average of the sector, and at least 2 birds")
    void givenRemovalsOfTheCage_whenListingTheAlerts_thenWarnOnlyAboveTwiceTheAverage(
            int removedToday, int removedInTheWeek, int cageDays, boolean expected) {
        // given
        CageWatch b07 = new CageWatch(UUID.randomUUID(), "B-07", removedToday, 135, 150, 3, null, null);

        // when
        List<DashboardAlert> alerts =
                alerts(completeToday(), List.of(b07), RANGE, new MortalityBaseline(removedInTheWeek, cageDays));

        // then
        assertThat(alerts.stream().anyMatch(alert -> alert.kind() == AlertKind.HIGH_MORTALITY)).isEqualTo(expected);
    }

    @Test
    @DisplayName("tells the birds of today and the average of the sector, and leads to the mortality of the report")
    void givenHighMortality_whenListingTheAlerts_thenExplainItWithTheNumbers() {
        // given
        CageWatch b07 = new CageWatch(UUID.randomUUID(), "B-07", 2, 135, 150, 3, null, null);

        // when
        DashboardAlert alert =
                alerts(completeToday(), List.of(b07), RANGE, new MortalityBaseline(11, 28)).get(0);

        // then
        assertThat(alert.kind()).isEqualTo(AlertKind.HIGH_MORTALITY);
        assertThat(alert.tone()).isEqualTo(AlertTone.WARNING);
        assertThat(alert.title()).isEqualTo("Mortalidade acima da média na gaiola B-07");
        assertThat(alert.detail()).isEqualTo("2 aves removidas hoje; a média do setor é 0,4 por gaiola ao dia.");
        assertThat(alert.target().reportId()).isEqualTo(REPORT);
        assertThat(alert.target().cageId()).isEqualTo(b07.cageId());
        assertThat(alert.target().cageCode()).isEqualTo("B-07");
    }

    // ---------------------------------------------------------------- baixa postura

    @ParameterizedTest(name = "{0} eggs of {1} birds in {2} reports: alert {3}")
    @CsvSource({"114, 150, 3, true", "255, 300, 3, false", "114, 150, 2, false"})
    @DisplayName("warns about a cage below the target of 85% in its last 3 reports, the target included")
    void givenRecentReportsOfTheCage_whenListingTheAlerts_thenWarnOnlyBelowTheTarget(
            int eggs, int birds, int reports, boolean expected) {
        // given
        CageWatch c03 = new CageWatch(UUID.randomUUID(), "C-03", 0, eggs, birds, reports, null, null);

        // when
        List<DashboardAlert> alerts = alerts(completeToday(), List.of(c03), RANGE, QUIET_WEEK);

        // then
        assertThat(alerts.stream().anyMatch(alert -> alert.kind() == AlertKind.LOW_LAYING)).isEqualTo(expected);
    }

    @Test
    @DisplayName("tells the rate of the cage and the one of the sector today, and leads to the cages")
    void givenLowLaying_whenListingTheAlerts_thenExplainItWithTheRates() {
        // given
        CageWatch c03 = new CageWatch(UUID.randomUUID(), "C-03", 0, 114, 150, 3, null, null);

        // when
        DashboardAlert alert = alerts(completeToday(), List.of(c03), RANGE, QUIET_WEEK).get(0);

        // then
        assertThat(alert.title()).isEqualTo("Baixa postura na gaiola C-03");
        assertThat(alert.detail()).isEqualTo("76,0% nos últimos 3 relatórios, contra 87,0% no setor hoje.");
        assertThat(alert.target().reportId()).isNull();
        assertThat(alert.target().cageCode()).isEqualTo("C-03");
    }

    @Test
    @DisplayName("compares the cage with the target when there is no report of today")
    void givenLowLayingWithoutReportToday_whenListingTheAlerts_thenCompareWithTheTarget() {
        // given
        CageWatch c03 = new CageWatch(UUID.randomUUID(), "C-03", 0, 114, 150, 3, null, null);

        // when
        DashboardAlert alert = alerts(null, List.of(c03), RANGE, QUIET_WEEK).get(1);

        // then
        assertThat(alert.kind()).isEqualTo(AlertKind.LOW_LAYING);
        assertThat(alert.detail()).isEqualTo("76,0% nos últimos 3 relatórios, abaixo da meta de 85%.");
    }

    // ---------------------------------------------------------------- pesagem fora da faixa

    @ParameterizedTest(name = "{0} g: alert {1}")
    @CsvSource({"150.8, true", "155.0, false", "175.0, false", "175.1, true"})
    @DisplayName("warns about a cage whose last weighing is out of the range of the sector, the limits included")
    void givenLastWeighing_whenListingTheAlerts_thenWarnOnlyOutOfTheRange(String weight, boolean expected) {
        // given
        CageWatch a02 = new CageWatch(UUID.randomUUID(), "A-02", 0, 135, 150, 3, TODAY, new BigDecimal(weight));

        // when
        List<DashboardAlert> alerts = alerts(completeToday(), List.of(a02), RANGE, QUIET_WEEK);

        // then
        assertThat(alerts.stream().anyMatch(alert -> alert.kind() == AlertKind.WEIGHT_OUT_OF_RANGE))
                .isEqualTo(expected);
    }

    @Test
    @DisplayName("tells the weight, the day and the range, and leads to the weight of the cage")
    void givenWeightOutOfRange_whenListingTheAlerts_thenExplainItWithTheRange() {
        // given
        CageWatch a02 =
                new CageWatch(UUID.randomUUID(), "A-02", 0, 135, 150, 3, TODAY, new BigDecimal("150.8"));

        // when
        DashboardAlert alert = alerts(completeToday(), List.of(a02), RANGE, QUIET_WEEK).get(0);

        // then
        assertThat(alert.title()).isEqualTo("Pesagem fora da faixa na gaiola A-02");
        assertThat(alert.detail()).isEqualTo("150,8 g em 24/09/2026; a faixa do setor é 155–175 g.");
        assertThat(alert.target().cageId()).isEqualTo(a02.cageId());
    }

    @Test
    @DisplayName("writes a whole weight without the decimal place")
    void givenWholeWeight_whenListingTheAlerts_thenWriteItWithoutDecimals() {
        // given
        CageWatch a02 = new CageWatch(UUID.randomUUID(), "A-02", 0, 135, 150, 3, TODAY, new BigDecimal("190.0"));

        // when
        DashboardAlert alert = alerts(completeToday(), List.of(a02), RANGE, QUIET_WEEK).get(0);

        // then
        assertThat(alert.detail()).startsWith("190 g em 24/09/2026");
    }

    @Test
    @DisplayName("says nothing about weight without a range in the sector or without a weighing in the cage")
    void givenNoRangeOrNoWeighing_whenListingTheAlerts_thenSayNothingAboutWeight() {
        // given
        CageWatch weighed = new CageWatch(UUID.randomUUID(), "A-02", 0, 135, 150, 3, TODAY, new BigDecimal("140"));
        CageWatch neverWeighed = quietCage("A-03");

        // when
        List<DashboardAlert> withoutRange = alerts(completeToday(), List.of(weighed), null, QUIET_WEEK);
        List<DashboardAlert> withoutWeighing = alerts(completeToday(), List.of(neverWeighed), RANGE, QUIET_WEEK);

        // then
        assertThat(withoutRange).isEmpty();
        assertThat(withoutWeighing).isEmpty();
    }

    // ---------------------------------------------------------------- ordem

    @Test
    @DisplayName("lists the pending entries first, then the cages by kind, each kind in the order of the cages")
    void givenEveryKind_whenListingTheAlerts_thenOrderThem() {
        // given
        CageWatch a02 = new CageWatch(UUID.randomUUID(), "A-02", 3, 114, 150, 3, TODAY, new BigDecimal("150"));
        CageWatch b07 = new CageWatch(UUID.randomUUID(), "B-07", 3, 135, 150, 3, null, null);

        // when
        List<DashboardAlert> alerts =
                alerts(today(4, 4, 3, 6, false), List.of(a02, b07), RANGE, QUIET_WEEK);

        // then
        assertThat(alerts).extracting(alert -> alert.kind() + " " + (alert.target().cageCode() == null ? "" : alert.target().cageCode()))
                .containsExactly(
                        "FEED_PENDING ",
                        "HIGH_MORTALITY A-02",
                        "HIGH_MORTALITY B-07",
                        "LOW_LAYING A-02",
                        "WEIGHT_OUT_OF_RANGE A-02");
    }
}
