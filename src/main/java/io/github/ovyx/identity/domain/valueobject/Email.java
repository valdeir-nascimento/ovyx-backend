package io.github.ovyx.identity.domain.valueobject;

import io.github.ovyx.identity.domain.Notification;

import java.util.Locale;
import java.util.regex.Pattern;

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

    /**
     * Recusa na hora, para quem valida um campo so.
     *
     * @throws io.github.ovyx.shared.domain.DomainException quando alguma regra do campo e violada
     */
    public static Email of(String raw) {
        Notification notification = new Notification();
        Email email = of(raw, notification);
        notification.throwIfAny();
        return email;
    }

    /**
     * Valida escrevendo no {@link Notification} de quem chamou, em vez de lancar.
     *
     * <p>E o caminho do agregado, que precisa reunir as violacoes de todos os campos antes de
     * recusar uma vez so (FR-017). As duas regras deste campo somam mensagens, sem uma descartar a
     * outra.
     *
     * @return o e-mail, ou {@code null} quando alguma regra do campo foi violada
     */
    public static Email of(String raw, Notification notification) {
        if (raw == null || raw.isBlank()) {
            notification.add(FIELD, "Informe o e-mail.");
            return null;
        }

        String normalized = raw.trim().toLowerCase(Locale.ROOT);
        boolean rejected = false;

        if (normalized.length() > MAXIMUM_LENGTH) {
            notification.add(FIELD, "O e-mail deve ter no máximo 254 caracteres.");
            rejected = true;
        }
        // O formato so e avaliado ate um teto de seguranca. A repeticao aninhada da expressao
        // recursa por segmento, e uma entrada absurda poderia estourar a pilha; acima do teto, o
        // "longo demais" ja basta para recusar.
        if (normalized.length() <= FORMAT_CHECK_LIMIT && !FORMAT.matcher(normalized).matches()) {
            notification.add(FIELD, "Informe um e-mail em formato válido.");
            rejected = true;
        }

        return rejected ? null : new Email(normalized);
    }

    @Override
    public String toString() {
        return value;
    }
}
