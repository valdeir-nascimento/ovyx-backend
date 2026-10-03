package io.github.ovyx.identity.application.recovery;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.ovyx.identity.domain.valueobject.RecoveryToken;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** O e-mail do link nunca mostra o código, nem o nome, no texto que um log registraria (FR-018 da 012). */
@DisplayName("RecoveryLinkMail")
class RecoveryLinkMailTest {

    @Test
    @DisplayName("shows only the recipient in its text form")
    void givenRecoveryLinkMail_whenPrinting_thenShowOnlyTheRecipient() {
        // given
        RecoveryLinkMail mail = new RecoveryLinkMail(
                "marina.costa@ovyx.com.br",
                "Marina Costa",
                RecoveryToken.of("3q2-7wq9Xk1vF0bQm8ZsY4tLr6NcHe2JpWdUaGo5iKx"),
                Instant.parse("2026-10-02T12:30:00Z"));

        // when
        String printed = mail.toString();

        // then
        assertThat(printed).contains("marina.costa@ovyx.com.br").doesNotContain("3q2-7wq9").doesNotContain("Marina Costa");
    }
}
