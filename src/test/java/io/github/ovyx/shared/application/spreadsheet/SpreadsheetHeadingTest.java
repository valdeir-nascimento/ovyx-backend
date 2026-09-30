package io.github.ovyx.shared.application.spreadsheet;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** O cabeçalho de cada aba (R-012 da 007): o título, as linhas de quem monta e quando foi gerada. */
@DisplayName("SpreadsheetHeading")
class SpreadsheetHeadingTest {

    @Test
    @DisplayName("puts the title first and the time of the farm last")
    void givenTitleAndLines_whenBuildingTheHeading_thenEndWithTheTimeOfTheFarm() {
        // given
        List<String> lines = List.of("Setor: Codornas — Galpão 1", "Período: 01/09/2026 a 28/09/2026");

        // when
        List<String> heading =
                SpreadsheetHeading.of("Ovyx — Relatórios diários", lines, LocalDate.of(2026, 9, 28), LocalTime.of(21, 40));

        // then
        assertThat(heading)
                .containsExactly(
                        "Ovyx — Relatórios diários",
                        "Setor: Codornas — Galpão 1",
                        "Período: 01/09/2026 a 28/09/2026",
                        "Gerada em 28/09/2026 às 21:40 (horário da granja)");
    }

    @Test
    @DisplayName("writes the hour and the minutes with two digits")
    void givenEarlyMorning_whenBuildingTheHeading_thenPadTheHourAndTheMinutes() {
        // given
        LocalTime early = LocalTime.of(6, 5);

        // when
        List<String> heading = SpreadsheetHeading.of("Ovyx — Gaiolas", List.of(), LocalDate.of(2026, 1, 2), early);

        // then
        assertThat(heading).containsExactly("Ovyx — Gaiolas", "Gerada em 02/01/2026 às 06:05 (horário da granja)");
    }
}
