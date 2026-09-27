package io.github.ovyx.production.domain.valueobject;

import static io.github.ovyx.shared.domain.DomainViolations.detailsOf;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalTime;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

/** Hora da coleta: obrigatória, em horas e minutos (FR-001, R-006). */
@DisplayName("CollectionTime")
class CollectionTimeTest {

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "  "})
    @DisplayName("rejects a missing time")
    void givenMissingTime_whenCreating_thenRejectAsRequired(String raw) {
        // given — valor bruto vindo do @NullSource e do @ValueSource

        // when
        Map<String, String> details = detailsOf(() -> CollectionTime.of(raw));

        // then
        assertThat(details).containsExactly(Map.entry("collectionTime", "Informe a hora da coleta."));
    }

    @ParameterizedTest(name = "\"{0}\"")
    @ValueSource(strings = {"25:00", "6h30", "06:30:15", "6:30", "06:60"})
    @DisplayName("rejects a time that is not in the HH:mm format")
    void givenInvalidTime_whenCreating_thenRejectAsInvalid(String raw) {
        // given — valor bruto vindo do @ValueSource

        // when
        Map<String, String> details = detailsOf(() -> CollectionTime.of(raw));

        // then
        assertThat(details)
                .containsExactly(Map.entry("collectionTime", "Informe a hora da coleta no formato HH:mm."));
    }

    @ParameterizedTest(name = "\"{0}\"")
    @ValueSource(strings = {"06:30", "00:00", "23:59"})
    @DisplayName("accepts a time in the HH:mm format")
    void givenValidTime_whenCreating_thenAcceptIt(String raw) {
        // given — valor bruto vindo do @ValueSource

        // when
        CollectionTime time = CollectionTime.of(raw);

        // then
        assertThat(time.value()).isEqualTo(LocalTime.parse(raw));
    }
}
