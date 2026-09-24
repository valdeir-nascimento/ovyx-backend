package io.github.ovyx.identity.presentation.caretaker;

import io.github.ovyx.identity.application.caretaker.CaretakerDetail;
import io.github.ovyx.identity.domain.model.CaretakerStatus;
import io.github.ovyx.identity.domain.model.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

/** Um responsavel, na tela de detalhe e de edicao. Nunca inclui o hash da senha. */
@Schema(description = "Responsável, sem a senha nem o hash dela")
public record CaretakerDetailResponse(
    @Schema(example = "9f8e7d6c-5b4a-4938-2716-0f1e2d3c4b5a") UUID id,
    @Schema(example = "João Pereira de Souza") String fullName,
    @Schema(example = "52998224725") String cpf,
    @Schema(example = "joao.pereira@ovyx.com.br") String email,
    @Schema(example = "91991234567") String mobilePhone,
    @Schema(example = "USER") Role role,
    @Schema(example = "ACTIVE") CaretakerStatus status,
    @Schema(example = "2026-09-18T13:45:10Z") Instant createdAt,
    @Schema(example = "2026-09-18T13:45:10Z") Instant updatedAt) {

    public static CaretakerDetailResponse from(CaretakerDetail detail) {
        return new CaretakerDetailResponse(
            detail.id().value(),
            detail.fullName(),
            detail.cpf(),
            detail.email(),
            detail.mobilePhone(),
            detail.role(),
            detail.status(),
            detail.createdAt(),
            detail.updatedAt());
    }
}
