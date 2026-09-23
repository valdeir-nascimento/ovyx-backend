package io.github.ovyx.identity.presentation.authentication;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Garante que o corpo de entrada nao expoe a senha em {@code toString()} (FR-021).
 *
 * <p>O Spring MVC registra o corpo desserializado em nivel DEBUG. Com o {@code toString()} gerado
 * para records, ligar DEBUG em producao para investigar um incidente gravaria senhas no log.
 */
@DisplayName("SignInRequest")
class SignInRequestTest {

    @Test
    @DisplayName("masks the password and keeps the identifier")
    void givenSignInBody_whenDescribingIt_thenMaskThePasswordAndKeepTheIdentifier() {
        // given
        SignInRequest body = new SignInRequest("maria.silva@ovyx.com.br", "GranjaNorte2026");

        // when
        String text = body.toString();

        // then
        assertThat(text).doesNotContain("GranjaNorte2026").contains("maria.silva@ovyx.com.br");
    }
}
