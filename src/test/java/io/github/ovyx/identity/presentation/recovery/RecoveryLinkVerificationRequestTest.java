package io.github.ovyx.identity.presentation.recovery;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** O corpo da conferência nunca mostra o código do link (FR-018 da 012). */
@DisplayName("RecoveryLinkVerificationRequest")
class RecoveryLinkVerificationRequestTest {

    @Test
    @DisplayName("never shows the code of the link in its text form")
    void givenVerificationRequest_whenPrinting_thenHideTheCode() {
        // given
        RecoveryLinkVerificationRequest request =
                new RecoveryLinkVerificationRequest("3q2-7wq9Xk1vF0bQm8ZsY4tLr6NcHe2JpWdUaGo5iKx");

        // when
        String printed = request.toString();

        // then
        assertThat(printed).doesNotContain("3q2-7wq9");
    }
}
