package io.github.ovyx.shared.presentation;

/**
 * Os nomes dos perfis que as regras de rota exigem, sem o prefixo {@code ROLE_}.
 *
 * <p>Ficam no {@code shared} porque todo contexto declara as próprias rotas (R-008 da feature 002), e
 * nenhum deles pode depender do {@code Role} do {@code identity} para isso. O nome precisa ser o mesmo
 * que a sessão carrega, e um teste do {@code identity} confere que é.
 */
public final class AccessRoles {

    /** O perfil que cadastra, edita, inativa e reativa. */
    public static final String ADMINISTRATOR = "ADMINISTRATOR";

    private AccessRoles() {}
}
