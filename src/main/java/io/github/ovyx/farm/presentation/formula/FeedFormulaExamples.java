package io.github.ovyx.farm.presentation.formula;

/**
 * Exemplos publicados das operacoes de formula de racao, gerados do contrato
 * ({@code contracts/feed-formulas-api.yaml}): o nome de cada constante e a operacao, o status e o nome
 * do exemplo no contrato. Os textos sao os que o sistema devolve de fato.
 */
public final class FeedFormulaExamples {

    private FeedFormulaExamples() {}

    public static final String LIST_200_FORMULAS_ATIVAS =
            """
            [
              {
                "id": "4e6a8c0e-2a4c-4e6a-9c0e-2a4c6e8a0c11",
                "name": "Postura Plus",
                "description": "Milho, farelo de soja, calcário e premix vitamínico; para codornas em postura",
                "pricePerKg": 2.85,
                "expectedIntake": 28,
                "costPerBirdDay": 0.08,
                "status": "ACTIVE",
                "createdAt": "2026-09-20T10:15:00Z",
                "updatedAt": "2026-09-24T17:40:12Z"
              },
              {
                "id": "8a0c2e4a-6c8e-4a0c-8e2a-4c6e8a0c2e22",
                "name": "Recria",
                "description": "Mais proteína, para as aves antes do pico de postura",
                "pricePerKg": 3.1,
                "expectedIntake": 24,
                "costPerBirdDay": 0.074,
                "status": "ACTIVE",
                "createdAt": "2026-09-20T10:16:40Z",
                "updatedAt": "2026-09-20T10:16:40Z"
              }
            ]""";

    public static final String LIST_400_SITUACAO_INVALIDA =
            """
            {
              "code": "VALIDATION_FAILED",
              "title": "Dados inválidos",
              "status": 400,
              "detail": "Valor inválido para o parâmetro 'status'.",
              "instance": "/api/v1/feed-formulas",
              "details": {
                "parameter": "status"
              }
            }""";

    public static final String LIST_401_SEM_SESSAO =
            """
            {
              "code": "UNAUTHENTICATED",
              "title": "Não autenticado",
              "status": 401,
              "detail": "Sua sessão expirou. Entre novamente para continuar.",
              "instance": "/api/v1/feed-formulas"
            }""";

    public static final String LIST_403_TROCA_DE_SENHA_PENDENTE =
            """
            {
              "code": "PASSWORD_CHANGE_REQUIRED",
              "title": "Troca de senha obrigatória",
              "status": 403,
              "detail": "É necessário trocar a senha antes de executar qualquer outra operação.",
              "instance": "/api/v1/feed-formulas"
            }""";

    public static final String LIST_406 =
            """
            {
              "code": "REQUEST_NOT_ACCEPTABLE",
              "title": "Requisição não suportada",
              "status": 406,
              "detail": "A requisição não é suportada por este endereço.",
              "instance": "/api/v1/feed-formulas"
            }""";

    public static final String LIST_500 =
            """
            {
              "code": "INTERNAL_ERROR",
              "title": "Erro inesperado",
              "status": 500,
              "detail": "Não foi possível concluir a operação. Tente novamente em instantes.",
              "instance": "/api/v1/feed-formulas"
            }""";

    public static final String REGISTER_REQUEST_NOVA_FORMULA =
            """
            {
              "name": "Postura Plus",
              "pricePerKg": 2.85,
              "expectedIntake": 28,
              "description": "Milho, farelo de soja, calcário e premix vitamínico; para codornas em postura"
            }""";

    public static final String REGISTER_201_FORMULA_CADASTRADA =
            """
            {
              "id": "4e6a8c0e-2a4c-4e6a-9c0e-2a4c6e8a0c11",
              "name": "Postura Plus",
              "description": "Milho, farelo de soja, calcário e premix vitamínico; para codornas em postura",
              "pricePerKg": 2.85,
              "expectedIntake": 28,
              "costPerBirdDay": 0.08,
              "status": "ACTIVE",
              "createdAt": "2026-09-26T13:02:11Z",
              "updatedAt": "2026-09-26T13:02:11Z"
            }""";

    public static final String REGISTER_400_CAMPOS_INVALIDOS =
            """
            {
              "code": "VALIDATION_FAILED",
              "title": "Dados inválidos",
              "status": 400,
              "detail": "Dados inválidos.",
              "instance": "/api/v1/feed-formulas",
              "details": {
                "name": "Informe o nome da fórmula.",
                "pricePerKg": "O preço deve ficar entre R$ 0,01 e R$ 1.000,00 o quilo.",
                "expectedIntake": "O consumo esperado deve ficar entre 1 e 200 gramas por ave ao dia."
              }
            }""";

    public static final String REGISTER_401_SEM_SESSAO =
            """
            {
              "code": "UNAUTHENTICATED",
              "title": "Não autenticado",
              "status": 401,
              "detail": "Sua sessão expirou. Entre novamente para continuar.",
              "instance": "/api/v1/feed-formulas"
            }""";

    public static final String REGISTER_403_SEM_PERMISSAO =
            """
            {
              "code": "FORBIDDEN",
              "title": "Acesso negado",
              "status": 403,
              "detail": "Você não tem permissão para executar esta operação.",
              "instance": "/api/v1/feed-formulas"
            }""";

    public static final String REGISTER_403_TOKEN_CSRF_AUSENTE =
            """
            {
              "code": "CSRF_TOKEN_INVALID",
              "title": "Proteção da requisição ausente",
              "status": 403,
              "detail": "O token de proteção da requisição está ausente ou expirou. Recarregue a página e tente novamente.",
              "instance": "/api/v1/feed-formulas"
            }""";

    public static final String REGISTER_406 =
            """
            {
              "code": "REQUEST_NOT_ACCEPTABLE",
              "title": "Requisição não suportada",
              "status": 406,
              "detail": "A requisição não é suportada por este endereço.",
              "instance": "/api/v1/feed-formulas"
            }""";

    public static final String REGISTER_409_NOME_EM_USO =
            """
            {
              "code": "FEED_FORMULA_NAME_IN_USE",
              "title": "Operação recusada",
              "status": 409,
              "detail": "Já existe uma fórmula com este nome.",
              "instance": "/api/v1/feed-formulas",
              "details": {
                "name": "Já existe uma fórmula com este nome. Se ela está inativa, reative-a."
              }
            }""";

    public static final String REGISTER_415 =
            """
            {
              "code": "REQUEST_NOT_ACCEPTABLE",
              "title": "Requisição não suportada",
              "status": 415,
              "detail": "A requisição não é suportada por este endereço.",
              "instance": "/api/v1/feed-formulas"
            }""";

    public static final String REGISTER_500 =
            """
            {
              "code": "INTERNAL_ERROR",
              "title": "Erro inesperado",
              "status": 500,
              "detail": "Não foi possível concluir a operação. Tente novamente em instantes.",
              "instance": "/api/v1/feed-formulas"
            }""";

    public static final String FIND_200_FORMULA =
            """
            {
              "id": "4e6a8c0e-2a4c-4e6a-9c0e-2a4c6e8a0c11",
              "name": "Postura Plus",
              "description": "Milho, farelo de soja, calcário e premix vitamínico; para codornas em postura",
              "pricePerKg": 2.85,
              "expectedIntake": 28,
              "costPerBirdDay": 0.08,
              "status": "ACTIVE",
              "createdAt": "2026-09-20T10:15:00Z",
              "updatedAt": "2026-09-24T17:40:12Z"
            }""";

    public static final String FIND_401_SEM_SESSAO =
            """
            {
              "code": "UNAUTHENTICATED",
              "title": "Não autenticado",
              "status": 401,
              "detail": "Sua sessão expirou. Entre novamente para continuar.",
              "instance": "/api/v1/feed-formulas/4e6a8c0e-2a4c-4e6a-9c0e-2a4c6e8a0c11"
            }""";

    public static final String FIND_403_TROCA_DE_SENHA_PENDENTE =
            """
            {
              "code": "PASSWORD_CHANGE_REQUIRED",
              "title": "Troca de senha obrigatória",
              "status": 403,
              "detail": "É necessário trocar a senha antes de executar qualquer outra operação.",
              "instance": "/api/v1/feed-formulas/4e6a8c0e-2a4c-4e6a-9c0e-2a4c6e8a0c11"
            }""";

    public static final String FIND_404_FORMULA_NAO_ENCONTRADA =
            """
            {
              "code": "FEED_FORMULA_NOT_FOUND",
              "title": "Não encontrado",
              "status": 404,
              "detail": "Fórmula não encontrada.",
              "instance": "/api/v1/feed-formulas/postura-plus"
            }""";

    public static final String FIND_406 =
            """
            {
              "code": "REQUEST_NOT_ACCEPTABLE",
              "title": "Requisição não suportada",
              "status": 406,
              "detail": "A requisição não é suportada por este endereço.",
              "instance": "/api/v1/feed-formulas/4e6a8c0e-2a4c-4e6a-9c0e-2a4c6e8a0c11"
            }""";

    public static final String FIND_500 =
            """
            {
              "code": "INTERNAL_ERROR",
              "title": "Erro inesperado",
              "status": 500,
              "detail": "Não foi possível concluir a operação. Tente novamente em instantes.",
              "instance": "/api/v1/feed-formulas/4e6a8c0e-2a4c-4e6a-9c0e-2a4c6e8a0c11"
            }""";

    public static final String UPDATE_REQUEST_PRECO_NOVO =
            """
            {
              "name": "Postura Plus",
              "pricePerKg": 2.9,
              "expectedIntake": 28,
              "description": "Milho, farelo de soja, calcário e premix vitamínico; para codornas em postura"
            }""";

    public static final String UPDATE_200_FORMULA_ATUALIZADA =
            """
            {
              "id": "4e6a8c0e-2a4c-4e6a-9c0e-2a4c6e8a0c11",
              "name": "Postura Plus",
              "description": "Milho, farelo de soja, calcário e premix vitamínico; para codornas em postura",
              "pricePerKg": 2.9,
              "expectedIntake": 28,
              "costPerBirdDay": 0.081,
              "status": "ACTIVE",
              "createdAt": "2026-09-20T10:15:00Z",
              "updatedAt": "2026-09-26T13:20:45Z"
            }""";

    public static final String UPDATE_400_PRECO_INVALIDO =
            """
            {
              "code": "VALIDATION_FAILED",
              "title": "Dados inválidos",
              "status": 400,
              "detail": "Dados inválidos.",
              "instance": "/api/v1/feed-formulas/4e6a8c0e-2a4c-4e6a-9c0e-2a4c6e8a0c11",
              "details": {
                "pricePerKg": "Informe o preço em reais, com até duas casas decimais."
              }
            }""";

    public static final String UPDATE_401_SEM_SESSAO =
            """
            {
              "code": "UNAUTHENTICATED",
              "title": "Não autenticado",
              "status": 401,
              "detail": "Sua sessão expirou. Entre novamente para continuar.",
              "instance": "/api/v1/feed-formulas/4e6a8c0e-2a4c-4e6a-9c0e-2a4c6e8a0c11"
            }""";

    public static final String UPDATE_403_SEM_PERMISSAO =
            """
            {
              "code": "FORBIDDEN",
              "title": "Acesso negado",
              "status": 403,
              "detail": "Você não tem permissão para executar esta operação.",
              "instance": "/api/v1/feed-formulas/4e6a8c0e-2a4c-4e6a-9c0e-2a4c6e8a0c11"
            }""";

    public static final String UPDATE_403_TOKEN_CSRF_AUSENTE =
            """
            {
              "code": "CSRF_TOKEN_INVALID",
              "title": "Proteção da requisição ausente",
              "status": 403,
              "detail": "O token de proteção da requisição está ausente ou expirou. Recarregue a página e tente novamente.",
              "instance": "/api/v1/feed-formulas/4e6a8c0e-2a4c-4e6a-9c0e-2a4c6e8a0c11"
            }""";

    public static final String UPDATE_404_FORMULA_NAO_ENCONTRADA =
            """
            {
              "code": "FEED_FORMULA_NOT_FOUND",
              "title": "Não encontrado",
              "status": 404,
              "detail": "Fórmula não encontrada.",
              "instance": "/api/v1/feed-formulas/postura-plus"
            }""";

    public static final String UPDATE_406 =
            """
            {
              "code": "REQUEST_NOT_ACCEPTABLE",
              "title": "Requisição não suportada",
              "status": 406,
              "detail": "A requisição não é suportada por este endereço.",
              "instance": "/api/v1/feed-formulas/4e6a8c0e-2a4c-4e6a-9c0e-2a4c6e8a0c11"
            }""";

    public static final String UPDATE_409_NOME_EM_USO =
            """
            {
              "code": "FEED_FORMULA_NAME_IN_USE",
              "title": "Operação recusada",
              "status": 409,
              "detail": "Já existe uma fórmula com este nome.",
              "instance": "/api/v1/feed-formulas/4e6a8c0e-2a4c-4e6a-9c0e-2a4c6e8a0c11",
              "details": {
                "name": "Já existe uma fórmula com este nome. Se ela está inativa, reative-a."
              }
            }""";

    public static final String UPDATE_415 =
            """
            {
              "code": "REQUEST_NOT_ACCEPTABLE",
              "title": "Requisição não suportada",
              "status": 415,
              "detail": "A requisição não é suportada por este endereço.",
              "instance": "/api/v1/feed-formulas/4e6a8c0e-2a4c-4e6a-9c0e-2a4c6e8a0c11"
            }""";

    public static final String UPDATE_500 =
            """
            {
              "code": "INTERNAL_ERROR",
              "title": "Erro inesperado",
              "status": 500,
              "detail": "Não foi possível concluir a operação. Tente novamente em instantes.",
              "instance": "/api/v1/feed-formulas/4e6a8c0e-2a4c-4e6a-9c0e-2a4c6e8a0c11"
            }""";

    public static final String DEACTIVATE_200_INATIVADA =
            """
            {
              "id": "8a0c2e4a-6c8e-4a0c-8e2a-4c6e8a0c2e22",
              "name": "Recria",
              "description": "Mais proteína, para as aves antes do pico de postura",
              "pricePerKg": 3.1,
              "expectedIntake": 24,
              "costPerBirdDay": 0.074,
              "status": "INACTIVE",
              "createdAt": "2026-09-20T10:16:40Z",
              "updatedAt": "2026-09-26T14:03:51Z"
            }""";

    public static final String DEACTIVATE_401_SEM_SESSAO =
            """
            {
              "code": "UNAUTHENTICATED",
              "title": "Não autenticado",
              "status": 401,
              "detail": "Sua sessão expirou. Entre novamente para continuar.",
              "instance": "/api/v1/feed-formulas/4e6a8c0e-2a4c-4e6a-9c0e-2a4c6e8a0c11/deactivation"
            }""";

    public static final String DEACTIVATE_403_SEM_PERMISSAO =
            """
            {
              "code": "FORBIDDEN",
              "title": "Acesso negado",
              "status": 403,
              "detail": "Você não tem permissão para executar esta operação.",
              "instance": "/api/v1/feed-formulas/4e6a8c0e-2a4c-4e6a-9c0e-2a4c6e8a0c11/deactivation"
            }""";

    public static final String DEACTIVATE_403_TOKEN_CSRF_AUSENTE =
            """
            {
              "code": "CSRF_TOKEN_INVALID",
              "title": "Proteção da requisição ausente",
              "status": 403,
              "detail": "O token de proteção da requisição está ausente ou expirou. Recarregue a página e tente novamente.",
              "instance": "/api/v1/feed-formulas/4e6a8c0e-2a4c-4e6a-9c0e-2a4c6e8a0c11/deactivation"
            }""";

    public static final String DEACTIVATE_404_FORMULA_NAO_ENCONTRADA =
            """
            {
              "code": "FEED_FORMULA_NOT_FOUND",
              "title": "Não encontrado",
              "status": 404,
              "detail": "Fórmula não encontrada.",
              "instance": "/api/v1/feed-formulas/postura-plus/deactivation"
            }""";

    public static final String DEACTIVATE_406 =
            """
            {
              "code": "REQUEST_NOT_ACCEPTABLE",
              "title": "Requisição não suportada",
              "status": 406,
              "detail": "A requisição não é suportada por este endereço.",
              "instance": "/api/v1/feed-formulas/4e6a8c0e-2a4c-4e6a-9c0e-2a4c6e8a0c11/deactivation"
            }""";

    public static final String DEACTIVATE_500 =
            """
            {
              "code": "INTERNAL_ERROR",
              "title": "Erro inesperado",
              "status": 500,
              "detail": "Não foi possível concluir a operação. Tente novamente em instantes.",
              "instance": "/api/v1/feed-formulas/4e6a8c0e-2a4c-4e6a-9c0e-2a4c6e8a0c11/deactivation"
            }""";

    public static final String REACTIVATE_200_REATIVADA =
            """
            {
              "id": "8a0c2e4a-6c8e-4a0c-8e2a-4c6e8a0c2e22",
              "name": "Recria",
              "description": "Mais proteína, para as aves antes do pico de postura",
              "pricePerKg": 3.1,
              "expectedIntake": 24,
              "costPerBirdDay": 0.074,
              "status": "ACTIVE",
              "createdAt": "2026-09-20T10:16:40Z",
              "updatedAt": "2026-09-26T14:10:02Z"
            }""";

    public static final String REACTIVATE_401_SEM_SESSAO =
            """
            {
              "code": "UNAUTHENTICATED",
              "title": "Não autenticado",
              "status": 401,
              "detail": "Sua sessão expirou. Entre novamente para continuar.",
              "instance": "/api/v1/feed-formulas/4e6a8c0e-2a4c-4e6a-9c0e-2a4c6e8a0c11/reactivation"
            }""";

    public static final String REACTIVATE_403_SEM_PERMISSAO =
            """
            {
              "code": "FORBIDDEN",
              "title": "Acesso negado",
              "status": 403,
              "detail": "Você não tem permissão para executar esta operação.",
              "instance": "/api/v1/feed-formulas/4e6a8c0e-2a4c-4e6a-9c0e-2a4c6e8a0c11/reactivation"
            }""";

    public static final String REACTIVATE_403_TOKEN_CSRF_AUSENTE =
            """
            {
              "code": "CSRF_TOKEN_INVALID",
              "title": "Proteção da requisição ausente",
              "status": 403,
              "detail": "O token de proteção da requisição está ausente ou expirou. Recarregue a página e tente novamente.",
              "instance": "/api/v1/feed-formulas/4e6a8c0e-2a4c-4e6a-9c0e-2a4c6e8a0c11/reactivation"
            }""";

    public static final String REACTIVATE_404_FORMULA_NAO_ENCONTRADA =
            """
            {
              "code": "FEED_FORMULA_NOT_FOUND",
              "title": "Não encontrado",
              "status": 404,
              "detail": "Fórmula não encontrada.",
              "instance": "/api/v1/feed-formulas/postura-plus/reactivation"
            }""";

    public static final String REACTIVATE_406 =
            """
            {
              "code": "REQUEST_NOT_ACCEPTABLE",
              "title": "Requisição não suportada",
              "status": 406,
              "detail": "A requisição não é suportada por este endereço.",
              "instance": "/api/v1/feed-formulas/4e6a8c0e-2a4c-4e6a-9c0e-2a4c6e8a0c11/reactivation"
            }""";

    public static final String REACTIVATE_500 =
            """
            {
              "code": "INTERNAL_ERROR",
              "title": "Erro inesperado",
              "status": 500,
              "detail": "Não foi possível concluir a operação. Tente novamente em instantes.",
              "instance": "/api/v1/feed-formulas/4e6a8c0e-2a4c-4e6a-9c0e-2a4c6e8a0c11/reactivation"
            }""";
}
