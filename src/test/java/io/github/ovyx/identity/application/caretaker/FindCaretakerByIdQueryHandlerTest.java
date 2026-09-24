package io.github.ovyx.identity.application.caretaker;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.ovyx.identity.domain.IdentityErrorCode;
import io.github.ovyx.identity.domain.model.CaretakerId;
import io.github.ovyx.identity.domain.model.CaretakerStatus;
import io.github.ovyx.identity.domain.model.Role;
import io.github.ovyx.shared.application.ErrorType;
import io.github.ovyx.shared.application.Result;
import java.lang.reflect.RecordComponent;
import java.time.Instant;
import java.util.Arrays;
import java.util.Locale;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Testes da consulta de um responsavel pelo administrador (FR-014).
 */
@DisplayName("FindCaretakerByIdQueryHandler")
class FindCaretakerByIdQueryHandlerTest {

    private static final CaretakerDetail JOAO = new CaretakerDetail(
            CaretakerId.generate(),
            "João Pereira de Souza",
            "52998224725",
            "joao.pereira@ovyx.com.br",
            "91991234567",
            Role.USER,
            CaretakerStatus.ACTIVE,
            Instant.parse("2026-09-18T13:45:10Z"),
            Instant.parse("2026-09-18T13:45:10Z"));

    private final RecordingCaretakerDirectory directory = new RecordingCaretakerDirectory().holding(JOAO);
    private final FindCaretakerByIdQueryHandler handler = new FindCaretakerByIdQueryHandler(directory);

    @Test
    @DisplayName("returns the detail of an existing caretaker")
    void givenExistingCaretaker_whenFinding_thenReturnItsDetail() {
        // given
        FindCaretakerByIdQuery query = new FindCaretakerByIdQuery(JOAO.id());

        // when
        Result<CaretakerDetail> result = handler.handle(query);

        // then
        assertThat(result.value()).isEqualTo(JOAO);
    }

    @Test
    @DisplayName("fails as not found for an unknown caretaker")
    void givenUnknownCaretaker_whenFinding_thenFailAsNotFound() {
        // given
        FindCaretakerByIdQuery query = new FindCaretakerByIdQuery(CaretakerId.generate());

        // when
        Result<CaretakerDetail> result = handler.handle(query);

        // then
        assertThat(result.error().type()).isEqualTo(ErrorType.NOT_FOUND);
        assertThat(result.error().code()).isEqualTo(IdentityErrorCode.CARETAKER_NOT_FOUND.code());
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(classes = {CaretakerDetail.class, CaretakerSummary.class})
    @DisplayName("no read model of caretakers has a place for the password or its hash")
    void givenCaretakerReadModel_whenListingItsComponents_thenFindNothingAboutThePassword(Class<?> readModel) {
        // given — a classe do modelo de leitura, vinda do @ValueSource

        // when
        RecordComponent[] components = readModel.getRecordComponents();

        // then
        assertThat(Arrays.stream(components).map(component -> component.getName().toLowerCase(Locale.ROOT)))
                .isNotEmpty()
                .noneMatch(name -> name.contains("password") || name.contains("hash"));
    }
}
