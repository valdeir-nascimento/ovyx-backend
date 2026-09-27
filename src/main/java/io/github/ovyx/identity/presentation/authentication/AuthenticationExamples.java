package io.github.ovyx.identity.presentation.authentication;


public final class AuthenticationExamples {

    public static final String SIGN_IN_VALIDATION_FAILED =
        """
            {
              "code": "VALIDATION_FAILED",
              "title": "Dados inválidos",
              "status": 400,
              "detail": "A requisição contém campos inválidos.",
              "instance": "/api/v1/auth/sign-in",
              "details": {
                "identifier": "Informe o e-mail ou o celular.",
                "password": "Informe a senha."
              }
            }""";

    public static final String SIGN_IN_BY_EMAIL =
        """
            {
              "identifier": "maria.silva@ovyx.com.br",
              "password": "GranjaNorte2026"
            }""";

    public static final String SIGN_IN_BY_MOBILE_PHONE =
        """
            {
              "identifier": "91988887777",
              "password": "GranjaNorte2026"
            }""";

    public static final String SIGN_IN_COMMON_USER =
        """
            {
              "id": "7c1f0b2e-3d4a-4f5b-8c9d-0e1f2a3b4c5d",
              "fullName": "Maria Silva",
              "role": "USER",
              "mustChangePassword": false
            }""";

    public static final String SIGN_IN_SEEDED_ADMINISTRATOR =
        """
            {
              "id": "1a2b3c4d-5e6f-4071-8293-a4b5c6d7e8f9",
              "fullName": "Administrador do Sistema",
              "role": "ADMINISTRATOR",
              "mustChangePassword": true
            }""";

    public static final String SIGN_IN_IDENTIFIER_TOO_LONG =
        """
            {
              "code": "VALIDATION_FAILED",
              "title": "Dados inválidos",
              "status": 400,
              "detail": "A requisição contém campos inválidos.",
              "instance": "/api/v1/auth/sign-in",
              "details": {
                "identifier": "O identificador deve ter no máximo 254 caracteres."
              }
            }""";

    public static final String SIGN_IN_UNREADABLE_BODY =
        """
            {
              "code": "REQUEST_NOT_ACCEPTABLE",
              "title": "Requisição inválida",
              "status": 400,
              "detail": "O corpo da requisição não pôde ser lido.",
              "instance": "/api/v1/auth/sign-in"
            }""";

    public static final String SIGN_IN_INVALID_CREDENTIALS =
        """
            {
              "code": "INVALID_CREDENTIALS",
              "title": "Não autenticado",
              "status": 401,
              "detail": "E-mail, celular ou senha inválidos.",
              "instance": "/api/v1/auth/sign-in"
            }""";

    public static final String SIGN_IN_CSRF_TOKEN_INVALID =
        """
            {
              "code": "CSRF_TOKEN_INVALID",
              "title": "Proteção da requisição ausente",
              "status": 403,
              "detail": "O token de proteção da requisição está ausente ou expirou. Recarregue a página e tente novamente.",
              "instance": "/api/v1/auth/sign-in"
            }""";

    public static final String SIGN_IN_NOT_ACCEPTABLE =
        """
            {
              "code": "REQUEST_NOT_ACCEPTABLE",
              "title": "Requisição não suportada",
              "status": 406,
              "detail": "A requisição não é suportada por este endereço.",
              "instance": "/api/v1/auth/sign-in"
            }""";

    public static final String SIGN_IN_UNSUPPORTED_MEDIA_TYPE =
        """
            {
              "code": "REQUEST_NOT_ACCEPTABLE",
              "title": "Requisição não suportada",
              "status": 415,
              "detail": "A requisição não é suportada por este endereço.",
              "instance": "/api/v1/auth/sign-in"
            }""";

    public static final String SIGN_IN_INTERNAL_ERROR =
        """
            {
              "code": "INTERNAL_ERROR",
              "title": "Erro inesperado",
              "status": 500,
              "detail": "Não foi possível concluir a operação. Tente novamente em instantes.",
              "instance": "/api/v1/auth/sign-in"
            }""";

    // ----------------------------------------------------------------- POST /api/v1/auth/sign-out

    public static final String SIGN_OUT_UNAUTHENTICATED =
        """
            {
              "code": "UNAUTHENTICATED",
              "title": "Não autenticado",
              "status": 401,
              "detail": "Sua sessão expirou. Entre novamente para continuar.",
              "instance": "/api/v1/auth/sign-out"
            }""";

    public static final String SIGN_OUT_CSRF_TOKEN_INVALID =
        """
            {
              "code": "CSRF_TOKEN_INVALID",
              "title": "Proteção da requisição ausente",
              "status": 403,
              "detail": "O token de proteção da requisição está ausente ou expirou. Recarregue a página e tente novamente.",
              "instance": "/api/v1/auth/sign-out"
            }""";

    public static final String SIGN_OUT_INTERNAL_ERROR =
        """
            {
              "code": "INTERNAL_ERROR",
              "title": "Erro inesperado",
              "status": 500,
              "detail": "Não foi possível concluir a operação. Tente novamente em instantes.",
              "instance": "/api/v1/auth/sign-out"
            }""";

    // ---------------------------------------------------------------------- GET /api/v1/auth/me

    public static final String ME_UNAUTHENTICATED =
        """
            {
              "code": "UNAUTHENTICATED",
              "title": "Não autenticado",
              "status": 401,
              "detail": "Sua sessão expirou. Entre novamente para continuar.",
              "instance": "/api/v1/auth/me"
            }""";

    public static final String ME_CARETAKER_UNAVAILABLE =
        """
            {
              "code": "CARETAKER_UNAVAILABLE",
              "title": "Não autenticado",
              "status": 401,
              "detail": "Responsável não encontrado ou inativo.",
              "instance": "/api/v1/auth/me"
            }""";

    public static final String ME_NOT_ACCEPTABLE =
        """
            {
              "code": "REQUEST_NOT_ACCEPTABLE",
              "title": "Requisição não suportada",
              "status": 406,
              "detail": "A requisição não é suportada por este endereço.",
              "instance": "/api/v1/auth/me"
            }""";

    public static final String ME_INTERNAL_ERROR =
        """
            {
              "code": "INTERNAL_ERROR",
              "title": "Erro inesperado",
              "status": 500,
              "detail": "Não foi possível concluir a operação. Tente novamente em instantes.",
              "instance": "/api/v1/auth/me"
            }""";

    private AuthenticationExamples() {
    }
}
