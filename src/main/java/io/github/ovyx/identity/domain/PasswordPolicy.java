package io.github.ovyx.identity.domain;

import io.github.ovyx.shared.domain.Notification;
import io.github.ovyx.shared.domain.Rule;
import java.util.List;
import java.util.stream.Stream;

/**
 * Politica minima de senha (FR-022).
 *
 * <p>Opera sobre a senha em texto claro, antes do hash — e o unico momento em que o dominio a ve.
 *
 * <p>Cada regra violada acrescenta a sua propria mensagem, dizendo o que falta. Uma mensagem unica
 * de "senha inválida" obrigaria a pessoa a descobrir por tentativa e erro qual das regras nao foi
 * atendida.
 *
 * <p>A politica e deliberadamente simples: comprimento e o fator que mais pesa contra ataque de
 * forca bruta, enquanto exigencias decorativas de simbolo empurram o usuario para senhas piores e
 * anotadas em papel.
 */
public final class PasswordPolicy {

    public static final int MINIMUM_LENGTH = 12;
    public static final int MAXIMUM_LENGTH = 128;

    /** As regras que valem para qualquer senha, independentemente de quem a escolhe. */
    private static final List<Rule<String>> RULES = List.of(
            Rule.of(
                    password -> password.length() >= MINIMUM_LENGTH,
                    IdentityErrorCode.PASSWORD_TOO_SHORT,
                    "A senha deve ter ao menos 12 caracteres."),
            // Limite maximo aqui, e nao como anotacao no corpo HTTP: na borda, a senha recusada era
            // registrada como "valor rejeitado" pelo log de validacao do Spring (FR-021).
            Rule.of(
                    password -> password.length() <= MAXIMUM_LENGTH,
                    IdentityErrorCode.PASSWORD_TOO_LONG,
                    "A senha deve ter no máximo 128 caracteres."),
            Rule.of(
                    password -> password.chars().anyMatch(Character::isLetter),
                    IdentityErrorCode.PASSWORD_WITHOUT_LETTER,
                    "A senha deve conter ao menos uma letra."),
            Rule.of(
                    password -> password.chars().anyMatch(Character::isDigit),
                    IdentityErrorCode.PASSWORD_WITHOUT_DIGIT,
                    "A senha deve conter ao menos um dígito."));

    private PasswordPolicy() {}

    /**
     * Valida a senha, registrando no {@link Notification} cada regra violada.
     *
     * @param rawPassword senha em texto claro
     * @param field nome do campo a associar as violacoes; e {@code password} no cadastro e
     *     {@code newPassword} na troca de senha, o que permite a mesma regra servir os dois casos
     * @param email e-mail do responsavel, para recusar senha igual ao identificador; pode ser nulo
     * @param cpf CPF do responsavel, pelo mesmo motivo; pode ser nulo
     * @param notification acumulador das violacoes
     */
    public static void validate(String rawPassword, String field, String email, String cpf, Notification notification) {
        if (notification.requirePresent(field, rawPassword, IdentityErrorCode.PASSWORD_REQUIRED, "Informe a senha.")) {
            notification.check(field, rawPassword, rulesFor(email, cpf));
        }
    }

    /**
     * As regras gerais, mais a que depende de quem escolhe a senha: ela nao pode repetir o proprio
     * identificador.
     */
    private static List<Rule<String>> rulesFor(String email, String cpf) {
        Rule<String> notAnIdentifier = Rule.of(
                password -> !isSameAs(password, email) && !isSameAs(password, cpf),
                IdentityErrorCode.PASSWORD_EQUALS_IDENTIFIER,
                "A senha não pode ser igual ao e-mail nem ao CPF.");
        return Stream.concat(RULES.stream(), Stream.of(notAnIdentifier)).toList();
    }

    private static boolean isSameAs(String rawPassword, String identifier) {
        return identifier != null && !identifier.isBlank() && rawPassword.equalsIgnoreCase(identifier.trim());
    }
}
