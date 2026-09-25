package io.github.ovyx.farm.presentation.sector;

/**
 * Exemplos publicados das operacoes de setor.
 *
 * <p>Cada corpo de erro para em {@code "instance": "}, e cada operacao o completa com o proprio
 * caminho, como nos exemplos de responsaveis. Os textos sao os que o sistema devolve de fato.
 */
public final class SectorExamples {

    private static final String COLLECTION = "/api/v1/sectors";
    private static final String ITEM = COLLECTION + "/3f6c2b1a-8d4e-4c7f-9a2b-1e5d7c9f0a11";
    /** O identificador que nao aponta setor nenhum aparece no {@code instance} como veio. */
    private static final String UNKNOWN_ITEM = COLLECTION + "/galpao-9";
    private static final String END = "\"\n}";

    // ------------------------------------------------------------------------ requisicao

    public static final String NEW_SECTOR =
            """
            {
              "name": "Codornas — Galpão 4",
              "description": "Codornas japonesas em postura, baterias A e B"
            }""";

    public static final String EDIT =
            """
            {
              "name": "Codornas — Galpão 1 (norte)",
              "description": "Codornas japonesas em postura, baterias A a D"
            }""";

    // ------------------------------------------------------------------------ sucesso

    public static final String ACTIVE_SECTORS =
            """
            [
              {
                "id": "3f6c2b1a-8d4e-4c7f-9a2b-1e5d7c9f0a11",
                "name": "Codornas — Galpão 1",
                "description": "Codornas japonesas em postura, baterias A a D",
                "status": "ACTIVE",
                "activeCageCount": 48,
                "birdCount": 2400
              },
              {
                "id": "7a1b9c3d-2e4f-4a6b-8c0d-5e7f9a1b3c22",
                "name": "Poedeiras brancas — Galpão 2",
                "description": "Linhagem Hy-Line W-36 em gaiolas convencionais",
                "status": "ACTIVE",
                "activeCageCount": 24,
                "birdCount": 1200
              }
            ]""";

    public static final String REGISTERED =
            """
            {
              "id": "5c8d2e4f-6a1b-4c3d-9e7f-0a2b4c6d8e33",
              "name": "Codornas — Galpão 4",
              "description": "Codornas japonesas em postura, baterias A e B",
              "status": "ACTIVE",
              "activeCageCount": 0,
              "birdCount": 0,
              "batteries": [],
              "createdAt": "2026-09-25T13:02:11Z",
              "updatedAt": "2026-09-25T13:02:11Z"
            }""";

    public static final String DETAIL =
            """
            {
              "id": "3f6c2b1a-8d4e-4c7f-9a2b-1e5d7c9f0a11",
              "name": "Codornas — Galpão 1",
              "description": "Codornas japonesas em postura, baterias A a D",
              "status": "ACTIVE",
              "activeCageCount": 48,
              "birdCount": 2400,
              "batteries": ["A", "B", "C", "D"],
              "createdAt": "2026-09-20T10:15:00Z",
              "updatedAt": "2026-09-24T17:40:12Z"
            }""";

    public static final String UPDATED =
            """
            {
              "id": "3f6c2b1a-8d4e-4c7f-9a2b-1e5d7c9f0a11",
              "name": "Codornas — Galpão 1 (norte)",
              "description": "Codornas japonesas em postura, baterias A a D",
              "status": "ACTIVE",
              "activeCageCount": 48,
              "birdCount": 2400,
              "batteries": ["A", "B", "C", "D"],
              "createdAt": "2026-09-20T10:15:00Z",
              "updatedAt": "2026-09-25T09:12:40Z"
            }""";

    public static final String DEACTIVATED =
            """
            {
              "id": "3f6c2b1a-8d4e-4c7f-9a2b-1e5d7c9f0a11",
              "name": "Codornas — Galpão 1",
              "description": "Codornas japonesas em postura, baterias A a D",
              "status": "INACTIVE",
              "activeCageCount": 0,
              "birdCount": 0,
              "batteries": ["A", "B", "C", "D"],
              "createdAt": "2026-09-20T10:15:00Z",
              "updatedAt": "2026-09-25T14:03:51Z"
            }""";

    public static final String REACTIVATED =
            """
            {
              "id": "3f6c2b1a-8d4e-4c7f-9a2b-1e5d7c9f0a11",
              "name": "Codornas — Galpão 1",
              "description": "Codornas japonesas em postura, baterias A a D",
              "status": "ACTIVE",
              "activeCageCount": 48,
              "birdCount": 2400,
              "batteries": ["A", "B", "C", "D"],
              "createdAt": "2026-09-20T10:15:00Z",
              "updatedAt": "2026-09-25T15:20:07Z"
            }""";

    // ------------------------------------------------ corpos de erro, sem o instance

    private static final String FIELDS_INVALID =
            """
            {
              "code": "VALIDATION_FAILED",
              "title": "Dados inválidos",
              "status": 400,
              "detail": "Dados inválidos.",
              "details": {
                "name": "O nome do setor deve ter ao menos 2 caracteres.",
                "description": "A descrição deve ter no máximo 500 caracteres."
              },
              "instance": \"""";

    private static final String NAME_MISSING =
            """
            {
              "code": "VALIDATION_FAILED",
              "title": "Dados inválidos",
              "status": 400,
              "detail": "Dados inválidos.",
              "details": {
                "name": "Informe o nome do setor."
              },
              "instance": \"""";

    private static final String STATUS_INVALID =
            """
            {
              "code": "VALIDATION_FAILED",
              "title": "Dados inválidos",
              "status": 400,
              "detail": "Valor inválido para o parâmetro 'status'.",
              "details": {
                "parameter": "status"
              },
              "instance": \"""";

    private static final String UNREADABLE_BODY =
            """
            {
              "code": "REQUEST_NOT_ACCEPTABLE",
              "title": "Requisição inválida",
              "status": 400,
              "detail": "O corpo da requisição não pôde ser lido.",
              "instance": \"""";

    private static final String UNAUTHENTICATED =
            """
            {
              "code": "UNAUTHENTICATED",
              "title": "Não autenticado",
              "status": 401,
              "detail": "Sua sessão expirou. Entre novamente para continuar.",
              "instance": \"""";

    private static final String FORBIDDEN =
            """
            {
              "code": "FORBIDDEN",
              "title": "Acesso negado",
              "status": 403,
              "detail": "Você não tem permissão para executar esta operação.",
              "instance": \"""";

    private static final String CSRF_TOKEN_INVALID =
            """
            {
              "code": "CSRF_TOKEN_INVALID",
              "title": "Proteção da requisição ausente",
              "status": 403,
              "detail": "O token de proteção da requisição está ausente ou expirou. Recarregue a página e tente novamente.",
              "instance": \"""";

    private static final String PASSWORD_CHANGE_REQUIRED =
            """
            {
              "code": "PASSWORD_CHANGE_REQUIRED",
              "title": "Troca de senha obrigatória",
              "status": 403,
              "detail": "É necessário trocar a senha antes de executar qualquer outra operação.",
              "instance": \"""";

    private static final String NOT_FOUND =
            """
            {
              "code": "SECTOR_NOT_FOUND",
              "title": "Não encontrado",
              "status": 404,
              "detail": "Setor não encontrado.",
              "instance": \"""";

    private static final String NOT_ACCEPTABLE =
            """
            {
              "code": "REQUEST_NOT_ACCEPTABLE",
              "title": "Requisição não suportada",
              "status": 406,
              "detail": "A requisição não é suportada por este endereço.",
              "instance": \"""";

    private static final String NAME_IN_USE =
            """
            {
              "code": "SECTOR_NAME_IN_USE",
              "title": "Operação recusada",
              "status": 409,
              "detail": "Já existe um setor ativo com este nome.",
              "details": {
                "name": "Já existe um setor ativo com este nome."
              },
              "instance": \"""";

    private static final String UNSUPPORTED_MEDIA_TYPE =
            """
            {
              "code": "REQUEST_NOT_ACCEPTABLE",
              "title": "Requisição não suportada",
              "status": 415,
              "detail": "A requisição não é suportada por este endereço.",
              "instance": \"""";

    private static final String INTERNAL_ERROR =
            """
            {
              "code": "INTERNAL_ERROR",
              "title": "Erro inesperado",
              "status": 500,
              "detail": "Não foi possível concluir a operação. Tente novamente em instantes.",
              "instance": \"""";

    // ------------------------------------------------------------ GET /api/v1/sectors

    public static final String LIST_STATUS_INVALID = STATUS_INVALID + COLLECTION + END;
    public static final String LIST_UNAUTHENTICATED = UNAUTHENTICATED + COLLECTION + END;
    public static final String LIST_PASSWORD_CHANGE_REQUIRED = PASSWORD_CHANGE_REQUIRED + COLLECTION + END;
    public static final String LIST_NOT_ACCEPTABLE = NOT_ACCEPTABLE + COLLECTION + END;
    public static final String LIST_INTERNAL_ERROR = INTERNAL_ERROR + COLLECTION + END;

    // ----------------------------------------------------------- POST /api/v1/sectors

    public static final String REGISTER_FIELDS_INVALID = FIELDS_INVALID + COLLECTION + END;
    public static final String REGISTER_UNREADABLE_BODY = UNREADABLE_BODY + COLLECTION + END;
    public static final String REGISTER_UNAUTHENTICATED = UNAUTHENTICATED + COLLECTION + END;
    public static final String REGISTER_FORBIDDEN = FORBIDDEN + COLLECTION + END;
    public static final String REGISTER_CSRF_TOKEN_INVALID = CSRF_TOKEN_INVALID + COLLECTION + END;
    public static final String REGISTER_PASSWORD_CHANGE_REQUIRED = PASSWORD_CHANGE_REQUIRED + COLLECTION + END;
    public static final String REGISTER_NOT_ACCEPTABLE = NOT_ACCEPTABLE + COLLECTION + END;
    public static final String REGISTER_NAME_IN_USE = NAME_IN_USE + COLLECTION + END;
    public static final String REGISTER_UNSUPPORTED_MEDIA_TYPE = UNSUPPORTED_MEDIA_TYPE + COLLECTION + END;
    public static final String REGISTER_INTERNAL_ERROR = INTERNAL_ERROR + COLLECTION + END;

    // ------------------------------------------------ GET /api/v1/sectors/{sectorId}

    public static final String FIND_UNAUTHENTICATED = UNAUTHENTICATED + ITEM + END;
    public static final String FIND_PASSWORD_CHANGE_REQUIRED = PASSWORD_CHANGE_REQUIRED + ITEM + END;
    public static final String FIND_NOT_FOUND = NOT_FOUND + UNKNOWN_ITEM + END;
    public static final String FIND_NOT_ACCEPTABLE = NOT_ACCEPTABLE + ITEM + END;
    public static final String FIND_INTERNAL_ERROR = INTERNAL_ERROR + ITEM + END;

    // ------------------------------------------------ PUT /api/v1/sectors/{sectorId}

    public static final String UPDATE_NAME_MISSING = NAME_MISSING + ITEM + END;
    public static final String UPDATE_UNREADABLE_BODY = UNREADABLE_BODY + ITEM + END;
    public static final String UPDATE_UNAUTHENTICATED = UNAUTHENTICATED + ITEM + END;
    public static final String UPDATE_FORBIDDEN = FORBIDDEN + ITEM + END;
    public static final String UPDATE_CSRF_TOKEN_INVALID = CSRF_TOKEN_INVALID + ITEM + END;
    public static final String UPDATE_PASSWORD_CHANGE_REQUIRED = PASSWORD_CHANGE_REQUIRED + ITEM + END;
    public static final String UPDATE_NOT_FOUND = NOT_FOUND + UNKNOWN_ITEM + END;
    public static final String UPDATE_NOT_ACCEPTABLE = NOT_ACCEPTABLE + ITEM + END;
    public static final String UPDATE_NAME_IN_USE = NAME_IN_USE + ITEM + END;
    public static final String UPDATE_UNSUPPORTED_MEDIA_TYPE = UNSUPPORTED_MEDIA_TYPE + ITEM + END;
    public static final String UPDATE_INTERNAL_ERROR = INTERNAL_ERROR + ITEM + END;

    // ------------------------------------ POST /api/v1/sectors/{sectorId}/deactivation e reactivation

    private static final String DEACTIVATION = ITEM + "/deactivation";
    private static final String REACTIVATION = ITEM + "/reactivation";

    public static final String DEACTIVATE_UNAUTHENTICATED = UNAUTHENTICATED + DEACTIVATION + END;
    public static final String DEACTIVATE_FORBIDDEN = FORBIDDEN + DEACTIVATION + END;
    public static final String DEACTIVATE_CSRF_TOKEN_INVALID = CSRF_TOKEN_INVALID + DEACTIVATION + END;
    public static final String DEACTIVATE_PASSWORD_CHANGE_REQUIRED = PASSWORD_CHANGE_REQUIRED + DEACTIVATION + END;
    public static final String DEACTIVATE_NOT_FOUND = NOT_FOUND + UNKNOWN_ITEM + "/deactivation" + END;
    public static final String DEACTIVATE_NOT_ACCEPTABLE = NOT_ACCEPTABLE + DEACTIVATION + END;
    public static final String DEACTIVATE_INTERNAL_ERROR = INTERNAL_ERROR + DEACTIVATION + END;

    public static final String REACTIVATE_UNAUTHENTICATED = UNAUTHENTICATED + REACTIVATION + END;
    public static final String REACTIVATE_FORBIDDEN = FORBIDDEN + REACTIVATION + END;
    public static final String REACTIVATE_CSRF_TOKEN_INVALID = CSRF_TOKEN_INVALID + REACTIVATION + END;
    public static final String REACTIVATE_PASSWORD_CHANGE_REQUIRED = PASSWORD_CHANGE_REQUIRED + REACTIVATION + END;
    public static final String REACTIVATE_NOT_FOUND = NOT_FOUND + UNKNOWN_ITEM + "/reactivation" + END;
    public static final String REACTIVATE_NOT_ACCEPTABLE = NOT_ACCEPTABLE + REACTIVATION + END;
    public static final String REACTIVATE_NAME_IN_USE = NAME_IN_USE + REACTIVATION + END;
    public static final String REACTIVATE_INTERNAL_ERROR = INTERNAL_ERROR + REACTIVATION + END;

    private SectorExamples() {}
}
