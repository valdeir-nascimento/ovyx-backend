package io.github.ovyx.farm.domain.model;

import static io.github.ovyx.farm.domain.model.SectorTestDataBuilder.aSector;
import static io.github.ovyx.shared.domain.DomainViolations.detailsOf;
import static io.github.ovyx.shared.domain.DomainViolations.refusalCodeOf;
import static io.github.ovyx.shared.domain.DomainViolations.refusalMessageOf;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.ovyx.farm.domain.FarmErrorCode;
import io.github.ovyx.farm.fixtures.InMemoryWeighingRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * O agregado {@link Weighing}: o registro da pesagem (US1 da 005; FR-003, FR-004, FR-007, FR-008, FR-014;
 * R-003). Sem mock: o setor é o do builder da 002, e o quadro das datas é o repositório em memória.
 */
@DisplayName("Weighing")
class WeighingTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 9, 24);
    private static final Instant NOW = Instant.parse("2026-09-24T10:12:40Z");
    private static final Actor MARINA = new Actor(UUID.fromString("5e7a9c1e-3b5d-4f7a-9c1e-3b5d7f9a1c22"), "Marina Alves");

    private final InMemoryWeighingRepository roster = new InMemoryWeighingRepository();

    private final Sector sector = aSector().withCage("A", 1, 48).withCage("B", 7, 50).build();
    private final CageId a01 = sector.cages().get(0).id();
    private final CageId b07 = sector.cages().get(1).id();

    private Weighing record(Sector of, CageId cageId, String day, String weight) {
        return Weighing.record(of, cageId, day, weight, TODAY, roster, MARINA, NOW);
    }

    private Weighing recorded(CageId cageId, String day, String weight) {
        Weighing weighing = record(sector, cageId, day, weight);
        roster.save(weighing);
        return weighing;
    }

    @Test
    @DisplayName("records a valid weighing of the cage, with who recorded it and when")
    void givenValidDayAndWeight_whenRecording_thenCreateAValidWeighing() {
        // given
        String weight = "161,4";

        // when
        Weighing weighing = record(sector, a01, "2026-09-24", weight);

        // then
        assertThat(weighing.sectorId()).isEqualTo(sector.id());
        assertThat(weighing.cageId()).isEqualTo(a01);
        assertThat(weighing.weighedOn().value()).isEqualTo(TODAY);
        assertThat(weighing.averageWeight().value()).isEqualByComparingTo("161.4");
        assertThat(weighing.status()).isEqualTo(WeighingStatus.VALID);
        assertThat(weighing.isValid()).isTrue();
        assertThat(weighing.recordedBy()).isEqualTo(MARINA);
        assertThat(weighing.recordedAt()).isEqualTo(NOW);
        assertThat(weighing.lastCorrectedBy()).isEmpty();
        assertThat(weighing.voidedBy()).isEmpty();
    }

    @Test
    @DisplayName("refuses a cage that is not of the sector")
    void givenCageOfAnotherSector_whenRecording_thenRefuseAsCageNotFound() {
        // given
        CageId elsewhere = aSector().withCage("A", 1, 48).build().cages().get(0).id();

        // when / then
        assertThat(refusalCodeOf(() -> record(sector, elsewhere, "2026-09-24", "161")))
                .isEqualTo(FarmErrorCode.CAGE_NOT_FOUND);
    }

    @Test
    @DisplayName("refuses a weighing in an inactive sector, which is only consulted")
    void givenInactiveSector_whenRecording_thenRefuseAsSectorInactive() {
        // given
        Sector inactive = aSector().withCage("A", 1, 48).inactive().build();
        CageId cage = inactive.cages().get(0).id();

        // when / then
        assertThat(refusalCodeOf(() -> record(inactive, cage, "2026-09-24", "161")))
                .isEqualTo(FarmErrorCode.SECTOR_INACTIVE);
        assertThat(refusalMessageOf(() -> record(inactive, cage, "2026-09-24", "161")))
                .isEqualTo("O setor está inativo; as pesagens dele são só para consulta.");
    }

    @Test
    @DisplayName("refuses a weighing of an inactive cage, which is only consulted")
    void givenInactiveCage_whenRecording_thenRefuseAsCageInactive() {
        // given
        Sector withInactiveCage = aSector().withInactiveCage("A", 1, 48).build();
        CageId cage = withInactiveCage.cages().get(0).id();

        // when / then
        assertThat(refusalCodeOf(() -> record(withInactiveCage, cage, "2026-09-24", "161")))
                .isEqualTo(FarmErrorCode.CAGE_INACTIVE);
        assertThat(refusalMessageOf(() -> record(withInactiveCage, cage, "2026-09-24", "161")))
                .isEqualTo("A gaiola está inativa; as pesagens dela são só para consulta.");
    }

    @Test
    @DisplayName("refuses the day and the weight at once, each in its field")
    void givenFutureDayAndMissingWeight_whenRecording_thenRefuseBothAtOnce() {
        // given
        String tomorrow = "2026-09-25";

        // when
        Map<String, String> details = detailsOf(() -> record(sector, a01, tomorrow, ""));

        // then
        assertThat(details)
                .containsOnly(
                        Map.entry("weighedOn", "A data da pesagem não pode ser futura."),
                        Map.entry("averageWeight", "Informe o peso médio em gramas."));
        assertThat(refusalCodeOf(() -> record(sector, a01, tomorrow, ""))).isEqualTo(FarmErrorCode.VALIDATION_FAILED);
    }

    @Test
    @DisplayName("refuses a day the cage already has a weighing on, in the day field")
    void givenWeighingOnTheDay_whenRecordingAnotherOnTheSameDay_thenRefuseAsDayInUse() {
        // given
        recorded(a01, "2026-09-24", "161");

        // when
        Map<String, String> details = detailsOf(() -> record(sector, a01, "2026-09-24", "158"));

        // then
        assertThat(refusalCodeOf(() -> record(sector, a01, "2026-09-24", "158")))
                .isEqualTo(FarmErrorCode.WEIGHING_DATE_IN_USE);
        assertThat(details)
                .containsOnly(Map.entry(
                        "weighedOn", "A gaiola já tem pesagem em 24/09/2026. Corrija a pesagem desse dia."));
    }

    @Test
    @DisplayName("checks the day in use only after the fields are valid")
    void givenWeighingOnTheDay_whenRecordingTheSameDayWithAnInvalidWeight_thenRefuseTheWeightFirst() {
        // given
        recorded(a01, "2026-09-24", "161");

        // when
        FarmErrorCode code = (FarmErrorCode) refusalCodeOf(() -> record(sector, a01, "2026-09-24", "0"));

        // then
        assertThat(code).isEqualTo(FarmErrorCode.VALIDATION_FAILED);
    }

    @Test
    @DisplayName("accepts the same day in another cage")
    void givenWeighingOnTheDay_whenRecordingTheSameDayInAnotherCage_thenAcceptIt() {
        // given
        recorded(a01, "2026-09-24", "161");

        // when
        Weighing other = record(sector, b07, "2026-09-24", "158");

        // then
        assertThat(other.cageId()).isEqualTo(b07);
    }

    // ---------------------------------------------------------------- correção (US4)

    private static final Instant LATER = Instant.parse("2026-09-24T10:15:02Z");
    private static final Actor ADMIN = new Actor(UUID.fromString("1c3e5a7c-9d1f-4b3d-8e5a-7c9d1f3b5e88"), "Administrador");

    @Test
    @DisplayName("corrects the day and the weight, and marks who corrected it and when")
    void givenWeighing_whenCorrecting_thenChangeItAndMarkTheCorrection() {
        // given
        Weighing weighing = recorded(a01, "2026-09-24", "116");

        // when
        weighing.correct(sector, "2026-09-23", "161", TODAY, roster, ADMIN, LATER);

        // then
        assertThat(weighing.weighedOn().value()).isEqualTo(LocalDate.of(2026, 9, 23));
        assertThat(weighing.averageWeight().value()).isEqualByComparingTo("161.0");
        assertThat(weighing.lastCorrectedBy()).contains(ADMIN);
        assertThat(weighing.lastCorrectedAt()).contains(LATER);
        assertThat(weighing.recordedBy()).isEqualTo(MARINA);
    }

    @Test
    @DisplayName("keeps its own day without conflicting with itself")
    void givenWeighing_whenCorrectingOnlyTheWeight_thenKeepTheDay() {
        // given
        Weighing weighing = recorded(a01, "2026-09-24", "116");

        // when
        weighing.correct(sector, "2026-09-24", "161", TODAY, roster, MARINA, LATER);

        // then
        assertThat(weighing.averageWeight().value()).isEqualByComparingTo("161.0");
    }

    @Test
    @DisplayName("refuses the day of another valid weighing of the cage, and keeps its data")
    void givenTwoWeighings_whenCorrectingOneToTheDayOfTheOther_thenRefuseAsDayInUse() {
        // given
        recorded(a01, "2026-09-17", "158");
        Weighing weighing = recorded(a01, "2026-09-24", "161");

        // when
        FarmErrorCode code = (FarmErrorCode) refusalCodeOf(
                () -> weighing.correct(sector, "2026-09-17", "161", TODAY, roster, MARINA, LATER));

        // then
        assertThat(code).isEqualTo(FarmErrorCode.WEIGHING_DATE_IN_USE);
        assertThat(weighing.weighedOn().value()).isEqualTo(TODAY);
        assertThat(weighing.lastCorrectedBy()).isEmpty();
    }

    @Test
    @DisplayName("refuses the fields as the record does, all at once")
    void givenWeighing_whenCorrectingWithInvalidFields_thenRefuseThemAtOnce() {
        // given
        Weighing weighing = recorded(a01, "2026-09-24", "161");

        // when
        Map<String, String> details = detailsOf(
                () -> weighing.correct(sector, "2026-09-25", "158,25", TODAY, roster, MARINA, LATER));

        // then
        assertThat(details).containsOnlyKeys("weighedOn", "averageWeight");
    }

    @Test
    @DisplayName("refuses to correct a voided weighing, which is not found")
    void givenVoidedWeighing_whenCorrecting_thenRefuseAsNotFound() {
        // given
        Weighing weighing = recorded(a01, "2026-09-24", "999");
        weighing.voidBy(sector, MARINA, LATER);

        // when
        FarmErrorCode code =
                (FarmErrorCode) refusalCodeOf(() -> weighing.correct(sector, "2026-09-24", "161", TODAY, roster, MARINA, LATER));

        // then
        assertThat(code).isEqualTo(FarmErrorCode.WEIGHING_NOT_FOUND);
    }

    @Test
    @DisplayName("refuses to correct a weighing of an inactive cage or sector")
    void givenInactiveSector_whenCorrecting_thenRefuseAsSectorInactive() {
        // given
        Weighing weighing = recorded(a01, "2026-09-24", "116");
        Sector inactive = Sector.restore(
                sector.id(),
                sector.name(),
                null,
                null,
                Status.INACTIVE,
                sector.cages(),
                sector.createdAt(),
                sector.updatedAt());

        // when
        FarmErrorCode code = (FarmErrorCode)
                refusalCodeOf(() -> weighing.correct(inactive, "2026-09-24", "161", TODAY, roster, MARINA, LATER));

        // then
        assertThat(code).isEqualTo(FarmErrorCode.SECTOR_INACTIVE);
    }

    // ---------------------------------------------------------------- anulação (US4)

    @Test
    @DisplayName("voids the weighing, keeping it with who voided it and when")
    void givenWeighing_whenVoiding_thenKeepItAsVoided() {
        // given
        Weighing weighing = recorded(a01, "2026-09-24", "999");

        // when
        boolean changed = weighing.voidBy(sector, ADMIN, LATER);

        // then
        assertThat(changed).isTrue();
        assertThat(weighing.status()).isEqualTo(WeighingStatus.VOIDED);
        assertThat(weighing.isValid()).isFalse();
        assertThat(weighing.voidedBy()).contains(ADMIN);
        assertThat(weighing.voidedAt()).contains(LATER);
        assertThat(weighing.averageWeight().value()).isEqualByComparingTo("999.0");
    }

    @Test
    @DisplayName("changes nothing when voiding a weighing already voided")
    void givenVoidedWeighing_whenVoidingAgain_thenChangeNothing() {
        // given
        Weighing weighing = recorded(a01, "2026-09-24", "999");
        weighing.voidBy(sector, MARINA, NOW);

        // when
        boolean changed = weighing.voidBy(sector, ADMIN, LATER);

        // then
        assertThat(changed).isFalse();
        assertThat(weighing.voidedBy()).contains(MARINA);
        assertThat(weighing.voidedAt()).contains(NOW);
    }

    @Test
    @DisplayName("frees the day of the voided weighing for a new one")
    void givenVoidedWeighing_whenRecordingTheSameDay_thenAcceptIt() {
        // given
        Weighing voided = recorded(a01, "2026-09-24", "999");
        voided.voidBy(sector, MARINA, LATER);
        roster.save(voided);

        // when
        Weighing again = record(sector, a01, "2026-09-24", "160");

        // then
        assertThat(again.averageWeight().value()).isEqualByComparingTo("160.0");
    }

    @Test
    @DisplayName("refuses to void a weighing of an inactive cage")
    void givenInactiveCage_whenVoiding_thenRefuseAsCageInactive() {
        // given
        Weighing weighing = recorded(a01, "2026-09-24", "999");
        Sector withInactiveCage = aSector().withInactiveCage("A", 1, 48).build();
        Weighing ofTheInactiveCage = Weighing.restore(
                weighing.id(),
                withInactiveCage.id(),
                withInactiveCage.cages().get(0).id(),
                weighing.weighedOn(),
                weighing.averageWeight(),
                WeighingStatus.VALID,
                MARINA,
                NOW,
                null,
                null,
                null,
                null);

        // when
        FarmErrorCode code = (FarmErrorCode) refusalCodeOf(() -> ofTheInactiveCage.voidBy(withInactiveCage, MARINA, LATER));

        // then
        assertThat(code).isEqualTo(FarmErrorCode.CAGE_INACTIVE);
        assertThat(ofTheInactiveCage.isValid()).isTrue();
    }
}
