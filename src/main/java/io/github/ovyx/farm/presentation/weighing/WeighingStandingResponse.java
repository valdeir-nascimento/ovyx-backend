package io.github.ovyx.farm.presentation.weighing;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.ovyx.shared.domain.WeighingSituation;
import io.github.ovyx.shared.domain.WeighingStanding;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

/** A situacao de uma gaiola ativa na agenda de pesagem do setor (feature 010), na lista de gaiolas e no Peso medio. */
@Schema(
        name = "WeighingStanding",
        description = "A situação da pesagem de uma gaiola ativa de setor ativo, calculada com o dia de hoje da granja."
                + " Ausente na gaiola inativa e nas gaiolas de setor inativo.")
public record WeighingStandingResponse(
        @Schema(
                description = "A pesagem da semana: em dia; a pesar hoje (é o dia da pesagem e falta a da semana);"
                        + " atrasada (o dia passou sem ela, ou a última tem mais de 7 dias sem dia definido); nunca"
                        + " pesada.",
                example = "LATE")
        WeighingSituation situation,
        @Schema(
                description = "Só na atrasada: o último dia de pesagem que passou sem ela; sem dia definido, o dia"
                        + " seguinte ao fim do prazo de 7 dias.",
                example = "2026-09-25")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        LocalDate lateSince,
        @Schema(
                description = "Só na em dia: o próximo dia de pesagem; sem dia definido, 7 dias depois da última.",
                example = "2026-10-02")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        LocalDate nextOn) {

    /** A situacao, ou nenhuma sem ela. */
    public static WeighingStandingResponse from(WeighingStanding standing) {
        return standing == null
                ? null
                : new WeighingStandingResponse(standing.situation(), standing.lateSince(), standing.nextOn());
    }
}
