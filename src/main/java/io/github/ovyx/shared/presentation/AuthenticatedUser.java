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
 */
public record AuthenticatedUser(UUID id, String fullName, String role, boolean mustChangePassword)
        implements Serializable {

    /** Autoridade no formato que o Spring Security espera. */
    public String authority() {
        return "ROLE_" + role;
    }

    /** Copia com a obrigacao de trocar a senha encerrada, apos a troca. */
    public AuthenticatedUser withPasswordChanged() {
        return new AuthenticatedUser(id, fullName, role, false);
    }

    /** Nunca inclui o nome: identifica sem expor dado pessoal em log. */
    @Override
    public String toString() {
        return "AuthenticatedUser[id=" + id + ", role=" + role + "]";
    }
}
