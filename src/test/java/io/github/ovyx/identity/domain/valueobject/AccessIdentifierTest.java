package io.github.ovyx.identity.domain.valueobject;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("AccessIdentifier")
class AccessIdentifierTest {

    @Test
    @DisplayName("normalizes an email to trimmed lowercase")
    void givenMixedCaseEmailWithSpaces_whenNormalizing_thenReturnTrimmedLowercase() {
        // given
        String typed = "  Maria.Silva@OVYX.com.br ";

        // when
        AccessIdentifier identifier = AccessIdentifier.of(typed);

        // then
        assertThat(identifier.value()).isEqualTo("maria.silva@ovyx.com.br");
    }

    @ParameterizedTest
    @ValueSource(strings = {"91988887777", "(91) 98888-7777", " 91 98888 7777 ", "91.98888.7777"})
    @DisplayName("reduces every formatting of the same mobile phone to the same digits")
    void givenFormattedMobilePhone_whenNormalizing_thenReturnDigitsOnly(String typed) {
        // given — digitado, vindo do @ValueSource

        // when
        AccessIdentifier identifier = AccessIdentifier.of(typed);

        // then
        assertThat(identifier.value()).isEqualTo("91988887777");
    }

    @Test
    @DisplayName("keeps letters instead of discarding them, so a mistyped identifier matches nobody")
    void givenPhoneWithStrayLetter_whenNormalizing_thenKeepTheLetter() {
        // given
        String mistyped = "x91988887777";

        // when
        AccessIdentifier identifier = AccessIdentifier.of(mistyped);

        // then
        assertThat(identifier.value()).isEqualTo("x91988887777");
    }

    @Test
    @DisplayName("treats anything containing an at sign as an email")
    void givenDigitsWithAtSign_whenNormalizing_thenTreatAsEmail() {
        // given
        String digitsAtDomain = "91988887777@qualquer.com";

        // when
        AccessIdentifier identifier = AccessIdentifier.of(digitsAtDomain);

        // then
        assertThat(identifier.value()).isEqualTo("91988887777@qualquer.com");
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"   "})
    @DisplayName("produces an empty key for a missing identifier")
    void givenMissingIdentifier_whenNormalizing_thenReturnEmptyKey(String typed) {
        // given — digitado, vindo do @NullSource e do @ValueSource

        // when
        AccessIdentifier identifier = AccessIdentifier.of(typed);

        // then
        assertThat(identifier.value()).isEmpty();
    }

    @ParameterizedTest(name = "\"{0}\" equals \"{1}\"")
    @CsvSource({"'(91) 98888-7777', 91988887777", "MARIA@ovyx.com.br, maria@ovyx.com.br"})
    @DisplayName("has value equality between different spellings of the same account")
    void givenTwoSpellingsOfTheSameAccount_whenComparing_thenTreatThemAsEqual(String spelling, String canonical) {
        // given — as duas grafias vindas do @CsvSource

        // when
        AccessIdentifier typed = AccessIdentifier.of(spelling);

        // then
        assertThat(typed).isEqualTo(AccessIdentifier.of(canonical));
    }
}
