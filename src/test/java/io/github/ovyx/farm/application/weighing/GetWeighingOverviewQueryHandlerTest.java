package io.github.ovyx.farm.application.weighing;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.ovyx.farm.domain.model.Actor;
import io.github.ovyx.farm.domain.model.CageId;
import io.github.ovyx.farm.domain.model.SectorId;
import io.github.ovyx.farm.domain.model.Status;
import io.github.ovyx.farm.domain.model.WeighingId;
import io.github.ovyx.shared.application.ErrorType;
import io.github.ovyx.shared.application.Result;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** A consulta do acompanhamento do peso da gaiola (US1 e US3 da 005; FR-009 a FR-013; R-008). */
@DisplayName("GetWeighingOverviewQueryHandler")
class GetWeighingOverviewQueryHandlerTest {

    private static final UUID SECTOR = UUID.fromString("3f6c2b1a-8d4e-4c7f-9a2b-1e5d7c9f0a11");
    private static final UUID CAGE = UUID.fromString("2a4c6e8a-0b1d-4f3a-9c5e-7a9b1d3f5a66");

    private final RecordingWeighingDirectory directory = new RecordingWeighingDirectory();
    private final GetWeighingOverviewQueryHandler handler = new GetWeighingOverviewQueryHandler(directory);

    GetWeighingOverviewQueryHandlerTest() {
        directory.knowSector(new WeighedSector(SectorId.of(SECTOR), "Codornas — Galpão 1", Status.ACTIVE, null));
        directory.knowCage(SECTOR, new WeighedCage(CageId.of(CAGE), "A-01", "A", 1, 48, Status.ACTIVE));
    }

    @Test
    @DisplayName("builds the overview of the cage from its weighings")
    void givenCageWithWeighings_whenAskingTheOverview_thenBuildItFromTheWeighings() {
        // given
        Actor marina = new Actor(UUID.randomUUID(), "Marina Alves");
        directory.knowWeighings(
                CAGE,
                List.of(
                        new WeighingEntry(WeighingId.generate(), LocalDate.parse("2026-09-17"), new BigDecimal("158.0"), marina, null),
                        new WeighingEntry(WeighingId.generate(), LocalDate.parse("2026-09-24"), new BigDecimal("161.0"), marina, null)));

        // when
        Result<WeighingOverview> result = handler.handle(new GetWeighingOverviewQuery(SECTOR.toString(), CAGE.toString()));

        // then
        assertThat(result.value().cage().code()).isEqualTo("A-01");
        assertThat(result.value().latest().averageWeight()).isEqualByComparingTo("161.0");
        assertThat(result.value().history()).hasSize(2);
    }

    @ParameterizedTest(name = "\"{0}\"")
    @ValueSource(strings = {"5c8d2e4f-6a1b-4c3d-9e7f-0a2b4c6d8e33", "galpao-9"})
    @DisplayName("fails as not found for a sector that does not exist")
    void givenUnknownSector_whenAskingTheOverview_thenFailAsSectorNotFound(String sectorId) {
        // given
        GetWeighingOverviewQuery query = new GetWeighingOverviewQuery(sectorId, CAGE.toString());

        // when
        Result<WeighingOverview> result = handler.handle(query);

        // then
        assertThat(result.error().type()).isEqualTo(ErrorType.NOT_FOUND);
        assertThat(result.error().code()).isEqualTo("SECTOR_NOT_FOUND");
    }

    @ParameterizedTest(name = "\"{0}\"")
    @ValueSource(strings = {"9d2e4f6a-1b3c-4d5e-8f7a-2b4c6d8e0f44", "a-01"})
    @DisplayName("fails as not found for a cage that does not exist or is not of the sector")
    void givenUnknownCage_whenAskingTheOverview_thenFailAsCageNotFound(String cageId) {
        // given
        GetWeighingOverviewQuery query = new GetWeighingOverviewQuery(SECTOR.toString(), cageId);

        // when
        Result<WeighingOverview> result = handler.handle(query);

        // then
        assertThat(result.error().type()).isEqualTo(ErrorType.NOT_FOUND);
        assertThat(result.error().code()).isEqualTo("CAGE_NOT_FOUND");
    }
}
