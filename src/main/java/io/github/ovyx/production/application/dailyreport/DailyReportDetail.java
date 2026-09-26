package io.github.ovyx.production.application.dailyreport;

import io.github.ovyx.production.domain.model.Actor;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

/**
 * O relatorio com as gaiolas, os lancamentos e os totais do dia.
 *
 * @param note a observacao, ou {@code null}
 * @param lastCorrectedBy quem fez a ultima correcao, ou {@code null}
 * @param lastCorrectedAt quando, ou {@code null}
 * @param cages as gaiolas do relatorio, por bateria e numero
 */
public record DailyReportDetail(
        UUID id,
        ReportingSector sector,
        LocalDate collectionDate,
        LocalTime collectionTime,
        int openingBirdCount,
        int flockAge,
        String note,
        boolean noMortalityConfirmed,
        Actor openedBy,
        Instant openedAt,
        Actor lastCorrectedBy,
        Instant lastCorrectedAt,
        ProductionTotals production,
        MortalityTotals mortality,
        List<ReportCageDetail> cages) {

    public DailyReportDetail {
        cages = List.copyOf(cages);
    }
}
