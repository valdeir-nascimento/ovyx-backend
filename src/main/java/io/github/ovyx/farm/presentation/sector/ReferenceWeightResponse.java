package io.github.ovyx.farm.presentation.sector;

import io.github.ovyx.farm.domain.valueobject.ReferenceWeight;
import io.swagger.v3.oas.annotations.media.Schema;

/** A faixa de peso de referencia das aves do setor, em gramas, com os limites incluidos (feature 005). */
@Schema(
        name = "ReferenceWeight",
        description = "Faixa de peso de referência das aves do setor, em gramas, com os limites incluídos")
public record ReferenceWeightResponse(
        @Schema(example = "155") int minimum, @Schema(example = "175") int maximum) {

    /** A faixa, ou {@code null} quando o setor nao tem. */
    public static ReferenceWeightResponse from(ReferenceWeight referenceWeight) {
        return referenceWeight == null
                ? null
                : new ReferenceWeightResponse(referenceWeight.minimum(), referenceWeight.maximum());
    }
}
