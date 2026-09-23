package io.github.ovyx.identity.application.authentication;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Garante que o comando de entrada nao expoe a senha em {@code toString()} (FR-021).
 *
 * <p>O {@code toString()} gerado para records inclui todos os componentes. Um log em DEBUG, uma
 * mensagem de excecao ou um despejo de objeto bastariam para gravar a senha em texto claro.
 */
@DisplayName("SignInCommand")
class SignInCommandTest {

    @Test
    @DisplayName("masks the password and keeps the identifier")
    void givenSignInCommand_whenDescribingIt_thenMaskThePasswordAndKeepTheIdentifier() {
        // given
        SignInCommand command = new SignInCommand("maria.silva@ovyx.com.br", "GranjaNorte2026", "203.0.113.42");

        // when
        String text = command.toString();

        // then
        assertThat(text).doesNotContain("GranjaNorte2026").contains("maria.silva@ovyx.com.br");
    }
}
