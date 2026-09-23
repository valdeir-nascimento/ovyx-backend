package io.github.ovyx.identity.domain.valueobject;

import static io.github.ovyx.identity.domain.DomainViolations.of;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("Cpf")
class CpfTest {

    @Test
    @DisplayName("accepts a valid CPF and stores digits only")
    void acceptsValidCpfAndStripsFormatting() {
        Cpf cpf = Cpf.of("529.982.247-25");

        assertThat(cpf.value()).isEqualTo("52998224725");
    }

    @Test
    @DisplayName("accepts a CPF without formatting")
    void acceptsUnformattedCpf() {
        assertThat(Cpf.of("11144477735").value()).isEqualTo("11144477735");
    }

    @Test
    @DisplayName("rejects a missing CPF")
    void rejectsMissingCpf() {
        assertThat(of(() -> Cpf.of(null))).containsEntry("cpf", "Informe o CPF.");
    }

    @ParameterizedTest
    @ValueSource(strings = {"1234567890", "123456789012", "5299822472"})
    @DisplayName("rejects a CPF without 11 digits")
    void rejectsWrongDigitCount(String raw) {
        assertThat(of(() -> Cpf.of(raw))).containsEntry("cpf", "CPF inválido.");
    }

    @ParameterizedTest
    @ValueSource(strings = {"00000000000", "11111111111", "99999999999"})
    @DisplayName("rejects repeated-digit sequences that pass the checksum but are not real CPFs")
    void rejectsRepeatedDigitSequences(String raw) {
        assertThat(of(() -> Cpf.of(raw))).containsEntry("cpf", "CPF inválido.");
    }

    @ParameterizedTest
    @ValueSource(strings = {"52998224724", "12345678901", "11144477734"})
    @DisplayName("rejects a CPF with wrong check digits")
    void rejectsWrongCheckDigits(String raw) {
        assertThat(of(() -> Cpf.of(raw))).containsEntry("cpf", "CPF inválido.");
    }

    @Test
    @DisplayName("rejects a CPF with letters")
    void rejectsLetters() {
        assertThat(of(() -> Cpf.of("5299822472A"))).containsEntry("cpf", "CPF inválido.");
    }
}
