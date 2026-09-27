package io.github.ovyx.farm.integration;

import static io.github.ovyx.farm.domain.model.FeedFormulaTestDataBuilder.aFormula;
import static io.github.ovyx.farm.domain.model.FeedFormulaTestDataBuilder.aUniqueFormula;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.ovyx.IntegrationTestSupport;
import io.github.ovyx.farm.application.common.StatusFilter;
import io.github.ovyx.farm.application.formula.FeedFormulaDirectory;
import io.github.ovyx.farm.application.formula.FeedFormulaSummary;
import io.github.ovyx.farm.domain.model.FeedFormula;
import io.github.ovyx.farm.domain.model.FeedFormulaId;
import io.github.ovyx.farm.domain.model.Status;
import io.github.ovyx.farm.domain.port.FeedFormulaRepository;
import io.github.ovyx.shared.domain.FixedClock;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * A leitura das fórmulas, direto do banco (US1 da 004; FR-005; R-007).
 *
 * <p>O banco é compartilhado com os outros testes: cada caso olha só as fórmulas que ele mesmo criou,
 * e confere que as que não deviam aparecer não aparecem.
 */
@DisplayName("Feed formula directory")
class FeedFormulaDirectoryIT extends IntegrationTestSupport {

    @Autowired
    private FeedFormulaDirectory directory;

    @Autowired
    private FeedFormulaRepository repository;

    /** Instantes inteiros: o banco guarda microssegundos, e o relógio do sistema pode ter mais. */
    private final FixedClock clock = FixedClock.at("2026-09-25T13:02:11Z");

    private FeedFormula saved(FeedFormula formula) {
        repository.save(formula);
        return formula;
    }

    private FeedFormula active(String name) {
        return saved(aFormula().named(name).withRoster(repository).withClock(clock).build());
    }

    private FeedFormula inactive() {
        return saved(aUniqueFormula().inactive().withClock(clock).build());
    }

    private static List<FeedFormulaId> idsAmong(List<FeedFormulaSummary> listed, FeedFormula... mine) {
        Set<FeedFormulaId> wanted = Arrays.stream(mine).map(FeedFormula::id).collect(Collectors.toSet());
        return listed.stream().map(FeedFormulaSummary::id).filter(wanted::contains).toList();
    }

    @Test
    @DisplayName("lists the active formulas by name, whatever the case, and leaves the inactive out")
    void givenActiveAndInactiveFormulas_whenListingTheActive_thenFindTheActiveByName() {
        // given
        String prefix = "Ordem " + UUID.randomUUID().toString().substring(0, 8);
        FeedFormula zeta = active(prefix + " — Zeta");
        FeedFormula alfa = active(prefix + " — alfa");
        FeedFormula beta = active(prefix + " — Beta");
        FeedFormula inactive = inactive();

        // when
        List<FeedFormulaSummary> listed = directory.list(StatusFilter.ACTIVE);

        // then
        assertThat(idsAmong(listed, zeta, alfa, beta, inactive)).containsExactly(alfa.id(), beta.id(), zeta.id());
    }

    @Test
    @DisplayName("lists only the inactive formulas when asked")
    void givenActiveAndInactiveFormulas_whenListingTheInactive_thenFindOnlyTheInactive() {
        // given
        FeedFormula active = saved(aUniqueFormula().withRoster(repository).withClock(clock).build());
        FeedFormula inactive = inactive();

        // when
        List<FeedFormulaSummary> listed = directory.list(StatusFilter.INACTIVE);

        // then
        assertThat(idsAmong(listed, active, inactive)).containsExactly(inactive.id());
        assertThat(listed).allSatisfy(summary -> assertThat(summary.status()).isEqualTo(Status.INACTIVE));
    }

    @Test
    @DisplayName("lists every formula when asked for all")
    void givenActiveAndInactiveFormulas_whenListingAll_thenFindBoth() {
        // given
        FeedFormula active = saved(aUniqueFormula().withRoster(repository).withClock(clock).build());
        FeedFormula inactive = inactive();

        // when
        List<FeedFormulaSummary> listed = directory.list(StatusFilter.ALL);

        // then
        assertThat(idsAmong(listed, active, inactive)).containsExactlyInAnyOrder(active.id(), inactive.id());
    }

    @Test
    @DisplayName("shows every field of the formula, with the cost per bird a day in three decimal places")
    void givenPosturaPlus_whenListing_thenFindItsFieldsAndTheCostPerBirdDay() {
        // given
        // R$ 2,85 o quilo × 28 g ÷ 1.000 = R$ 0,0798, arredondado a partir da metade: R$ 0,080.
        FeedFormula formula = saved(aUniqueFormula()
                .pricedAt("2,85")
                .expecting("28")
                .describedAs("Para codornas em postura")
                .withRoster(repository)
                .withClock(clock)
                .build());

        // when
        FeedFormulaSummary summary = directory.list(StatusFilter.ACTIVE).stream()
                .filter(listed -> listed.id().equals(formula.id()))
                .findFirst()
                .orElseThrow();

        // then
        assertThat(summary.name()).isEqualTo(formula.name().value());
        assertThat(summary.description()).isEqualTo("Para codornas em postura");
        assertThat(summary.pricePerKg()).isEqualByComparingTo("2.85").hasScaleOf(2);
        assertThat(summary.expectedIntake()).isEqualTo(28);
        assertThat(summary.costPerBirdDay()).isEqualByComparingTo("0.080").hasScaleOf(3);
        assertThat(summary.status()).isEqualTo(Status.ACTIVE);
        assertThat(summary.createdAt()).isEqualTo(Instant.parse("2026-09-25T13:02:11Z"));
        assertThat(summary.updatedAt()).isEqualTo(Instant.parse("2026-09-25T13:02:11Z"));
    }

    @Test
    @DisplayName("finds one formula, inactive included, with the cost per bird a day")
    void givenInactiveRecria_whenFindingIt_thenFindItsDetail() {
        // given
        // R$ 3,10 × 24 g ÷ 1.000 = R$ 0,0744: R$ 0,074.
        FeedFormula formula = saved(aUniqueFormula()
                .pricedAt("3,10")
                .expecting("24")
                .describedAs(null)
                .inactive()
                .withClock(clock)
                .build());

        // when
        FeedFormulaSummary found = directory.findById(formula.id()).orElseThrow();

        // then
        assertThat(found.costPerBirdDay()).isEqualByComparingTo("0.074").hasScaleOf(3);
        assertThat(found.description()).isNull();
        assertThat(found.status()).isEqualTo(Status.INACTIVE);
    }

    @Test
    @DisplayName("finds nothing for an identifier of no formula")
    void givenUnknownIdentifier_whenFinding_thenFindNothing() {
        // given
        FeedFormulaId unknown = FeedFormulaId.generate();

        // when
        Optional<FeedFormulaSummary> found = directory.findById(unknown);

        // then
        assertThat(found).isEmpty();
    }
}
