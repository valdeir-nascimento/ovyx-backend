package io.github.ovyx.shared.presentation;

import java.io.Serializable;
import java.util.UUID;

/**
 * Identidade de quem esta fazendo a requisicao.
 *
 * <p>E o tipo que permite a regra "identidade vence parametro": o controller decide o identificador
 * efetivo pela sessao, e nao pelo que o chamador informou na rota.
 *
 * <p>Aqui a identidade vem da <strong>sessao no servidor</strong>, e nao de um token: e o que FR-004
 * exige, para que a saida invalide a credencial na hora. Por isso e {@link Serializable} — o Spring
 * Session a grava junto com o contexto de seguranca.
 *
 * <p>O perfil e {@code String}, e nao o enum de um contexto: este tipo vive no shared, que nao
 * conhece o vocabulario de nenhum modulo.
 *
 * @param id identificador do responsavel autenticado
 * @param fullName nome exibido na interface
 * @param role perfil, no vocabulario do contexto que autenticou
 * @param mustChangePassword troca de senha provisoria pendente (FR-025)
 * @param sessionGeneration a geracao de sessao do responsavel na entrada (R-005 da 012): a redefinicao da senha pelo
 *     link a muda, e a sessao com a geracao antiga e encerrada. A sessao gravada antes da 012 nao a tem e, lida, vale
 *     0, a geracao de todos ate a primeira redefinicao.
 */
public record AuthenticatedUser(
        UUID id, String fullName, String role, boolean mustChangePassword, int sessionGeneration)
        implements Serializable {

    /** Autoridade no formato que o Spring Security espera. */
    public String authority() {
        return "ROLE_" + role;
    }

    /** Copia com a obrigacao de trocar a senha encerrada, apos a troca. */
    public AuthenticatedUser withPasswordChanged() {
        return new AuthenticatedUser(id, fullName, role, false, sessionGeneration);
    }

    /** Nunca inclui o nome: identifica sem expor dado pessoal em log. */
    @Override
    public String toString() {
        return "AuthenticatedUser[id=" + id + ", role=" + role + "]";
    }
}
