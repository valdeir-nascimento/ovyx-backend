package io.github.ovyx.identity.infrastructure.bootstrap;

import static io.github.ovyx.identity.domain.model.CaretakerTestDataBuilder.aCaretaker;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.ovyx.identity.fixtures.InMemoryCaretakerRepository;
import io.github.ovyx.identity.application.administratorseeding.SeedInitialAdministratorCommandHandler;
import io.github.ovyx.identity.domain.FakePasswordHasher;
import io.github.ovyx.identity.domain.model.Caretaker;
import io.github.ovyx.identity.domain.model.Role;
import io.github.ovyx.shared.application.Dispatcher;
import io.github.ovyx.shared.domain.FixedClock;
import io.github.ovyx.shared.infrastructure.SpringBeanDispatcher;
import java.util.List;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.transaction.support.TransactionOperations;

/**
 * Testes da execucao da semeadura na subida da aplicacao.
 *
 * <p>A regra esta coberta em {@code SeedInitialAdministratorCommandHandlerTest}. Aqui se verifica
 * so o que cabe a este adaptador: levar a configuracao ao caso de uso e recusar a subida quando ele
 * falha, com uma mensagem que diga o que fazer e nao repita a senha.
 */
@DisplayName("BootstrapAdministratorInitializer")
@ExtendWith(OutputCaptureExtension.class)
class BootstrapAdministratorInitializerTest {

    private static final String PASSWORD = "TrocarNoPrimeiroAcesso2026";

    private final FakePasswordHasher hasher = new FakePasswordHasher();
    private final InMemoryCaretakerRepository repository = new InMemoryCaretakerRepository();
    private final Dispatcher dispatcher = new SpringBeanDispatcher(
            List.of(new SeedInitialAdministratorCommandHandler(
                    repository, hasher, FixedClock.at("2026-09-21T12:00:00Z"))),
            List.of(),
            TransactionOperations.withoutTransaction());

    private BootstrapAdministratorInitializer initializer(String cpf, String password) {
        BootstrapAdministratorProperties properties = new BootstrapAdministratorProperties(
                "Administrador do Sistema", cpf, "admin@ovyx.com.br", "11988880000", password);
        return new BootstrapAdministratorInitializer(dispatcher, properties);
    }

    @Test
    @DisplayName("creates the initial administrator from the configuration")
    void givenValidConfiguration_whenStarting_thenCreateTheInitialAdministrator() {
        // given
        BootstrapAdministratorInitializer initializer = initializer("87543210932", PASSWORD);

        // when
        initializer.run(null);

        // then
        assertThat(repository.findByEmailOrMobilePhone("admin@ovyx.com.br")).isPresent();
    }

    @Test
    @DisplayName("starts restoring the only inactive administrator, instead of refusing to start")
    void givenOnlyAnInactiveAdministratorWithTheConfiguredCpf_whenStarting_thenStartWithThemActive() {
        // given
        Caretaker former = aCaretaker()
                .withCpf("87543210932")
                .withRole(Role.ADMINISTRATOR)
                .withHasher(hasher)
                .buildInactive();
        repository.save(former);
        BootstrapAdministratorInitializer initializer = initializer("87543210932", PASSWORD);

        // when
        initializer.run(null);

        // then
        assertThat(repository.findById(former.id()).orElseThrow().isActive()).isTrue();
    }

    @Test
    @DisplayName("tells, when it restores, the email to sign in with and what the caretaker was before")
    void givenOnlyAnInactiveAdministratorWithTheConfiguredCpf_whenStarting_thenLogTheEmailToSignInWith(
            CapturedOutput output) {
        // given
        repository.save(aCaretaker()
                .withCpf("87543210932")
                .withRole(Role.ADMINISTRATOR)
                .withHasher(hasher)
                .buildInactive());
        BootstrapAdministratorInitializer initializer = initializer("87543210932", PASSWORD);

        // when
        initializer.run(null);

        // then
        assertThat(output).contains("maria.silva@ovyx.com.br").contains("antes administrador e inativo");
    }

    @Test
    @DisplayName("refuses to start without a password and says which variable to set")
    void givenMissingPassword_whenStarting_thenRefuseNamingTheVariableToSet() {
        // given
        BootstrapAdministratorInitializer initializer = initializer("87543210932", "");

        // when
        ThrowingCallable startup = () -> initializer.run(null);

        // then
        assertThatThrownBy(startup)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("OVYX_BOOTSTRAP_ADMIN_PASSWORD");
    }

    @Test
    @DisplayName("refuses to start with an invalid configuration and does not echo the password")
    void givenInvalidCpfInConfiguration_whenStarting_thenRefuseWithoutEchoingThePassword() {
        // given
        BootstrapAdministratorInitializer initializer = initializer("12345678901", PASSWORD);

        // when
        ThrowingCallable startup = () -> initializer.run(null);

        // then
        assertThatThrownBy(startup)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("cpf=CPF inválido.")
                .hasMessageNotContaining(PASSWORD);
    }
}
