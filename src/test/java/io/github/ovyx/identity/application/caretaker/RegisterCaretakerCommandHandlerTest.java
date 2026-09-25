package io.github.ovyx.identity.application.caretaker;

import static io.github.ovyx.identity.domain.model.CaretakerTestDataBuilder.aCaretaker;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.ovyx.identity.fixtures.InMemoryCaretakerRepository;
import io.github.ovyx.identity.domain.FakePasswordHasher;
import io.github.ovyx.identity.domain.IdentityErrorCode;
import io.github.ovyx.identity.domain.model.Caretaker;
import io.github.ovyx.identity.domain.model.CaretakerId;
import io.github.ovyx.identity.domain.model.Role;
import io.github.ovyx.identity.domain.port.PasswordHasher;
import io.github.ovyx.shared.application.ErrorType;
import io.github.ovyx.shared.application.Result;
import io.github.ovyx.shared.domain.FixedClock;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Testes do cadastro de responsavel pelo administrador (FR-013, cenarios 1 a 4 da Historia 2).
 *
 * <p>As regras em si sao do agregado e estao testadas nele; aqui se verifica o que e do caso de
 * uso: o que e gravado, o que volta no {@code Result} e com qual natureza de falha.
 */
@DisplayName("RegisterCaretakerCommandHandler")
class RegisterCaretakerCommandHandlerTest {

    private static final String FULL_NAME = "João Pereira de Souza";
    private static final String CPF = "11144477735";
    private static final String EMAIL = "joao.pereira@ovyx.com.br";
    private static final String MOBILE_PHONE = "91991234567";
    private static final String PASSWORD = "AviarioSul2026";

    private static final String MARIA_CPF = "52998224725";
    private static final String MARIA_EMAIL = "maria.silva@ovyx.com.br";
    private static final String MARIA_MOBILE_PHONE = "91988887777";

    private final PasswordHasher hasher = new FakePasswordHasher();
    private final FixedClock clock = FixedClock.at("2026-09-19T12:00:00Z");
    private final InMemoryCaretakerRepository repository = new InMemoryCaretakerRepository();
    private final RegisterCaretakerCommandHandler handler =
            new RegisterCaretakerCommandHandler(repository, hasher, clock);

    private static RegisterCaretakerCommand joao() {
        return new RegisterCaretakerCommand(FULL_NAME, CPF, EMAIL, MOBILE_PHONE, PASSWORD, Role.USER);
    }

    private void registerMaria() {
        repository.save(aCaretaker().withHasher(hasher).withRoster(repository).withClock(clock).build());
    }

    @Test
    @DisplayName("registers the caretaker, who can then sign in with the informed password")
    void givenValidData_whenRegistering_thenSaveACaretakerThatAuthenticates() {
        // given
        RegisterCaretakerCommand joao = joao();

        // when
        Result<CaretakerId> result = handler.handle(joao);

        // then
        Caretaker saved = repository.findById(result.value()).orElseThrow();
        assertThat(saved.email().value()).isEqualTo(EMAIL);
        assertThat(saved.authenticate(PASSWORD, hasher)).isTrue();
    }

    @Test
    @DisplayName("a password set by the administrator is provisional: the caretaker must change it")
    void givenValidData_whenRegistering_thenRequireAPasswordChangeOnFirstAccess() {
        // given
        // Quem cadastra conhece a senha; so a troca pela propria pessoa encerra isso (data-model.md).
        RegisterCaretakerCommand joao = joao();

        // when
        Result<CaretakerId> result = handler.handle(joao);

        // then
        assertThat(repository.findById(result.value()).orElseThrow().mustChangePassword()).isTrue();
    }

    private static Stream<Arguments> oneInvalidField() {
        return Stream.of(
                Arguments.of("fullName", new RegisterCaretakerCommand("", CPF, EMAIL, MOBILE_PHONE, PASSWORD, Role.USER)),
                Arguments.of("cpf", new RegisterCaretakerCommand(FULL_NAME, "12345678900", EMAIL, MOBILE_PHONE, PASSWORD, Role.USER)),
                Arguments.of("email", new RegisterCaretakerCommand(FULL_NAME, CPF, "sem-arroba", MOBILE_PHONE, PASSWORD, Role.USER)),
                Arguments.of("mobilePhone", new RegisterCaretakerCommand(FULL_NAME, CPF, EMAIL, "123", PASSWORD, Role.USER)),
                Arguments.of("password", new RegisterCaretakerCommand(FULL_NAME, CPF, EMAIL, MOBILE_PHONE, "curta1", Role.USER)),
                Arguments.of("role", new RegisterCaretakerCommand(FULL_NAME, CPF, EMAIL, MOBILE_PHONE, PASSWORD, (String) null)));
    }

    @ParameterizedTest(name = "invalid {0}")
    @MethodSource("oneInvalidField")
    @DisplayName("fails as a validation failure naming the invalid field, and saves nothing")
    void givenOneInvalidField_whenRegistering_thenFailAsValidationOnThatFieldAndSaveNothing(
            String field, RegisterCaretakerCommand invalid) {
        // given — o comando com um campo invalido, vindo do @MethodSource

        // when
        Result<CaretakerId> result = handler.handle(invalid);

        // then
        assertThat(result.error().type()).isEqualTo(ErrorType.VALIDATION);
        assertThat(result.error().code()).isEqualTo(IdentityErrorCode.VALIDATION_FAILED.code());
        assertThat(result.error().details()).containsOnlyKeys(field);
        assertThat(repository.size()).isZero();
    }

    private static Stream<Arguments> oneIdentifierHeldByMaria() {
        return Stream.of(
                Arguments.of(
                        "cpf",
                        IdentityErrorCode.CPF_ALREADY_IN_USE,
                        new RegisterCaretakerCommand(FULL_NAME, MARIA_CPF, EMAIL, MOBILE_PHONE, PASSWORD, Role.USER)),
                Arguments.of(
                        "email",
                        IdentityErrorCode.EMAIL_ALREADY_IN_USE,
                        new RegisterCaretakerCommand(FULL_NAME, CPF, MARIA_EMAIL, MOBILE_PHONE, PASSWORD, Role.USER)),
                Arguments.of(
                        "mobilePhone",
                        IdentityErrorCode.MOBILE_PHONE_ALREADY_IN_USE,
                        new RegisterCaretakerCommand(FULL_NAME, CPF, EMAIL, MARIA_MOBILE_PHONE, PASSWORD, Role.USER)));
    }

    @ParameterizedTest(name = "{0} held by Maria -> {1}")
    @MethodSource("oneIdentifierHeldByMaria")
    @DisplayName("fails as a conflict when an identifier already has an owner, and saves nothing")
    void givenIdentifierHeldByMaria_whenRegistering_thenFailAsConflictAndSaveNothing(
            String field, IdentityErrorCode conflict, RegisterCaretakerCommand sameAsMaria) {
        // given
        registerMaria();

        // when
        Result<CaretakerId> result = handler.handle(sameAsMaria);

        // then
        assertThat(result.error().type()).isEqualTo(ErrorType.CONFLICT);
        assertThat(result.error().code()).isEqualTo(conflict.code());
        assertThat(result.error().details()).containsOnlyKeys(field);
        assertThat(repository.size()).as("only Maria is stored").isEqualTo(1);
    }

    @Test
    @DisplayName("never shows the password when the command is printed")
    void givenCommandWithPassword_whenPrinting_thenHideThePassword() {
        // given
        RegisterCaretakerCommand joao = joao();

        // when
        String printed = joao.toString();

        // then
        assertThat(printed).doesNotContain(PASSWORD).contains("password=****");
    }
}
