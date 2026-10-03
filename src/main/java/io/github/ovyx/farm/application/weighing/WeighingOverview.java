package io.github.ovyx.farm.application.weighing;

import io.github.ovyx.farm.domain.model.Status;
import io.github.ovyx.shared.domain.WeighingSchedule;
import java.time.LocalDate;
import io.github.ovyx.shared.domain.WeighingStanding;
import io.github.ovyx.farm.domain.model.WeightRangeStatus;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * O acompanhamento do peso de uma gaiola, para a tela Peso medio (R-008 da 005): uma leitura so, com as contas
 * feitas aqui, onde se testam sem banco.
 *
 * @param latest a ultima pesagem valida, ou {@code null} sem pesagem
 * @param fourWeekChange a ultima menos a pesagem mais recente feita ate 28 dias antes dela, ou {@code null}
 *     sem essa pesagem
 * @param rangeStatus a ultima pesagem diante da faixa do setor, ou {@code null} sem pesagem
 * @param chart as ultimas 12 pesagens validas, da mais antiga para a mais recente
 * @param history todas as pesagens validas, da mais recente para a mais antiga, com a variacao sobre a
 *     anterior
 * @param schedule a situacao da gaiola na agenda de pesagem do setor, com a proxima pesagem (feature 010);
 *     {@code null} na gaiola inativa e em setor inativo
 */
public record WeighingOverview(
        WeighedCage cage,
        WeighedSector sector,
        LatestWeighing latest,
        FourWeekChange fourWeekChange,
        WeightRangeStatus rangeStatus,
        List<WeighingPoint> chart,
        List<WeighingHistoryEntry> history,
        WeighingStanding schedule) {

    /** A distancia, em dias, ate a pesagem de referencia da variacao em 4 semanas. */
    private static final int FOUR_WEEKS = 28;

    /** Quantas pesagens o grafico mostra. */
    private static final int CHARTED = 12;

    public WeighingOverview {
        chart = List.copyOf(chart);
        history = List.copyOf(history);
    }

    /** O acompanhamento a partir das pesagens validas da gaiola, em qualquer ordem. */
    public static WeighingOverview of(
            WeighedCage cage, WeighedSector sector, List<WeighingEntry> weighings, LocalDate today) {
        List<WeighingEntry> byDay = weighings.stream()
                .sorted(Comparator.comparing(WeighingEntry::weighedOn))
                .toList();
        if (byDay.isEmpty()) {
            return new WeighingOverview(
                    cage, sector, null, null, null, List.of(), List.of(), scheduleOf(cage, sector, null, today));
        }
        WeighingEntry last = byDay.getLast();
        return new WeighingOverview(
                cage,
                sector,
                new LatestWeighing(last.id(), last.weighedOn(), last.averageWeight()),
                fourWeekChangeOf(byDay, last),
                rangeStatusOf(sector, last),
                chartOf(byDay),
                historyOf(byDay),
                scheduleOf(cage, sector, last.weighedOn(), today));
    }

    /**
     * A situacao da gaiola na agenda do setor, com a proxima pesagem: so na gaiola ativa de setor ativo (FR-015 da
     * 010).
     */
    private static WeighingStanding scheduleOf(
            WeighedCage cage, WeighedSector sector, LocalDate lastWeighedOn, LocalDate today) {
        if (cage.status() != Status.ACTIVE || sector.status() != Status.ACTIVE) {
            return null;
        }
        return new WeighingSchedule(sector.weighingDay()).standingOf(today, lastWeighedOn);
    }

    /** A ultima menos a pesagem mais recente com data ate 28 dias antes da dela; sem essa, nenhuma. */
    private static FourWeekChange fourWeekChangeOf(List<WeighingEntry> byDay, WeighingEntry last) {
        return byDay.reversed().stream()
                .filter(weighing -> !weighing.weighedOn().isAfter(last.weighedOn().minusDays(FOUR_WEEKS)))
                .findFirst()
                .map(reference -> new FourWeekChange(
                        last.averageWeight().subtract(reference.averageWeight()), reference.weighedOn()))
                .orElse(null);
    }

    /** A ultima pesagem diante da faixa do setor, com os limites incluidos. */
    private static WeightRangeStatus rangeStatusOf(WeighedSector sector, WeighingEntry last) {
        if (sector.referenceWeight() == null) {
            return WeightRangeStatus.NO_RANGE;
        }
        return sector.referenceWeight().contains(last.averageWeight())
                ? WeightRangeStatus.WITHIN
                : WeightRangeStatus.OUTSIDE;
    }

    private static List<WeighingPoint> chartOf(List<WeighingEntry> byDay) {
        return byDay.subList(Math.max(0, byDay.size() - CHARTED), byDay.size()).stream()
                .map(weighing -> new WeighingPoint(weighing.weighedOn(), weighing.averageWeight()))
                .toList();
    }

    private static List<WeighingHistoryEntry> historyOf(List<WeighingEntry> byDay) {
        List<WeighingHistoryEntry> history = new ArrayList<>();
        WeighingEntry previous = null;
        for (WeighingEntry weighing : byDay) {
            history.addFirst(new WeighingHistoryEntry(
                    weighing.id(),
                    weighing.weighedOn(),
                    weighing.averageWeight(),
                    previous == null ? null : weighing.averageWeight().subtract(previous.averageWeight()),
                    weighing.recordedBy(),
                    weighing.lastCorrectedBy()));
            previous = weighing;
        }
        return history;
    }
}
