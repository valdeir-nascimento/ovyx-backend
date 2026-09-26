package io.github.ovyx.production.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * A situação da mortalidade (data-model.md, Situações derivadas): lançada com ocorrência ou com a
 * confirmação do dia sem ocorrência, pendente sem nenhuma das duas.
 */
@DisplayName("MortalityStatus")
class MortalityStatusTest {

    @ParameterizedTest(name = "confirmed {0} and {1} removed: {2}")
    @CsvSource({"true, 0, RECORDED", "false, 1, RECORDED", "true, 2, RECORDED", "false, 0, PENDING"})
    @DisplayName("records the mortality with an occurrence or with the confirmation, and keeps it pending without both")
    void givenConfirmationAndRemovals_whenDerivingTheStatus_thenFindTheStatusOfTheTable(
            boolean confirmed, int removedBirds, MortalityStatus expected) {
        // given — valores vindos do @CsvSource

        // when
        MortalityStatus status = MortalityStatus.of(confirmed, removedBirds);

        // then
        assertThat(status).isEqualTo(expected);
    }
}
