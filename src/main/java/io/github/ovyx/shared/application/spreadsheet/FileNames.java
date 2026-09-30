package io.github.ovyx.shared.application.spreadsheet;

import java.text.Normalizer;
import java.util.Locale;

/**
 * O nome do setor no nome do arquivo (R-013 da 007): sem acento, em minusculas, com hifen no lugar do que nao
 * e letra nem digito. "Codornas — Galpao 1" vira {@code codornas-galpao-1}.
 */
public final class FileNames {

    /** Quando nada sobra do nome, o arquivo ainda precisa de um. */
    private static final String FALLBACK = "setor";

    private FileNames() {}

    public static String slug(String text) {
        String withoutAccents = Normalizer.normalize(text, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        String hyphenated = withoutAccents.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-");
        // As sequencias viraram um hifen so: sobra no maximo um em cada ponta.
        int start = hyphenated.startsWith("-") ? 1 : 0;
        int end = hyphenated.endsWith("-") ? hyphenated.length() - 1 : hyphenated.length();
        return start >= end ? FALLBACK : hyphenated.substring(start, end);
    }
}
