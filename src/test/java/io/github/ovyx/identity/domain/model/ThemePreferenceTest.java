package io.github.ovyx.identity.domain.model;

import static io.github.ovyx.shared.domain.DomainViolations.violationsOf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import io.github.ovyx.identity.domain.IdentityErrorCode;
import io.github.ovyx.shared.domain.Violation;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

/** A preferência de tema do responsável: claro, escuro ou igual ao sistema (R-001 da 011). */
@DisplayName("ThemePreference")
class ThemePreferenceTest {

    @ParameterizedTest(name = "\"{0}\" is {1}")
    @CsvSource({"LIGHT, LIGHT", "DARK, DARK", "SYSTEM, SYSTEM", "' dark ', DARK", "Dark, DARK", "system, SYSTEM"})
    @DisplayName("reads the theme regardless of case and surrounding spaces")
    void givenThemeName_whenReading_thenFindTheTheme(String raw, ThemePreference expected) {
        // given — valor bruto vindo do @CsvSource

        // when
        ThemePreference theme = ThemePreference.of(raw);

        // then
        assertThat(theme).isEqualTo(expected);
    }

    @ParameterizedTest(name = "\"{0}\"")
    @NullSource
    @ValueSource(strings = {"", "  ", "AZUL", "ESCURO", "1"})
    @DisplayName("refuses what is not a theme, on the field, with the message the contract publishes")
    void givenTextThatIsNotATheme_whenReading_thenRefuseOnTheField(String raw) {
        // given — valor bruto vindo do @NullSource e do @ValueSource

        // when
        List<Violation> violations = violationsOf(() -> ThemePreference.of(raw));

        // then
        assertThat(violations)
                .extracting(Violation::field, Violation::code, Violation::message)
                .containsExactly(tuple(
                        "theme",
                        IdentityErrorCode.THEME_INVALID,
                        "Escolha o tema claro, o escuro ou o igual ao sistema."));
    }
}
