package io.github.ovyx.production.application.dailyreport;

import io.github.ovyx.production.domain.model.ReportCage;
import java.util.UUID;

/**
 * Uma gaiola do relatorio, como estava na abertura, com os lancamentos que tiver.
 *
 * @param code bateria, hifen e numero com ao menos dois digitos ("B-07")
 * @param birdCount as aves da gaiola na abertura
 * @param production a producao lancada, ou {@code null}
 * @param mortality a mortalidade lancada, ou {@code null}
 * @param feed a racao lancada, ou {@code null} (feature 004)
 */
public record ReportCageDetail(
        UUID cageId,
        String code,
        String battery,
        int number,
        int birdCount,
        CageProduction production,
        CageMortality mortality,
        CageFeed feed) {

    /** O codigo da gaiola: o numero completado com um zero abaixo de 10, como no farm. */
    public static String codeOf(String battery, int number) {
        return ReportCage.codeOf(battery, number);
    }
}
