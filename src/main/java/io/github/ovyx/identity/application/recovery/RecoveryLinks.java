package io.github.ovyx.identity.application.recovery;

import io.github.ovyx.identity.domain.IdentityErrorCode;
import io.github.ovyx.identity.domain.model.Caretaker;
import io.github.ovyx.identity.domain.port.CaretakerRepository;
import io.github.ovyx.identity.domain.valueobject.RecoveryToken;
import io.github.ovyx.shared.application.ApplicationError;
import io.github.ovyx.shared.application.ErrorType;
import io.github.ovyx.shared.domain.DomainException;
import java.util.Optional;

/**
 * O que a conferencia e a redefinicao dividem: achar a conta do link e a recusa unica do link que nao vale.
 */
final class RecoveryLinks {

    private RecoveryLinks() {}

    /** A conta do link pendente com o resumo deste codigo; vazio para o codigo fora do formato ou de ninguem. */
    static Optional<Caretaker> ownerOf(String rawToken, CaretakerRepository caretakerRepository) {
        try {
            return caretakerRepository.findByRecoveryTokenHash(RecoveryToken.of(rawToken).hash());
        } catch (DomainException outsideTheFormat) {
            return Optional.empty();
        }
    }

    /** A recusa do link que nao vale, a mesma para toda causa (FR-008). */
    static ApplicationError invalidLink() {
        return ApplicationError.of(
                ErrorType.VALIDATION,
                IdentityErrorCode.RECOVERY_LINK_INVALID.code(),
                IdentityErrorCode.RECOVERY_LINK_INVALID_MESSAGE);
    }
}
