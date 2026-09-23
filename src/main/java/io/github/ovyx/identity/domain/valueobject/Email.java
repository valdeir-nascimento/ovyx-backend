package io.github.ovyx.identity.domain.valueobject;

import io.github.ovyx.identity.domain.Violations;

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
     * @throws io.github.ovyx.shared.domain.DomainException quando alguma regra do campo e violada
     */
    public static Email of(String raw) {
        if (raw == null || raw.isBlank()) {
            throw Violations.of(FIELD, "Informe o e-mail.");
        }

        String normalized = raw.trim().toLowerCase(Locale.ROOT);
        Violations violations = new Violations();

        if (normalized.length() > MAXIMUM_LENGTH) {
            violations.add(FIELD, "O e-mail deve ter no máximo 254 caracteres.");
        }
        // O formato so e avaliado ate um teto de seguranca. A repeticao aninhada da expressao
        // recursa por segmento, e uma entrada absurda poderia estourar a pilha; acima do teto, o
        // "longo demais" ja basta para recusar.
        if (normalized.length() <= FORMAT_CHECK_LIMIT && !FORMAT.matcher(normalized).matches()) {
            violations.add(FIELD, "Informe um e-mail em formato válido.");
        }
        violations.throwIfAny();

        return new Email(normalized);
    }

    @Override
    public String toString() {
        return value;
    }
}
