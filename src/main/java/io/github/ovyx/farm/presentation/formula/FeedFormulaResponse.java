package io.github.ovyx.farm.presentation.formula;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.ovyx.farm.application.formula.FeedFormulaSummary;
import io.github.ovyx.farm.domain.model.Status;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Uma formula de racao, com o custo por ave ao dia (FR-005 da 004). A lista e o detalhe usam a mesma forma. */
@Schema(name = "FeedFormula", description = "Fórmula de ração, com o custo por ave ao dia")
public record FeedFormulaResponse(
        @Schema(example = "4e6a8c0e-2a4c-4e6a-9c0e-2a4c6e8a0c11") UUID id,
        @Schema(example = "Postura Plus") String name,
        @Schema(
                description = "Ausente quando não há descrição",
                example = "Milho, farelo de soja, calcário e premix vitamínico; para codornas em postura")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String description,
        @Schema(description = "Preço por quilo, em reais", example = "2.85") BigDecimal pricePerKg,
        @Schema(description = "Consumo esperado por ave ao dia, em gramas", example = "28") int expectedIntake,
        @Schema(description = "Preço × consumo esperado ÷ 1.000, em reais, com três casas", example = "0.080")
        BigDecimal costPerBirdDay,
        @Schema(example = "ACTIVE") Status status,
        @Schema(example = "2026-09-20T10:15:00Z") Instant createdAt,
        @Schema(example = "2026-09-24T17:40:12Z") Instant updatedAt) {

    public static FeedFormulaResponse from(FeedFormulaSummary summary) {
        return new FeedFormulaResponse(
                summary.id().value(),
                summary.name(),
                summary.description(),
                summary.pricePerKg(),
                summary.expectedIntake(),
                summary.costPerBirdDay(),
                summary.status(),
                summary.createdAt(),
                summary.updatedAt());
    }
}
