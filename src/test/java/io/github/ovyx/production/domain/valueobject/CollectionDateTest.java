package io.github.ovyx.production.domain.valueobject;

import static io.github.ovyx.shared.domain.DomainViolations.detailsOf;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Data da coleta: obrigatória, válida e não posterior a hoje no fuso da granja (FR-001, R-006). "Hoje"
 * chega calculado: o domínio não lê relógio.
 */
@DisplayName("CollectionDate")
class CollectionDateTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 9, 25);

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "  "})
    @DisplayName("rejects a missing date")
    void givenMissingDate_whenCreating_thenRejectAsRequired(String raw) {
        // given — valor bruto vindo do @NullSource e do @ValueSource

        // when
        Map<String, String> details = detailsOf(() -> CollectionDate.of(raw, TODAY));

        // then
        assertThat(details).containsExactly(Map.entry("collectionDate", "Informe a data da coleta."));
    }

    @ParameterizedTest(name = "\"{0}\"")
    @ValueSource(strings = {"24/09/2026", "2026-02-30", "ontem", "2026-9-24"})
    @DisplayName("rejects a date that is not a valid date in the ISO format")
    void givenInvalidDate_whenCreating_thenRejectAsInvalid(String raw) {
        // given — valor bruto vindo do @ValueSource

        // when
        Map<String, String> details = detailsOf(() -> CollectionDate.of(raw, TODAY));

        // then
        assertThat(details)
                .containsExactly(Map.entry("collectionDate", "Informe a data da coleta no formato AAAA-MM-DD."));
    }

    @Test
    @DisplayName("rejects a date after today at the farm")
    void givenTomorrow_whenCreating_thenRejectAsFuture() {
        // given
        String tomorrow = "2026-09-26";

        // when
        Map<String, String> details = detailsOf(() -> CollectionDate.of(tomorrow, TODAY));

        // then
        assertThat(details).containsExactly(Map.entry("collectionDate", "A data da coleta não pode ser futura."));
    }

    @ParameterizedTest(name = "\"{0}\"")
    @ValueSource(strings = {"2026-09-25", "2025-01-01", " 2026-09-24 "})
    @DisplayName("accepts today and any past date")
    void givenTodayOrPastDate_whenCreating_thenAcceptIt(String raw) {
        // given — valor bruto vindo do @ValueSource

        // when
        CollectionDate date = CollectionDate.of(raw, TODAY);

        // then
        assertThat(date.value()).isEqualTo(LocalDate.parse(raw.strip()));
    }
}
