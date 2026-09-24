package io.github.ovyx.identity.domain.valueobject;

import static io.github.ovyx.identity.domain.DomainViolations.violationsOf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import io.github.ovyx.identity.domain.IdentityErrorCode;
import io.github.ovyx.shared.domain.Violation;
import java.util.List;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("Email")
class EmailTest {

    @Test
    @DisplayName("accepts a valid email")
    void givenValidEmail_whenCreating_thenKeepIt() {
        // given
        String valid = "maria.silva@ovyx.com.br";

        // when
        Email email = Email.of(valid);

        // then
        assertThat(email.value()).isEqualTo("maria.silva@ovyx.com.br");
    }

    @Test
    @DisplayName("normalizes to lowercase and trims surrounding spaces")
    void givenMixedCaseEmailWithSpaces_whenCreating_thenStoreLowercaseAndTrimmed() {
        // given
        String mixedCase = "  Maria.Silva@OVYX.com.BR ";

        // when
        Email email = Email.of(mixedCase);

        // then
        assertThat(email.value()).isEqualTo("maria.silva@ovyx.com.br");
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"  "})
    @DisplayName("rejects a missing email")
    void givenMissingEmail_whenCreating_thenRejectAsRequired(String raw) {
        // given — raw from @NullSource and @ValueSource

        // when
        List<Violation> violations = violationsOf(() -> Email.of(raw));

        // then
        assertThat(violations)
                .extracting(Violation::field, Violation::code)
                .containsExactly(tuple("email", IdentityErrorCode.EMAIL_REQUIRED));
    }

    @ParameterizedTest
    @ValueSource(
            strings = {"sem-arroba", "@ovyx.com.br", "maria@", "maria@ovyx", "ma ria@ovyx.com.br", "maria@@ovyx.com.br"
            })
    @DisplayName("rejects an email in an invalid format")
    void givenMalformedEmail_whenCreating_thenRejectAsMalformed(String raw) {
        // given — raw from @ValueSource

        // when
        List<Violation> violations = violationsOf(() -> Email.of(raw));

        // then
        assertThat(violations).extracting(Violation::code).containsExactly(IdentityErrorCode.EMAIL_MALFORMED);
    }

    @Test
    @DisplayName("accepts an email of exactly 254 characters")
    void givenEmailOfExactly254Characters_whenCreating_thenKeepIt() {
        // given
        // 254 e o teto do modelo de dados, do tamanho da coluna; um limite deslocado reprova aqui.
        String atTheLimit = "a".repeat(242) + "@ovyx.com.br";
        assertThat(atTheLimit).as("precondition: the email sits on the limit").hasSize(254);

        // when
        Email email = Email.of(atTheLimit);

        // then
        assertThat(email.value()).isEqualTo(atTheLimit);
    }

    @Test
    @DisplayName("rejects an email of 255 characters, one past the limit")
    void givenEmailOfOneCharacterPastTheLimit_whenCreating_thenRejectAsTooLong() {
        // given
        String tooLong = "a".repeat(243) + "@ovyx.com.br";
        assertThat(tooLong).as("precondition: the email is one past the limit").hasSize(255);

        // when
        List<Violation> violations = violationsOf(() -> Email.of(tooLong));

        // then
        assertThat(violations).extracting(Violation::code).containsExactly(IdentityErrorCode.EMAIL_TOO_LONG);
    }

    @Test
    @DisplayName("reports length and format violations of the same field together")
    void givenLongAndMalformedEmail_whenCreating_thenReportBothRules() {
        // given
        String longAndMalformed = "a".repeat(260);

        // when
        List<Violation> violations = violationsOf(() -> Email.of(longAndMalformed));

        // then
        assertThat(violations)
                .extracting(Violation::code)
                .containsExactly(IdentityErrorCode.EMAIL_TOO_LONG, IdentityErrorCode.EMAIL_MALFORMED);
    }

    @Test
    @DisplayName("skips the format check beyond the safety ceiling, where length alone refuses")
    void givenEmailBeyondTheFormatCheckCeiling_whenCreating_thenReportOnlyTheLength() {
        // given
        // A expressao de formato recursa por segmento; acima do teto, uma entrada absurda poderia
        // estourar a pilha, e o "longo demais" ja basta para recusar.
        String absurd = "a".repeat(2000);

        // when
        List<Violation> violations = violationsOf(() -> Email.of(absurd));

        // then
        assertThat(violations).extracting(Violation::code).containsExactly(IdentityErrorCode.EMAIL_TOO_LONG);
    }

    @Test
    @DisplayName("refuses a null value on the canonical constructor, the rehydration path")
    void givenNullValue_whenConstructingDirectly_thenThrowNullPointerException() {
        // given
        String missing = null;

        // when
        ThrowingCallable constructing = () -> new Email(missing);

        // then
        assertThatThrownBy(constructing).isInstanceOf(NullPointerException.class).hasMessage("value");
    }
}
