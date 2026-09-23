package io.github.ovyx.identity.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Testes da politica minima de senha (FR-022).
 *
 * <p>A politica acumula: quem digita uma senha ruim precisa ver tudo o que falta de uma vez, e nao
 * descobrir uma regra por submissao.
 */
@DisplayName("PasswordPolicy")
class PasswordPolicyTest {

    private static final String FIELD = "password";

    private static String messageOf(String rawPassword, String field, String email, String cpf) {
        Notification notification = new Notification();
        PasswordPolicy.validate(rawPassword, field, email, cpf, notification);
        return notification.errors().get(field);
    }

    private static String messageOf(String rawPassword) {
        return messageOf(rawPassword, FIELD, null, null);
    }

    @Test
    @DisplayName("accepts a password that meets the policy")
    void acceptsCompliantPassword() {
        Notification notification = new Notification();

        PasswordPolicy.validate("GranjaNorte2026", FIELD, "maria.silva@ovyx.com.br", "52998224725", notification);

        assertThat(notification.hasErrors()).isFalse();
    }

    @Test
    @DisplayName("rejects a missing password")
    void rejectsMissingPassword() {
        assertThat(messageOf(null)).isEqualTo("Informe a senha.");
    }

    @Test
    @DisplayName("rejects a password shorter than 12 characters")
    void rejectsTooShortPassword() {
        assertThat(messageOf("Granja2026")).contains("ao menos 12 caracteres");
    }

    @Test
    @DisplayName("rejects a password without a letter")
    void rejectsPasswordWithoutLetter() {
        assertThat(messageOf("123456789012")).contains("ao menos uma letra");
    }

    @Test
    @DisplayName("rejects a password without a digit")
    void rejectsPasswordWithoutDigit() {
        assertThat(messageOf("GranjaDoNorte")).contains("ao menos um dígito");
    }

    @Test
    @DisplayName("rejects a password equal to the email or the CPF")
    void rejectsPasswordEqualToIdentifiers() {
        assertThat(messageOf("maria.silva@ovyx.com.br", FIELD, "maria.silva@ovyx.com.br", null))
                .contains("igual ao e-mail nem ao CPF");
        assertThat(messageOf("52998224725", FIELD, null, "52998224725")).contains("igual ao e-mail nem ao CPF");
    }

    @Test
    @DisplayName("returns ALL violations at once, not just the first")
    void reportsEveryViolationAtOnce() {
        // "abc" e curta demais E nao tem digito.
        assertThat(messageOf("abc")).contains("ao menos 12 caracteres").contains("ao menos um dígito");
    }

    @Test
    @DisplayName("uses the given field name to serve registration and password change")
    void usesTheProvidedFieldName() {
        Notification notification = new Notification();

        PasswordPolicy.validate("abc", "newPassword", null, null, notification);

        assertThat(notification.errors()).containsOnlyKeys("newPassword");
    }

    @Test
    @DisplayName("rejects a password longer than 128 characters without echoing it")
    void rejectsTooLongPassword() {
        // O limite fica aqui, e nao como anotacao no corpo HTTP: na borda, a senha recusada era
        // registrada como "valor rejeitado" pelo log de validacao do Spring (FR-021).
        String message = messageOf("Aa1" + "x".repeat(126));

        assertThat(message).contains("no máximo 128 caracteres").doesNotContain("xxxx");
    }

    @Test
    @DisplayName("accepts exactly 128 characters")
    void acceptsExactly128Characters() {
        Notification notification = new Notification();

        PasswordPolicy.validate("Aa1" + "x".repeat(125), FIELD, null, null, notification);

        assertThat(notification.hasErrors()).isFalse();
    }
}
