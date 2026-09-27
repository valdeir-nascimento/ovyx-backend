package io.github.ovyx.farm.presentation.cage;

/**
 * Exemplos publicados das operacoes de gaiola.
 *
 * <p>Cada corpo de erro para em {@code "instance": "}, e cada operacao o completa com o proprio
 * caminho, como nos exemplos de setor. Os textos sao os que o sistema devolve de fato.
 */
public final class CageExamples {

    private static final String CAGES = "/api/v1/sectors/3f6c2b1a-8d4e-4c7f-9a2b-1e5d7c9f0a11/cages";
    private static final String CAGE = CAGES + "/9d2e4f6a-1b3c-4d5e-8f7a-2b4c6d8e0f44";
    /** O identificador que nao aponta gaiola nenhuma aparece no {@code instance} como veio. */
    private static final String UNKNOWN_CAGE = CAGES + "/b-07";
    private static final String UNKNOWN_SECTOR_CAGES = "/api/v1/sectors/galpao-9/cages";
    private static final String END = "\"\n}";

    // ------------------------------------------------------------------------ requisicao

    public static final String NEW_CAGE =
            """
            {
              "battery": "B",
              "number": 13,
              "birdCount": 50
            }""";

    public static final String CORRECTED_BIRDS =
            """
            {
              "battery": "B",
              "number": 7,
              "birdCount": 48
            }""";

    // ------------------------------------------------------------------------ sucesso

    public static final String BATTERY_B_PAGE =
            """
            {
              "content": [
                {
                  "id": "9d2e4f6a-1b3c-4d5e-8f7a-2b4c6d8e0f44",
                  "sectorId": "3f6c2b1a-8d4e-4c7f-9a2b-1e5d7c9f0a11",
                  "code": "B-07",
                  "battery": "B",
                  "number": 7,
                  "birdCount": 50,
                  "status": "ACTIVE"
                }
              ],
              "page": 0,
              "size": 20,
              "totalElements": 1,
              "totalPages": 1
            }""";

    public static final String REGISTERED =
            """
            {
              "id": "0e3f5a7b-2c4d-4e6f-9a8b-3c5d7e9f1a55",
              "sectorId": "3f6c2b1a-8d4e-4c7f-9a2b-1e5d7c9f0a11",
              "code": "B-13",
              "battery": "B",
              "number": 13,
              "birdCount": 50,
              "status": "ACTIVE",
              "createdAt": "2026-09-25T13:10:42Z",
              "updatedAt": "2026-09-25T13:10:42Z"
            }""";

    public static final String DETAIL =
            """
            {
              "id": "9d2e4f6a-1b3c-4d5e-8f7a-2b4c6d8e0f44",
              "sectorId": "3f6c2b1a-8d4e-4c7f-9a2b-1e5d7c9f0a11",
              "code": "B-07",
              "battery": "B",
              "number": 7,
              "birdCount": 50,
              "status": "ACTIVE",
              "createdAt": "2026-09-21T08:30:00Z",
              "updatedAt": "2026-09-24T17:42:05Z"
            }""";

    public static final String UPDATED =
            """
            {
              "id": "9d2e4f6a-1b3c-4d5e-8f7a-2b4c6d8e0f44",
              "sectorId": "3f6c2b1a-8d4e-4c7f-9a2b-1e5d7c9f0a11",
              "code": "B-07",
              "battery": "B",
              "number": 7,
              "birdCount": 48,
              "status": "ACTIVE",
              "createdAt": "2026-09-21T08:30:00Z",
              "updatedAt": "2026-09-25T09:20:13Z"
            }""";

    public static final String DEACTIVATED =
            """
            {
              "id": "0e3f5a7b-2c4d-4e6f-9a8b-3c5d7e9f1a55",
              "sectorId": "3f6c2b1a-8d4e-4c7f-9a2b-1e5d7c9f0a11",
              "code": "A-02",
              "battery": "A",
              "number": 2,
              "birdCount": 48,
              "status": "INACTIVE",
              "createdAt": "2026-09-21T08:30:00Z",
              "updatedAt": "2026-09-25T14:10:02Z"
            }""";

    public static final String REACTIVATED =
            """
            {
              "id": "0e3f5a7b-2c4d-4e6f-9a8b-3c5d7e9f1a55",
              "sectorId": "3f6c2b1a-8d4e-4c7f-9a2b-1e5d7c9f0a11",
              "code": "A-02",
              "battery": "A",
              "number": 2,
              "birdCount": 48,
              "status": "ACTIVE",
              "createdAt": "2026-09-21T08:30:00Z",
              "updatedAt": "2026-09-25T15:02:40Z"
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
                "battery": "Informe a bateria, com até 3 letras ou dígitos.",
                "number": "O número da gaiola deve ficar entre 1 e 999.",
                "birdCount": "A quantidade de aves deve ficar entre 0 e 1.000."
              },
              "instance": \"""";

    private static final String BIRDS_NOT_INTEGER =
            """
            {
              "code": "VALIDATION_FAILED",
              "title": "Dados inválidos",
              "status": 400,
              "detail": "Dados inválidos.",
              "details": {
                "birdCount": "A quantidade de aves deve ser um número inteiro."
              },
              "instance": \"""";

    private static final String PAGE_INVALID =
            """
            {
              "code": "VALIDATION_FAILED",
              "title": "Dados inválidos",
              "status": 400,
              "detail": "A requisição contém campos inválidos.",
              "details": {
                "size": "O tamanho da página deve estar entre 1 e 100."
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

    private static final String SECTOR_NOT_FOUND =
            """
            {
              "code": "SECTOR_NOT_FOUND",
              "title": "Não encontrado",
              "status": 404,
              "detail": "Setor não encontrado.",
              "instance": \"""";

    private static final String CAGE_NOT_FOUND =
            """
            {
              "code": "CAGE_NOT_FOUND",
              "title": "Não encontrado",
              "status": 404,
              "detail": "Gaiola não encontrada.",
              "instance": \"""";

    private static final String NOT_ACCEPTABLE =
            """
            {
              "code": "REQUEST_NOT_ACCEPTABLE",
              "title": "Requisição não suportada",
              "status": 406,
              "detail": "A requisição não é suportada por este endereço.",
              "instance": \"""";

    private static final String CAGE_EXISTS =
            """
            {
              "code": "CAGE_ALREADY_EXISTS",
              "title": "Operação recusada",
              "status": 409,
              "detail": "Já existe uma gaiola ativa B-07 neste setor.",
              "details": {
                "battery": "Já existe uma gaiola ativa B-07 neste setor.",
                "number": "Já existe uma gaiola ativa B-07 neste setor."
              },
              "instance": \"""";

    private static final String SECTOR_INACTIVE =
            """
            {
              "code": "SECTOR_INACTIVE",
              "title": "Operação recusada",
              "status": 409,
              "detail": "O setor está inativo. Reative o setor antes de mexer nas gaiolas dele.",
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

    // ---------------------------------------------- GET /api/v1/sectors/{sectorId}/cages

    public static final String SEARCH_PAGE_INVALID = PAGE_INVALID + CAGES + END;
    public static final String SEARCH_STATUS_INVALID = STATUS_INVALID + CAGES + END;
    public static final String SEARCH_UNAUTHENTICATED = UNAUTHENTICATED + CAGES + END;
    public static final String SEARCH_PASSWORD_CHANGE_REQUIRED = PASSWORD_CHANGE_REQUIRED + CAGES + END;
    public static final String SEARCH_SECTOR_NOT_FOUND = SECTOR_NOT_FOUND + UNKNOWN_SECTOR_CAGES + END;
    public static final String SEARCH_NOT_ACCEPTABLE = NOT_ACCEPTABLE + CAGES + END;
    public static final String SEARCH_INTERNAL_ERROR = INTERNAL_ERROR + CAGES + END;

    // --------------------------------------------- POST /api/v1/sectors/{sectorId}/cages

    public static final String REGISTER_FIELDS_INVALID = FIELDS_INVALID + CAGES + END;
    public static final String REGISTER_BIRDS_NOT_INTEGER = BIRDS_NOT_INTEGER + CAGES + END;
    public static final String REGISTER_UNREADABLE_BODY = UNREADABLE_BODY + CAGES + END;
    public static final String REGISTER_UNAUTHENTICATED = UNAUTHENTICATED + CAGES + END;
    public static final String REGISTER_FORBIDDEN = FORBIDDEN + CAGES + END;
    public static final String REGISTER_CSRF_TOKEN_INVALID = CSRF_TOKEN_INVALID + CAGES + END;
    public static final String REGISTER_PASSWORD_CHANGE_REQUIRED = PASSWORD_CHANGE_REQUIRED + CAGES + END;
    public static final String REGISTER_SECTOR_NOT_FOUND = SECTOR_NOT_FOUND + UNKNOWN_SECTOR_CAGES + END;
    public static final String REGISTER_NOT_ACCEPTABLE = NOT_ACCEPTABLE + CAGES + END;
    public static final String REGISTER_CAGE_EXISTS = CAGE_EXISTS + CAGES + END;
    public static final String REGISTER_SECTOR_INACTIVE = SECTOR_INACTIVE + CAGES + END;
    public static final String REGISTER_UNSUPPORTED_MEDIA_TYPE = UNSUPPORTED_MEDIA_TYPE + CAGES + END;
    public static final String REGISTER_INTERNAL_ERROR = INTERNAL_ERROR + CAGES + END;

    // ------------------------------------- GET /api/v1/sectors/{sectorId}/cages/{cageId}

    public static final String FIND_UNAUTHENTICATED = UNAUTHENTICATED + CAGE + END;
    public static final String FIND_PASSWORD_CHANGE_REQUIRED = PASSWORD_CHANGE_REQUIRED + CAGE + END;
    public static final String FIND_CAGE_NOT_FOUND = CAGE_NOT_FOUND + UNKNOWN_CAGE + END;
    public static final String FIND_NOT_ACCEPTABLE = NOT_ACCEPTABLE + CAGE + END;
    public static final String FIND_INTERNAL_ERROR = INTERNAL_ERROR + CAGE + END;

    // ------------------------------------- PUT /api/v1/sectors/{sectorId}/cages/{cageId}

    public static final String UPDATE_FIELDS_INVALID = FIELDS_INVALID + CAGE + END;
    public static final String UPDATE_UNREADABLE_BODY = UNREADABLE_BODY + CAGE + END;
    public static final String UPDATE_UNAUTHENTICATED = UNAUTHENTICATED + CAGE + END;
    public static final String UPDATE_FORBIDDEN = FORBIDDEN + CAGE + END;
    public static final String UPDATE_CSRF_TOKEN_INVALID = CSRF_TOKEN_INVALID + CAGE + END;
    public static final String UPDATE_PASSWORD_CHANGE_REQUIRED = PASSWORD_CHANGE_REQUIRED + CAGE + END;
    public static final String UPDATE_CAGE_NOT_FOUND = CAGE_NOT_FOUND + UNKNOWN_CAGE + END;
    public static final String UPDATE_NOT_ACCEPTABLE = NOT_ACCEPTABLE + CAGE + END;
    public static final String UPDATE_CAGE_EXISTS = CAGE_EXISTS + CAGE + END;
    public static final String UPDATE_SECTOR_INACTIVE = SECTOR_INACTIVE + CAGE + END;
    public static final String UPDATE_UNSUPPORTED_MEDIA_TYPE = UNSUPPORTED_MEDIA_TYPE + CAGE + END;
    public static final String UPDATE_INTERNAL_ERROR = INTERNAL_ERROR + CAGE + END;

    // ------------------------- POST /api/v1/sectors/{sectorId}/cages/{cageId}/deactivation e reactivation

    private static final String DEACTIVATION = CAGE + "/deactivation";
    private static final String REACTIVATION = CAGE + "/reactivation";

    public static final String DEACTIVATE_UNAUTHENTICATED = UNAUTHENTICATED + DEACTIVATION + END;
    public static final String DEACTIVATE_FORBIDDEN = FORBIDDEN + DEACTIVATION + END;
    public static final String DEACTIVATE_CSRF_TOKEN_INVALID = CSRF_TOKEN_INVALID + DEACTIVATION + END;
    public static final String DEACTIVATE_PASSWORD_CHANGE_REQUIRED = PASSWORD_CHANGE_REQUIRED + DEACTIVATION + END;
    public static final String DEACTIVATE_CAGE_NOT_FOUND = CAGE_NOT_FOUND + UNKNOWN_CAGE + "/deactivation" + END;
    public static final String DEACTIVATE_NOT_ACCEPTABLE = NOT_ACCEPTABLE + DEACTIVATION + END;
    public static final String DEACTIVATE_INTERNAL_ERROR = INTERNAL_ERROR + DEACTIVATION + END;

    public static final String REACTIVATE_UNAUTHENTICATED = UNAUTHENTICATED + REACTIVATION + END;
    public static final String REACTIVATE_FORBIDDEN = FORBIDDEN + REACTIVATION + END;
    public static final String REACTIVATE_CSRF_TOKEN_INVALID = CSRF_TOKEN_INVALID + REACTIVATION + END;
    public static final String REACTIVATE_PASSWORD_CHANGE_REQUIRED = PASSWORD_CHANGE_REQUIRED + REACTIVATION + END;
    public static final String REACTIVATE_CAGE_NOT_FOUND = CAGE_NOT_FOUND + UNKNOWN_CAGE + "/reactivation" + END;
    public static final String REACTIVATE_NOT_ACCEPTABLE = NOT_ACCEPTABLE + REACTIVATION + END;
    public static final String REACTIVATE_CAGE_EXISTS = CAGE_EXISTS + REACTIVATION + END;
    public static final String REACTIVATE_SECTOR_INACTIVE = SECTOR_INACTIVE + REACTIVATION + END;
    public static final String REACTIVATE_INTERNAL_ERROR = INTERNAL_ERROR + REACTIVATION + END;

    private CageExamples() {}
}
