package io.github.ovyx.identity.domain;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.ovyx.shared.domain.ErrorCode;
import io.github.ovyx.shared.domain.Notification;
import io.github.ovyx.shared.domain.Violation;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Testes da politica minima de senha (FR-022).
 *
 * <p>A politica acumula: quem digita uma senha ruim precisa ver tudo o que falta de uma vez, e nao
 * descobrir uma regra por submissao. Cada regra tem o seu codigo, e e ele que os testes verificam.
 */
@DisplayName("PasswordPolicy")
class PasswordPolicyTest {

    private static final String FIELD = "password";

    private static List<ErrorCode> codesOf(String rawPassword, String email, String cpf) {
        Notification notification = new Notification();
        PasswordPolicy.validate(rawPassword, FIELD, email, cpf, notification);
        return notification.violations().stream().map(Violation::code).toList();
    }

    private static List<ErrorCode> codesOf(String rawPassword) {
        return codesOf(rawPassword, null, null);
    }

    @Test
    @DisplayName("accepts a password that meets the policy")
    void givenCompliantPassword_whenValidating_thenReportNothing() {
        // given
        String compliant = "GranjaNorte2026";

        // when
        List<ErrorCode> codes = codesOf(compliant, "maria.silva@ovyx.com.br", "52998224725");

        // then
        assertThat(codes).isEmpty();
    }

    @Test
    @DisplayName("rejects a missing password, and nothing else about it")
    void givenNullPassword_whenValidating_thenRejectAsRequiredOnly() {
        // given
        String missing = null;

        // when
        List<ErrorCode> codes = codesOf(missing);

        // then
        assertThat(codes).containsExactly(IdentityErrorCode.PASSWORD_REQUIRED);
    }

    @ParameterizedTest(name = "\"{0}\" breaks {1}")
    @CsvSource({
        "Granja12345, PASSWORD_TOO_SHORT", // 11 caracteres: um a menos que o minimo do FR-022
        "123456789012, PASSWORD_WITHOUT_LETTER",
        "GranjaDoNorte, PASSWORD_WITHOUT_DIGIT"
    })
    @DisplayName("rejects a password that breaks one rule, with that rule's code")
    void givenPasswordBreakingOneRule_whenValidating_thenReportThatRuleOnly(
            String rawPassword, IdentityErrorCode rule) {
        // given — senha e regra violada vindas do @CsvSource

        // when
        List<ErrorCode> codes = codesOf(rawPassword);

        // then
        assertThat(codes).containsExactly(rule);
    }

    @ParameterizedTest(name = "password [{0}], email [{1}], cpf [{2}]")
    @CsvSource({
        "maria.silva@ovyx.com.br, maria.silva@ovyx.com.br, ",
        "52998224725, , 52998224725",
        // Outra grafia do mesmo identificador continua sendo o identificador.
        "'MARIA.SILVA@OVYX.COM.BR', '  maria.silva@ovyx.com.br ', ",
        "'52998224725', , '  52998224725 '"
    })
    @DisplayName("rejects a password equal to the email or the CPF")
    void givenPasswordEqualToAnIdentifier_whenValidating_thenRejectIt(String rawPassword, String email, String cpf) {
        // given — senha, e-mail e CPF vindos do @CsvSource

        // when
        List<ErrorCode> codes = codesOf(rawPassword, email, cpf);

        // then
        assertThat(codes).contains(IdentityErrorCode.PASSWORD_EQUALS_IDENTIFIER);
    }

    @Test
    @DisplayName("returns ALL violations at once, not just the first")
    void givenShortPasswordWithoutDigit_whenValidating_thenReportBothRules() {
        // given
        // "abc" e curta demais E nao tem digito.
        String shortWithoutDigit = "abc";

        // when
        List<ErrorCode> codes = codesOf(shortWithoutDigit);

        // then
        assertThat(codes)
                .containsExactly(IdentityErrorCode.PASSWORD_TOO_SHORT, IdentityErrorCode.PASSWORD_WITHOUT_DIGIT);
    }

    @Test
    @DisplayName("uses the given field name to serve registration and password change")
    void givenNewPasswordField_whenValidating_thenReportUnderThatField() {
        // given
        Notification notification = new Notification();

        // when
        PasswordPolicy.validate("abc", "newPassword", null, null, notification);

        // then
        assertThat(notification.violations()).extracting(Violation::field).containsOnly("newPassword");
    }

    @Test
    @DisplayName("rejects a password longer than 128 characters without echoing it")
    void givenPasswordLongerThan128Characters_whenValidating_thenRejectWithoutEchoingIt() {
        // given
        // O limite fica aqui, e nao como anotacao no corpo HTTP: na borda, a senha recusada era
        // registrada como "valor rejeitado" pelo log de validacao do Spring (FR-021).
        String tooLong = "Aa1" + "x".repeat(126);
        Notification notification = new Notification();

        // when
        PasswordPolicy.validate(tooLong, FIELD, null, null, notification);

        // then
        assertThat(notification.violations())
                .singleElement()
                .satisfies(violation -> {
                    assertThat(violation.code()).isEqualTo(IdentityErrorCode.PASSWORD_TOO_LONG);
                    assertThat(violation.message()).doesNotContain("xxxx");
                });
    }

    private static Stream<Arguments> passwordsOnTheLengthBoundaries() {
        return Stream.of(Arguments.of(12, "Granja123456"), Arguments.of(128, "Aa1" + "x".repeat(125)));
    }

    @ParameterizedTest(name = "accepts {0} characters")
    @MethodSource("passwordsOnTheLengthBoundaries")
    @DisplayName("accepts exactly 12 and exactly 128 characters")
    void givenPasswordAtALengthBoundary_whenValidating_thenReportNothing(int length, String atTheBoundary) {
        // given
        // 12 e o minimo e 128 o maximo do FR-022; um limite deslocado de um so caractere reprova aqui.
        assertThat(atTheBoundary).as("precondition: the candidate sits on the boundary").hasSize(length);

        // when
        List<ErrorCode> codes = codesOf(atTheBoundary);

        // then
        assertThat(codes).isEmpty();
    }
}
