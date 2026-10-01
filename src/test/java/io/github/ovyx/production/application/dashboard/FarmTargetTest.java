package io.github.ovyx.production.application.dashboard;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.ovyx.production.application.dashboard.FarmTarget.WeightedTarget;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * A meta da granja (R-005 da 009): a media das metas dos setores ponderada pelas aves do inicio do dia dos
 * relatorios, a produtividade que a granja atinge quando cada setor atinge a sua.
 */
@DisplayName("FarmTarget")
class FarmTargetTest {

    private static WeightedTarget part(String target, int birds) {
        return new WeightedTarget(new LayingRateTarget(new BigDecimal(target)), birds);
    }

    @Test
    @DisplayName("weighs each target by the birds of its reports, rounding half up only at the end")
    void givenTwoSectorsOfDifferentSizes_whenWeighingTheirTargets_thenGiveTheWeightedAverage() {
        // given
        List<WeightedTarget> parts = List.of(part("85", 2000), part("72", 1200));

        // when
        LayingRateTarget target = FarmTarget.of(parts);

        // then
        assertThat(target.value()).isEqualByComparingTo("80.13").hasScaleOf(2);
    }

    @Test
    @DisplayName("gives the target of the only sector when it is alone")
    void givenOneSector_whenWeighingTheTargets_thenGiveItsTarget() {
        // given
        List<WeightedTarget> parts = List.of(part("82.5", 2000), part("82.5", 1800));

        // when
        LayingRateTarget target = FarmTarget.of(parts);

        // then
        assertThat(target.value()).isEqualByComparingTo("82.50");
    }

    @Test
    @DisplayName("counts every report, so the sector with more reports weighs more in the 7 days")
    void givenOneSectorWithMoreReports_whenWeighingTheTargets_thenWeighItMore() {
        // given
        List<WeightedTarget> parts =
                List.of(part("85", 2000), part("85", 2000), part("85", 2000), part("72", 1200), part("72", 1200));

        // when
        LayingRateTarget target = FarmTarget.of(parts);

        // then
        assertThat(target.value()).isEqualByComparingTo("81.29");
    }

    @Test
    @DisplayName("has no target without a report or without birds")
    void givenNoReportOrNoBirds_whenWeighingTheTargets_thenHaveNoTarget() {
        // given
        List<WeightedTarget> none = List.of();
        List<WeightedTarget> noBirds = List.of(part("85", 0));

        // when
        LayingRateTarget withoutReport = FarmTarget.of(none);
        LayingRateTarget withoutBirds = FarmTarget.of(noBirds);

        // then
        assertThat(withoutReport).isNull();
        assertThat(withoutBirds).isNull();
    }
}
