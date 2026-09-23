package io.github.ovyx.identity.presentation.account;

public final class OwnAccountExamples {

    public static final String PASSWORD_VALIDATION_FAILED =
        """
            {
              "code": "VALIDATION_FAILED",
              "title": "Dados inválidos",
              "status": 400,
              "detail": "Dados inválidos.",
              "instance": "/api/v1/me/password",
              "details": {
                "currentPassword": "A senha atual está incorreta.",
                "newPassword": "A senha deve ter ao menos 12 caracteres. A senha deve conter ao menos um dígito."
              }
            }""";

    public static final String PASSWORD_CURRENT_MISSING =
        """
            {
              "code": "VALIDATION_FAILED",
              "title": "Dados inválidos",
              "status": 400,
              "detail": "Dados inválidos.",
              "instance": "/api/v1/me/password",
              "details": {
                "currentPassword": "Informe a senha atual.",
                "newPassword": "Informe a senha."
              }
            }""";

    public static final String PASSWORD_UNAUTHENTICATED =
        """
            {
              "code": "UNAUTHENTICATED",
              "title": "Não autenticado",
              "status": 401,
              "detail": "Sua sessão expirou. Entre novamente para continuar.",
              "instance": "/api/v1/me/password"
            }""";

    public static final String PASSWORD_CARETAKER_UNAVAILABLE =
        """
            {
              "code": "CARETAKER_UNAVAILABLE",
              "title": "Não autenticado",
              "status": 401,
              "detail": "Responsável não encontrado ou inativo.",
              "instance": "/api/v1/me/password"
            }""";

    public static final String PASSWORD_CSRF_TOKEN_INVALID =
        """
            {
              "code": "CSRF_TOKEN_INVALID",
              "title": "Proteção da requisição ausente",
              "status": 403,
              "detail": "O token de proteção da requisição está ausente ou expirou. Recarregue a página e tente novamente.",
              "instance": "/api/v1/me/password"
            }""";

    public static final String PASSWORD_UNSUPPORTED_MEDIA_TYPE =
        """
            {
              "code": "REQUEST_NOT_ACCEPTABLE",
              "title": "Requisição não suportada",
              "status": 415,
              "detail": "A requisição não é suportada por este endereço.",
              "instance": "/api/v1/me/password"
            }""";

    public static final String PASSWORD_INTERNAL_ERROR =
        """
            {
              "code": "INTERNAL_ERROR",
              "title": "Erro inesperado",
              "status": 500,
              "detail": "Não foi possível concluir a operação. Tente novamente em instantes.",
              "instance": "/api/v1/me/password"
            }""";

    private OwnAccountExamples() {
    }
}
