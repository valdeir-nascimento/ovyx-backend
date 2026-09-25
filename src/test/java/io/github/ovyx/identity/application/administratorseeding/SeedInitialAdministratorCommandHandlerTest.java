package io.github.ovyx.identity.application.administratorseeding;

import static io.github.ovyx.identity.domain.model.CaretakerTestDataBuilder.aCaretaker;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.ovyx.identity.fixtures.InMemoryCaretakerRepository;
import io.github.ovyx.identity.domain.FakePasswordHasher;
import io.github.ovyx.identity.domain.model.Caretaker;
import io.github.ovyx.identity.domain.model.CaretakerStatus;
import io.github.ovyx.identity.domain.model.CaretakerTestDataBuilder;
import io.github.ovyx.identity.domain.model.Role;
import io.github.ovyx.identity.domain.valueobject.Cpf;
import io.github.ovyx.shared.application.ErrorType;
import io.github.ovyx.shared.application.Result;
import io.github.ovyx.shared.domain.FixedClock;
import java.util.function.Function;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

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
    private static final String CONFIGURED_CPF = "87543210932";

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
        // given — o repositorio comeca vazio

        // when
        Result<SeedingReport> result = handler.handle(command("87543210932", PASSWORD));

        // then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.value().outcome()).isEqualTo(SeedingOutcome.CREATED);
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
        Result<SeedingReport> result = handler.handle(command("87543210932", ""));

        // then
        assertThat(result.value().outcome()).isEqualTo(SeedingOutcome.NOT_NEEDED);
        assertThat(repository.activeAdministrators()).hasSize(1);
        assertThat(repository.findByEmailOrMobilePhone("admin@ovyx.com.br")).isEmpty();
    }

    @Test
    @DisplayName("fails without a password instead of generating one")
    void givenBlankPassword_whenSeeding_thenFailInsteadOfGeneratingOne() {
        // given
        // A versao anterior gerava uma senha e a imprimia no log, contrariando FR-021.
        String blankPassword = " ";

        // when
        Result<SeedingReport> result = handler.handle(command("87543210932", blankPassword));

        // then
        assertThat(result.isFailure()).isTrue();
        assertThat(result.error().code()).isEqualTo(SeedInitialAdministratorCommandHandler.PASSWORD_REQUIRED);
        assertThat(repository.activeAdministrators()).isEmpty();
    }

    @Test
    @DisplayName("fails with every violation of an invalid configuration and never echoes the password")
    void givenInvalidCpfInConfiguration_whenSeeding_thenFailWithoutEchoingThePassword() {
        // given
        String invalidCpf = "12345678901";

        // when
        Result<SeedingReport> result = handler.handle(command(invalidCpf, PASSWORD));

        // then
        assertThat(result.isFailure()).isTrue();
        assertThat(result.error().details()).containsEntry("cpf", "CPF inválido.");
        assertThat(result.error().details().toString()).doesNotContain(PASSWORD);
        assertThat(result.error().message()).doesNotContain(PASSWORD);
        assertThat(repository.activeAdministrators()).isEmpty();
    }

    /** Quem ja tem o CPF configurado: outra pessoa nao pode te-lo, porque o CPF nao se repete. */
    private CaretakerTestDataBuilder holderOfTheConfiguredCpf(Role role) {
        return aCaretaker()
                .withCpf(CONFIGURED_CPF)
                .withEmail("antigo.administrador@ovyx.com.br")
                .withMobilePhone("91977776666")
                .withRole(role)
                .withHasher(hasher)
                .withClock(clock);
    }

    private static Stream<Arguments> holdersOfTheConfiguredCpf() {
        return Stream.of(
                Arguments.of(
                        "an inactive former administrator",
                        (Function<CaretakerTestDataBuilder, Caretaker>) CaretakerTestDataBuilder::buildInactive,
                        Role.ADMINISTRATOR),
                Arguments.of(
                        "an inactive common user",
                        (Function<CaretakerTestDataBuilder, Caretaker>) CaretakerTestDataBuilder::buildInactive,
                        Role.USER),
                Arguments.of(
                        "an active common user",
                        (Function<CaretakerTestDataBuilder, Caretaker>) CaretakerTestDataBuilder::build,
                        Role.USER));
    }

    @ParameterizedTest(name = "restores {0}")
    @MethodSource("holdersOfTheConfiguredCpf")
    @DisplayName("gives the administration back to whoever holds the configured CPF, instead of refusing to start")
    void givenNoActiveAdministratorAndAHolderOfTheConfiguredCpf_whenSeeding_thenRestoreThatCaretakerAsAdministrator(
            String situation, Function<CaretakerTestDataBuilder, Caretaker> stored, Role formerRole) {
        // given
        // Sem administrador ativo, cadastrar de novo esbarrava no proprio CPF (CPF_ALREADY_IN_USE),
        // e a aplicacao recusava subir: a instalacao ficava sem nenhuma saida (FR-025).
        Caretaker holder = stored.apply(holderOfTheConfiguredCpf(formerRole));
        repository.save(holder);

        // when
        Result<SeedingReport> result = handler.handle(command(CONFIGURED_CPF, PASSWORD));

        // then
        assertThat(result.value().outcome()).isEqualTo(SeedingOutcome.RESTORED);
        Caretaker restored = repository.findById(holder.id()).orElseThrow();
        assertThat(restored.isActive()).isTrue();
        assertThat(restored.role()).isEqualTo(Role.ADMINISTRATOR);
        assertThat(restored.mustChangePassword()).isTrue();
        assertThat(restored.authenticate(PASSWORD, hasher)).isTrue();
        assertThat(repository.findByEmailOrMobilePhone("admin@ovyx.com.br"))
                .as("nobody registered with the configured email")
                .isEmpty();
    }

    @Test
    @DisplayName("reports the email to sign in with and what the restored caretaker was before")
    void givenInactiveCommonUserWithTheConfiguredCpf_whenSeeding_thenReportTheEmailAndThePreviousSituation() {
        // given
        // O restaurado mantem o e-mail que ja tinha: sem ele no aviso, quem seguia o guia com o
        // e-mail configurado recebia a recusa generica.
        repository.save(holderOfTheConfiguredCpf(Role.USER).buildInactive());

        // when
        Result<SeedingReport> result = handler.handle(command(CONFIGURED_CPF, PASSWORD));

        // then
        assertThat(result.value())
                .isEqualTo(SeedingReport.restored("antigo.administrador@ovyx.com.br", Role.USER, CaretakerStatus.INACTIVE));
    }

    @Test
    @DisplayName("refuses the restoration as a conflict when another active caretaker took the email meanwhile")
    void givenInactiveAdministratorWhoseEmailAnotherActiveTook_whenSeeding_thenFailAsConflictAndRestoreNobody() {
        // given
        Caretaker former = holderOfTheConfiguredCpf(Role.ADMINISTRATOR).buildInactive();
        repository.save(former);
        repository.save(aCaretaker()
                .withCpf("11144477735")
                .withEmail("antigo.administrador@ovyx.com.br")
                .withMobilePhone("91991234567")
                .withHasher(hasher)
                .withClock(clock)
                .build());

        // when
        Result<SeedingReport> result = handler.handle(command(CONFIGURED_CPF, PASSWORD));

        // then
        assertThat(result.error().type()).isEqualTo(ErrorType.CONFLICT);
        assertThat(result.error().code()).isEqualTo("EMAIL_ALREADY_IN_USE");
        assertThat(repository.activeAdministrators()).isEmpty();
    }

    @Test
    @DisplayName("fails as a conflict when an active common user already has the configured email, and saves nothing")
    void givenConfiguredEmailHeldByAnActiveCommonUser_whenSeeding_thenFailAsConflictAndSaveNothing() {
        // given
        // Um e-mail ja usado e conflito com outro responsavel, e nao dado invalido: a mensagem de
        // subida precisa apontar para a configuracao, e nao para o formato do e-mail.
        repository.save(aCaretaker()
                .withCpf("11144477735")
                .withEmail("admin@ovyx.com.br")
                .withMobilePhone("91991234567")
                .withHasher(hasher)
                .withClock(clock)
                .build());

        // when
        Result<SeedingReport> result = handler.handle(command(CONFIGURED_CPF, PASSWORD));

        // then
        assertThat(result.error().type()).isEqualTo(ErrorType.CONFLICT);
        assertThat(result.error().code()).isEqualTo("EMAIL_ALREADY_IN_USE");
        assertThat(repository.findByCpf(Cpf.of(CONFIGURED_CPF))).as("nothing saved").isEmpty();
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
