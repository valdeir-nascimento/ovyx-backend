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

@DisplayName("Cpf")
class CpfTest {

    @Test
    @DisplayName("accepts a valid CPF and stores digits only")
    void givenFormattedCpf_whenCreating_thenStoreDigitsOnly() {
        // given
        String formatted = "529.982.247-25";

        // when
        Cpf cpf = Cpf.of(formatted);

        // then
        assertThat(cpf.value()).isEqualTo("52998224725");
    }

    @Test
    @DisplayName("accepts a CPF without formatting")
    void givenUnformattedValidCpf_whenCreating_thenKeepItAsIs() {
        // given
        String unformatted = "11144477735";

        // when
        Cpf cpf = Cpf.of(unformatted);

        // then
        assertThat(cpf.value()).isEqualTo("11144477735");
    }

    @Test
    @DisplayName("rejects a missing CPF")
    void givenNullCpf_whenCreating_thenRejectAsRequired() {
        // given
        String missing = null;

        // when
        List<Violation> violations = violationsOf(() -> Cpf.of(missing));

        // then
        assertThat(violations)
                .extracting(Violation::field, Violation::code)
                .containsExactly(tuple("cpf", IdentityErrorCode.CPF_REQUIRED));
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "1234567890", "123456789012", "5299822472", // sem 11 digitos
                "00000000000", "11111111111", "99999999999", // sequencia repetida: passa no calculo, nao e CPF
                "52998224724", "12345678901", "11144477734", // digitos verificadores errados
                "5299822472A" // letra
            })
    @DisplayName("rejects a CPF that is not a real one, as a single violation")
    void givenInvalidCpf_whenCreating_thenRejectWithASingleViolation(String raw) {
        // given — raw from @ValueSource

        // when
        List<Violation> violations = violationsOf(() -> Cpf.of(raw));

        // then
        assertThat(violations)
                .extracting(Violation::field, Violation::code)
                .containsExactly(tuple("cpf", IdentityErrorCode.CPF_INVALID));
    }
}
