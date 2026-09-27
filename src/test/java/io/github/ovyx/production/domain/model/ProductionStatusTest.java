package io.github.ovyx.production.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** A situação da produção (data-model.md, Situações derivadas): completa só sem gaiola pendente. */
@DisplayName("ProductionStatus")
class ProductionStatusTest {

    @ParameterizedTest(name = "{0} pending: {1}")
    @CsvSource({"0, COMPLETE", "1, PENDING", "300, PENDING"})
    @DisplayName("completes the production only when no cage is pending")
    void givenPendingCages_whenDerivingTheStatus_thenCompleteOnlyWithNone(int pendingCages, ProductionStatus expected) {
        // given — valores vindos do @CsvSource

        // when
        ProductionStatus status = ProductionStatus.of(pendingCages);

        // then
        assertThat(status).isEqualTo(expected);
    }
}
