package io.github.ovyx.farm.application.cage;

import io.github.ovyx.shared.domain.WeighingSituation;
import io.github.ovyx.farm.application.common.StatusFilter;
import io.github.ovyx.farm.application.sector.SectorDetail;
import io.github.ovyx.farm.domain.model.Status;
import io.github.ovyx.farm.domain.model.WeightRangeStatus;
import io.github.ovyx.farm.domain.valueobject.ReferenceWeight;
import io.github.ovyx.shared.application.spreadsheet.Cell;
import io.github.ovyx.shared.application.spreadsheet.CellFormat;
import io.github.ovyx.shared.application.spreadsheet.Column;
import io.github.ovyx.shared.application.spreadsheet.FileNames;
import io.github.ovyx.shared.application.spreadsheet.Sheet;
import io.github.ovyx.shared.application.spreadsheet.Spreadsheet;
import io.github.ovyx.shared.application.spreadsheet.SpreadsheetHeading;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * A planilha das gaiolas de um setor (US3 da 007; data-model §4), montada sem banco: o cabecalho com os filtros,
 * a faixa de peso e os totais do setor, e uma linha por gaiola com a ultima pesagem valida e a situacao dela
 * diante da faixa ({@link WeightRangeStatus#of}), e a situacao dela na agenda de pesagem, com o dia do atraso
 * (feature 010).
 */
public final class CagesSpreadsheet {

    private static final Locale PORTUGUESE = Locale.forLanguageTag("pt-BR");
    private static final String SHEET = "Gaiolas";

    private static final List<Column> COLUMNS = List.of(
            new Column("Gaiola", 10),
            new Column("Bateria", 9),
            new Column("Número", 9),
            new Column("Aves", 9),
            new Column("Situação", 10),
            new Column("Peso médio (g)", 14),
            new Column("Data da pesagem", 14),
            new Column("Faixa", 20),
            new Column("Pesagem", 13),
            new Column("Atrasada desde", 14));

    private CagesSpreadsheet() {}

    /**
     * A planilha das gaiolas.
     *
     * @param filter os filtros da lista
     * @param cages as gaiolas dos filtros, na ordem da lista
     */
    public static Spreadsheet of(
            SectorDetail sector,
            CageFilter filter,
            List<CageSummary> cages,
            LocalDate today,
            LocalTime now) {
        List<String> heading = SpreadsheetHeading.of(
                "Ovyx — Gaiolas",
                List.of(
                        "Setor: " + sector.name(),
                        "Filtros: " + filtersOf(filter),
                        "Faixa de peso: " + rangeOf(sector.referenceWeight()),
                        "Gaiolas ativas: " + count(sector.activeCageCount()) + ". Aves: " + count(sector.birdCount())),
                today,
                now);
        String fileName = "gaiolas-" + FileNames.slug(sector.name()) + ".xlsx";
        Sheet sheet = cages.isEmpty()
                ? Sheet.empty(SHEET, heading, COLUMNS, "Nenhuma gaiola com esses filtros")
                : Sheet.withRows(
                        SHEET,
                        heading,
                        COLUMNS,
                        cages.stream().map(cage -> rowOf(cage, sector.referenceWeight())).toList(),
                        List.of());
        return new Spreadsheet(fileName, List.of(sheet));
    }

    private static List<Cell> rowOf(CageSummary cage, ReferenceWeight range) {
        CageLastWeighing last = cage.lastWeighing();
        return List.of(
                Cell.text(cage.code()),
                Cell.text(cage.battery()),
                Cell.count(cage.number()),
                Cell.count(cage.birdCount()),
                Cell.text(cage.status() == Status.ACTIVE ? "Ativa" : "Inativa"),
                last == null ? Cell.blank() : Cell.number(last.averageWeight(), CellFormat.GRAMS),
                last == null ? Cell.blank() : Cell.date(last.weighedOn()),
                Cell.text(last == null ? null : rangeStatusOf(WeightRangeStatus.of(range, last.averageWeight()))),
                Cell.text(cage.weighing() == null ? null : situationOf(cage.weighing().situation())),
                cage.weighing() == null || cage.weighing().lateSince() == null
                        ? Cell.blank()
                        : Cell.date(cage.weighing().lateSince()));
    }

    private static String situationOf(WeighingSituation situation) {
        return switch (situation) {
            case UP_TO_DATE -> "Em dia";
            case DUE_TODAY -> "Pesar hoje";
            case LATE -> "Atrasada";
            case NEVER_WEIGHED -> "Nunca pesada";
        };
    }

    private static String rangeStatusOf(WeightRangeStatus status) {
        return switch (status) {
            case WITHIN -> "Dentro da faixa";
            case OUTSIDE -> "Fora da faixa";
            case NO_RANGE -> "Sem faixa definida";
        };
    }

    /**
     * "busca "B-0", bateria B, pesagem pendente, so as ativas", ou "nenhum, so as ativas" sem busca, bateria nem
     * pesagem.
     */
    private static String filtersOf(CageFilter filter) {
        List<String> parts = new ArrayList<>();
        if (filter.code() != null) {
            parts.add("busca \"" + filter.code() + "\"");
        }
        if (filter.battery() != null) {
            parts.add("bateria " + filter.battery());
        }
        if (filter.pendingSince() != null) {
            parts.add("pesagem pendente");
        }
        StatusFilter status = filter.status();
        if (parts.isEmpty()) {
            parts.add("nenhum");
        }
        parts.add(switch (status) {
            case ACTIVE -> "só as ativas";
            case INACTIVE -> "só as inativas";
            case ALL -> "todas as situações";
        });
        return String.join(", ", parts);
    }

    private static String rangeOf(ReferenceWeight range) {
        return range == null ? "sem faixa definida" : range.minimum() + " a " + range.maximum() + " g";
    }

    private static String count(int value) {
        return String.format(PORTUGUESE, "%,d", value);
    }
}
