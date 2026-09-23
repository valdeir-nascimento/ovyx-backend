package io.github.ovyx.identity.domain.valueobject;

import static io.github.ovyx.identity.domain.DomainViolations.violationsOf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import io.github.ovyx.identity.domain.IdentityErrorCode;
import io.github.ovyx.shared.domain.Violation;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("FullName")
class FullNameTest {

    @Test
    @DisplayName("accepts a valid name and trims surrounding spaces")
    void givenNameWithSurroundingSpaces_whenCreating_thenStoreItTrimmed() {
        // given
        String padded = "  João Pereira de Souza  ";

        // when
        FullName name = FullName.of(padded);

        // then
        assertThat(name.value()).isEqualTo("João Pereira de Souza");
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"   "})
    @DisplayName("rejects a missing name")
    void givenMissingName_whenCreating_thenRejectAsRequired(String raw) {
        // given — raw from @NullSource and @ValueSource

        // when
        List<Violation> violations = violationsOf(() -> FullName.of(raw));

        // then
        assertThat(violations)
                .extracting(Violation::field, Violation::code)
                .containsExactly(tuple("fullName", IdentityErrorCode.FULL_NAME_REQUIRED));
    }

    @Test
    @DisplayName("rejects a name shorter than 3 characters after trimming")
    void givenNameShorterThanThreeCharactersAfterTrimming_whenCreating_thenRejectAsTooShort() {
        // given
        String tooShort = "  Jo  ";

        // when
        List<Violation> violations = violationsOf(() -> FullName.of(tooShort));

        // then
        assertThat(violations).extracting(Violation::code).containsExactly(IdentityErrorCode.FULL_NAME_TOO_SHORT);
    }

    @Test
    @DisplayName("rejects a name longer than 120 characters")
    void givenNameLongerThan120Characters_whenCreating_thenRejectAsTooLong() {
        // given
        String tooLong = "a".repeat(121);

        // when
        List<Violation> violations = violationsOf(() -> FullName.of(tooLong));

        // then
        assertThat(violations).extracting(Violation::code).containsExactly(IdentityErrorCode.FULL_NAME_TOO_LONG);
    }

    @ParameterizedTest(name = "accepts exactly {0} characters")
    @ValueSource(ints = {3, 120})
    @DisplayName("accepts exactly 3 and exactly 120 characters")
    void givenNameAtLengthBoundary_whenCreating_thenAcceptIt(int length) {
        // given
        String atBoundary = "a".repeat(length);

        // when
        FullName name = FullName.of(atBoundary);

        // then
        assertThat(name.value()).hasSize(length);
    }

    @Test
    @DisplayName("rejects a name without any letter")
    void givenNameWithoutLetters_whenCreating_thenRejectAsWithoutLetter() {
        // given
        String digitsOnly = "123 456";

        // when
        List<Violation> violations = violationsOf(() -> FullName.of(digitsOnly));

        // then
        assertThat(violations)
                .extracting(Violation::code)
                .containsExactly(IdentityErrorCode.FULL_NAME_WITHOUT_LETTER);
    }

    @Test
    @DisplayName("reports length and letter violations of the same field together")
    void givenShortNameWithoutLetters_whenCreating_thenReportBothRules() {
        // given
        // "12" e curto demais E nao tem letra. As duas violacoes precisam voltar juntas, senao a
        // pessoa corrige o comprimento e so entao descobre o segundo problema.
        String shortWithoutLetters = "12";

        // when
        List<Violation> violations = violationsOf(() -> FullName.of(shortWithoutLetters));

        // then
        assertThat(violations)
                .extracting(Violation::code)
                .containsExactly(IdentityErrorCode.FULL_NAME_TOO_SHORT, IdentityErrorCode.FULL_NAME_WITHOUT_LETTER);
    }
}
