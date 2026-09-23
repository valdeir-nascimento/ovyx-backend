package io.github.ovyx.identity.domain.port;

import io.github.ovyx.identity.domain.valueobject.PasswordHash;

/**
 * Porta de saida para protecao de senha.
 *
 * <p>Declarada no dominio e implementada em {@code infrastructure}: o dominio precisa saber que
 * existe um jeito de proteger e conferir senha, mas nao pode conhecer Argon2, BCrypt nem Spring
 * Security — isso violaria o principio I.
 *
 * <p>Nenhum tipo de framework aparece nesta assinatura, o que e verificado pela suite ArchUnit.
 */
public interface PasswordHasher {

    /**
     * Protege uma senha em texto claro.
     *
     * @param rawPassword senha como digitada; vive apenas em memoria, durante a requisicao
     * @return hash com prefixo de algoritmo
     */
    PasswordHash hash(String rawPassword);

    /**
     * Confere uma senha digitada contra o hash guardado.
     *
     * <p>A implementacao precisa ser resistente a ataque de tempo: o custo da comparacao nao pode
     * variar conforme a senha esteja certa ou errada.
     */
    boolean matches(String rawPassword, PasswordHash hash);
}
