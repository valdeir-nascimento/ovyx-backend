package io.github.ovyx.farm.application.weighing;

import static io.github.ovyx.farm.domain.model.SectorTestDataBuilder.aSector;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.ovyx.farm.domain.model.Actor;
import io.github.ovyx.farm.domain.model.Sector;
import io.github.ovyx.farm.domain.model.Weighing;
import io.github.ovyx.farm.domain.model.WeighingId;
import io.github.ovyx.farm.fixtures.InMemorySectorRepository;
import io.github.ovyx.farm.fixtures.InMemoryWeighingRepository;
import io.github.ovyx.shared.application.ErrorType;
import io.github.ovyx.shared.application.FarmCalendar;
import io.github.ovyx.shared.application.Result;
import io.github.ovyx.shared.domain.FixedClock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** A correção da pesagem por qualquer responsável (US4 da 005; FR-005, FR-007, FR-008). */
@DisplayName("CorrectWeighingCommandHandler")
class CorrectWeighingCommandHandlerTest {

    private static final Actor MARINA = new Actor(UUID.fromString("5e7a9c1e-3b5d-4f7a-9c1e-3b5d7f9a1c22"), "Marina Alves");

    private final FixedClock clock = FixedClock.at("2026-09-24T10:12:40Z");
    private final FarmCalendar calendar = new FarmCalendar(clock, ZoneId.of("America/Sao_Paulo"));
    private final InMemorySectorRepository sectors = new InMemorySectorRepository();
    private final InMemoryWeighingRepository weighings = new InMemoryWeighingRepository();
    private final CorrectWeighingCommandHandler handler =
            new CorrectWeighingCommandHandler(sectors, weighings, calendar, clock);

    private final Sector sector = saved(aSector().withCage("A", 1, 48).withCage("B", 7, 50).build());
    private final String a01 = sector.cages().get(0).id().toString();

    private Sector saved(Sector built) {
        sectors.save(built);
        return built;
    }

    private Weighing recorded(String cageId, String day, String weight) {
        Weighing weighing = Weighing.record(
                sector,
                sector.cages().stream().filter(cage -> cage.id().toString().equals(cageId)).findFirst().orElseThrow().id(),
                day,
                weight,
                LocalDate.of(2026, 9, 24),
                weighings,
                MARINA,
                clock.instant());
        weighings.save(weighing);
        return weighing;
    }

    private CorrectWeighingCommand command(String cageId, String weighingId, String day, String weight) {
        return new CorrectWeighingCommand(sector.id().toString(), cageId, weighingId, day, weight, MARINA);
    }

    @Test
    @DisplayName("corrects the weighing and saves it")
    void givenWeighing_whenCorrecting_thenSaveTheCorrection() {
        // given
        Weighing weighing = recorded(a01, "2026-09-24", "116");

        // when
        Result<WeighingId> result = handler.handle(command(a01, weighing.id().toString(), "2026-09-24", "161"));

        // then
        Weighing saved = weighings.findById(result.value()).orElseThrow();
        assertThat(saved.averageWeight().value()).isEqualByComparingTo("161.0");
        assertThat(saved.lastCorrectedBy()).contains(MARINA);
    }

    @ParameterizedTest(name = "\"{0}\"")
    @ValueSource(strings = {"4e6a8c0e-2b4d-4f6a-8c0e-2b4d6f8a0c44", "pesagem-24-09"})
    @DisplayName("fails as not found for a weighing that does not exist or is malformed")
    void givenUnknownWeighing_whenCorrecting_thenFailAsWeighingNotFound(String weighingId) {
        // given
        CorrectWeighingCommand command = command(a01, weighingId, "2026-09-24", "161");

        // when
        Result<WeighingId> result = handler.handle(command);

        // then
        assertThat(result.error().type()).isEqualTo(ErrorType.NOT_FOUND);
        assertThat(result.error().code()).isEqualTo("WEIGHING_NOT_FOUND");
    }

    @Test
    @DisplayName("fails as not found for a weighing of another cage")
    void givenWeighingOfAnotherCage_whenCorrectingThroughThisCage_thenFailAsWeighingNotFound() {
        // given
        String b07 = sector.cages().get(1).id().toString();
        Weighing ofB07 = recorded(b07, "2026-09-24", "158");

        // when
        Result<WeighingId> result = handler.handle(command(a01, ofB07.id().toString(), "2026-09-24", "161"));

        // then
        assertThat(result.error().code()).isEqualTo("WEIGHING_NOT_FOUND");
        assertThat(weighings.findById(ofB07.id()).orElseThrow().averageWeight().value()).isEqualByComparingTo("158.0");
    }

    @Test
    @DisplayName("fails as not found for a voided weighing")
    void givenVoidedWeighing_whenCorrecting_thenFailAsWeighingNotFound() {
        // given
        Weighing weighing = recorded(a01, "2026-09-24", "999");
        weighing.voidBy(sector, MARINA, clock.instant());
        weighings.save(weighing);

        // when
        Result<WeighingId> result = handler.handle(command(a01, weighing.id().toString(), "2026-09-24", "161"));

        // then
        assertThat(result.error().type()).isEqualTo(ErrorType.NOT_FOUND);
        assertThat(result.error().code()).isEqualTo("WEIGHING_NOT_FOUND");
    }

    @Test
    @DisplayName("fails as validation with every invalid field, and saves nothing")
    void givenInvalidFields_whenCorrecting_thenFailAsValidation() {
        // given
        Weighing weighing = recorded(a01, "2026-09-24", "116");
        int before = weighings.saves();

        // when
        Result<WeighingId> result = handler.handle(command(a01, weighing.id().toString(), "", "0"));

        // then
        assertThat(result.error().type()).isEqualTo(ErrorType.VALIDATION);
        assertThat(result.error().details()).containsOnlyKeys("weighedOn", "averageWeight");
        assertThat(weighings.saves()).isEqualTo(before);
    }

    @Test
    @DisplayName("fails as conflict with the day of another weighing of the cage")
    void givenTwoWeighings_whenCorrectingToTheDayOfTheOther_thenFailAsConflict() {
        // given
        recorded(a01, "2026-09-17", "158");
        Weighing weighing = recorded(a01, "2026-09-24", "161");

        // when
        Result<WeighingId> result = handler.handle(command(a01, weighing.id().toString(), "2026-09-17", "161"));

        // then
        assertThat(result.error().type()).isEqualTo(ErrorType.CONFLICT);
        assertThat(result.error().code()).isEqualTo("WEIGHING_DATE_IN_USE");
    }

    @Test
    @DisplayName("fails as not found for a sector that does not exist")
    void givenUnknownSector_whenCorrecting_thenFailAsSectorNotFound() {
        // given
        Weighing weighing = recorded(a01, "2026-09-24", "116");
        CorrectWeighingCommand command = new CorrectWeighingCommand(
                UUID.randomUUID().toString(), a01, weighing.id().toString(), "2026-09-24", "161", MARINA);

        // when
        Result<WeighingId> result = handler.handle(command);

        // then
        assertThat(result.error().code()).isEqualTo("SECTOR_NOT_FOUND");
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"unknown", "other sector"})
    @DisplayName("fails as not found for a cage that does not exist or belongs to another sector")
    void givenCageOutOfTheSector_whenCorrecting_thenFailAsCageNotFound(String which) {
        // given
        Weighing weighing = recorded(a01, "2026-09-24", "116");
        Sector other = saved(aSector().withCage("C", 1, 48).build());
        String cageId = which.equals("unknown")
                ? UUID.randomUUID().toString()
                : other.cages().get(0).id().toString();
        int before = weighings.saves();

        // when
        Result<WeighingId> result = handler.handle(command(cageId, weighing.id().toString(), "2026-09-24", "161"));

        // then
        assertThat(result.error().type()).isEqualTo(ErrorType.NOT_FOUND);
        assertThat(result.error().code()).isEqualTo("CAGE_NOT_FOUND");
        assertThat(weighings.saves()).isEqualTo(before);
    }

    @Test
    @DisplayName("fails as conflict in an inactive sector, and saves nothing")
    void givenInactiveSector_whenCorrecting_thenFailAsSectorInactive() {
        // given
        Weighing weighing = recorded(a01, "2026-09-24", "116");
        sector.deactivate(clock);
        sectors.save(sector);
        int before = weighings.saves();

        // when
        Result<WeighingId> result = handler.handle(command(a01, weighing.id().toString(), "2026-09-24", "161"));

        // then
        assertThat(result.error().type()).isEqualTo(ErrorType.CONFLICT);
        assertThat(result.error().code()).isEqualTo("SECTOR_INACTIVE");
        assertThat(weighings.saves()).isEqualTo(before);
        assertThat(weighings.findById(weighing.id()).orElseThrow().averageWeight().value())
                .isEqualByComparingTo("116.0");
    }

    @Test
    @DisplayName("fails as conflict for an inactive cage, and saves nothing")
    void givenInactiveCage_whenCorrecting_thenFailAsCageInactive() {
        // given
        Weighing weighing = recorded(a01, "2026-09-24", "116");
        sector.deactivateCage(sector.cages().get(0).id(), clock);
        sectors.save(sector);
        int before = weighings.saves();

        // when
        Result<WeighingId> result = handler.handle(command(a01, weighing.id().toString(), "2026-09-24", "161"));

        // then
        assertThat(result.error().type()).isEqualTo(ErrorType.CONFLICT);
        assertThat(result.error().code()).isEqualTo("CAGE_INACTIVE");
        assertThat(weighings.saves()).isEqualTo(before);
    }
}
