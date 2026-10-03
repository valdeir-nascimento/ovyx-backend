package io.github.ovyx.identity.application.account;

import static io.github.ovyx.identity.domain.model.CaretakerTestDataBuilder.aCaretaker;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.ovyx.identity.domain.IdentityErrorCode;
import io.github.ovyx.identity.domain.model.Caretaker;
import io.github.ovyx.identity.domain.model.CaretakerId;
import io.github.ovyx.identity.domain.model.ThemePreference;
import io.github.ovyx.identity.fixtures.InMemoryCaretakerRepository;
import io.github.ovyx.shared.application.ErrorType;
import io.github.ovyx.shared.application.Result;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** A troca do próprio tema (US2 da 011): só o responsável da sessão, e só o tema dele. */
@DisplayName("ChangeOwnThemeCommandHandler")
class ChangeOwnThemeCommandHandlerTest {

    private final InMemoryCaretakerRepository repository = new InMemoryCaretakerRepository();
    private final ChangeOwnThemeCommandHandler handler = new ChangeOwnThemeCommandHandler(repository);

    private Caretaker saved(Caretaker caretaker) {
        repository.save(caretaker);
        return caretaker;
    }

    @Test
    @DisplayName("saves the theme of the caretaker of the command, and no one else's")
    void givenTwoCaretakers_whenOneChoosesTheDarkTheme_thenSaveItOnlyForHer() {
        // given
        Caretaker maria = saved(aCaretaker().build());
        Caretaker joao = saved(aCaretaker().build());

        // when
        Result<CaretakerId> result = handler.handle(new ChangeOwnThemeCommand(maria.id(), "DARK"));

        // then
        assertThat(result.value()).isEqualTo(maria.id());
        assertThat(repository.findById(maria.id()).orElseThrow().themePreference()).isEqualTo(ThemePreference.DARK);
        assertThat(repository.findById(joao.id()).orElseThrow().themePreference()).isEqualTo(ThemePreference.SYSTEM);
    }

    @Test
    @DisplayName("fails as validation with a value that is not a theme, in its field")
    void givenValueThatIsNotATheme_whenChoosing_thenFailAsValidationInTheThemeField() {
        // given
        Caretaker maria = saved(aCaretaker().build());

        // when
        Result<CaretakerId> result = handler.handle(new ChangeOwnThemeCommand(maria.id(), "AZUL"));

        // then
        assertThat(result.error().type()).isEqualTo(ErrorType.VALIDATION);
        assertThat(result.error().code()).isEqualTo("VALIDATION_FAILED");
        assertThat(result.error().details())
                .containsOnly(Map.entry("theme", "Escolha o tema claro, o escuro ou o igual ao sistema."));
        assertThat(maria.themePreference()).isEqualTo(ThemePreference.SYSTEM);
    }

    @Test
    @DisplayName("fails as unavailable for a caretaker that no longer exists or is inactive, as the password change")
    void givenMissingOrInactiveCaretaker_whenChoosing_thenFailAsUnavailable() {
        // given
        Caretaker inactive = saved(aCaretaker().buildInactive());

        // when
        Result<CaretakerId> missing = handler.handle(new ChangeOwnThemeCommand(CaretakerId.generate(), "DARK"));
        Result<CaretakerId> ofInactive = handler.handle(new ChangeOwnThemeCommand(inactive.id(), "DARK"));

        // then
        assertThat(missing.error().type()).isEqualTo(ErrorType.UNAUTHENTICATED);
        assertThat(missing.error().code()).isEqualTo(IdentityErrorCode.CARETAKER_UNAVAILABLE.code());
        assertThat(ofInactive.error().type()).isEqualTo(ErrorType.UNAUTHENTICATED);
        assertThat(ofInactive.error().code()).isEqualTo(IdentityErrorCode.CARETAKER_UNAVAILABLE.code());
    }
}
