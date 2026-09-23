package io.github.ovyx.identity.domain.valueobject;

import static io.github.ovyx.identity.domain.DomainViolations.of;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("Email")
class EmailTest {

    @Test
    @DisplayName("accepts a valid email")
    void acceptsValidEmail() {
        Email email = Email.of("maria.silva@ovyx.com.br");

        assertThat(email.value()).isEqualTo("maria.silva@ovyx.com.br");
    }

    @Test
    @DisplayName("normalizes to lowercase and trims surrounding spaces")
    void normalizesAndTrims() {
        Email email = Email.of("  Maria.Silva@OVYX.com.BR ");

        assertThat(email.value()).isEqualTo("maria.silva@ovyx.com.br");
    }

    @Test
    @DisplayName("rejects a missing email")
    void rejectsMissingEmail() {
        assertThat(of(() -> Email.of(null))).containsEntry("email", "Informe o e-mail.");
        assertThat(of(() -> Email.of("  "))).containsEntry("email", "Informe o e-mail.");
    }

    @ParameterizedTest
    @ValueSource(
            strings = {"sem-arroba", "@ovyx.com.br", "maria@", "maria@ovyx", "ma ria@ovyx.com.br", "maria@@ovyx.com.br"
            })
    @DisplayName("rejects an email in an invalid format")
    void rejectsInvalidFormat(String raw) {
        assertThat(of(() -> Email.of(raw)))
                .hasEntrySatisfying("email", message -> assertThat(message).contains("formato válido"));
    }

    @Test
    @DisplayName("rejects an email longer than 254 characters")
    void rejectsTooLongEmail() {
        String tooLong = "a".repeat(250) + "@ovyx.com.br";

        assertThat(of(() -> Email.of(tooLong)))
                .hasEntrySatisfying("email", message -> assertThat(message).contains("no máximo 254 caracteres"));
    }

    @Test
    @DisplayName("reports length and format violations of the same field together")
    void reportsLengthAndFormatViolationsTogether() {
        String longAndMalformed = "a".repeat(260);

        assertThat(of(() -> Email.of(longAndMalformed)))
                .hasEntrySatisfying("email", message -> assertThat(message)
                        .contains("no máximo 254 caracteres")
                        .contains("formato válido"));
    }
}
