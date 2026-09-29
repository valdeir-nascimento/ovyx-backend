package io.github.ovyx.farm.application.weighing;

import static io.github.ovyx.farm.domain.model.SectorTestDataBuilder.aSector;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.ovyx.farm.domain.model.Actor;
import io.github.ovyx.farm.domain.model.CageId;
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
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** O registro da pesagem por qualquer responsável (US1 da 005; FR-003, FR-004, FR-007, FR-008, FR-014). */
@DisplayName("RecordWeighingCommandHandler")
class RecordWeighingCommandHandlerTest {

    private static final Actor MARINA = new Actor(UUID.fromString("5e7a9c1e-3b5d-4f7a-9c1e-3b5d7f9a1c22"), "Marina Alves");

    /** 10h12 em UTC é 7h12 em São Paulo, do dia 24/09. */
    private final FixedClock clock = FixedClock.at("2026-09-24T10:12:40Z");
    private final FarmCalendar calendar = new FarmCalendar(clock, ZoneId.of("America/Sao_Paulo"));
    private final InMemorySectorRepository sectors = new InMemorySectorRepository();
    private final InMemoryWeighingRepository weighings = new InMemoryWeighingRepository();
    private final RecordWeighingCommandHandler handler =
            new RecordWeighingCommandHandler(sectors, weighings, calendar, clock);

    private Sector known(Sector sector) {
        sectors.save(sector);
        return sector;
    }

    private final Sector sector = known(aSector().withCage("A", 1, 48).build());
    private final String a01 = sector.cages().get(0).id().toString();

    private RecordWeighingCommand command(String sectorId, String cageId, String day, String weight) {
        return new RecordWeighingCommand(sectorId, cageId, day, weight, MARINA);
    }

    @Test
    @DisplayName("records the weighing, with who recorded it, and saves it")
    void givenValidWeighing_whenRecording_thenSaveIt() {
        // given
        RecordWeighingCommand command = command(sector.id().toString(), a01, "2026-09-24", "161,4");

        // when
        Result<WeighingId> result = handler.handle(command);

        // then
        Weighing saved = weighings.findById(result.value()).orElseThrow();
        assertThat(saved.weighedOn().value()).isEqualTo(LocalDate.of(2026, 9, 24));
        assertThat(saved.averageWeight().value()).isEqualByComparingTo("161.4");
        assertThat(saved.recordedBy()).isEqualTo(MARINA);
        assertThat(saved.recordedAt()).isEqualTo(clock.instant());
    }

    @ParameterizedTest(name = "\"{0}\"")
    @ValueSource(strings = {"3f6c2b1a-8d4e-4c7f-9a2b-1e5d7c9f0a11", "galpao-9"})
    @DisplayName("fails as not found for a sector that does not exist, and saves nothing")
    void givenUnknownSector_whenRecording_thenFailAsSectorNotFound(String sectorId) {
        // given
        RecordWeighingCommand command = command(sectorId, a01, "2026-09-24", "161");

        // when
        Result<WeighingId> result = handler.handle(command);

        // then
        assertThat(result.error().type()).isEqualTo(ErrorType.NOT_FOUND);
        assertThat(result.error().code()).isEqualTo("SECTOR_NOT_FOUND");
        assertThat(weighings.saves()).isZero();
    }

    @ParameterizedTest(name = "\"{0}\"")
    @ValueSource(strings = {"9d2e4f6a-1b3c-4d5e-8f7a-2b4c6d8e0f44", "a-01"})
    @DisplayName("fails as not found for a cage that does not exist or is not of the sector")
    void givenUnknownCage_whenRecording_thenFailAsCageNotFound(String cageId) {
        // given
        RecordWeighingCommand command = command(sector.id().toString(), cageId, "2026-09-24", "161");

        // when
        Result<WeighingId> result = handler.handle(command);

        // then
        assertThat(result.error().type()).isEqualTo(ErrorType.NOT_FOUND);
        assertThat(result.error().code()).isEqualTo("CAGE_NOT_FOUND");
    }

    @Test
    @DisplayName("fails as not found for a cage of another sector")
    void givenCageOfAnotherSector_whenRecording_thenFailAsCageNotFound() {
        // given
        Sector other = known(aSector().withCage("A", 1, 48).build());
        RecordWeighingCommand command =
                command(sector.id().toString(), other.cages().get(0).id().toString(), "2026-09-24", "161");

        // when
        Result<WeighingId> result = handler.handle(command);

        // then
        assertThat(result.error().type()).isEqualTo(ErrorType.NOT_FOUND);
        assertThat(result.error().code()).isEqualTo("CAGE_NOT_FOUND");
    }

    @Test
    @DisplayName("fails as validation with the day and the weight at once, and saves nothing")
    void givenInvalidDayAndWeight_whenRecording_thenFailAsValidation() {
        // given
        RecordWeighingCommand command = command(sector.id().toString(), a01, "2026-09-25", null);

        // when
        Result<WeighingId> result = handler.handle(command);

        // then
        assertThat(result.error().type()).isEqualTo(ErrorType.VALIDATION);
        assertThat(result.error().details())
                .containsOnly(
                        Map.entry("weighedOn", "A data da pesagem não pode ser futura."),
                        Map.entry("averageWeight", "Informe o peso médio em gramas."));
        assertThat(weighings.saves()).isZero();
    }

    @Test
    @DisplayName("fails as conflict when the cage already has a weighing on the day")
    void givenWeighingOnTheDay_whenRecordingTheSameDay_thenFailAsConflict() {
        // given
        handler.handle(command(sector.id().toString(), a01, "2026-09-24", "161"));

        // when
        Result<WeighingId> result = handler.handle(command(sector.id().toString(), a01, "2026-09-24", "158"));

        // then
        assertThat(result.error().type()).isEqualTo(ErrorType.CONFLICT);
        assertThat(result.error().code()).isEqualTo("WEIGHING_DATE_IN_USE");
        assertThat(weighings.saves()).isEqualTo(1);
    }

    @Test
    @DisplayName("fails as conflict in an inactive sector")
    void givenInactiveSector_whenRecording_thenFailAsConflict() {
        // given
        Sector inactive = known(aSector().withCage("A", 1, 48).inactive().build());
        RecordWeighingCommand command =
                command(inactive.id().toString(), inactive.cages().get(0).id().toString(), "2026-09-24", "161");

        // when
        Result<WeighingId> result = handler.handle(command);

        // then
        assertThat(result.error().type()).isEqualTo(ErrorType.CONFLICT);
        assertThat(result.error().code()).isEqualTo("SECTOR_INACTIVE");
    }

    @Test
    @DisplayName("fails as conflict for an inactive cage")
    void givenInactiveCage_whenRecording_thenFailAsConflict() {
        // given
        Sector withInactiveCage = known(aSector().withInactiveCage("A", 1, 48).build());
        CageId cage = withInactiveCage.cages().get(0).id();

        // when
        Result<WeighingId> result = handler.handle(
                command(withInactiveCage.id().toString(), cage.toString(), "2026-09-24", "161"));

        // then
        assertThat(result.error().type()).isEqualTo(ErrorType.CONFLICT);
        assertThat(result.error().code()).isEqualTo("CAGE_INACTIVE");
    }
}
