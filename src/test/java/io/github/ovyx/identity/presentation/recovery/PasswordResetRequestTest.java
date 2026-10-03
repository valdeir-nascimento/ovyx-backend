package io.github.ovyx.identity.presentation.recovery;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** O corpo da redefinição nunca mostra o código nem a senha, nem no log de DEBUG do Spring MVC (FR-018 da 012). */
@DisplayName("PasswordResetRequest")
class PasswordResetRequestTest {

    @Test
    @DisplayName("never shows the code of the link nor the new password in its text form")
    void givenResetRequest_whenPrinting_thenHideTheCodeAndThePassword() {
        // given
        PasswordResetRequest request =
                new PasswordResetRequest("3q2-7wq9Xk1vF0bQm8ZsY4tLr6NcHe2JpWdUaGo5iKx", "PosturaAviario2027");

        // when
        String printed = request.toString();

        // then
        assertThat(printed).doesNotContain("3q2-7wq9").doesNotContain("PosturaAviario2027");
    }
}
