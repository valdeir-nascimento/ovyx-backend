package io.github.ovyx.production.application.dashboard;

import java.time.DayOfWeek;
import io.github.ovyx.shared.domain.WeighingSchedule;
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

    /** A meta dos setores cadastrados antes da feature 008. */
    private static final LayingRateTarget EIGHTY_FIVE = new LayingRateTarget(new BigDecimal("85"));

    private static List<DashboardAlert> alerts(ReportDay today, List<CageWatch> cages, ReferenceWeight range,
            MortalityBaseline baseline) {
        return alerts(today, cages, range, baseline, EIGHTY_FIVE);
    }

    private static List<DashboardAlert> alerts(ReportDay today, List<CageWatch> cages, ReferenceWeight range,
            MortalityBaseline baseline, LayingRateTarget target) {
        return DashboardAlerts.of(TODAY, today, new CageWatchReading(cages, range, baseline, null), target);
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
    @DisplayName("tells the rate of the cage, the target of the sector and its rate today, and leads to the cages")
    void givenLowLaying_whenListingTheAlerts_thenExplainItWithTheRates() {
        // given
        CageWatch c03 = new CageWatch(UUID.randomUUID(), "C-03", 0, 114, 150, 3, null, null);

        // when
        DashboardAlert alert = alerts(completeToday(), List.of(c03), RANGE, QUIET_WEEK).get(0);

        // then
        assertThat(alert.title()).isEqualTo("Baixa postura na gaiola C-03");
        assertThat(alert.detail())
                .isEqualTo("76,0% nos últimos 3 relatórios, abaixo da meta de 85% do setor; o setor fez 87,0% hoje.");
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
        assertThat(alert.detail()).isEqualTo("76,0% nos últimos 3 relatórios, abaixo da meta de 85% do setor.");
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

    // ---------------------------------------------------------------- meta do setor (008)

    @ParameterizedTest(name = "a target of {0}%: alert {1}")
    @CsvSource({"72, false", "75, false", "85, true", "75.1, true"})
    @DisplayName("compares the last 3 reports of the cage with the target of its sector, the target included")
    void givenCageAt75Percent_whenListingTheAlertsWithTheTargetOfTheSector_thenWarnOnlyBelowIt(
            String target, boolean expected) {
        // given
        CageWatch c03 = new CageWatch(UUID.randomUUID(), "C-03", 0, 225, 300, 3, null, null);

        // when
        List<DashboardAlert> alerts = alerts(
                completeToday(), List.of(c03), RANGE, QUIET_WEEK, new LayingRateTarget(new BigDecimal(target)));

        // then
        assertThat(alerts.stream().anyMatch(alert -> alert.kind() == AlertKind.LOW_LAYING)).isEqualTo(expected);
    }

    @Test
    @DisplayName("writes a target with a decimal in the alert")
    void givenTargetOf82Point5_whenListingTheAlerts_thenWriteItWithAComma() {
        // given
        CageWatch c03 = new CageWatch(UUID.randomUUID(), "C-03", 0, 225, 300, 3, null, null);

        // when
        DashboardAlert alert = alerts(null, List.of(c03), RANGE, QUIET_WEEK, new LayingRateTarget(new BigDecimal("82.5")))
                .stream()
                .filter(each -> each.kind() == AlertKind.LOW_LAYING)
                .findFirst()
                .orElseThrow();

        // then
        assertThat(alert.detail()).isEqualTo("75,0% nos últimos 3 relatórios, abaixo da meta de 82,5% do setor.");
    }

    // ---------------------------------------------------------------- avisos de pesagem (010)

    private static final WeighingSchedule FRIDAYS = new WeighingSchedule(DayOfWeek.FRIDAY);
    private static final WeighingSchedule EVERY_SEVEN_DAYS = new WeighingSchedule(null);
    private static final LocalDate FRIDAY = LocalDate.of(2026, 9, 25);
    private static final LocalDate SUNDAY = LocalDate.of(2026, 9, 27);

    /** Uma gaiola sem outro alerta, com a ultima pesagem valida no dia dado, ou nunca pesada. */
    private static CageWatch weighedOn(String code, String lastWeighedOn) {
        LocalDate day = lastWeighedOn == null ? null : LocalDate.parse(lastWeighedOn);
        return new CageWatch(UUID.randomUUID(), code, 0, 135, 150, 3, day, day == null ? null : new BigDecimal("160"));
    }

    /** A-01 e A-02 pesadas na quinta, 24/09; B-07 pela ultima vez em 18/09; C-03 nunca pesada. */
    private static List<CageWatch> galpaoUm() {
        return List.of(
                weighedOn("A-01", "2026-09-24"),
                weighedOn("A-02", "2026-09-24"),
                weighedOn("B-07", "2026-09-18"),
                weighedOn("C-03", null));
    }

    private static List<DashboardAlert> weighingAlerts(
            LocalDate today, WeighingSchedule schedule, List<CageWatch> cages) {
        return DashboardAlerts.of(
                        today,
                        completeToday(),
                        new CageWatchReading(cages, null, new MortalityBaseline(0, 0), schedule),
                        EIGHTY_FIVE)
                .stream()
                .filter(alert -> alert.kind() == AlertKind.WEIGHING_DUE || alert.kind() == AlertKind.WEIGHING_LATE)
                .toList();
    }

    @Test
    @DisplayName("tells, on the weighing day, how many cages are still to weigh and in which batteries")
    void givenWeighingOnFridaysAndTwoCagesWithoutTheWeighing_whenReadingOnFriday_thenInformTheWeighingOfToday() {
        // given
        List<CageWatch> cages = galpaoUm();

        // when
        List<DashboardAlert> alerts = weighingAlerts(FRIDAY, FRIDAYS, cages);

        // then
        assertThat(alerts).containsExactly(new DashboardAlert(
                AlertKind.WEIGHING_DUE,
                AlertTone.INFO,
                "Pesagem semanal hoje",
                "2 gaiolas a pesar, baterias B e C.",
                new AlertTarget(null, null, null)));
    }

    @Test
    @DisplayName("warns, after the weighing day, of the cages without the weighing of the week, since that day")
    void givenWeighingOnFridaysAndTwoCagesWithoutTheWeighing_whenReadingOnSunday_thenWarnOfTheLateWeighing() {
        // given
        List<CageWatch> cages = galpaoUm();

        // when
        List<DashboardAlert> alerts = weighingAlerts(SUNDAY, FRIDAYS, cages);

        // then
        assertThat(alerts).containsExactly(new DashboardAlert(
                AlertKind.WEIGHING_LATE,
                AlertTone.WARNING,
                "Pesagem atrasada",
                "2 gaiolas sem a pesagem de sexta-feira, 25/09, baterias B e C.",
                new AlertTarget(null, null, null)));
    }

    @Test
    @DisplayName("writes a single cage and a single battery in the singular")
    void givenOneCageWithoutTheWeighing_whenReadingOnFriday_thenWriteItInTheSingular() {
        // given
        List<CageWatch> cages = List.of(weighedOn("A-01", "2026-09-24"), weighedOn("B-07", "2026-09-18"));

        // when
        List<DashboardAlert> alerts = weighingAlerts(FRIDAY, FRIDAYS, cages);

        // then
        assertThat(alerts).extracting(DashboardAlert::detail).containsExactly("1 gaiola a pesar, bateria B.");
    }

    @Test
    @DisplayName("names each battery once, in the order of the cages, joined by commas and \"e\"")
    void givenCagesOfThreeBatteriesWithoutTheWeighing_whenReadingOnFriday_thenNameEachBatteryOnce() {
        // given
        List<CageWatch> cages = List.of(
                weighedOn("A-03", null),
                weighedOn("B-07", null),
                weighedOn("B-08", "2026-09-18"),
                weighedOn("C-03", null));

        // when
        List<DashboardAlert> alerts = weighingAlerts(FRIDAY, FRIDAYS, cages);

        // then
        assertThat(alerts).extracting(DashboardAlert::detail).containsExactly("4 gaiolas a pesar, baterias A, B e C.");
    }

    @Test
    @DisplayName("counts a weighing made after the weighing day for the week")
    void givenCageWeighedTheDayAfterTheWeighingDay_whenReadingOnSunday_thenNotCountItAsLate() {
        // given
        List<CageWatch> cages = List.of(weighedOn("B-07", "2026-09-26"), weighedOn("C-03", null));

        // when
        List<DashboardAlert> alerts = weighingAlerts(SUNDAY, FRIDAYS, cages);

        // then
        assertThat(alerts)
                .extracting(DashboardAlert::detail)
                .containsExactly("1 gaiola sem a pesagem de sexta-feira, 25/09, bateria C.");
    }

    @Test
    @DisplayName("without a weighing day, warns of the cages weighed more than 7 days ago, and never informs")
    void givenNoWeighingDay_whenReadingOnAnyDay_thenWarnOfTheCagesWeighedMoreThanSevenDaysAgo() {
        // given
        List<CageWatch> cages = List.of(weighedOn("A-01", "2026-09-18"), weighedOn("B-07", "2026-09-17"));

        // when
        List<DashboardAlert> onFriday = weighingAlerts(FRIDAY, EVERY_SEVEN_DAYS, cages);

        // then
        assertThat(onFriday).containsExactly(new DashboardAlert(
                AlertKind.WEIGHING_LATE,
                AlertTone.WARNING,
                "Pesagem atrasada",
                "1 gaiola sem pesagem há mais de 7 dias, bateria B.",
                new AlertTarget(null, null, null)));
    }

    @Test
    @DisplayName("gives no weighing alert with every active cage weighed in the week")
    void givenEveryCageWeighedInTheWeek_whenReadingOnFridayAndSunday_thenGiveNoWeighingAlert() {
        // given
        List<CageWatch> cages = List.of(weighedOn("A-01", "2026-09-19"), weighedOn("B-07", "2026-09-25"));

        // when
        List<DashboardAlert> onFriday = weighingAlerts(FRIDAY, FRIDAYS, cages);
        List<DashboardAlert> onSunday = weighingAlerts(SUNDAY, FRIDAYS, cages);

        // then
        assertThat(onFriday).isEmpty();
        assertThat(onSunday).isEmpty();
    }

    @Test
    @DisplayName("gives no weighing alert without the schedule of the sector, as in an inactive sector")
    void givenNoSchedule_whenReadingWithCagesWithoutWeighing_thenGiveNoWeighingAlert() {
        // given
        List<CageWatch> cages = galpaoUm();

        // when
        List<DashboardAlert> alerts = weighingAlerts(SUNDAY, null, cages);

        // then
        assertThat(alerts).isEmpty();
    }

    @Test
    @DisplayName("puts the weighing alert after the weight out of range, as the last one")
    void givenWeightOutOfRangeAndLateWeighing_whenReadingOnSunday_thenPutTheWeighingAlertLast() {
        // given
        CageWatch a02 = new CageWatch(
                UUID.randomUUID(), "A-02", 0, 135, 150, 3, LocalDate.of(2026, 9, 24), new BigDecimal("140"));
        CageWatch b07 = weighedOn("B-07", null);
        CageWatchReading reading =
                new CageWatchReading(List.of(a02, b07), new ReferenceWeight(155, 175), new MortalityBaseline(0, 0), FRIDAYS);

        // when
        List<DashboardAlert> alerts = DashboardAlerts.of(SUNDAY, completeToday(), reading, EIGHTY_FIVE);

        // then
        assertThat(alerts)
                .extracting(DashboardAlert::kind)
                .containsExactly(AlertKind.WEIGHT_OUT_OF_RANGE, AlertKind.WEIGHING_LATE);
    }
}
