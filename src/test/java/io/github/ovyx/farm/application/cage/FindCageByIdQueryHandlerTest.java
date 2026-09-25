package io.github.ovyx.farm.application.cage;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.ovyx.farm.domain.model.CageId;
import io.github.ovyx.farm.domain.model.SectorId;
import io.github.ovyx.farm.domain.model.Status;
import io.github.ovyx.shared.application.ErrorType;
import io.github.ovyx.shared.application.Result;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Consulta de uma gaiola do setor, ativa ou inativa. */
@DisplayName("FindCageByIdQueryHandler")
class FindCageByIdQueryHandlerTest {

    private final SectorId sectorId = SectorId.generate();
    private final CageDetail b07 = new CageDetail(
            CageId.generate(),
            sectorId,
            "B-07",
            "B",
            7,
            50,
            Status.ACTIVE,
            Instant.parse("2026-09-21T08:30:00Z"),
            Instant.parse("2026-09-24T17:42:05Z"));
    private final RecordingCageDirectory directory = new RecordingCageDirectory().holding(b07);
    private final FindCageByIdQueryHandler handler = new FindCageByIdQueryHandler(directory);

    @Test
    @DisplayName("finds the cage of the sector")
    void givenCageOfTheSector_whenFinding_thenReturnIt() {
        // given
        FindCageByIdQuery query = new FindCageByIdQuery(sectorId.toString(), b07.id().toString());

        // when
        Result<CageDetail> result = handler.handle(query);

        // then
        assertThat(result.value()).isEqualTo(b07);
    }

    @Test
    @DisplayName("fails as cage not found for a cage of another sector")
    void givenCageOfAnotherSector_whenFinding_thenFailAsCageNotFound() {
        // given
        SectorId another = SectorId.generate();
        directory.withSector(another);

        // when
        Result<CageDetail> result = handler.handle(new FindCageByIdQuery(another.toString(), b07.id().toString()));

        // then
        assertThat(result.error().type()).isEqualTo(ErrorType.NOT_FOUND);
        assertThat(result.error().code()).isEqualTo("CAGE_NOT_FOUND");
    }

    @Test
    @DisplayName("fails as cage not found for a malformed cage identifier")
    void givenMalformedCageIdentifier_whenFinding_thenFailAsCageNotFound() {
        // given
        FindCageByIdQuery query = new FindCageByIdQuery(sectorId.toString(), "b-07");

        // when
        Result<CageDetail> result = handler.handle(query);

        // then
        assertThat(result.error().code()).isEqualTo("CAGE_NOT_FOUND");
    }

    @Test
    @DisplayName("fails as sector not found for a sector that does not exist")
    void givenUnknownSector_whenFinding_thenFailAsSectorNotFound() {
        // given
        FindCageByIdQuery query = new FindCageByIdQuery(SectorId.generate().toString(), b07.id().toString());

        // when
        Result<CageDetail> result = handler.handle(query);

        // then
        assertThat(result.error().code()).isEqualTo("SECTOR_NOT_FOUND");
    }
}
