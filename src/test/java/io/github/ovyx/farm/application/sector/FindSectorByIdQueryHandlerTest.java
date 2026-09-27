package io.github.ovyx.farm.application.sector;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.ovyx.farm.domain.model.SectorId;
import io.github.ovyx.farm.domain.model.Status;
import io.github.ovyx.shared.application.ErrorType;
import io.github.ovyx.shared.application.Result;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Consulta de um setor, ativo ou inativo (FR-004, FR-012). */
@DisplayName("FindSectorByIdQueryHandler")
class FindSectorByIdQueryHandlerTest {

    private final RecordingSectorDirectory directory = new RecordingSectorDirectory();
    private final FindSectorByIdQueryHandler handler = new FindSectorByIdQueryHandler(directory);

    @Test
    @DisplayName("finds the sector with its totals")
    void givenExistingSector_whenFinding_thenReturnItsDetail() {
        // given
        SectorDetail galpao = new SectorDetail(
                SectorId.generate(),
                "Codornas — Galpão 1",
                "Codornas japonesas em postura, baterias A a D",
                Status.ACTIVE,
                48,
                2400,
                List.of("A", "B", "C", "D"),
                Instant.parse("2026-09-20T10:15:00Z"),
                Instant.parse("2026-09-24T17:40:12Z"));
        directory.holding(galpao);

        // when
        Result<SectorDetail> result = handler.handle(new FindSectorByIdQuery(galpao.id().toString()));

        // then
        assertThat(result.value()).isEqualTo(galpao);
    }

    @Test
    @DisplayName("fails as not found for an identifier of no sector")
    void givenUnknownIdentifier_whenFinding_thenFailAsNotFound() {
        // given
        FindSectorByIdQuery query = new FindSectorByIdQuery("9d2e4f6a-1b3c-4d5e-8f7a-2b4c6d8e0f44");

        // when
        Result<SectorDetail> result = handler.handle(query);

        // then
        assertThat(result.error().type()).isEqualTo(ErrorType.NOT_FOUND);
        assertThat(result.error().code()).isEqualTo("SECTOR_NOT_FOUND");
    }

    @Test
    @DisplayName("fails as not found for a malformed identifier, without asking the directory")
    void givenMalformedIdentifier_whenFinding_thenFailAsNotFoundWithoutAsking() {
        // given
        FindSectorByIdQuery query = new FindSectorByIdQuery("galpao-9");

        // when
        Result<SectorDetail> result = handler.handle(query);

        // then
        assertThat(result.error().code()).isEqualTo("SECTOR_NOT_FOUND");
        assertThat(directory.askedDetails()).isEmpty();
    }
}
