package io.github.ovyx.identity.domain.valueobject;

import static io.github.ovyx.identity.domain.DomainViolations.of;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("MobilePhone")
class MobilePhoneTest {

    private static final String INVALID = "Informe um celular com DDD, contendo 10 ou 11 dígitos.";

    @Test
    @DisplayName("accepts an 11-digit mobile phone and stores digits only")
    void acceptsElevenDigitsAndStripsFormatting() {
        MobilePhone phone = MobilePhone.of("(91) 98888-7777");

        assertThat(phone.value()).isEqualTo("91988887777");
    }

    @Test
    @DisplayName("accepts a 10-digit phone")
    void acceptsTenDigits() {
        assertThat(MobilePhone.of("9132244556").value()).isEqualTo("9132244556");
    }

    @Test
    @DisplayName("rejects a missing mobile phone")
    void rejectsMissingMobilePhone() {
        assertThat(of(() -> MobilePhone.of(null))).containsEntry("mobilePhone", "Informe o celular.");
    }

    @ParameterizedTest
    @ValueSource(strings = {"919888877", "919888877771", "9"})
    @DisplayName("rejects a digit count other than 10 or 11")
    void rejectsWrongDigitCount(String raw) {
        assertThat(of(() -> MobilePhone.of(raw))).containsEntry("mobilePhone", INVALID);
    }

    @ParameterizedTest
    @ValueSource(strings = {"01988887777", "10988887777"})
    @DisplayName("rejects an area code outside 11 to 99")
    void rejectsInvalidAreaCode(String raw) {
        assertThat(of(() -> MobilePhone.of(raw))).containsEntry("mobilePhone", INVALID);
    }

    @Test
    @DisplayName("rejects a mobile phone with letters")
    void rejectsLetters() {
        assertThat(of(() -> MobilePhone.of("9198888777A"))).containsEntry("mobilePhone", INVALID);
    }
}
