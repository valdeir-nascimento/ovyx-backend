package io.github.ovyx.identity.application.administratorseeding;

import static io.github.ovyx.identity.domain.model.CaretakerTestDataBuilder.aCaretaker;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.ovyx.identity.application.InMemoryCaretakerRepository;
import io.github.ovyx.identity.domain.FakePasswordHasher;
import io.github.ovyx.identity.domain.model.Caretaker;
import io.github.ovyx.identity.domain.model.Role;
import io.github.ovyx.shared.application.Result;
import io.github.ovyx.shared.domain.FixedClock;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Testes da semeadura do administrador inicial (FR-025).
 *
 * <p>A regra — criar so quando nao ha administrador ativo, obrigar a troca da senha, recusar dados
 * invalidos — vive aqui, em {@code application}, e volta como {@code Result}. Quem roda a semeadura
 * na subida da aplicacao so decide o que fazer com a falha.
 */
@DisplayName("SeedInitialAdministratorCommandHandler")
class SeedInitialAdministratorCommandHandlerTest {

    private static final String PASSWORD = "TrocarNoPrimeiroAcesso2026";

    private final FakePasswordHasher hasher = new FakePasswordHasher();
    private final FixedClock clock = FixedClock.at("2026-09-21T12:00:00Z");
    private final InMemoryCaretakerRepository repository = new InMemoryCaretakerRepository();
    private final SeedInitialAdministratorCommandHandler handler =
            new SeedInitialAdministratorCommandHandler(repository, hasher, clock);

    private static SeedInitialAdministratorCommand command(String cpf, String password) {
        return new SeedInitialAdministratorCommand(
                "Administrador do Sistema", cpf, "admin@ovyx.com.br", "11988880000", password);
    }

    @Test
    @DisplayName("creates the initial administrator obliged to change the password")
    void givenNoActiveAdministrator_whenSeeding_thenCreateAnAdministratorObligedToChangeThePassword() {
        // given — the repository starts empty

        // when
        Result<SeedingOutcome> result = handler.handle(command("87543210932", PASSWORD));

        // then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.value()).isEqualTo(SeedingOutcome.CREATED);
        Caretaker admin = repository.findByEmailOrMobilePhone("admin@ovyx.com.br").orElseThrow();
        assertThat(admin.role()).isEqualTo(Role.ADMINISTRATOR);
        assertThat(admin.mustChangePassword()).isTrue();
        assertThat(admin.authenticate(PASSWORD, hasher)).isTrue();
    }

    @Test
    @DisplayName("does nothing when an active administrator already exists, even without a password")
    void givenActiveAdministrator_whenSeedingWithoutPassword_thenCreateNobody() {
        // given
        repository.save(
                aCaretaker().withRole(Role.ADMINISTRATOR).withHasher(hasher).withClock(clock).build());

        // when
        Result<SeedingOutcome> result = handler.handle(command("87543210932", ""));

        // then
        assertThat(result.value()).isEqualTo(SeedingOutcome.NOT_NEEDED);
        assertThat(repository.countActiveAdministrators()).isEqualTo(1);
        assertThat(repository.findByEmailOrMobilePhone("admin@ovyx.com.br")).isEmpty();
    }

    @Test
    @DisplayName("fails without a password instead of generating one")
    void givenBlankPassword_whenSeeding_thenFailInsteadOfGeneratingOne() {
        // given
        // A versao anterior gerava uma senha e a imprimia no log, contrariando FR-021.
        String blankPassword = " ";

        // when
        Result<SeedingOutcome> result = handler.handle(command("87543210932", blankPassword));

        // then
        assertThat(result.isFailure()).isTrue();
        assertThat(result.error().code()).isEqualTo(SeedInitialAdministratorCommandHandler.PASSWORD_REQUIRED);
        assertThat(repository.countActiveAdministrators()).isZero();
    }

    @Test
    @DisplayName("fails with every violation of an invalid configuration and never echoes the password")
    void givenInvalidCpfInConfiguration_whenSeeding_thenFailWithoutEchoingThePassword() {
        // given
        String invalidCpf = "12345678901";

        // when
        Result<SeedingOutcome> result = handler.handle(command(invalidCpf, PASSWORD));

        // then
        assertThat(result.isFailure()).isTrue();
        assertThat(result.error().details()).containsEntry("cpf", "CPF inválido.");
        assertThat(result.error().details().toString()).doesNotContain(PASSWORD);
        assertThat(result.error().message()).doesNotContain(PASSWORD);
        assertThat(repository.countActiveAdministrators()).isZero();
    }

    @Test
    @DisplayName("never exposes the password in the command description")
    void givenSeedingCommandWithPassword_whenDescribingIt_thenMaskThePassword() {
        // given
        SeedInitialAdministratorCommand command = command("87543210932", PASSWORD);

        // when
        String description = command.toString();

        // then
        assertThat(description).doesNotContain(PASSWORD).contains("****");
    }
}
