package io.github.ovyx.shared.application.spreadsheet;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.DisplayName;

/** O nome simplificado do arquivo (R-013 da 007): sem acento, minúsculas e hífens. */
@DisplayName("FileNames")
class FileNamesTest {

    @ParameterizedTest(name = "given \"{0}\" then \"{1}\"")
    @CsvSource(
            delimiter = '|',
            value = {
                "Codornas — Galpão 1|codornas-galpao-1",
                "Poedeiras  /  Galpão 2 (Novo)|poedeiras-galpao-2-novo",
                "Çãõ ÁÉÍ|cao-aei",
                "--Setor A--|setor-a",
                "B-07|b-07"
            })
    @DisplayName("simplifies the name of the sector for the file")
    void givenNameOfTheSector_whenSlugging_thenLeaveLettersDigitsAndHyphensOnly(String name, String expected) {
        // given
        String text = name;

        // when
        String slug = FileNames.slug(text);

        // then
        assertThat(slug).isEqualTo(expected);
    }

    @ParameterizedTest(name = "given \"{0}\" then setor")
    @CsvSource(delimiter = '|', value = {"''", "'   '", "— / —"})
    @DisplayName("falls back to setor when nothing is left of the name")
    void givenNameWithoutLettersNorDigits_whenSlugging_thenAnswerSetor(String name) {
        // given
        String text = name;

        // when
        String slug = FileNames.slug(text);

        // then
        assertThat(slug).isEqualTo("setor");
    }
}
