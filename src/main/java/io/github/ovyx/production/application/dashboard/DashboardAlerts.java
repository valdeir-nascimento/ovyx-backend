package io.github.ovyx.production.application.dashboard;

import io.github.ovyx.production.application.dailyreport.DailyReportTotals;
import io.github.ovyx.production.domain.model.MortalityStatus;
import io.github.ovyx.shared.domain.WeighingSchedule;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Os alertas e as pendencias de hoje (US3 da 006; FR-015 a FR-018, R-007), calculados sem banco a partir do
 * relatorio de hoje e das gaiolas.
 *
 * <p>Primeiro as pendencias do relatorio de hoje; depois os alertas das gaiolas, por tipo, cada tipo na ordem
 * das gaiolas. Os limites vem do prototipo: a mortalidade acima do dobro da media diaria por gaiola, com ao
 * menos 2 aves; a produtividade abaixo da meta do setor nos ultimos 3 relatorios (feature 008); e a pesagem
 * fora da faixa.
 *
 * <p>Por ultimo, no maximo um aviso de pesagem por setor, pela agenda dele (feature 010): no dia da pesagem, a
 * informacao das gaiolas que faltam pesar; depois dele, ou sem dia definido, a atencao das atrasadas.
 */
public final class DashboardAlerts {

    /** O minimo de aves removidas hoje para o alerta de mortalidade. */
    private static final int MINIMUM_REMOVED = 2;
    /** Quantas vezes a media do setor a gaiola precisa passar. */
    private static final BigDecimal TIMES_THE_AVERAGE = BigDecimal.valueOf(2);
    /** Os relatorios que a baixa postura olha. */
    private static final int RECENT_REPORTS = 3;
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DAY_AND_MONTH = DateTimeFormatter.ofPattern("dd/MM");
    /** O dia da pesagem como a granja o le: "sexta-feira, 25/09". */
    private static final DateTimeFormatter WEEKDAY_AND_DAY =
            DateTimeFormatter.ofPattern("EEEE, dd/MM", Locale.forLanguageTag("pt-BR"));

    private DashboardAlerts() {}

    /**
     * Os alertas de hoje.
     *
     * @param todayReport o relatorio de hoje somado; {@code null} se hoje ainda nao foi aberto
     * @param target a meta de produtividade do setor, que a baixa postura usa (feature 008)
     */
    public static List<DashboardAlert> of(
            LocalDate today, ReportDay todayReport, CageWatchReading reading, LayingRateTarget target) {
        List<DashboardAlert> alerts = new ArrayList<>(pending(today, todayReport));
        if (todayReport != null) {
            BigDecimal average = reading.baseline().dailyAverage();
            reading.cages().stream()
                    .filter(cage -> isHighMortality(cage, average))
                    .map(cage -> highMortality(cage, average, todayReport))
                    .forEach(alerts::add);
        }
        reading.cages().stream()
                .filter(cage -> isLowLaying(cage, target))
                .map(cage -> lowLaying(cage, todayReport, target))
                .forEach(alerts::add);
        if (reading.range() != null) {
            reading.cages().stream()
                    .filter(cage -> cage.lastWeight() != null && !reading.range().contains(cage.lastWeight()))
                    .map(cage -> weightOutOfRange(cage, reading.range()))
                    .forEach(alerts::add);
        }
        if (reading.schedule() != null) {
            weighing(today, reading.cages(), reading.schedule()).ifPresent(alerts::add);
        }
        return List.copyOf(alerts);
    }

    // ---------------------------------------------------------------- pendencias do relatorio de hoje

    private static List<DashboardAlert> pending(LocalDate today, ReportDay report) {
        if (report == null) {
            return List.of(new DashboardAlert(
                    AlertKind.REPORT_NOT_OPENED,
                    AlertTone.WARNING,
                    "Relatório de hoje não aberto",
                    "Abra o relatório de " + DAY_AND_MONTH.format(today)
                            + " para lançar a produção, a ração e a mortalidade.",
                    new AlertTarget(null, null, null)));
        }
        AlertTarget target = new AlertTarget(report.reportId(), null, null);
        List<DashboardAlert> alerts = new ArrayList<>();
        int productionPending = report.cages() - report.cagesWithProduction();
        if (productionPending > 0) {
            alerts.add(new DashboardAlert(
                    AlertKind.PRODUCTION_PENDING,
                    AlertTone.WARNING,
                    "Produção pendente no relatório de hoje",
                    "Falta lançar a produção de " + cages(productionPending) + ".",
                    target));
        }
        int feedPending = report.cages() - report.cagesWithFeed();
        if (feedPending > 0) {
            alerts.add(new DashboardAlert(
                    AlertKind.FEED_PENDING,
                    AlertTone.INFO,
                    "Ração pendente no relatório de hoje",
                    "Lance a ração de " + cages(feedPending) + " para calcular o custo por ovo.",
                    target));
        }
        if (report.mortalityStatus() == MortalityStatus.PENDING) {
            alerts.add(new DashboardAlert(
                    AlertKind.MORTALITY_PENDING,
                    AlertTone.INFO,
                    "Mortalidade pendente no relatório de hoje",
                    "Lance as mortes e os descartes, ou confirme o dia sem ocorrência.",
                    target));
        }
        return alerts;
    }

    // ---------------------------------------------------------------- gaiolas

    private static boolean isHighMortality(CageWatch cage, BigDecimal average) {
        return cage.removedToday() >= MINIMUM_REMOVED
                && BigDecimal.valueOf(cage.removedToday()).compareTo(average.multiply(TIMES_THE_AVERAGE)) > 0;
    }

    private static DashboardAlert highMortality(CageWatch cage, BigDecimal average, ReportDay report) {
        return new DashboardAlert(
                AlertKind.HIGH_MORTALITY,
                AlertTone.WARNING,
                "Mortalidade acima da média na gaiola " + cage.code(),
                cage.removedToday() + " aves removidas hoje; a média do setor é " + oneDecimal(average)
                        + " por gaiola ao dia.",
                new AlertTarget(report.reportId(), cage.cageId(), cage.code()));
    }

    private static boolean isLowLaying(CageWatch cage, LayingRateTarget target) {
        return cage.recentReports() >= RECENT_REPORTS
                && cage.recentBirds() > 0
                && !target.isMetBy(DailyReportTotals.percent(cage.recentEggs(), cage.recentBirds()));
    }

    /**
     * A baixa postura diz sempre a meta do setor, que explica o alerta, e, com o relatorio de hoje aberto e com
     * aves, a produtividade do setor hoje, como comparacao (R-009 da 008).
     */
    private static DashboardAlert lowLaying(CageWatch cage, ReportDay report, LayingRateTarget target) {
        String rate = oneDecimal(DailyReportTotals.percent(cage.recentEggs(), cage.recentBirds())) + "%";
        String sectorToday = report == null || report.openingBirdCount() == 0
                ? "."
                : "; o setor fez " + oneDecimal(DailyReportTotals.percent(report.eggs(), report.openingBirdCount()))
                        + "% hoje.";
        return new DashboardAlert(
                AlertKind.LOW_LAYING,
                AlertTone.WARNING,
                "Baixa postura na gaiola " + cage.code(),
                rate + " nos últimos " + RECENT_REPORTS + " relatórios, abaixo da meta de " + target.label()
                        + "% do setor" + sectorToday,
                new AlertTarget(null, cage.cageId(), cage.code()));
    }

    private static DashboardAlert weightOutOfRange(CageWatch cage, ReferenceWeight range) {
        return new DashboardAlert(
                AlertKind.WEIGHT_OUT_OF_RANGE,
                AlertTone.WARNING,
                "Pesagem fora da faixa na gaiola " + cage.code(),
                grams(cage.lastWeight()) + " g em " + DAY.format(cage.lastWeighedOn()) + "; a faixa do setor é "
                        + range.minimum() + "–" + range.maximum() + " g.",
                new AlertTarget(null, cage.cageId(), cage.code()));
    }

    // ---------------------------------------------------------------- agenda de pesagem (010)

    /**
     * O aviso de pesagem do setor: as gaiolas ativas sem a pesagem da semana, pela mesma regra da lista de
     * gaiolas ({@link WeighingSchedule}); nenhum, com todas em dia.
     */
    private static Optional<DashboardAlert> weighing(LocalDate today, List<CageWatch> cages, WeighingSchedule schedule) {
        List<CageWatch> pending = cages.stream()
                .filter(cage -> schedule.standingOf(today, cage.lastWeighedOn()).isPending())
                .toList();
        if (pending.isEmpty()) {
            return Optional.empty();
        }
        String batteries = batteriesOf(pending);
        AlertTarget target = new AlertTarget(null, null, null);
        if (schedule.isWeighingDay(today)) {
            return Optional.of(new DashboardAlert(
                    AlertKind.WEIGHING_DUE,
                    AlertTone.INFO,
                    "Pesagem semanal hoje",
                    cages(pending.size()) + " a pesar, " + batteries + ".",
                    target));
        }
        String missed = schedule.weighingDay() == null
                ? " sem pesagem há mais de 7 dias, "
                : " sem a pesagem de " + WEEKDAY_AND_DAY.format(schedule.dueOn(today)) + ", ";
        return Optional.of(new DashboardAlert(
                AlertKind.WEIGHING_LATE,
                AlertTone.WARNING,
                "Pesagem atrasada",
                cages(pending.size()) + missed + batteries + ".",
                target));
    }

    /** As baterias das gaiolas, sem repetir, na ordem delas: "bateria B", "baterias A, B e C". */
    private static String batteriesOf(List<CageWatch> cages) {
        List<String> batteries = cages.stream().map(CageWatch::battery).distinct().toList();
        if (batteries.size() == 1) {
            return "bateria " + batteries.get(0);
        }
        return "baterias " + String.join(", ", batteries.subList(0, batteries.size() - 1)) + " e "
                + batteries.get(batteries.size() - 1);
    }

    // ---------------------------------------------------------------- numeros como a tela os escreve

    private static String cages(int count) {
        return count == 1 ? "1 gaiola" : count + " gaiolas";
    }

    private static String oneDecimal(BigDecimal value) {
        return value.setScale(1, RoundingMode.HALF_UP).toPlainString().replace('.', ',');
    }

    /** O peso com a casa so quando houver: "150,8", "190". */
    private static String grams(BigDecimal weight) {
        BigDecimal stripped = weight.stripTrailingZeros();
        return (stripped.scale() <= 0 ? stripped.setScale(0) : stripped).toPlainString().replace('.', ',');
    }
}
