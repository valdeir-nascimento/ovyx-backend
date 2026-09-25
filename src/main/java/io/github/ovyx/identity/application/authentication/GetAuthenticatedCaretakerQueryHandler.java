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
            .map(Result::success)
            .orElseGet(() -> Result.failure(ApplicationError.of(
                ErrorType.UNAUTHENTICATED,
                IdentityErrorCode.CARETAKER_UNAVAILABLE.code(),
                IdentityErrorCode.CARETAKER_UNAVAILABLE_MESSAGE)));
    }
}
