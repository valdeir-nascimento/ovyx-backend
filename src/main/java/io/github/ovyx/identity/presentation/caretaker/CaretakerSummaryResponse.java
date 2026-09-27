package io.github.ovyx.identity.presentation.caretaker;

import io.github.ovyx.identity.application.caretaker.CaretakerSummary;
import io.github.ovyx.identity.domain.model.CaretakerStatus;
import io.github.ovyx.identity.domain.model.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

/** Uma linha da lista de responsaveis. */
@Schema(description = "Responsável na lista")
public record CaretakerSummaryResponse(
    @Schema(example = "9f8e7d6c-5b4a-4938-2716-0f1e2d3c4b5a") UUID id,
    @Schema(example = "João Pereira de Souza") String fullName,
    @Schema(example = "joao.pereira@ovyx.com.br") String email,
    @Schema(example = "91991234567") String mobilePhone,
    @Schema(example = "USER") Role role,
    @Schema(example = "ACTIVE") CaretakerStatus status) {

    public static CaretakerSummaryResponse from(CaretakerSummary summary) {
        return new CaretakerSummaryResponse(
            summary.id().value(),
            summary.fullName(),
            summary.email(),
            summary.mobilePhone(),
            summary.role(),
            summary.status());
    }
}
