package io.github.ovyx.identity.application.authentication;

import io.github.ovyx.identity.domain.IdentityErrorCode;
import io.github.ovyx.shared.application.ApplicationError;
import io.github.ovyx.shared.application.ErrorType;
import io.github.ovyx.shared.application.QueryHandler;
import io.github.ovyx.shared.application.Result;

/**
 * Consulta o responsavel autenticado.
 *
 * <p>A falha existe por um motivo concreto: a sessao pode sobreviver ao responsavel. Se alguem for
 * removido da base enquanto sua sessao esta aberta, a consulta precisa dizer isso em vez de
 * devolver um modelo vazio que a tela exibiria como se estivesse tudo bem.
 *
 * <p>Com a geracao de sessao informada, a consulta tambem recusa a sessao aberta antes de a senha ser redefinida pelo
 * link: a geracao do responsavel mudou, e a da sessao ficou para tras (R-005 da 012).
 */
public class GetAuthenticatedCaretakerQueryHandler implements QueryHandler<GetAuthenticatedCaretakerQuery, AuthenticatedCaretaker> {

    private final CaretakerReadModels caretakerReadModels;

    public GetAuthenticatedCaretakerQueryHandler(CaretakerReadModels caretakerReadModels) {
        this.caretakerReadModels = caretakerReadModels;
    }

    @Override
    public Result<AuthenticatedCaretaker> handle(GetAuthenticatedCaretakerQuery query) {
        return caretakerReadModels
            .findAuthenticatedById(query.caretakerId())
            .map(caretaker -> isRevoked(query, caretaker)
                ? Result.<AuthenticatedCaretaker>failure(ApplicationError.of(
                    ErrorType.UNAUTHENTICATED,
                    IdentityErrorCode.SESSION_REVOKED.code(),
                    IdentityErrorCode.SESSION_REVOKED_MESSAGE))
                : Result.success(caretaker))
            .orElseGet(() -> Result.failure(ApplicationError.of(
                ErrorType.UNAUTHENTICATED,
                IdentityErrorCode.CARETAKER_UNAVAILABLE.code(),
                IdentityErrorCode.CARETAKER_UNAVAILABLE_MESSAGE)));
    }

    private static boolean isRevoked(GetAuthenticatedCaretakerQuery query, AuthenticatedCaretaker caretaker) {
        return query.sessionGeneration() != null && query.sessionGeneration() != caretaker.sessionGeneration();
    }
}
