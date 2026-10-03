package io.github.ovyx.identity.presentation.recovery;

/**
 * Exemplos reais da documentacao da recuperacao de senha (feature 012), iguais ao que a API responde.
 */
final class PasswordRecoveryExamples {

    private PasswordRecoveryExamples() {}

    static final String REQUEST = """
            {
              "email": "marina.costa@ovyx.com.br"
            }""";

    static final String EMAIL_REQUIRED = """
            {
              "code": "VALIDATION_FAILED",
              "title": "Dados inválidos",
              "status": 400,
              "detail": "Dados inválidos.",
              "instance": "/api/v1/auth/password-recovery",
              "details": {
                "email": "Informe o e-mail."
              }
            }""";

    static final String EMAIL_MALFORMED = """
            {
              "code": "VALIDATION_FAILED",
              "title": "Dados inválidos",
              "status": 400,
              "detail": "Dados inválidos.",
              "instance": "/api/v1/auth/password-recovery",
              "details": {
                "email": "Informe um e-mail em formato válido."
              }
            }""";

    static final String REQUEST_CSRF_TOKEN_INVALID = """
            {
              "code": "CSRF_TOKEN_INVALID",
              "title": "Proteção da requisição ausente",
              "status": 403,
              "detail": "O token de proteção da requisição está ausente ou expirou. Recarregue a página e tente novamente.",
              "instance": "/api/v1/auth/password-recovery"
            }""";

    static final String REQUEST_UNSUPPORTED_MEDIA_TYPE = """
            {
              "code": "REQUEST_NOT_ACCEPTABLE",
              "title": "Requisição não suportada",
              "status": 415,
              "detail": "A requisição não é suportada por este endereço.",
              "instance": "/api/v1/auth/password-recovery"
            }""";

    static final String VERIFICATION = """
            {
              "token": "3q2-7wq9Xk1vF0bQm8ZsY4tLr6NcHe2JpWdUaGo5iKx"
            }""";

    static final String VERIFICATION_LINK_INVALID = """
            {
              "code": "RECOVERY_LINK_INVALID",
              "title": "Dados inválidos",
              "status": 400,
              "detail": "Este link de recuperação não vale mais. Peça um novo na tela de entrada.",
              "instance": "/api/v1/auth/password-recovery/verification"
            }""";

    static final String VERIFICATION_CSRF_TOKEN_INVALID = """
            {
              "code": "CSRF_TOKEN_INVALID",
              "title": "Proteção da requisição ausente",
              "status": 403,
              "detail": "O token de proteção da requisição está ausente ou expirou. Recarregue a página e tente novamente.",
              "instance": "/api/v1/auth/password-recovery/verification"
            }""";

    static final String VERIFICATION_UNSUPPORTED_MEDIA_TYPE = """
            {
              "code": "REQUEST_NOT_ACCEPTABLE",
              "title": "Requisição não suportada",
              "status": 415,
              "detail": "A requisição não é suportada por este endereço.",
              "instance": "/api/v1/auth/password-recovery/verification"
            }""";

    static final String VERIFICATION_INTERNAL_ERROR = """
            {
              "code": "INTERNAL_ERROR",
              "title": "Erro inesperado",
              "status": 500,
              "detail": "Não foi possível concluir a operação. Tente novamente em instantes.",
              "instance": "/api/v1/auth/password-recovery/verification"
            }""";

    static final String RESET = """
            {
              "token": "3q2-7wq9Xk1vF0bQm8ZsY4tLr6NcHe2JpWdUaGo5iKx",
              "newPassword": "PosturaAviario2027"
            }""";

    static final String RESET_POLICY = """
            {
              "code": "VALIDATION_FAILED",
              "title": "Dados inválidos",
              "status": 400,
              "detail": "Dados inválidos.",
              "instance": "/api/v1/auth/password-reset",
              "details": {
                "newPassword": "A senha deve ter ao menos 12 caracteres. A senha deve conter ao menos um dígito."
              }
            }""";

    static final String RESET_LINK_INVALID = """
            {
              "code": "RECOVERY_LINK_INVALID",
              "title": "Dados inválidos",
              "status": 400,
              "detail": "Este link de recuperação não vale mais. Peça um novo na tela de entrada.",
              "instance": "/api/v1/auth/password-reset"
            }""";

    static final String RESET_CSRF_TOKEN_INVALID = """
            {
              "code": "CSRF_TOKEN_INVALID",
              "title": "Proteção da requisição ausente",
              "status": 403,
              "detail": "O token de proteção da requisição está ausente ou expirou. Recarregue a página e tente novamente.",
              "instance": "/api/v1/auth/password-reset"
            }""";

    static final String RESET_UNSUPPORTED_MEDIA_TYPE = """
            {
              "code": "REQUEST_NOT_ACCEPTABLE",
              "title": "Requisição não suportada",
              "status": 415,
              "detail": "A requisição não é suportada por este endereço.",
              "instance": "/api/v1/auth/password-reset"
            }""";

    static final String RESET_INTERNAL_ERROR = """
            {
              "code": "INTERNAL_ERROR",
              "title": "Erro inesperado",
              "status": 500,
              "detail": "Não foi possível concluir a operação. Tente novamente em instantes.",
              "instance": "/api/v1/auth/password-reset"
            }""";

    static final String REQUEST_INTERNAL_ERROR = """
            {
              "code": "INTERNAL_ERROR",
              "title": "Erro inesperado",
              "status": 500,
              "detail": "Não foi possível concluir a operação. Tente novamente em instantes.",
              "instance": "/api/v1/auth/password-recovery"
            }""";
}
