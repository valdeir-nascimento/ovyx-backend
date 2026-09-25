package io.github.ovyx.farm.application.sector;

import static io.github.ovyx.farm.domain.model.SectorTestDataBuilder.aSector;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.ovyx.farm.domain.model.Sector;
import io.github.ovyx.farm.domain.model.SectorId;
import io.github.ovyx.farm.fixtures.InMemorySectorRepository;
import io.github.ovyx.shared.application.ErrorType;
import io.github.ovyx.shared.application.Result;
import io.github.ovyx.shared.domain.FixedClock;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Cadastro de setor pelo administrador (US1, cenários 1 a 3; FR-001, FR-002, FR-017). */
@DisplayName("RegisterSectorCommandHandler")
class RegisterSectorCommandHandlerTest {

    private final FixedClock clock = FixedClock.at("2026-09-25T13:02:11Z");
    private final InMemorySectorRepository repository = new InMemorySectorRepository();
    private final RegisterSectorCommandHandler handler = new RegisterSectorCommandHandler(repository, clock);

    @Test
    @DisplayName("registers the sector and saves it")
    void givenValidNameAndDescription_whenRegistering_thenSaveTheSector() {
        // given
        RegisterSectorCommand command =
                new RegisterSectorCommand("Codornas — Galpão 4", "Codornas japonesas em postura, baterias A e B");

        // when
        Result<SectorId> result = handler.handle(command);

        // then
        Sector saved = repository.findById(result.value()).orElseThrow();
        assertThat(saved.name().value()).isEqualTo("Codornas — Galpão 4");
        assertThat(saved.isActive()).isTrue();
    }

    @Test
    @DisplayName("fails as validation with every invalid field, and saves nothing")
    void givenShortNameAndLongDescription_whenRegistering_thenFailAsValidationWithEveryField() {
        // given
        RegisterSectorCommand command = new RegisterSectorCommand("A", "d".repeat(501));

        // when
        Result<SectorId> result = handler.handle(command);

        // then
        assertThat(result.error().type()).isEqualTo(ErrorType.VALIDATION);
        assertThat(result.error().code()).isEqualTo("VALIDATION_FAILED");
        assertThat(result.error().details()).containsOnlyKeys("name", "description");
        assertThat(repository.saves()).isZero();
    }

    @Test
    @DisplayName("fails as conflict when another active sector has the name, and saves nothing")
    void givenNameOfAnotherActiveSector_whenRegistering_thenFailAsConflict() {
        // given
        repository.save(aSector().named("Codornas — Galpão 4").withRoster(repository).build());
        RegisterSectorCommand command = new RegisterSectorCommand("CODORNAS — GALPÃO 4", null);

        // when
        Result<SectorId> result = handler.handle(command);

        // then
        assertThat(result.error().type()).isEqualTo(ErrorType.CONFLICT);
        assertThat(result.error().code()).isEqualTo("SECTOR_NAME_IN_USE");
        assertThat(result.error().details())
                .containsExactly(Map.entry("name", "Já existe um setor ativo com este nome."));
        assertThat(repository.saves()).isEqualTo(1);
    }
}
