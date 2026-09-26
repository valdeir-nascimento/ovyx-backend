package io.github.ovyx.production.application.dailyreport;

import io.github.ovyx.production.domain.model.MortalityStatus;
import io.github.ovyx.production.domain.model.ProductionStatus;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * O relatorio na lista do setor (FR-016).
 *
 * @param note a observacao, ou {@code null}
 * @param removedBirds mortes mais descartes do dia
 * @param closingBirdCount aves do inicio do dia menos as removidas
 * @param pendingCages gaiolas sem producao lancada
 */
public record DailyReportSummary(
        UUID id,
        LocalDate collectionDate,
        LocalTime collectionTime,
        String openedByName,
        int flockAge,
        int collectedEggs,
        int removedBirds,
        int closingBirdCount,
        String note,
        ProductionStatus productionStatus,
        int pendingCages,
        MortalityStatus mortalityStatus) {}
