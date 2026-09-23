package io.github.ovyx.identity.domain.valueobject;

import io.github.ovyx.identity.domain.Notification;

/**
 * Celular do responsavel, o segundo identificador de acesso.
 *
 * <p>Guardado apenas com digitos, incluindo o DDD. Aceita 10 ou 11 digitos, porque a base herdada
 * do legado tem numeros dos dois formatos.
 *
 * @param value 10 ou 11 digitos, sem formatacao
 */
public record MobilePhone(String value) {

    private static final String FIELD = "mobilePhone";


    private static final int MINIMUM_DIGITS = 10;
    private static final int MAXIMUM_DIGITS = 11;
    private static final int MINIMUM_AREA_CODE = 11;
    private static final int MAXIMUM_AREA_CODE = 99;

    /**
     * Caracteres de formatacao aceitos. Letra nao entra nesta lista de proposito: apagar letras em
     * silencio transformaria {@code 9198888777A} em um numero valido de 10 digitos, aceitando um
     * dado que a pessoa digitou errado.
     */
    // Visivel no pacote porque AccessIdentifier aplica a mesma regra ao identificador de acesso:
    // duas copias da regra divergiriam, e a divergencia vira brecha na contencao de tentativas.
    static final String FORMATTING_CHARACTERS = "[()\\-.\\s]";

    /**
     * @throws io.github.ovyx.shared.domain.DomainException quando o celular e ausente ou invalido
     */
    public static MobilePhone of(String raw) {
        Notification notification = new Notification();
        MobilePhone mobilePhone = of(raw, notification);
        notification.throwIfAny();
        return mobilePhone;
    }

    /**
     * Valida escrevendo no {@link Notification} de quem chamou, em vez de lancar.
     *
     * <p>E o caminho do agregado, que precisa reunir as violacoes de todos os campos antes de
     * recusar uma vez so (FR-017).
     *
     * @return o celular, ou {@code null} quando alguma regra do campo foi violada
     */
    public static MobilePhone of(String raw, Notification notification) {
        if (raw == null || raw.isBlank()) {
            notification.add(FIELD, "Informe o celular.");
            return null;
        }

        String digits = raw.replaceAll(FORMATTING_CHARACTERS, "");

        if (!isValid(digits)) {
            notification.add(FIELD, "Informe um celular com DDD, contendo 10 ou 11 dígitos.");
            return null;
        }

        return new MobilePhone(digits);
    }

    private static boolean isValid(String digits) {
        if (digits.length() < MINIMUM_DIGITS || digits.length() > MAXIMUM_DIGITS) {
            return false;
        }
        if (!digits.chars().allMatch(Character::isDigit)) {
            return false;
        }
        int areaCode = Integer.parseInt(digits.substring(0, 2));
        return areaCode >= MINIMUM_AREA_CODE && areaCode <= MAXIMUM_AREA_CODE;
    }

    @Override
    public String toString() {
        return value;
    }
}
