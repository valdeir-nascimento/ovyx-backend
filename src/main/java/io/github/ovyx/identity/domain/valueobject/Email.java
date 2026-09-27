package io.github.ovyx.identity.domain.valueobject;

import io.github.ovyx.identity.domain.IdentityErrorCode;
import io.github.ovyx.shared.domain.Notification;
import io.github.ovyx.shared.domain.Rule;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * E-mail do responsavel, o primeiro identificador de acesso.
 *
 * @param value e-mail aparado e em minusculas
 */
public record Email(String value) {

    private static final int MAXIMUM_LENGTH = 254;
    private static final int FORMAT_CHECK_LIMIT = 1024;
    private static final String FIELD = "email";

    /**
     * Formato aceito: parte local sem espaco nem arroba, arroba unica, e dominio com ao menos um
     * ponto. Deliberadamente mais estrito que a RFC 5322 — validacao exaustiva de e-mail e
     * conhecida por aceitar coisas que nenhum servidor entrega.
     */
    private static final Pattern FORMAT = Pattern.compile("^[^\\s@]+@[^\\s@.]+(\\.[^\\s@.]+)+$");

    private static final List<Rule<String>> RULES = List.of(
            Rule.of(
                    email -> email.length() <= MAXIMUM_LENGTH,
                    IdentityErrorCode.EMAIL_TOO_LONG,
                    "O e-mail deve ter no máximo 254 caracteres."),
            // O formato so e avaliado ate um teto de seguranca. A repeticao aninhada da expressao
            // recursa por segmento, e uma entrada absurda poderia estourar a pilha; acima do teto, o
            // "longo demais" ja basta para recusar.
            Rule.of(
                    email -> email.length() > FORMAT_CHECK_LIMIT
                            || FORMAT.matcher(email).matches(),
                    IdentityErrorCode.EMAIL_MALFORMED,
                    "Informe um e-mail em formato válido."));

    /**
     * Construtor canonico: so a garantia estrutural, sem regra de negocio.
     *
     * <p>E o caminho da reidratacao, que le do banco um valor ja validado. Revalidar ali reprovaria
     * registros antigos a cada regra nova; aceitar nulo deixaria um objeto de valor sem valor.
     */
    public Email {
        Objects.requireNonNull(value, "value");
    }

    /**
     * Registra no {@link Notification} as violacoes do campo, sem lancar.
     *
     * <p>E o caminho do agregado, que reune as violacoes de todos os campos antes de recusar (FR-017).
     */
    public static void validate(String raw, Notification notification) {
        if (notification.requirePresent(FIELD, raw, IdentityErrorCode.EMAIL_REQUIRED, "Informe o e-mail.")) {
            notification.check(FIELD, normalized(raw), RULES);
        }
    }

    /**
     * Cria o e-mail, recusando na hora com as violacoes do campo.
     *
     * @throws io.github.ovyx.shared.domain.DomainException quando alguma regra do campo e violada
     */
    public static Email of(String raw) {
        Notification notification = new Notification();
        validate(raw, notification);
        notification.throwIfAny(IdentityErrorCode.VALIDATION_FAILED);
        return new Email(normalized(raw));
    }

    private static String normalized(String raw) {
        return raw.trim().toLowerCase(Locale.ROOT);
    }

    @Override
    public String toString() {
        return value;
    }
}
