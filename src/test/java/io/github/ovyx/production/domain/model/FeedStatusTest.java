package io.github.ovyx.production.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** A situação da ração: completa sem gaiola pendente, pendente com qualquer uma (R-008 da 004). */
@DisplayName("FeedStatus")
class FeedStatusTest {

    @ParameterizedTest(name = "{0} pending cages is {1}")
    @CsvSource({"0, COMPLETE", "1, PENDING", "48, PENDING"})
    @DisplayName("is complete only without pending cages")
    void givenPendingCages_whenDerivingTheStatus_thenCompleteOnlyWithoutPendingCages(int pending, FeedStatus expected) {
        // given
        int pendingCages = pending;

        // when
        FeedStatus status = FeedStatus.of(pendingCages);

        // then
        assertThat(status).isEqualTo(expected);
    }
}
