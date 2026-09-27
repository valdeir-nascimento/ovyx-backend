package io.github.ovyx.farm.integration;

import static io.github.ovyx.farm.domain.model.FeedFormulaTestDataBuilder.aUniqueFormula;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.ovyx.IntegrationTestSupport;
import io.github.ovyx.farm.domain.model.FeedFormula;
import io.github.ovyx.farm.domain.model.FeedFormulaId;
import io.github.ovyx.farm.domain.port.FeedFormulaRepository;
import io.github.ovyx.farm.domain.valueobject.FeedFormulaDescription;
import io.github.ovyx.farm.domain.valueobject.FeedFormulaName;
import io.github.ovyx.shared.domain.FixedClock;
import java.math.BigDecimal;
import java.util.Map;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * O repositório de fórmulas contra PostgreSQL real (R-011 e R-012 da 004).
 *
 * <p>Cada gravação e cada leitura passam pelo adaptador, em transações separadas: o que volta vem do
 * banco, e não de um cache do ORM.
 */
@DisplayName("Feed formula repository")
class FeedFormulaRepositoryIT extends IntegrationTestSupport {

    @Autowired
    private FeedFormulaRepository repository;

    @Autowired
    private JdbcTemplate jdbc;

    /** Instantes inteiros: o banco guarda microssegundos, e o relógio do sistema pode ter mais. */
    private final FixedClock clock = FixedClock.at("2026-09-25T13:02:11Z");

    private FeedFormula saved(FeedFormula formula) {
        repository.save(formula);
        return formula;
    }

    private Map<String, Object> rowOf(FeedFormulaId id) {
        return jdbc.queryForMap(
                "select name, price_per_kg, expected_intake, description, status, version from feed_formula"
                        + " where id = ?",
                id.value());
    }

    @Test
    @DisplayName("saves the formula and reads it back as it was, with the price in two decimal places")
    void givenRegisteredFormula_whenSavingAndReadingBack_thenFindTheSameData() {
        // given
        FeedFormula formula = aUniqueFormula()
                .pricedAt("2,85")
                .expecting("28")
                .describedAs("Milho, farelo de soja e calcário")
                .withRoster(repository)
                .withClock(clock)
                .build();

        // when
        repository.save(formula);

        // then
        FeedFormula read = repository.findById(formula.id()).orElseThrow();
        assertThat(read.name()).isEqualTo(formula.name());
        assertThat(read.pricePerKg().value()).isEqualByComparingTo("2.85").hasScaleOf(2);
        assertThat(read.expectedIntake().value()).isEqualTo(28);
        assertThat(read.description()).map(FeedFormulaDescription::value).contains("Milho, farelo de soja e calcário");
        assertThat(read.isActive()).isTrue();
        assertThat(read.createdAt()).isEqualTo(formula.createdAt());
        assertThat(rowOf(formula.id()))
                .containsEntry("status", "ACTIVE")
                .containsEntry("price_per_kg", new BigDecimal("2.85"));
    }

    @Test
    @DisplayName("saves a formula without description as a null column")
    void givenFormulaWithoutDescription_whenSaving_thenStoreNoDescription() {
        // given
        FeedFormula formula = aUniqueFormula().describedAs(null).withRoster(repository).withClock(clock).build();

        // when
        repository.save(formula);

        // then
        assertThat(rowOf(formula.id())).containsEntry("description", null);
        assertThat(repository.findById(formula.id()).orElseThrow().description()).isEmpty();
    }

    @Test
    @DisplayName("starts the version at zero and raises it on every change")
    void givenSavedFormula_whenUpdatingAndSaving_thenRaiseTheVersion() {
        // given
        FeedFormula formula = saved(aUniqueFormula().withRoster(repository).withClock(clock).build());
        long before = (long) rowOf(formula.id()).get("version");
        FeedFormula loaded = repository.findById(formula.id()).orElseThrow();
        loaded.update(loaded.name().value(), "2,90", "28", null, repository, clock);

        // when
        repository.save(loaded);

        // then
        assertThat(before).isZero();
        assertThat(rowOf(formula.id()))
                .containsEntry("version", 1L)
                .containsEntry("price_per_kg", new BigDecimal("2.90"));
    }

    @Test
    @DisplayName("keeps a deactivated formula, with its data")
    void givenSavedFormula_whenDeactivatingAndSaving_thenKeepItInactive() {
        // given
        FeedFormula formula = saved(aUniqueFormula().withRoster(repository).withClock(clock).build());
        FeedFormula loaded = repository.findById(formula.id()).orElseThrow();
        loaded.deactivate(clock);

        // when
        repository.save(loaded);

        // then
        assertThat(rowOf(formula.id())).containsEntry("status", "INACTIVE").containsEntry("expected_intake", 28);
        assertThat(repository.findById(formula.id()).orElseThrow().isActive()).isFalse();
    }

    @Test
    @DisplayName("finds the name of another formula, active or inactive, whatever the case")
    void givenInactiveFormula_whenAskingForItsNameInOtherCase_thenFindItInUse() {
        // given
        FeedFormula inactive = saved(aUniqueFormula().inactive().withClock(clock).build());
        FeedFormulaName otherCase = FeedFormulaName.of(inactive.name().value().toUpperCase());

        // when
        boolean inUse = repository.nameInUse(otherCase, FeedFormulaId.generate());

        // then
        assertThat(inUse).isTrue();
    }

    @Test
    @DisplayName("does not count the formula itself as using its own name")
    void givenFormula_whenAskingForItsOwnName_thenFindItFree() {
        // given
        FeedFormula formula = saved(aUniqueFormula().withRoster(repository).withClock(clock).build());

        // when
        boolean inUse = repository.nameInUse(formula.name(), formula.id());

        // then
        assertThat(inUse).isFalse();
    }

    @Test
    @DisplayName("lets the database refuse a second formula with the same name, the net of the race")
    void givenFormula_whenSavingAnotherWithTheSameNameDirectly_thenRefuseAtTheDatabase() {
        // given
        FeedFormula first = aUniqueFormula().withClock(clock).build();
        FeedFormula second =
                aUniqueFormula().named(first.name().value().toLowerCase()).withClock(clock).build();
        repository.save(first);

        // when
        ThrowingCallable savingTheSecond = () -> repository.save(second);

        // then
        assertThatThrownBy(savingTheSecond)
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ux_feed_formula_name");
    }
}
