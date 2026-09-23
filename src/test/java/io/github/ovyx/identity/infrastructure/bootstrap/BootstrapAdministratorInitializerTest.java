package io.github.ovyx.identity.infrastructure.bootstrap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.ovyx.identity.application.InMemoryCaretakerRepository;
import io.github.ovyx.identity.application.administratorseeding.SeedInitialAdministratorCommandHandler;
import io.github.ovyx.identity.domain.FakePasswordHasher;
import io.github.ovyx.shared.domain.FixedClock;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Testes da execucao da semeadura na subida da aplicacao.
 *
 * <p>A regra esta coberta em {@code SeedInitialAdministratorCommandHandlerTest}. Aqui se verifica
 * so o que cabe a este adaptador: levar a configuracao ao caso de uso e recusar a subida quando ele
 * falha, com uma mensagem que diga o que fazer e nao repita a senha.
 */
@DisplayName("BootstrapAdministratorInitializer")
class BootstrapAdministratorInitializerTest {

    private static final String PASSWORD = "TrocarNoPrimeiroAcesso2026";

    private final InMemoryCaretakerRepository repository = new InMemoryCaretakerRepository();
    private final SeedInitialAdministratorCommandHandler handler = new SeedInitialAdministratorCommandHandler(
            repository, new FakePasswordHasher(), FixedClock.at("2026-09-21T12:00:00Z"));

    private BootstrapAdministratorInitializer initializer(String cpf, String password) {
        BootstrapAdministratorProperties properties = new BootstrapAdministratorProperties(
                "Administrador do Sistema", cpf, "admin@ovyx.com.br", "11988880000", password);
        return new BootstrapAdministratorInitializer(handler, properties);
    }

    @Test
    @DisplayName("creates the initial administrator from the configuration")
    void createsAdministratorFromConfiguration() {
        initializer("87543210932", PASSWORD).run(null);

        assertThat(repository.findByEmailOrMobilePhone("admin@ovyx.com.br")).isPresent();
    }

    @Test
    @DisplayName("refuses to start without a password and says which variable to set")
    void refusesToStartWithoutPassword() {
        assertThatThrownBy(() -> initializer("87543210932", "").run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("OVYX_BOOTSTRAP_ADMIN_PASSWORD");
    }

    @Test
    @DisplayName("refuses to start with an invalid configuration and does not echo the password")
    void refusesInvalidConfigurationWithoutEchoingPassword() {
        assertThatThrownBy(() -> initializer("12345678901", PASSWORD).run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("cpf=CPF inválido.")
                .hasMessageNotContaining(PASSWORD);
    }
}
