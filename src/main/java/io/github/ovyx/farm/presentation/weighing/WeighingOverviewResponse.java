package io.github.ovyx.farm.presentation.weighing;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.ovyx.farm.application.weighing.FourWeekChange;
import io.github.ovyx.farm.application.weighing.LatestWeighing;
import io.github.ovyx.farm.application.weighing.WeighedCage;
import io.github.ovyx.farm.application.weighing.WeighedSector;
import io.github.ovyx.farm.application.weighing.WeighingHistoryEntry;
import io.github.ovyx.farm.application.weighing.WeighingOverview;
import io.github.ovyx.farm.application.weighing.WeighingPoint;
import io.github.ovyx.farm.domain.model.Status;
import io.github.ovyx.farm.domain.model.WeightRangeStatus;
import io.github.ovyx.farm.presentation.sector.ReferenceWeightResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** O acompanhamento do peso da gaiola, para a tela Peso medio (R-008 da 005). */
@Schema(name = "WeighingOverview", description = "O acompanhamento do peso da gaiola, para a tela Peso médio")
public record WeighingOverviewResponse(
        OverviewCage cage,
        OverviewSector sector,
        @Schema(description = "A última pesagem válida; ausente sem pesagem") @JsonInclude(JsonInclude.Include.NON_NULL)
        Latest latest,
        @Schema(description = "A última pesagem menos a pesagem mais recente feita até 28 dias antes dela; ausente sem ela")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        FourWeeks fourWeekChange,
        @Schema(
                description = "A última pesagem diante da faixa do setor; `NO_RANGE` quando o setor não tem faixa;"
                        + " ausente sem pesagem",
                example = "WITHIN")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        WeightRangeStatus rangeStatus,
        @Schema(description = "A situação da gaiola na agenda de pesagem, com a próxima pesagem; ausente na gaiola inativa"
                + " e em setor inativo")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        WeighingStandingResponse schedule,
        @Schema(description = "As últimas 12 pesagens válidas, da mais antiga para a mais recente")
        List<Point> chart,
        @Schema(description = "Todas as pesagens válidas, da mais recente para a mais antiga")
        List<HistoryEntry> history) {

    /** A gaiola pesada. */
    @Schema(name = "WeighingOverviewCage", description = "A gaiola pesada")
    public record OverviewCage(
            @Schema(example = "2a4c6e8a-0b1d-4f3a-9c5e-7a9b1d3f5a66") UUID id,
            @Schema(example = "A-01") String code,
            @Schema(example = "A") String battery,
            @Schema(example = "1") int number,
            @Schema(example = "48") int birdCount,
            @Schema(example = "ACTIVE") Status status) {

        static OverviewCage from(WeighedCage cage) {
            return new OverviewCage(
                    cage.id().value(), cage.code(), cage.battery(), cage.number(), cage.birdCount(), cage.status());
        }
    }

    /** O setor da gaiola, com a faixa de peso de referencia. */
    @Schema(name = "WeighingOverviewSector", description = "O setor da gaiola")
    public record OverviewSector(
            @Schema(example = "3f6c2b1a-8d4e-4c7f-9a2b-1e5d7c9f0a11") UUID id,
            @Schema(example = "Codornas — Galpão 1") String name,
            @Schema(example = "ACTIVE") Status status,
            @Schema(description = "Faixa de peso de referência das aves, em gramas; ausente sem faixa")
            @JsonInclude(JsonInclude.Include.NON_NULL)
            ReferenceWeightResponse referenceWeight) {

        static OverviewSector from(WeighedSector sector) {
            return new OverviewSector(
                    sector.id().value(),
                    sector.name(),
                    sector.status(),
                    ReferenceWeightResponse.from(sector.referenceWeight()));
        }
    }

    /** A variacao em 4 semanas. */
    @Schema(name = "FourWeekChange", description = "A última pesagem menos a pesagem mais recente feita até 28 dias antes dela")
    public record FourWeeks(
            @Schema(description = "A variação, em gramas, com uma casa", example = "11.0") BigDecimal change,
            @Schema(description = "O dia da pesagem de referência", example = "2026-08-27") LocalDate since) {

        static FourWeeks from(FourWeekChange change) {
            return change == null ? null : new FourWeeks(change.change(), change.since());
        }
    }

    /** Um ponto do grafico. */
    @Schema(name = "WeighingPoint", description = "Uma pesagem resumida à data e ao peso")
    public record Point(
            @Schema(example = "2026-09-24") LocalDate weighedOn,
            @Schema(description = "Peso médio, em gramas, com uma casa", example = "161.0") BigDecimal averageWeight) {

        static Point from(WeighingPoint point) {
            return new Point(point.weighedOn(), point.averageWeight());
        }
    }

    /** A ultima pesagem valida. */
    @Schema(name = "LatestWeighing", description = "A última pesagem válida")
    public record Latest(
            @Schema(example = "7b9d1f3a-5c7e-4a9b-8d1f-3a5c7e9b1d77") UUID id,
            @Schema(example = "2026-09-24") LocalDate weighedOn,
            @Schema(example = "161.0") BigDecimal averageWeight) {

        static Latest from(LatestWeighing latest) {
            return latest == null ? null : new Latest(latest.id().value(), latest.weighedOn(), latest.averageWeight());
        }
    }

    /** Uma pesagem no historico, com a variacao sobre a anterior. */
    @Schema(name = "WeighingHistoryEntry", description = "Uma pesagem no histórico, com a variação sobre a anterior")
    public record HistoryEntry(
            @Schema(example = "7b9d1f3a-5c7e-4a9b-8d1f-3a5c7e9b1d77") UUID id,
            @Schema(example = "2026-09-24") LocalDate weighedOn,
            @Schema(example = "161.0") BigDecimal averageWeight,
            @Schema(
                    description = "Peso desta pesagem menos o da anterior, em gramas, com uma casa; ausente na primeira",
                    example = "3.0")
            @JsonInclude(JsonInclude.Include.NON_NULL)
            BigDecimal change,
            ActorResponse recordedBy,
            @JsonInclude(JsonInclude.Include.NON_NULL) ActorResponse lastCorrectedBy) {

        static HistoryEntry from(WeighingHistoryEntry entry) {
            return new HistoryEntry(
                    entry.id().value(),
                    entry.weighedOn(),
                    entry.averageWeight(),
                    entry.change(),
                    ActorResponse.from(entry.recordedBy()),
                    ActorResponse.from(entry.lastCorrectedBy()));
        }
    }

    public static WeighingOverviewResponse from(WeighingOverview overview) {
        return new WeighingOverviewResponse(
                OverviewCage.from(overview.cage()),
                OverviewSector.from(overview.sector()),
                Latest.from(overview.latest()),
                FourWeeks.from(overview.fourWeekChange()),
                overview.rangeStatus(),
                WeighingStandingResponse.from(overview.schedule()),
                overview.chart().stream().map(Point::from).toList(),
                overview.history().stream().map(HistoryEntry::from).toList());
    }
}
