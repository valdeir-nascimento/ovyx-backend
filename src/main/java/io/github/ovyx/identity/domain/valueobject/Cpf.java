package io.github.ovyx.identity.domain.valueobject;

import io.github.ovyx.identity.domain.Violations;

public record Cpf(String value) {

    private static final int LENGTH = 11;

    private static final String FIELD = "cpf";


    /**
     * Caracteres de formatacao aceitos na entrada. Qualquer outro caractere invalida o CPF.
     */
    private static final String FORMATTING_CHARACTERS = "[.\\-\\s]";

    /**
     * @throws io.github.ovyx.shared.domain.DomainException quando o CPF e ausente ou invalido
     */
    public static Cpf of(String raw) {
        if (raw == null || raw.isBlank()) {
            throw Violations.of(FIELD, "Informe o CPF.");
        }

        String digits = raw.replaceAll(FORMATTING_CHARACTERS, "");

        if (!isValid(digits)) {
            throw Violations.of(FIELD, "CPF inválido.");
        }

        return new Cpf(digits);
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

    /**
     * Calcula o digito verificador da posicao informada, pelo modulo 11.
     */
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
