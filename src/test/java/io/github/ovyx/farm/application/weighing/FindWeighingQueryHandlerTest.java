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
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Uma pesagem válida da gaiola, para o que o registro devolve e para o diálogo de correção (US1 e US4 da
 * 005). A anulada, a de outra gaiola e a inexistente não são encontradas.
 */
@DisplayName("FindWeighingQueryHandler")
class FindWeighingQueryHandlerTest {

    private static final UUID SECTOR = UUID.fromString("3f6c2b1a-8d4e-4c7f-9a2b-1e5d7c9f0a11");
    private static final UUID CAGE = UUID.fromString("2a4c6e8a-0b1d-4f3a-9c5e-7a9b1d3f5a66");
    private static final WeighingId WEIGHING = WeighingId.of(UUID.fromString("7b9d1f3a-5c7e-4a9b-8d1f-3a5c7e9b1d77"));

    private final RecordingWeighingDirectory directory = new RecordingWeighingDirectory();
    private final FindWeighingQueryHandler handler = new FindWeighingQueryHandler(directory);

    FindWeighingQueryHandlerTest() {
        directory.knowSector(new WeighedSector(SectorId.of(SECTOR), "Codornas — Galpão 1", Status.ACTIVE, null));
        directory.knowCage(SECTOR, new WeighedCage(CageId.of(CAGE), "A-01", "A", 1, 48, Status.ACTIVE));
        directory.knowWeighing(CAGE, new WeighingDetail(
                WEIGHING,
                LocalDate.parse("2026-09-24"),
                new BigDecimal("161.4"),
                new Actor(UUID.randomUUID(), "Marina Alves"),
                Instant.parse("2026-09-24T10:12:40Z"),
                null,
                null));
    }

    @Test
    @DisplayName("finds the weighing of the cage")
    void givenWeighingOfTheCage_whenFindingIt_thenAnswerIt() {
        // given
        FindWeighingQuery query = new FindWeighingQuery(SECTOR.toString(), CAGE.toString(), WEIGHING.toString());

        // when
        Result<WeighingDetail> result = handler.handle(query);

        // then
        assertThat(result.value().id()).isEqualTo(WEIGHING);
        assertThat(result.value().averageWeight()).isEqualByComparingTo("161.4");
    }

    @ParameterizedTest(name = "\"{0}\"")
    @ValueSource(strings = {"4e6a8c0e-2b4d-4f6a-8c0e-2b4d6f8a0c44", "pesagem-24-09"})
    @DisplayName("fails as not found for a weighing that does not exist, is voided or is of another cage")
    void givenUnknownWeighing_whenFindingIt_thenFailAsWeighingNotFound(String weighingId) {
        // given
        FindWeighingQuery query = new FindWeighingQuery(SECTOR.toString(), CAGE.toString(), weighingId);

        // when
        Result<WeighingDetail> result = handler.handle(query);

        // then
        assertThat(result.error().type()).isEqualTo(ErrorType.NOT_FOUND);
        assertThat(result.error().code()).isEqualTo("WEIGHING_NOT_FOUND");
    }

    @Test
    @DisplayName("fails as not found for a cage that is not of the sector")
    void givenUnknownCage_whenFindingTheWeighing_thenFailAsCageNotFound() {
        // given
        FindWeighingQuery query =
                new FindWeighingQuery(SECTOR.toString(), UUID.randomUUID().toString(), WEIGHING.toString());

        // when
        Result<WeighingDetail> result = handler.handle(query);

        // then
        assertThat(result.error().code()).isEqualTo("CAGE_NOT_FOUND");
    }

    @Test
    @DisplayName("fails as not found for a sector that does not exist")
    void givenUnknownSector_whenFindingTheWeighing_thenFailAsSectorNotFound() {
        // given
        FindWeighingQuery query =
                new FindWeighingQuery(UUID.randomUUID().toString(), CAGE.toString(), WEIGHING.toString());

        // when
        Result<WeighingDetail> result = handler.handle(query);

        // then
        assertThat(result.error().code()).isEqualTo("SECTOR_NOT_FOUND");
    }
}
