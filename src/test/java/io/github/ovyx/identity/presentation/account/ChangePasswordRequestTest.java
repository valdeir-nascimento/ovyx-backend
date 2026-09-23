package io.github.ovyx.identity.presentation.account;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Garante que o corpo de troca de senha nao expoe nenhuma das duas senhas em {@code toString()}
 * (FR-021): o Spring MVC registra o corpo desserializado em nivel DEBUG.
 */
@DisplayName("ChangePasswordRequest")
class ChangePasswordRequestTest {

    @Test
    @DisplayName("masks both passwords")
    void givenPasswordChangeBody_whenDescribingIt_thenMaskBothPasswords() {
        // given
        ChangePasswordRequest body = new ChangePasswordRequest("GranjaNorte2026", "PosturaAviario2027");

        // when
        String text = body.toString();

        // then
        assertThat(text).doesNotContain("GranjaNorte2026").doesNotContain("PosturaAviario2027");
    }
}
