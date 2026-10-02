package io.github.ovyx.farm.domain.model;

import static io.github.ovyx.farm.domain.model.SectorTestDataBuilder.aSector;
import static io.github.ovyx.shared.domain.DomainViolations.detailsOf;
import static io.github.ovyx.shared.domain.DomainViolations.refusalCodeOf;
import static io.github.ovyx.shared.domain.DomainViolations.refusalMessageOf;
import static io.github.ovyx.shared.domain.DomainViolations.violationsOf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import io.github.ovyx.farm.domain.FarmErrorCode;
import io.github.ovyx.farm.domain.valueobject.LayingRateTarget;
import io.github.ovyx.farm.domain.valueobject.ReferenceWeight;
import io.github.ovyx.farm.domain.valueobject.SectorDescription;
import io.github.ovyx.farm.domain.valueobject.SectorName;
import io.github.ovyx.farm.domain.valueobject.WeighingDay;
import io.github.ovyx.farm.fixtures.InMemorySectorRepository;
import io.github.ovyx.shared.domain.FixedClock;
import io.github.ovyx.shared.domain.Violation;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * O agregado {@link Sector}: cadastro e edição (US1; FR-001 a FR-003, FR-017; invariante 1 do
 * data-model.md). Sem mock: o quadro dos setores é o repositório em memória.
 */
@DisplayName("Sector")
class SectorTest {

    private static final String NAME_IN_USE = "Já existe um setor ativo com este nome.";

    private final FixedClock clock = FixedClock.at("2026-09-25T13:02:11Z");
    private final InMemorySectorRepository roster = new InMemorySectorRepository();

    private Sector saved(SectorTestDataBuilder builder) {
        Sector sector = builder.withRoster(roster).withClock(clock).build();
        roster.save(sector);
        return sector;
    }

    @Test
    @DisplayName("registers an active sector, without cages")
    void givenValidNameAndDescription_whenRegistering_thenCreateAnActiveSectorWithoutCages() {
        // given
        String name = "Codornas — Galpão 4";

        // when
        Sector sector = Sector.register(name, "Codornas japonesas em postura, baterias A e B", null, null, "85", null, roster, clock);

        // then
        assertThat(sector.name().value()).isEqualTo(name);
        assertThat(sector.description())
                .map(SectorDescription::value)
                .contains("Codornas japonesas em postura, baterias A e B");
        assertThat(sector.status()).isEqualTo(Status.ACTIVE);
        assertThat(sector.createdAt()).isEqualTo(clock.instant());
        assertThat(sector.updatedAt()).isEqualTo(clock.instant());
    }

    @Test
    @DisplayName("registers a sector with a blank description as having no description")
    void givenBlankDescription_whenRegistering_thenKeepNoDescription() {
        // given
        String blank = "   ";

        // when
        Sector sector = Sector.register("Codornas — Galpão 4", blank, null, null, "85", null, roster, clock);

        // then
        assertThat(sector.description()).isEmpty();
    }

    @Test
    @DisplayName("refuses a short name and a long description at once, one violation per field")
    void givenShortNameAndLongDescription_whenRegistering_thenRefuseWithBothViolationsAtOnce() {
        // given
        String shortName = "A";
        String longDescription = "d".repeat(501);

        // when
        List<Violation> violations = violationsOf(() -> Sector.register(shortName, longDescription, null, null, "85", null, roster, clock));

        // then
        assertThat(violations)
                .extracting(Violation::field, Violation::code)
                .containsExactly(
                        tuple("name", FarmErrorCode.SECTOR_NAME_TOO_SHORT),
                        tuple(
                                "description", FarmErrorCode.SECTOR_DESCRIPTION_TOO_LONG));
        assertThat(refusalCodeOf(() -> Sector.register(shortName, longDescription, null, null, "85", null, roster, clock)))
                .isEqualTo(FarmErrorCode.VALIDATION_FAILED);
    }

    @Test
    @DisplayName("refuses the name of another active sector, differing only in case and spaces")
    void givenActiveSectorWithTheSameName_whenRegisteringWithOtherCaseAndSpaces_thenRefuseAsNameInUse() {
        // given
        saved(aSector().named("Codornas — Galpão 4"));

        // when
        Map<String, String> details =
                detailsOf(() -> Sector.register(" codornas — galpão 4 ", null, null, null, "85", null, roster, clock));

        // then
        assertThat(details).containsExactly(Map.entry("name", NAME_IN_USE));
        assertThat(refusalCodeOf(() -> Sector.register(" codornas — galpão 4 ", null, null, null, "85", null, roster, clock)))
                .isEqualTo(FarmErrorCode.SECTOR_NAME_IN_USE);
        assertThat(refusalMessageOf(() -> Sector.register(" codornas — galpão 4 ", null, null, null, "85", null, roster, clock)))
                .isEqualTo(NAME_IN_USE);
    }

    @Test
    @DisplayName("accepts the name of an inactive sector")
    void givenInactiveSectorWithTheSameName_whenRegistering_thenAcceptIt() {
        // given
        saved(aSector().named("Codornas — Galpão 4").inactive());

        // when
        Sector sector = Sector.register("Codornas — Galpão 4", null, null, null, "85", null, roster, clock);

        // then
        assertThat(sector.isActive()).isTrue();
    }

    @Test
    @DisplayName("refuses the fields before looking for the name in use")
    void givenNameInUseAndLongDescription_whenRegistering_thenRefuseForTheFieldsFirst() {
        // given
        // Mesma ordem da 001: primeiro o que a pessoa digitou errado, depois o conflito com os outros.
        saved(aSector().named("Codornas — Galpão 4"));
        String longDescription = "d".repeat(501);

        // when
        FarmErrorCode code = (FarmErrorCode)
                refusalCodeOf(() -> Sector.register("Codornas — Galpão 4", longDescription, null, null, "85", null, roster, clock));

        // then
        assertThat(code).isEqualTo(FarmErrorCode.VALIDATION_FAILED);
    }

    @Test
    @DisplayName("updates the name and the description, and the instant of the last change")
    void givenValidData_whenUpdating_thenChangeNameAndDescription() {
        // given
        Sector sector = saved(aSector());
        clock.advance(Duration.ofHours(2));

        // when
        sector.update("Codornas — Galpão 1 (norte)", "Baterias A a D, ala norte", null, null, "85", null, roster, clock);

        // then
        assertThat(sector.name().value()).isEqualTo("Codornas — Galpão 1 (norte)");
        assertThat(sector.description()).map(SectorDescription::value).contains("Baterias A a D, ala norte");
        assertThat(sector.updatedAt()).isEqualTo(clock.instant());
        assertThat(sector.createdAt()).isBefore(sector.updatedAt());
    }

    @Test
    @DisplayName("keeps its own name without conflicting with itself")
    void givenOwnNameInOtherCase_whenUpdating_thenAcceptIt() {
        // given
        Sector sector = saved(aSector().named("Codornas — Galpão 1"));

        // when
        sector.update("CODORNAS — GALPÃO 1", null, null, null, "85", null, roster, clock);

        // then
        assertThat(sector.name().value()).isEqualTo("CODORNAS — GALPÃO 1");
        assertThat(sector.description()).isEmpty();
    }

    @Test
    @DisplayName("refuses the name of another active sector and keeps its data")
    void givenNameOfAnotherActiveSector_whenUpdating_thenRefuseAsNameInUseAndKeepTheData() {
        // given
        saved(aSector().named("Poedeiras brancas — Galpão 2"));
        Sector sector = saved(aSector().named("Codornas — Galpão 1"));

        // when
        FarmErrorCode code = (FarmErrorCode) refusalCodeOf(
                () -> sector.update("poedeiras brancas — galpão 2", null, null, null, "85", null, roster, clock));

        // then
        assertThat(code).isEqualTo(FarmErrorCode.SECTOR_NAME_IN_USE);
        assertThat(sector.name().value()).isEqualTo("Codornas — Galpão 1");
    }

    @Test
    @DisplayName("refuses every invalid field at once and keeps its data")
    void givenMissingNameAndLongDescription_whenUpdating_thenRefuseWithAllViolationsAndKeepTheData() {
        // given
        Sector sector = saved(aSector());

        // when
        Map<String, String> details = detailsOf(() -> sector.update("  ", "d".repeat(501), null, null, "85", null, roster, clock));

        // then
        assertThat(details)
                .containsExactly(
                        Map.entry("name", "Informe o nome do setor."),
                        Map.entry("description", "A descrição deve ter no máximo 500 caracteres."));
        assertThat(sector.name().value()).isEqualTo("Codornas — Galpão 1");
    }

    // ---------------------------------------------------------------- faixa de peso de referência (005)

    @Test
    @DisplayName("registers a sector with the reference weight range")
    void givenReferenceWeight_whenRegistering_thenKeepTheRange() {
        // given
        String minimum = "155";

        // when
        Sector sector = Sector.register("Codornas — Galpão 4", null, minimum, "175", "85", null, roster, clock);

        // then
        assertThat(sector.referenceWeight()).contains(new ReferenceWeight(155, 175));
    }

    @Test
    @DisplayName("refuses an inverted range together with an invalid name, at once")
    void givenInvertedRangeAndMissingName_whenRegistering_thenRefuseBothAtOnce() {
        // given
        String blankName = "  ";

        // when
        Map<String, String> details = detailsOf(() -> Sector.register(blankName, null, "180", "170", "85", null, roster, clock));

        // then
        assertThat(details)
                .containsOnly(
                        Map.entry("name", "Informe o nome do setor."),
                        Map.entry("minimumWeight", "O peso mínimo deve ser menor que o máximo."));
    }

    @Test
    @DisplayName("sets the range on update, and clears it when both limits are left out")
    void givenSector_whenUpdatingWithAndWithoutTheRange_thenSetAndClearIt() {
        // given
        Sector sector = saved(aSector());

        // when
        sector.update(sector.name().value(), null, "155", "175", "85", null, roster, clock);
        Optional<ReferenceWeight> set = sector.referenceWeight();
        sector.update(sector.name().value(), null, null, null, "85", null, roster, clock);

        // then
        assertThat(set).contains(new ReferenceWeight(155, 175));
        assertThat(sector.referenceWeight()).isEmpty();
    }

    @Test
    @DisplayName("keeps the range when the update is refused")
    void givenSectorWithRange_whenTheUpdateIsRefused_thenKeepTheRange() {
        // given
        Sector sector = saved(aSector().withReferenceWeight(155, 175));

        // when
        detailsOf(() -> sector.update(sector.name().value(), null, "155", null, "85", null, roster, clock));

        // then
        assertThat(sector.referenceWeight()).contains(new ReferenceWeight(155, 175));
    }

    // ---------------------------------------------------------------- meta de produtividade (008)

    @Test
    @DisplayName("registers a sector with the laying rate target typed with a comma")
    void givenTargetWithComma_whenRegistering_thenKeepTheTarget() {
        // given
        String target = "82,5";

        // when
        Sector sector = Sector.register("Codornas — Galpão 4", null, null, null, target, null, roster, clock);

        // then
        assertThat(sector.layingRateTarget()).isEqualTo(new LayingRateTarget(new BigDecimal("82.5")));
    }

    @Test
    @DisplayName("changes the laying rate target on update")
    void givenSectorWith85_whenUpdatingTheTargetTo72_thenKeep72() {
        // given
        Sector sector = saved(aSector());

        // when
        sector.update(sector.name().value(), null, null, null, "72", null, roster, clock);

        // then
        assertThat(sector.layingRateTarget().value()).isEqualByComparingTo("72.0");
    }

    @Test
    @DisplayName("refuses a missing target together with the other fields, in the order of the form")
    void givenMissingNameInvertedRangeAndMissingTarget_whenRegistering_thenRefuseAllAtOnceInOrder() {
        // given
        String blankTarget = " ";

        // when
        Map<String, String> details =
                detailsOf(() -> Sector.register("  ", "d".repeat(501), "180", "170", blankTarget, null, roster, clock));

        // then
        assertThat(details)
                .containsExactly(
                        Map.entry("name", "Informe o nome do setor."),
                        Map.entry("description", "A descrição deve ter no máximo 500 caracteres."),
                        Map.entry("minimumWeight", "O peso mínimo deve ser menor que o máximo."),
                        Map.entry("layingRateTarget", "Informe a meta de produtividade, de 1 a 100%."));
    }

    @Test
    @DisplayName("keeps every field when an update is refused for the target")
    void givenSector_whenTheUpdateIsRefusedForTheTarget_thenChangeNothing() {
        // given
        Sector sector = saved(aSector().withReferenceWeight(155, 175).withLayingRateTarget("82,5"));
        Instant updatedAt = sector.updatedAt();
        clock.advance(Duration.ofHours(1));

        // when
        Map<String, String> details = detailsOf(
                () -> sector.update("Codornas — Galpão 1 (norte)", null, "150", "180", "101", null, roster, clock));

        // then
        assertThat(details).containsOnly(Map.entry("layingRateTarget", "A meta deve ficar entre 1% e 100%."));
        assertThat(sector.name().value()).isEqualTo("Codornas — Galpão 1");
        assertThat(sector.referenceWeight()).contains(new ReferenceWeight(155, 175));
        assertThat(sector.layingRateTarget().value()).isEqualByComparingTo("82.5");
        assertThat(sector.updatedAt()).isEqualTo(updatedAt);
    }

    @Test
    @DisplayName("keeps the target through deactivation and reactivation")
    void givenSectorWith72_whenDeactivatingAndReactivating_thenKeep72() {
        // given
        Sector sector = saved(aSector().withLayingRateTarget("72"));

        // when
        sector.deactivate(clock);
        sector.reactivate(roster, clock);

        // then
        assertThat(sector.layingRateTarget().value()).isEqualByComparingTo("72.0");
    }

    @Test
    @DisplayName("restores the target it was given")
    void givenStoredTarget_whenRestoring_thenKeepIt() {
        // given
        LayingRateTarget stored = new LayingRateTarget(new BigDecimal("91.5"));

        // when
        Sector sector = Sector.restore(
                SectorId.generate(),
                SectorName.of("Poedeiras — Galpão 2"),
                null,
                null,
                stored,
                null,
                Status.ACTIVE,
                List.of(),
                clock.instant(),
                clock.instant());

        // then
        assertThat(sector.layingRateTarget()).isEqualTo(stored);
    }

    // ---------------------------------------------------------------- dia da pesagem (010)

    @Test
    @DisplayName("registers a sector with the weighing day")
    void givenFriday_whenRegistering_thenKeepTheWeighingDay() {
        // given
        String weighingDay = "FRIDAY";

        // when
        Sector sector = Sector.register("Codornas — Galpão 4", null, null, null, "85", weighingDay, roster, clock);

        // then
        assertThat(sector.weighingDay()).map(WeighingDay::value).contains(DayOfWeek.FRIDAY);
    }

    @Test
    @DisplayName("registers a sector without a weighing day when none is given")
    void givenNoWeighingDay_whenRegistering_thenFindNoWeighingDay() {
        // given
        String weighingDay = null;

        // when
        Sector sector = Sector.register("Codornas — Galpão 4", null, null, null, "85", weighingDay, roster, clock);

        // then
        assertThat(sector.weighingDay()).isEmpty();
    }

    @Test
    @DisplayName("takes the weighing day away on update")
    void givenSectorWeighedOnFridays_whenUpdatingWithoutTheDay_thenFindNoWeighingDay() {
        // given
        Sector sector = saved(aSector().withWeighingDay("FRIDAY"));

        // when
        sector.update(sector.name().value(), null, null, null, "85", null, roster, clock);

        // then
        assertThat(sector.weighingDay()).isEmpty();
    }

    @Test
    @DisplayName("changes the weighing day on update")
    void givenSectorWeighedOnFridays_whenUpdatingToMonday_thenKeepMonday() {
        // given
        Sector sector = saved(aSector().withWeighingDay("FRIDAY"));

        // when
        sector.update(sector.name().value(), null, null, null, "85", "MONDAY", roster, clock);

        // then
        assertThat(sector.weighingDay()).map(WeighingDay::value).contains(DayOfWeek.MONDAY);
    }

    @Test
    @DisplayName("refuses an invalid weighing day together with the other fields, in the order of the form")
    void givenMissingNameAndInvalidDay_whenRegistering_thenRefuseBothAtOnceInOrder() {
        // given
        String invalidDay = "FUNDAY";

        // when
        Map<String, String> details =
                detailsOf(() -> Sector.register("  ", null, null, null, "85", invalidDay, roster, clock));

        // then
        assertThat(details)
                .containsExactly(
                        Map.entry("name", "Informe o nome do setor."),
                        Map.entry("weighingDay", "Escolha um dia da semana, de segunda a domingo."));
    }

    @Test
    @DisplayName("keeps the weighing day when an update is refused for it")
    void givenSectorWeighedOnFridays_whenTheUpdateIsRefusedForTheDay_thenChangeNothing() {
        // given
        Sector sector = saved(aSector().withWeighingDay("FRIDAY"));

        // when
        Map<String, String> details =
                detailsOf(() -> sector.update("Outro nome", null, null, null, "85", "FUNDAY", roster, clock));

        // then
        assertThat(details).containsOnlyKeys("weighingDay");
        assertThat(sector.name().value()).isEqualTo("Codornas — Galpão 1");
        assertThat(sector.weighingDay()).map(WeighingDay::value).contains(DayOfWeek.FRIDAY);
    }

    @Test
    @DisplayName("restores the weighing day it was given")
    void givenStoredWeighingDay_whenRestoring_thenKeepIt() {
        // given
        WeighingDay stored = new WeighingDay(DayOfWeek.SATURDAY);

        // when
        Sector sector = Sector.restore(
                SectorId.generate(),
                SectorName.of("Poedeiras — Galpão 2"),
                null,
                null,
                LayingRateTarget.of("85"),
                stored,
                Status.ACTIVE,
                List.of(),
                clock.instant(),
                clock.instant());

        // then
        assertThat(sector.weighingDay()).contains(stored);
    }
}
