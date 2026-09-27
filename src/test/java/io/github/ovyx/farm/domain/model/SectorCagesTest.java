package io.github.ovyx.farm.domain.model;

import static io.github.ovyx.farm.domain.model.SectorTestDataBuilder.aSector;
import static io.github.ovyx.shared.domain.DomainViolations.detailsOf;
import static io.github.ovyx.shared.domain.DomainViolations.refusalCodeOf;
import static io.github.ovyx.shared.domain.DomainViolations.refusalMessageOf;
import static io.github.ovyx.shared.domain.DomainViolations.violationsOf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import io.github.ovyx.farm.domain.FarmErrorCode;
import io.github.ovyx.shared.domain.FixedClock;
import io.github.ovyx.shared.domain.Violation;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * As gaiolas dentro do agregado {@link Sector}: cadastro, edição, código e unicidade (US2; FR-006 a
 * FR-009; invariantes 2 e 4 do data-model.md). Sem mock.
 */
@DisplayName("Sector cages")
class SectorCagesTest {

    private static final String B07_EXISTS = "Já existe uma gaiola ativa B-07 neste setor.";

    private final FixedClock clock = FixedClock.at("2026-09-25T13:10:42Z");

    @Test
    @DisplayName("registers an active cage, not deactivated with the sector, with its code")
    void givenValidCage_whenRegistering_thenAddAnActiveCageWithItsCode() {
        // given
        Sector sector = aSector().withClock(clock).build();
        clock.advance(Duration.ofMinutes(5));

        // when
        CageId id = sector.registerCage("b", "7", "50", clock);

        // then
        Cage cage = sector.cage(id).orElseThrow();
        assertThat(cage.code()).isEqualTo("B-07");
        assertThat(cage.battery().value()).isEqualTo("B");
        assertThat(cage.number().value()).isEqualTo(7);
        assertThat(cage.birdCount().value()).isEqualTo(50);
        assertThat(cage.isActive()).isTrue();
        assertThat(cage.deactivatedWithSector()).isFalse();
        assertThat(cage.createdAt()).isEqualTo(clock.instant());
        assertThat(sector.updatedAt()).isEqualTo(clock.instant());
        assertThat(sector.cages()).containsExactly(cage);
    }

    @Test
    @DisplayName("accepts an empty cage, with zero birds")
    void givenZeroBirds_whenRegistering_thenAcceptAnEmptyCage() {
        // given
        Sector sector = aSector().withClock(clock).build();

        // when
        CageId id = sector.registerCage("A", "3", "0", clock);

        // then
        assertThat(sector.cage(id).orElseThrow().birdCount().value()).isZero();
    }

    @Test
    @DisplayName("refuses an empty battery, number zero and negative birds at once")
    void givenThreeInvalidFields_whenRegistering_thenRefuseWithTheThreeViolationsAtOnce() {
        // given
        Sector sector = aSector().withClock(clock).build();

        // when
        List<Violation> violations = violationsOf(() -> sector.registerCage("", "0", "-1", clock));

        // then
        assertThat(violations)
                .extracting(Violation::field, Violation::code)
                .containsExactly(
                        tuple("battery", FarmErrorCode.CAGE_BATTERY_REQUIRED),
                        tuple("number", FarmErrorCode.CAGE_NUMBER_OUT_OF_RANGE),
                        tuple("birdCount", FarmErrorCode.BIRD_COUNT_OUT_OF_RANGE));
        assertThat(refusalCodeOf(() -> sector.registerCage("", "0", "-1", clock)))
                .isEqualTo(FarmErrorCode.VALIDATION_FAILED);
        assertThat(sector.cages()).isEmpty();
    }

    @ParameterizedTest(name = "battery \"{0}\", number \"{1}\"")
    @CsvSource({"B, 7", "b, 07", " B , 7"})
    @DisplayName("refuses the battery and number of another active cage, in both fields")
    void givenActiveCageB07_whenRegisteringTheSameBatteryAndNumber_thenRefuseAsAlreadyExisting(
            String battery, String number) {
        // given
        Sector sector = aSector().withCage("B", 7, 50).withClock(clock).build();

        // when
        Map<String, String> details = detailsOf(() -> sector.registerCage(battery, number, "48", clock));

        // then
        assertThat(details).containsExactly(Map.entry("battery", B07_EXISTS), Map.entry("number", B07_EXISTS));
        assertThat(refusalCodeOf(() -> sector.registerCage(battery, number, "48", clock)))
                .isEqualTo(FarmErrorCode.CAGE_ALREADY_EXISTS);
        assertThat(refusalMessageOf(() -> sector.registerCage(battery, number, "48", clock)))
                .isEqualTo(B07_EXISTS);
        assertThat(sector.cages()).hasSize(1);
    }

    @Test
    @DisplayName("accepts the battery and number of an inactive cage")
    void givenInactiveCageB07_whenRegisteringB07_thenAcceptIt() {
        // given
        Sector sector = aSector().withInactiveCage("B", 7, 50).withClock(clock).build();

        // when
        CageId id = sector.registerCage("B", "7", "50", clock);

        // then
        assertThat(sector.cage(id).orElseThrow().isActive()).isTrue();
        assertThat(sector.cages()).hasSize(2);
    }

    @Test
    @DisplayName("updates the battery, the number and the birds, and the instants of the change")
    void givenCage_whenUpdating_thenChangeItsFields() {
        // given
        Sector sector = aSector().withClock(clock).build();
        CageId id = sector.registerCage("B", "7", "50", clock);
        clock.advance(Duration.ofHours(1));

        // when
        sector.updateCage(id, "C", "120", "48", clock);

        // then
        Cage cage = sector.cage(id).orElseThrow();
        assertThat(cage.code()).isEqualTo("C-120");
        assertThat(cage.birdCount().value()).isEqualTo(48);
        assertThat(cage.updatedAt()).isEqualTo(clock.instant());
        assertThat(sector.updatedAt()).isEqualTo(clock.instant());
    }

    @Test
    @DisplayName("keeps its own battery and number without conflicting with itself")
    void givenCage_whenUpdatingOnlyTheBirds_thenAcceptIt() {
        // given
        Sector sector = aSector().withClock(clock).build();
        CageId id = sector.registerCage("B", "7", "50", clock);

        // when
        sector.updateCage(id, "B", "07", "48", clock);

        // then
        assertThat(sector.cage(id).orElseThrow().birdCount().value()).isEqualTo(48);
    }

    @Test
    @DisplayName("refuses on update the battery and number of another active cage, and keeps the data")
    void givenTwoCages_whenUpdatingOneToTheCodeOfTheOther_thenRefuseAsAlreadyExisting() {
        // given
        Sector sector = aSector().withCage("B", 7, 50).withClock(clock).build();
        CageId other = sector.registerCage("A", "12", "40", clock);

        // when
        FarmErrorCode code = (FarmErrorCode) refusalCodeOf(() -> sector.updateCage(other, "B", "7", "40", clock));

        // then
        assertThat(code).isEqualTo(FarmErrorCode.CAGE_ALREADY_EXISTS);
        assertThat(sector.cage(other).orElseThrow().code()).isEqualTo("A-12");
    }

    @Test
    @DisplayName("refuses every invalid field of an update at once, and keeps the data")
    void givenCage_whenUpdatingWithInvalidFields_thenRefuseWithAllViolationsAndKeepTheData() {
        // given
        Sector sector = aSector().withClock(clock).build();
        CageId id = sector.registerCage("B", "7", "50", clock);

        // when
        Map<String, String> details = detailsOf(() -> sector.updateCage(id, "ABCD", "sete", "12.5", clock));

        // then
        assertThat(details).containsOnlyKeys("battery", "number", "birdCount");
        assertThat(sector.cage(id).orElseThrow().code()).isEqualTo("B-07");
    }

    @Test
    @DisplayName("refuses to update a cage the sector does not have")
    void givenCageOfNoSector_whenUpdating_thenRefuseAsNotFound() {
        // given
        Sector sector = aSector().withCage("B", 7, 50).withClock(clock).build();

        // when
        FarmErrorCode code =
                (FarmErrorCode) refusalCodeOf(() -> sector.updateCage(CageId.generate(), "B", "8", "50", clock));

        // then
        assertThat(code).isEqualTo(FarmErrorCode.CAGE_NOT_FOUND);
    }

    @ParameterizedTest(name = "{0} {1} is {2}")
    @CsvSource({"B, 7, B-07", "A, 12, A-12", "C, 120, C-120", "A1, 1, A1-01"})
    @DisplayName("writes the code as the battery, a hyphen and the number with at least two digits")
    void givenBatteryAndNumber_whenWritingTheCode_thenPadTheNumberToTwoDigits(String battery, int number, String code) {
        // given
        Sector sector = aSector().withCage(battery, number, 10).withClock(clock).build();

        // when
        String written = sector.cages().getFirst().code();

        // then
        assertThat(written).isEqualTo(code);
    }
}
