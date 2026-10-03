package io.github.ovyx.identity.application.authentication;

import io.github.ovyx.identity.domain.model.CaretakerId;
import io.github.ovyx.shared.application.Query;

/**
 * Consulta "quem esta autenticado", a partir da identidade guardada na sessao.
 *
 * @param caretakerId o responsavel da sessao
 * @param sessionGeneration a geracao de sessao gravada na sessao, para a consulta recusar a sessao aberta antes de a
 *     senha ser redefinida pelo link (R-005 da 012); {@code null} quando quem pergunta nao tem sessao a conferir, como
 *     a propria entrada
 */
public record GetAuthenticatedCaretakerQuery(CaretakerId caretakerId, Integer sessionGeneration)
        implements Query<AuthenticatedCaretaker> {

    /** A consulta sem conferir a geracao de sessao. */
    public GetAuthenticatedCaretakerQuery(CaretakerId caretakerId) {
        this(caretakerId, null);
    }
}
