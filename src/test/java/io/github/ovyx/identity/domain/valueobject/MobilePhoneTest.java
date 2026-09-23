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
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("MobilePhone")
class MobilePhoneTest {

    @Test
    @DisplayName("accepts an 11-digit mobile phone and stores digits only")
    void givenFormattedElevenDigitPhone_whenCreating_thenStoreDigitsOnly() {
        // given
        String formatted = "(91) 98888-7777";

        // when
        MobilePhone phone = MobilePhone.of(formatted);

        // then
        assertThat(phone.value()).isEqualTo("91988887777");
    }

    @Test
    @DisplayName("accepts a 10-digit phone")
    void givenTenDigitPhone_whenCreating_thenAcceptIt() {
        // given
        String tenDigits = "9132244556";

        // when
        MobilePhone phone = MobilePhone.of(tenDigits);

        // then
        assertThat(phone.value()).isEqualTo("9132244556");
    }

    @Test
    @DisplayName("rejects a missing mobile phone")
    void givenNullMobilePhone_whenCreating_thenRejectAsRequired() {
        // given
        String missing = null;

        // when
        List<Violation> violations = violationsOf(() -> MobilePhone.of(missing));

        // then
        assertThat(violations)
                .extracting(Violation::field, Violation::code)
                .containsExactly(tuple("mobilePhone", IdentityErrorCode.MOBILE_PHONE_REQUIRED));
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "919888877", "919888877771", "9", // quantidade de digitos diferente de 10 ou 11
                "01988887777", "10988887777", // DDD fora de 11 a 99
                "9198888777A" // letra
            })
    @DisplayName("rejects a phone that is not a valid mobile phone, as a single violation")
    void givenInvalidMobilePhone_whenCreating_thenRejectWithASingleViolation(String raw) {
        // given — raw from @ValueSource

        // when
        List<Violation> violations = violationsOf(() -> MobilePhone.of(raw));

        // then
        assertThat(violations)
                .extracting(Violation::field, Violation::code)
                .containsExactly(tuple("mobilePhone", IdentityErrorCode.MOBILE_PHONE_INVALID));
    }
}
