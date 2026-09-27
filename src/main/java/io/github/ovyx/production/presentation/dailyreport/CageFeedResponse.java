package io.github.ovyx.production.presentation.dailyreport;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.ovyx.production.application.dailyreport.CageFeed;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.UUID;

/** A racao lancada numa gaiola, com o preco e o esperado guardados no lancamento (feature 004). */
@Schema(name = "CageFeed", description = "Ração lançada numa gaiola, com o preço e o esperado guardados no lançamento")
public record CageFeedResponse(
        @Schema(example = "4e6a8c0e-2a4c-4e6a-9c0e-2a4c6e8a0c11") UUID formulaId,
        @Schema(description = "Nome atual da fórmula", example = "Postura Plus") String formulaName,
        @Schema(description = "Preço por quilo guardado no lançamento, em reais", example = "2.85")
        BigDecimal pricePerKg,
        @Schema(description = "Consumo esperado por ave ao dia guardado no lançamento, em gramas", example = "28")
        int expectedIntake,
        @Schema(description = "Consumo da gaiola no dia, em gramas", example = "1250") int consumption,
        @Schema(description = "Consumo ÷ 1.000 × preço, em reais, com duas casas", example = "3.56") BigDecimal cost,
        @Schema(description = "Consumo ÷ aves da gaiola, em gramas, com uma casa; ausente com 0 aves", example = "25.0")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        BigDecimal intakePerBird,
        @Schema(
                description = "Desvio do consumo por ave em relação ao esperado, em porcentagem, com uma casa; ausente"
                        + " com 0 aves",
                example = "-10.7")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        BigDecimal deviation) {

    /** A racao da gaiola, ou nenhuma: a gaiola sem racao nao traz o campo. */
    public static CageFeedResponse from(CageFeed feed) {
        return feed == null
                ? null
                : new CageFeedResponse(
                        feed.formulaId(),
                        feed.formulaName(),
                        feed.pricePerKg(),
                        feed.expectedIntake(),
                        feed.consumption(),
                        feed.cost(),
                        feed.intakePerBird(),
                        feed.deviation());
    }
}
