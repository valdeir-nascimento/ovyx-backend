package io.github.ovyx.identity.domain.valueobject;

import io.github.ovyx.identity.domain.Notification;

public record Cpf(String value) {

    private static final int LENGTH = 11;

    private static final String FIELD = "cpf";


    /**
     * Caracteres de formatacao aceitos na entrada. Qualquer outro caractere invalida o CPF.
     */
    private static final String FORMATTING_CHARACTERS = "[.\\-\\s]";

    /**
     * Recusa na hora, para quem valida um campo so.
     *
     * @throws io.github.ovyx.shared.domain.DomainException quando o CPF e ausente ou invalido
     */
    public static Cpf of(String raw) {
        Notification notification = new Notification();
        Cpf cpf = of(raw, notification);
        notification.throwIfAny();
        return cpf;
    }

    /**
     * Valida escrevendo no {@link Notification} de quem chamou, em vez de lancar.
     *
     * <p>E o caminho do agregado, que precisa reunir as violacoes de todos os campos antes de
     * recusar uma vez so (FR-017).
     *
     * @return o CPF, ou {@code null} quando alguma regra do campo foi violada
     */
    public static Cpf of(String raw, Notification notification) {
        if (raw == null || raw.isBlank()) {
            notification.add(FIELD, "Informe o CPF.");
            return null;
        }

        String digits = raw.replaceAll(FORMATTING_CHARACTERS, "");

        if (!isValid(digits)) {
            notification.add(FIELD, "CPF inválido.");
            return null;
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
