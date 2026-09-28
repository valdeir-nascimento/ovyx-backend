package io.github.ovyx.production.application.dashboard;

import io.github.ovyx.production.domain.model.FeedStatus;
import io.github.ovyx.production.domain.model.MortalityStatus;
import io.github.ovyx.production.domain.model.ProductionStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Um relatorio do setor, somado no banco (R-004 da 006): os numeros crus de que o painel precisa, sem
 * nenhuma conta feita.
 *
 * @param cages as gaiolas do relatorio
 * @param cagesWithProduction as gaiolas com a producao lancada
 * @param cagesWithFeed as gaiolas com a racao lancada
 * @param eggs os ovos coletados nas gaiolas com producao
 * @param exactFeedCost a soma de consumo vezes preco guardado, das gaiolas com racao, antes de dividir por 1.000
 * @param removedBirds as mortes mais os descartes
 */
public record ReportDay(
        UUID reportId,
        LocalDate date,
        int openingBirdCount,
        int cages,
        int cagesWithProduction,
        int cagesWithFeed,
        int eggs,
        int small,
        int jumbo,
        int dirty,
        int cracked,
        int bloodSpot,
        int abnormal,
        BigDecimal exactFeedCost,
        int removedBirds,
        boolean noMortalityConfirmed) {

    public ProductionStatus productionStatus() {
        return ProductionStatus.of(cages - cagesWithProduction);
    }

    public FeedStatus feedStatus() {
        return FeedStatus.of(cages - cagesWithFeed);
    }

    public MortalityStatus mortalityStatus() {
        return MortalityStatus.of(noMortalityConfirmed, removedBirds);
    }

    /** Os ovos classificados fora do padrao. */
    public int graded() {
        return small + jumbo + dirty + cracked + bloodSpot + abnormal;
    }
}
