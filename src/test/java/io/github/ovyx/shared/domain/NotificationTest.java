package io.github.ovyx.shared.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.assertj.core.api.Assertions.entry;
import static org.assertj.core.api.Assertions.tuple;

import java.util.List;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Testes do Notification do kernel (FR-017).
 *
 * <p>O Notification existe para que a pessoa veja todos os campos errados de uma vez; se recusasse no
 * primeiro, o requisito estaria quebrado e nenhum outro teste perceberia. Cada violacao carrega o
 * codigo da regra, para que quem verifica a recusa nao dependa do texto da mensagem (principio III).
 */
@DisplayName("Notification")
class NotificationTest {

    private enum SampleCode implements ErrorCode {
        VALIDATION_FAILED,
        TOO_SHORT,
        WITHOUT_DIGIT;

        @Override
        public String code() {
            return name();
        }
    }

    private static final List<Rule<String>> PASSWORD_RULES = List.of(
            Rule.of(password -> password.length() >= 12, SampleCode.TOO_SHORT, "A senha deve ter ao menos 12 caracteres."),
            Rule.of(
                    password -> password.chars().anyMatch(Character::isDigit),
                    SampleCode.WITHOUT_DIGIT,
                    "A senha deve conter ao menos um dígito."));

    @Test
    @DisplayName("keeps quiet while nothing was violated")
    void givenNoViolation_whenRefusingIfAny_thenDoNotThrow() {
        // given
        Notification notification = new Notification();

        // when
        ThrowingCallable refusal = () -> notification.throwIfAny(SampleCode.VALIDATION_FAILED);

        // then
        assertThatCode(refusal).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("records nothing when every rule is satisfied")
    void givenValueSatisfyingEveryRule_whenChecking_thenRecordNothing() {
        // given
        Notification notification = new Notification();

        // when
        notification.check("password", "GranjaNorte2026", PASSWORD_RULES);

        // then
        assertThat(notification.hasErrors()).isFalse();
    }

    @Test
    @DisplayName("records one violation per broken rule, each with its own code")
    void givenValueBreakingTwoRules_whenChecking_thenRecordOneViolationPerRuleWithItsCode() {
        // given
        Notification notification = new Notification();

        // when
        notification.check("password", "abc", PASSWORD_RULES);

        // then
        assertThat(notification.violations())
                .extracting(Violation::field, Violation::code)
                .containsExactly(tuple("password", SampleCode.TOO_SHORT), tuple("password", SampleCode.WITHOUT_DIGIT));
    }

    @Test
    @DisplayName("records a missing value and tells the caller to skip the remaining rules")
    void givenBlankValue_whenRequiringPresence_thenRecordTheViolationAndReportAbsence() {
        // given
        // Sem valor, as demais regras nao fazem sentido: "curta demais" e "sem digito" sobre um campo
        // vazio so repetiriam, com outras palavras, que ele esta vazio.
        Notification notification = new Notification();

        // when
        boolean present = notification.requirePresent("password", "   ", SampleCode.TOO_SHORT, "Informe a senha.");

        // then
        assertThat(present).isFalse();
        assertThat(notification.violations())
                .extracting(Violation::field, Violation::code)
                .containsExactly(tuple("password", SampleCode.TOO_SHORT));
    }

    @Test
    @DisplayName("records nothing when the value is present")
    void givenFilledValue_whenRequiringPresence_thenRecordNothingAndReportPresence() {
        // given
        Notification notification = new Notification();

        // when
        boolean present =
                notification.requirePresent("password", "GranjaNorte2026", SampleCode.TOO_SHORT, "Informe a senha.");

        // then
        assertThat(present).isTrue();
        assertThat(notification.hasErrors()).isFalse();
    }

    @Test
    @DisplayName("refuses once, carrying every violation and the code of the refusal")
    void givenViolationsInTwoFields_whenRefusingIfAny_thenCarryEveryViolationUnderTheGivenCode() {
        // given
        Notification notification = new Notification();
        notification.add("email", SampleCode.TOO_SHORT, "Informe o e-mail.");
        notification.add("cpf", SampleCode.WITHOUT_DIGIT, "CPF inválido.");

        // when
        DomainException refusal = catchThrowableOfType(
                DomainException.class, () -> notification.throwIfAny(SampleCode.VALIDATION_FAILED));

        // then
        assertThat(refusal.errorCode()).isEqualTo(SampleCode.VALIDATION_FAILED);
        assertThat(refusal.violations()).extracting(Violation::field).containsExactly("email", "cpf");
    }

    @Test
    @DisplayName("keeps the field order of the violations in the details the API publishes")
    void givenViolationsInTwoFields_whenRefusingIfAny_thenKeepTheirOrderInTheDetails() {
        // given
        // O corpo HTTP expoe details campo -> mensagem. A ordem e a em que os campos foram recusados,
        // que e a ordem do formulario; um mapa sem ordem embaralhava a lista para quem a le.
        Notification notification = new Notification();
        notification.add("fullName", SampleCode.TOO_SHORT, "Informe o nome completo.");
        notification.add("cpf", SampleCode.WITHOUT_DIGIT, "CPF inválido.");
        notification.add("email", SampleCode.TOO_SHORT, "Informe o e-mail.");

        // when
        DomainException refusal = catchThrowableOfType(
                DomainException.class, () -> notification.throwIfAny(SampleCode.VALIDATION_FAILED));

        // then
        assertThat(refusal.details())
                .containsExactly(
                        entry("fullName", "Informe o nome completo."),
                        entry("cpf", "CPF inválido."),
                        entry("email", "Informe o e-mail."));
    }

    @Test
    @DisplayName("joins two messages of the same field in the details instead of dropping one")
    void givenTwoViolationsOfTheSameField_whenRefusingIfAny_thenJoinBothMessagesInTheDetails() {
        // given
        Notification notification = new Notification();
        notification.check("password", "abc", PASSWORD_RULES);

        // when
        DomainException refusal = catchThrowableOfType(
                DomainException.class, () -> notification.throwIfAny(SampleCode.VALIDATION_FAILED));

        // then
        assertThat(refusal.details())
                .containsEntry(
                        "password",
                        "A senha deve ter ao menos 12 caracteres. A senha deve conter ao menos um dígito.");
    }

    @Test
    @DisplayName("does not repeat a message that two rules of the same field share")
    void givenTwoViolationsWithTheSameMessage_whenRefusingIfAny_thenWriteTheMessageOnce() {
        // given
        Notification notification = new Notification();
        notification.add("cpf", SampleCode.TOO_SHORT, "CPF inválido.");
        notification.add("cpf", SampleCode.WITHOUT_DIGIT, "CPF inválido.");

        // when
        DomainException refusal = catchThrowableOfType(
                DomainException.class, () -> notification.throwIfAny(SampleCode.VALIDATION_FAILED));

        // then
        assertThat(refusal.details()).containsEntry("cpf", "CPF inválido.");
        assertThat(refusal.violations()).hasSize(2);
    }
}
