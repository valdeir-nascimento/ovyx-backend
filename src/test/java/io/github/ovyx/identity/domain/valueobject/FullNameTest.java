package io.github.ovyx.identity.domain.valueobject;

import static io.github.ovyx.identity.domain.DomainViolations.of;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("FullName")
class FullNameTest {

    @Test
    @DisplayName("accepts a valid name and trims surrounding spaces")
    void acceptsValidNameAndTrims() {
        FullName name = FullName.of("  João Pereira de Souza  ");

        assertThat(name.value()).isEqualTo("João Pereira de Souza");
    }

    @Test
    @DisplayName("rejects a missing name")
    void rejectsMissingName() {
        assertThat(of(() -> FullName.of(null))).containsEntry("fullName", "Informe o nome completo.");
        assertThat(of(() -> FullName.of("   "))).containsEntry("fullName", "Informe o nome completo.");
    }

    @Test
    @DisplayName("rejects a name shorter than 3 characters after trimming")
    void rejectsTooShortName() {
        assertThat(of(() -> FullName.of("  Jo  ")))
                .hasEntrySatisfying("fullName", message -> assertThat(message).contains("ao menos 3 caracteres"));
    }

    @Test
    @DisplayName("rejects a name longer than 120 characters")
    void rejectsTooLongName() {
        assertThat(of(() -> FullName.of("a".repeat(121))))
                .hasEntrySatisfying("fullName", message -> assertThat(message).contains("no máximo 120 caracteres"));
    }

    @Test
    @DisplayName("accepts exactly 3 and exactly 120 characters")
    void acceptsBoundaryLengths() {
        assertThat(FullName.of("Ana").value()).isEqualTo("Ana");
        assertThat(FullName.of("a".repeat(120)).value()).hasSize(120);
    }

    @Test
    @DisplayName("rejects a name without any letter")
    void rejectsNameWithoutLetters() {
        assertThat(of(() -> FullName.of("123 456")))
                .hasEntrySatisfying("fullName", message -> assertThat(message).contains("ao menos uma letra"));
    }

    @Test
    @DisplayName("reports length and letter violations of the same field together")
    void reportsLengthAndLetterViolationsTogether() {
        // "12" e curto demais E nao tem letra. As duas violacoes precisam voltar juntas, senao a
        // pessoa corrige o comprimento e so entao descobre o segundo problema.
        assertThat(of(() -> FullName.of("12")))
                .hasEntrySatisfying("fullName", message -> assertThat(message)
                        .contains("ao menos 3 caracteres")
                        .contains("ao menos uma letra"));
    }
}
