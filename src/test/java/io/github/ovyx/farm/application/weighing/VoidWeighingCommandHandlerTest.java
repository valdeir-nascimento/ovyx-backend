package io.github.ovyx.farm.application.weighing;

import static io.github.ovyx.farm.domain.model.SectorTestDataBuilder.aSector;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.ovyx.farm.domain.model.Actor;
import io.github.ovyx.farm.domain.model.Sector;
import io.github.ovyx.farm.domain.model.Status;
import io.github.ovyx.farm.domain.model.Weighing;
import io.github.ovyx.farm.domain.model.WeighingId;
import io.github.ovyx.farm.domain.model.WeighingStatus;
import io.github.ovyx.farm.fixtures.InMemorySectorRepository;
import io.github.ovyx.farm.fixtures.InMemoryWeighingRepository;
import io.github.ovyx.shared.application.ErrorType;
import io.github.ovyx.shared.application.Result;
import io.github.ovyx.shared.domain.FixedClock;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * A exclusão da pesagem, que a anula e não a apaga (US4 da 005; FR-006, FR-015): a pesagem fica guardada,
 * com quem a anulou e quando.
 */
@DisplayName("VoidWeighingCommandHandler")
class VoidWeighingCommandHandlerTest {

    private static final Actor MARINA = new Actor(UUID.fromString("5e7a9c1e-3b5d-4f7a-9c1e-3b5d7f9a1c22"), "Marina Alves");

    private final FixedClock clock = FixedClock.at("2026-09-24T10:12:40Z");
    private final InMemorySectorRepository sectors = new InMemorySectorRepository();
    private final InMemoryWeighingRepository weighings = new InMemoryWeighingRepository();
    private final VoidWeighingCommandHandler handler = new VoidWeighingCommandHandler(sectors, weighings, clock);

    private final Sector sector = saved(aSector().withCage("A", 1, 48).build());
    private final String a01 = sector.cages().get(0).id().toString();

    private Sector saved(Sector built) {
        sectors.save(built);
        return built;
    }

    private Weighing recorded() {
        Weighing weighing = Weighing.record(
                sector,
                sector.cages().get(0).id(),
                "2026-09-24",
                "999",
                LocalDate.of(2026, 9, 24),
                weighings,
                MARINA,
                clock.instant());
        weighings.save(weighing);
        return weighing;
    }

    private VoidWeighingCommand command(String weighingId) {
        return new VoidWeighingCommand(sector.id().toString(), a01, weighingId, MARINA);
    }

    @Test
    @DisplayName("voids the weighing and saves it, with who voided it")
    void givenWeighing_whenVoiding_thenSaveItAsVoided() {
        // given
        Weighing weighing = recorded();

        // when
        Result<WeighingId> result = handler.handle(command(weighing.id().toString()));

        // then
        Weighing saved = weighings.findById(result.value()).orElseThrow();
        assertThat(saved.status()).isEqualTo(WeighingStatus.VOIDED);
        assertThat(saved.voidedBy()).contains(MARINA);
        assertThat(saved.voidedAt()).contains(clock.instant());
    }

    @Test
    @DisplayName("answers success again for a weighing already voided, without saving it again")
    void givenVoidedWeighing_whenVoidingAgain_thenSucceedWithoutSaving() {
        // given
        Weighing weighing = recorded();
        handler.handle(command(weighing.id().toString()));
        int before = weighings.saves();

        // when
        Result<WeighingId> result = handler.handle(command(weighing.id().toString()));

        // then
        assertThat(result.isSuccess()).isTrue();
        assertThat(weighings.saves()).isEqualTo(before);
    }

    @Test
    @DisplayName("fails as not found for a weighing that does not exist")
    void givenUnknownWeighing_whenVoiding_thenFailAsWeighingNotFound() {
        // given
        VoidWeighingCommand command = command(UUID.randomUUID().toString());

        // when
        Result<WeighingId> result = handler.handle(command);

        // then
        assertThat(result.error().type()).isEqualTo(ErrorType.NOT_FOUND);
        assertThat(result.error().code()).isEqualTo("WEIGHING_NOT_FOUND");
    }

    @Test
    @DisplayName("fails as conflict in an inactive sector, and keeps the weighing")
    void givenInactiveSector_whenVoiding_thenFailAsConflict() {
        // given
        Weighing weighing = recorded();
        sectors.save(Sector.restore(
                sector.id(),
                sector.name(),
                null,
                null,
                sector.layingRateTarget(),
                Status.INACTIVE,
                sector.cages(),
                sector.createdAt(),
                sector.updatedAt()));

        // when
        Result<WeighingId> result = handler.handle(command(weighing.id().toString()));

        // then
        assertThat(result.error().type()).isEqualTo(ErrorType.CONFLICT);
        assertThat(result.error().code()).isEqualTo("SECTOR_INACTIVE");
        assertThat(weighings.findById(weighing.id()).orElseThrow().isValid()).isTrue();
    }

    @ParameterizedTest(name = "\"{0}\"")
    @ValueSource(strings = {"4e6a8c0e-2b4d-4f6a-8c0e-2b4d6f8a0c44", "setor-1"})
    @DisplayName("fails as not found for a sector that does not exist or is malformed, and keeps the weighing")
    void givenUnknownSector_whenVoiding_thenFailAsSectorNotFound(String sectorId) {
        // given
        Weighing weighing = recorded();
        VoidWeighingCommand command = new VoidWeighingCommand(sectorId, a01, weighing.id().toString(), MARINA);

        // when
        Result<WeighingId> result = handler.handle(command);

        // then
        assertThat(result.error().type()).isEqualTo(ErrorType.NOT_FOUND);
        assertThat(result.error().code()).isEqualTo("SECTOR_NOT_FOUND");
        assertThat(weighings.findById(weighing.id()).orElseThrow().isValid()).isTrue();
    }

    @Test
    @DisplayName("fails as not found for a cage of another sector, and keeps the weighing")
    void givenCageOfAnotherSector_whenVoiding_thenFailAsCageNotFound() {
        // given
        Weighing weighing = recorded();
        Sector other = saved(aSector().withCage("C", 1, 48).build());
        VoidWeighingCommand command = new VoidWeighingCommand(
                sector.id().toString(), other.cages().get(0).id().toString(), weighing.id().toString(), MARINA);

        // when
        Result<WeighingId> result = handler.handle(command);

        // then
        assertThat(result.error().type()).isEqualTo(ErrorType.NOT_FOUND);
        assertThat(result.error().code()).isEqualTo("CAGE_NOT_FOUND");
        assertThat(weighings.findById(weighing.id()).orElseThrow().isValid()).isTrue();
    }

    @Test
    @DisplayName("fails as conflict for an inactive cage, and keeps the weighing valid")
    void givenInactiveCage_whenVoiding_thenFailAsCageInactive() {
        // given
        Weighing weighing = recorded();
        sector.deactivateCage(sector.cages().get(0).id(), clock);
        sectors.save(sector);
        int before = weighings.saves();

        // when
        Result<WeighingId> result = handler.handle(command(weighing.id().toString()));

        // then
        assertThat(result.error().type()).isEqualTo(ErrorType.CONFLICT);
        assertThat(result.error().code()).isEqualTo("CAGE_INACTIVE");
        assertThat(weighings.saves()).isEqualTo(before);
        assertThat(weighings.findById(weighing.id()).orElseThrow().status()).isEqualTo(WeighingStatus.VALID);
    }
}
