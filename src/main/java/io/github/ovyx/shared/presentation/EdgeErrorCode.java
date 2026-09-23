package io.github.ovyx.shared.presentation;

/**
 * Codigos das recusas que nascem na borda HTTP, antes de qualquer caso de uso.
 *
 * <p>Ficam aqui, e nao no {@code IdentityErrorCode}, porque valem para qualquer contexto: sao da
 * cadeia de seguranca e do proprio Spring MVC, nao de uma regra de negocio.
 */
public final class EdgeErrorCode {

    /** Sessao ausente ou expirada. */
    public static final String UNAUTHENTICATED = "UNAUTHENTICATED";

    /** Autenticado, mas sem permissao para a operacao. */
    public static final String FORBIDDEN = "FORBIDDEN";

    /** Token de protecao contra CSRF ausente ou vencido. */
    public static final String CSRF_TOKEN_INVALID = "CSRF_TOKEN_INVALID";

    /** Troca de senha provisoria pendente (FR-025). */
    public static final String PASSWORD_CHANGE_REQUIRED = "PASSWORD_CHANGE_REQUIRED";

    private EdgeErrorCode() {}
}
