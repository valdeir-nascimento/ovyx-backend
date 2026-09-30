package io.github.ovyx.shared.application.spreadsheet;

import java.util.Objects;

/**
 * Uma coluna da aba: o titulo e a largura, em caracteres (R-012 da 007).
 *
 * @param width de 6 a 60 caracteres
 */
public record Column(String title, int width) {

    private static final int NARROWEST = 6;
    private static final int WIDEST = 60;

    public Column {
        Objects.requireNonNull(title, "titulo");
        if (width < NARROWEST || width > WIDEST) {
            throw new IllegalArgumentException(
                    "A largura da coluna deve ficar entre " + NARROWEST + " e " + WIDEST + " caracteres: " + width);
        }
    }
}
