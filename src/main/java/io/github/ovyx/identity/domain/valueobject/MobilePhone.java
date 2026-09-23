package io.github.ovyx.identity.domain.valueobject;

import io.github.ovyx.identity.domain.IdentityErrorCode;
import io.github.ovyx.shared.domain.Notification;
import io.github.ovyx.shared.domain.Rule;
import java.util.List;

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
     * Uma regra so, de proposito: quantidade de digitos, DDD e letra levam a mesma correcao, e a
     * mensagem diz de uma vez o formato esperado.
     */
    private static final List<Rule<String>> RULES = List.of(Rule.of(
            MobilePhone::isValid,
            IdentityErrorCode.MOBILE_PHONE_INVALID,
            "Informe um celular com DDD, contendo 10 ou 11 dígitos."));

    /**
     * Registra no {@link Notification} as violacoes do campo, sem lancar.
     *
     * <p>E o caminho do agregado, que reune as violacoes de todos os campos antes de recusar (FR-017).
     */
    public static void validate(String raw, Notification notification) {
        if (notification.requirePresent(
                FIELD, raw, IdentityErrorCode.MOBILE_PHONE_REQUIRED, "Informe o celular.")) {
            notification.check(FIELD, digitsOf(raw), RULES);
        }
    }

    /**
     * Cria o celular, recusando na hora com as violacoes do campo.
     *
     * @throws io.github.ovyx.shared.domain.DomainException quando o celular e ausente ou invalido
     */
    public static MobilePhone of(String raw) {
        Notification notification = new Notification();
        validate(raw, notification);
        notification.throwIfAny(IdentityErrorCode.VALIDATION_FAILED);
        return new MobilePhone(digitsOf(raw));
    }

    private static String digitsOf(String raw) {
        return raw.replaceAll(FORMATTING_CHARACTERS, "");
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
