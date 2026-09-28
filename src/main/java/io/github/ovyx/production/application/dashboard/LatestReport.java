package io.github.ovyx.production.application.dashboard;

import io.github.ovyx.production.domain.model.Actor;
import io.github.ovyx.production.domain.model.FeedStatus;
import io.github.ovyx.production.domain.model.MortalityStatus;
import io.github.ovyx.production.domain.model.ProductionStatus;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Um dos relatorios mais recentes do setor (FR-019 da 006), com os ovos, as aves removidas e a situacao de cada
 * lancamento, pelas regras da 003 e da 004.
 */
public record LatestReport(
        UUID id,
        LocalDate collectionDate,
        LocalTime collectionTime,
        Actor openedBy,
        int collectedEggs,
        int removedBirds,
        ProductionStatus productionStatus,
        FeedStatus feedStatus,
        MortalityStatus mortalityStatus) {}
