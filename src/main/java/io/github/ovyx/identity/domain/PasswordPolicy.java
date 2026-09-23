package io.github.ovyx.identity.domain;



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

    private PasswordPolicy() {}

    /**
     * Valida a senha, acumulando cada violacao encontrada.
     *
     * @param rawPassword senha em texto claro
     * @param field nome do campo a associar as violacoes; e {@code password} no cadastro e
     *     {@code newPassword} na troca de senha, o que permite a mesma regra servir os dois casos
     * @param email e-mail do responsavel, para recusar senha igual ao identificador; pode ser nulo
     * @param cpf CPF do responsavel, pelo mesmo motivo; pode ser nulo
     * @param notification acumulador das violacoes
     */
    public static void validate(String rawPassword, String field, String email, String cpf, Notification notification) {

        if (rawPassword == null || rawPassword.isBlank()) {
            notification.add(field, "Informe a senha.");
            return;
        }

        if (rawPassword.length() < MINIMUM_LENGTH) {
            notification.add(field, "A senha deve ter ao menos 12 caracteres.");
        } else if (rawPassword.length() > MAXIMUM_LENGTH) {
            // Limite maximo aqui, e nao como anotacao no corpo HTTP: na borda, a senha recusada
            // era registrada como "valor rejeitado" pelo log de validacao do Spring (FR-021).
            notification.add(field, "A senha deve ter no máximo 128 caracteres.");
        }
        if (rawPassword.chars().noneMatch(Character::isLetter)) {
            notification.add(field, "A senha deve conter ao menos uma letra.");
        }
        if (rawPassword.chars().noneMatch(Character::isDigit)) {
            notification.add(field, "A senha deve conter ao menos um dígito.");
        }
        if (isSameAs(rawPassword, email) || isSameAs(rawPassword, cpf)) {
            notification.add(field, "A senha não pode ser igual ao e-mail nem ao CPF.");
        }
    }

    private static boolean isSameAs(String rawPassword, String identifier) {
        return identifier != null && !identifier.isBlank() && rawPassword.equalsIgnoreCase(identifier.trim());
    }
}
