package io.github.ovyx.identity.presentation.caretaker;

/**
 * Exemplos publicados das operacoes de responsaveis.
 *
 * <p>Cada corpo de erro aqui para em {@code "instance": "}, e cada operacao o completa com o
 * proprio caminho. Concatenar constantes continua sendo constante — cabe na anotacao — e nenhum
 * exemplo consegue mostrar o caminho de outro endpoint, o defeito que a T212 corrigiu quando os
 * exemplos eram compartilhados com o {@code instance} fixo.
 *
 * <p>Os textos sao os que o sistema devolve de fato: os das recusas do dominio, os da borda de
 * seguranca e os do tratamento global.
 */
public final class CaretakerExamples {

    private static final String COLLECTION = "/api/v1/caretakers";
    private static final String ITEM = "/api/v1/caretakers/9f8e7d6c-5b4a-4938-2716-0f1e2d3c4b5a";
    private static final String DEACTIVATION = ITEM + "/deactivation";
    /** O identificador inválido aparece no {@code instance} como a pessoa o mandou. */
    private static final String INVALID_ITEM = "/api/v1/caretakers/lote-12-galpao-3";
    private static final String END = "\"\n}";

    // ------------------------------------------------------------------------ sucesso

    public static final String DETAIL =
        """
            {
              "id": "9f8e7d6c-5b4a-4938-2716-0f1e2d3c4b5a",
              "fullName": "João Pereira de Souza",
              "cpf": "35392919707",
              "email": "joao.pereira@ovyx.com.br",
              "mobilePhone": "91991234567",
              "role": "USER",
              "status": "ACTIVE",
              "createdAt": "2026-09-18T13:45:10Z",
              "updatedAt": "2026-09-18T13:45:10Z"
            }""";

    public static final String UPDATED_DETAIL =
        """
            {
              "id": "9f8e7d6c-5b4a-4938-2716-0f1e2d3c4b5a",
              "fullName": "João Pereira de Souza",
              "cpf": "35392919707",
              "email": "joao.souza@ovyx.com.br",
              "mobilePhone": "91991234567",
              "role": "ADMINISTRATOR",
              "status": "ACTIVE",
              "createdAt": "2026-09-18T13:45:10Z",
              "updatedAt": "2026-09-18T16:02:44Z"
            }""";

    public static final String DEACTIVATED_DETAIL =
        """
            {
              "id": "9f8e7d6c-5b4a-4938-2716-0f1e2d3c4b5a",
              "fullName": "João Pereira de Souza",
              "cpf": "35392919707",
              "email": "joao.souza@ovyx.com.br",
              "mobilePhone": "91991234567",
              "role": "USER",
              "status": "INACTIVE",
              "createdAt": "2026-09-18T13:45:10Z",
              "updatedAt": "2026-09-18T17:20:03Z"
            }""";

    public static final String PAGE =
        """
            {
              "content": [
                {
                  "id": "9f8e7d6c-5b4a-4938-2716-0f1e2d3c4b5a",
                  "fullName": "João Pereira de Souza",
                  "email": "joao.pereira@ovyx.com.br",
                  "mobilePhone": "91991234567",
                  "role": "USER",
                  "status": "ACTIVE"
                },
                {
                  "id": "7c1f0b2e-3d4a-4f5b-8c9d-0e1f2a3b4c5d",
                  "fullName": "Maria Silva",
                  "email": "maria.silva@ovyx.com.br",
                  "mobilePhone": "91988887777",
                  "role": "USER",
                  "status": "ACTIVE"
                }
              ],
              "page": 0,
              "size": 20,
              "totalElements": 2,
              "totalPages": 1
            }""";

    // ------------------------------------------------ corpos de erro, sem o instance

    private static final String REGISTRATION_INVALID =
        """
            {
              "code": "VALIDATION_FAILED",
              "title": "Dados inválidos",
              "status": 400,
              "detail": "Dados inválidos.",
              "details": {
                "fullName": "Informe o nome completo.",
                "cpf": "CPF inválido.",
                "email": "Informe um e-mail em formato válido.",
                "password": "A senha deve ter ao menos 12 caracteres."
              },
              "instance": \"""";

    private static final String UPDATE_INVALID =
        """
            {
              "code": "VALIDATION_FAILED",
              "title": "Dados inválidos",
              "status": 400,
              "detail": "Dados inválidos.",
              "details": {
                "cpf": "CPF inválido.",
                "mobilePhone": "Informe um celular com DDD, contendo 10 ou 11 dígitos."
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

    private static final String ID_INVALID =
        """
            {
              "code": "VALIDATION_FAILED",
              "title": "Dados inválidos",
              "status": 400,
              "detail": "Valor inválido para o parâmetro 'caretakerId'.",
              "details": {
                "parameter": "caretakerId"
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

    private static final String CARETAKER_UNAVAILABLE =
        """
            {
              "code": "CARETAKER_UNAVAILABLE",
              "title": "Não autenticado",
              "status": 401,
              "detail": "Responsável não encontrado ou inativo.",
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
              "code": "CARETAKER_NOT_FOUND",
              "title": "Não encontrado",
              "status": 404,
              "detail": "Responsável não encontrado.",
              "instance": \"""";

    private static final String NOT_ACCEPTABLE =
        """
            {
              "code": "REQUEST_NOT_ACCEPTABLE",
              "title": "Requisição não suportada",
              "status": 406,
              "detail": "A requisição não é suportada por este endereço.",
              "instance": \"""";

    private static final String CPF_IN_USE =
        """
            {
              "code": "CPF_ALREADY_IN_USE",
              "title": "Operação recusada",
              "status": 409,
              "detail": "Já existe um responsável com este CPF.",
              "details": {
                "cpf": "Já existe um responsável com este CPF."
              },
              "instance": \"""";

    private static final String EMAIL_IN_USE =
        """
            {
              "code": "EMAIL_ALREADY_IN_USE",
              "title": "Operação recusada",
              "status": 409,
              "detail": "Já existe um responsável ativo com este e-mail.",
              "details": {
                "email": "Já existe um responsável ativo com este e-mail."
              },
              "instance": \"""";

    private static final String MOBILE_PHONE_IN_USE =
        """
            {
              "code": "MOBILE_PHONE_ALREADY_IN_USE",
              "title": "Operação recusada",
              "status": 409,
              "detail": "Já existe um responsável ativo com este celular.",
              "details": {
                "mobilePhone": "Já existe um responsável ativo com este celular."
              },
              "instance": \"""";

    private static final String LAST_ADMINISTRATOR_DEMOTED =
        """
            {
              "code": "LAST_ADMINISTRATOR",
              "title": "Operação recusada",
              "status": 409,
              "detail": "Este é o último administrador ativo do sistema.",
              "details": {
                "role": "O sistema precisa de ao menos um administrador ativo."
              },
              "instance": \"""";

    private static final String LAST_ADMINISTRATOR_DEACTIVATED =
        """
            {
              "code": "LAST_ADMINISTRATOR",
              "title": "Operação recusada",
              "status": 409,
              "detail": "Este é o último administrador ativo do sistema.",
              "details": {
                "status": "O sistema precisa de ao menos um administrador ativo."
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

    // ------------------------------------------------------ POST /api/v1/caretakers

    public static final String REGISTER_VALIDATION_FAILED = REGISTRATION_INVALID + COLLECTION + END;
    public static final String REGISTER_UNREADABLE_BODY = UNREADABLE_BODY + COLLECTION + END;
    public static final String REGISTER_UNAUTHENTICATED = UNAUTHENTICATED + COLLECTION + END;
    public static final String REGISTER_CARETAKER_UNAVAILABLE = CARETAKER_UNAVAILABLE + COLLECTION + END;
    public static final String REGISTER_FORBIDDEN = FORBIDDEN + COLLECTION + END;
    public static final String REGISTER_CSRF_TOKEN_INVALID = CSRF_TOKEN_INVALID + COLLECTION + END;
    public static final String REGISTER_PASSWORD_CHANGE_REQUIRED = PASSWORD_CHANGE_REQUIRED + COLLECTION + END;
    public static final String REGISTER_NOT_ACCEPTABLE = NOT_ACCEPTABLE + COLLECTION + END;
    public static final String REGISTER_CPF_ALREADY_IN_USE = CPF_IN_USE + COLLECTION + END;
    public static final String REGISTER_EMAIL_ALREADY_IN_USE = EMAIL_IN_USE + COLLECTION + END;
    public static final String REGISTER_MOBILE_PHONE_ALREADY_IN_USE = MOBILE_PHONE_IN_USE + COLLECTION + END;
    public static final String REGISTER_UNSUPPORTED_MEDIA_TYPE = UNSUPPORTED_MEDIA_TYPE + COLLECTION + END;
    public static final String REGISTER_INTERNAL_ERROR = INTERNAL_ERROR + COLLECTION + END;

    // ------------------------------------------------------- GET /api/v1/caretakers

    public static final String SEARCH_INVALID_PAGE = PAGE_INVALID + COLLECTION + END;
    public static final String SEARCH_UNAUTHENTICATED = UNAUTHENTICATED + COLLECTION + END;
    public static final String SEARCH_CARETAKER_UNAVAILABLE = CARETAKER_UNAVAILABLE + COLLECTION + END;
    public static final String SEARCH_FORBIDDEN = FORBIDDEN + COLLECTION + END;
    public static final String SEARCH_PASSWORD_CHANGE_REQUIRED = PASSWORD_CHANGE_REQUIRED + COLLECTION + END;
    public static final String SEARCH_NOT_ACCEPTABLE = NOT_ACCEPTABLE + COLLECTION + END;
    public static final String SEARCH_INTERNAL_ERROR = INTERNAL_ERROR + COLLECTION + END;

    // ------------------------------------------ GET /api/v1/caretakers/{caretakerId}

    public static final String FIND_INVALID_ID = ID_INVALID + INVALID_ITEM + END;
    public static final String FIND_UNAUTHENTICATED = UNAUTHENTICATED + ITEM + END;
    public static final String FIND_CARETAKER_UNAVAILABLE = CARETAKER_UNAVAILABLE + ITEM + END;
    public static final String FIND_FORBIDDEN = FORBIDDEN + ITEM + END;
    public static final String FIND_PASSWORD_CHANGE_REQUIRED = PASSWORD_CHANGE_REQUIRED + ITEM + END;
    public static final String FIND_NOT_FOUND = NOT_FOUND + ITEM + END;
    public static final String FIND_NOT_ACCEPTABLE = NOT_ACCEPTABLE + ITEM + END;
    public static final String FIND_INTERNAL_ERROR = INTERNAL_ERROR + ITEM + END;

    // ------------------------------------------ PUT /api/v1/caretakers/{caretakerId}

    public static final String UPDATE_VALIDATION_FAILED = UPDATE_INVALID + ITEM + END;
    public static final String UPDATE_INVALID_ID = ID_INVALID + INVALID_ITEM + END;
    public static final String UPDATE_UNREADABLE_BODY = UNREADABLE_BODY + ITEM + END;
    public static final String UPDATE_UNAUTHENTICATED = UNAUTHENTICATED + ITEM + END;
    public static final String UPDATE_CARETAKER_UNAVAILABLE = CARETAKER_UNAVAILABLE + ITEM + END;
    public static final String UPDATE_FORBIDDEN = FORBIDDEN + ITEM + END;
    public static final String UPDATE_CSRF_TOKEN_INVALID = CSRF_TOKEN_INVALID + ITEM + END;
    public static final String UPDATE_PASSWORD_CHANGE_REQUIRED = PASSWORD_CHANGE_REQUIRED + ITEM + END;
    public static final String UPDATE_NOT_FOUND = NOT_FOUND + ITEM + END;
    public static final String UPDATE_NOT_ACCEPTABLE = NOT_ACCEPTABLE + ITEM + END;
    public static final String UPDATE_EMAIL_ALREADY_IN_USE = EMAIL_IN_USE + ITEM + END;
    public static final String UPDATE_LAST_ADMINISTRATOR = LAST_ADMINISTRATOR_DEMOTED + ITEM + END;
    public static final String UPDATE_UNSUPPORTED_MEDIA_TYPE = UNSUPPORTED_MEDIA_TYPE + ITEM + END;
    public static final String UPDATE_INTERNAL_ERROR = INTERNAL_ERROR + ITEM + END;

    // ------------------------ POST /api/v1/caretakers/{caretakerId}/deactivation

    public static final String DEACTIVATE_INVALID_ID = ID_INVALID + INVALID_ITEM + "/deactivation" + END;
    public static final String DEACTIVATE_UNAUTHENTICATED = UNAUTHENTICATED + DEACTIVATION + END;
    public static final String DEACTIVATE_CARETAKER_UNAVAILABLE = CARETAKER_UNAVAILABLE + DEACTIVATION + END;
    public static final String DEACTIVATE_FORBIDDEN = FORBIDDEN + DEACTIVATION + END;
    public static final String DEACTIVATE_CSRF_TOKEN_INVALID = CSRF_TOKEN_INVALID + DEACTIVATION + END;
    public static final String DEACTIVATE_PASSWORD_CHANGE_REQUIRED = PASSWORD_CHANGE_REQUIRED + DEACTIVATION + END;
    public static final String DEACTIVATE_NOT_FOUND = NOT_FOUND + DEACTIVATION + END;
    public static final String DEACTIVATE_NOT_ACCEPTABLE = NOT_ACCEPTABLE + DEACTIVATION + END;
    public static final String DEACTIVATE_LAST_ADMINISTRATOR = LAST_ADMINISTRATOR_DEACTIVATED + DEACTIVATION + END;
    public static final String DEACTIVATE_INTERNAL_ERROR = INTERNAL_ERROR + DEACTIVATION + END;

    private CaretakerExamples() {}
}
