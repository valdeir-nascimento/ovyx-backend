package io.github.ovyx.production.application.dailyreport;

import java.util.UUID;

/**
 * Uma gaiola do relatorio, como estava na abertura, com os lancamentos que tiver.
 *
 * @param code bateria, hifen e numero com ao menos dois digitos ("B-07")
 * @param birdCount as aves da gaiola na abertura
 * @param production a producao lancada, ou {@code null}
 * @param mortality a mortalidade lancada, ou {@code null}
 */
public record ReportCageDetail(
        UUID cageId,
        String code,
        String battery,
        int number,
        int birdCount,
        CageProduction production,
        CageMortality mortality) {

    /** O codigo da gaiola: o numero completado com um zero abaixo de 10, como no farm. */
    public static String codeOf(String battery, int number) {
        return battery + "-" + (number < 10 ? "0" + number : String.valueOf(number));
    }
}
