package io.github.ovyx.identity.domain.valueobject;

import io.github.ovyx.identity.domain.IdentityErrorCode;
import io.github.ovyx.shared.domain.Notification;
import io.github.ovyx.shared.domain.Rule;
import java.util.List;
import java.util.Objects;

/**
 * CPF do responsavel, guardado so com digitos.
 *
 * @param value 11 digitos, com digitos verificadores validos
 */
public record Cpf(String value) {

    private static final int LENGTH = 11;

    private static final String FIELD = "cpf";

    /** Caracteres de formatacao aceitos na entrada. Qualquer outro caractere invalida o CPF. */
    private static final String FORMATTING_CHARACTERS = "[.\\-\\s]";

    /**
     * Uma regra so, de proposito: formato, sequencia repetida e digito verificador errados levam a
     * mesma correcao — digitar o CPF de novo —, e tres mensagens iguais so atrapalhariam.
     */
    private static final List<Rule<String>> RULES =
            List.of(Rule.of(Cpf::isValid, IdentityErrorCode.CPF_INVALID, "CPF inválido."));

    /**
     * Construtor canonico: so a garantia estrutural, sem regra de negocio.
     *
     * <p>E o caminho da reidratacao, que le do banco um valor ja validado. Revalidar ali reprovaria
     * registros antigos a cada regra nova; aceitar nulo deixaria um objeto de valor sem valor.
     */
    public Cpf {
        Objects.requireNonNull(value, "value");
    }

    /**
     * Registra no {@link Notification} as violacoes do campo, sem lancar.
     *
     * <p>E o caminho do agregado, que reune as violacoes de todos os campos antes de recusar (FR-017).
     */
    public static void validate(String raw, Notification notification) {
        if (notification.requirePresent(FIELD, raw, IdentityErrorCode.CPF_REQUIRED, "Informe o CPF.")) {
            notification.check(FIELD, digitsOf(raw), RULES);
        }
    }

    /**
     * Cria o CPF, recusando na hora com as violacoes do campo.
     *
     * @throws io.github.ovyx.shared.domain.DomainException quando o CPF e ausente ou invalido
     */
    public static Cpf of(String raw) {
        Notification notification = new Notification();
        validate(raw, notification);
        notification.throwIfAny(IdentityErrorCode.VALIDATION_FAILED);
        return new Cpf(digitsOf(raw));
    }

    private static String digitsOf(String raw) {
        return raw.replaceAll(FORMATTING_CHARACTERS, "");
    }

    private static boolean isValid(String digits) {
        if (digits.length() != LENGTH || !digits.chars().allMatch(Character::isDigit)) {
            return false;
        }
        // Sequencias como 11111111111 passam no calculo dos verificadores, mas nao sao CPF real.
        if (digits.chars().distinct().count() == 1) {
            return false;
        }
        return digits.charAt(9) == checkDigit(digits, 9) && digits.charAt(10) == checkDigit(digits, 10);
    }

    /** Calcula o digito verificador da posicao informada, pelo modulo 11. */
    private static char checkDigit(String digits, int position) {
        int weight = position + 1;
        int sum = 0;
        for (int i = 0; i < position; i++) {
            sum += Character.getNumericValue(digits.charAt(i)) * weight--;
        }
        int remainder = sum % LENGTH;
        int digit = remainder < 2 ? 0 : LENGTH - remainder;
        return Character.forDigit(digit, 10);
    }

    @Override
    public String toString() {
        return value;
    }
}
