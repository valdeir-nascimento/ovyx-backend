package io.github.ovyx.production.presentation.dailyreport;

/**
 * Exemplos publicados das operacoes de relatorio diario, gerados do contrato
 * ({@code contracts/production-api.yaml}): o nome de cada constante e a operacao, o status e o nome do
 * exemplo no contrato. Os textos sao os que o sistema devolve de fato.
 */
public final class DailyReportExamples {

    private DailyReportExamples() {}

    public static final String LIST_200_RELATORIOS_DO_SETOR =
            """
            {
              "sector": {
                "id": "5c8d2e4f-6a1b-4c3d-9e7f-0a2b4c6d8e33",
                "name": "Codornas — Galpão 4",
                "status": "ACTIVE"
              },
              "content": [
                {
                  "id": "8e2a4c6e-0a2c-4e6a-8c0e-2a4c6e8a0c99",
                  "collectionDate": "2026-09-25",
                  "collectionTime": "06:42",
                  "openedByName": "Marina Alves",
                  "flockAge": 20,
                  "collectedEggs": 44,
                  "removedBirds": 0,
                  "closingBirdCount": 96,
                  "productionStatus": "PENDING",
                  "pendingCages": 1,
                  "mortalityStatus": "PENDING"
                },
                {
                  "id": "6b1d3f5a-7c9e-4a2b-8d4f-1e3a5c7b9d55",
                  "collectionDate": "2026-09-24",
                  "collectionTime": "06:30",
                  "openedByName": "Marina Alves",
                  "flockAge": 20,
                  "collectedEggs": 89,
                  "removedBirds": 2,
                  "closingBirdCount": 96,
                  "note": "Bebedouro da bateria B trocado.",
                  "productionStatus": "COMPLETE",
                  "pendingCages": 0,
                  "mortalityStatus": "RECORDED"
                },
                {
                  "id": "4f6b8d0f-2b4d-4f6b-9d1f-3b5d7f9b1d11",
                  "collectionDate": "2026-09-23",
                  "collectionTime": "06:35",
                  "openedByName": "João Pereira",
                  "flockAge": 20,
                  "collectedEggs": 91,
                  "removedBirds": 0,
                  "closingBirdCount": 98,
                  "productionStatus": "COMPLETE",
                  "pendingCages": 0,
                  "mortalityStatus": "RECORDED"
                }
              ],
              "page": 0,
              "size": 20,
              "totalElements": 3,
              "totalPages": 1
            }""";

    public static final String LIST_400_PAGINA_INVALIDA =
            """
            {
              "code": "VALIDATION_FAILED",
              "title": "Dados inválidos",
              "status": 400,
              "detail": "A requisição contém campos inválidos.",
              "instance": "/api/v1/sectors/5c8d2e4f-6a1b-4c3d-9e7f-0a2b4c6d8e33/daily-reports",
              "details": {
                "size": "O tamanho da página deve estar entre 1 e 100."
              }
            }""";

    public static final String LIST_400_DATA_INVALIDA =
            """
            {
              "code": "VALIDATION_FAILED",
              "title": "Dados inválidos",
              "status": 400,
              "detail": "Valor inválido para o parâmetro 'collectionDate'.",
              "instance": "/api/v1/sectors/5c8d2e4f-6a1b-4c3d-9e7f-0a2b4c6d8e33/daily-reports",
              "details": {
                "parameter": "collectionDate"
              }
            }""";

    public static final String LIST_401_SEM_SESSAO =
            """
            {
              "code": "UNAUTHENTICATED",
              "title": "Não autenticado",
              "status": 401,
              "detail": "Sua sessão expirou. Entre novamente para continuar.",
              "instance": "/api/v1/sectors/5c8d2e4f-6a1b-4c3d-9e7f-0a2b4c6d8e33/daily-reports"
            }""";

    public static final String LIST_403_TROCA_DE_SENHA_PENDENTE =
            """
            {
              "code": "PASSWORD_CHANGE_REQUIRED",
              "title": "Troca de senha obrigatória",
              "status": 403,
              "detail": "É necessário trocar a senha antes de executar qualquer outra operação.",
              "instance": "/api/v1/sectors/5c8d2e4f-6a1b-4c3d-9e7f-0a2b4c6d8e33/daily-reports"
            }""";

    public static final String LIST_404_SETOR_NAO_ENCONTRADO =
            """
            {
              "code": "SECTOR_NOT_FOUND",
              "title": "Não encontrado",
              "status": 404,
              "detail": "Setor não encontrado.",
              "instance": "/api/v1/sectors/galpao-9/daily-reports"
            }""";

    public static final String LIST_406 =
            """
            {
              "code": "REQUEST_NOT_ACCEPTABLE",
              "title": "Requisição não suportada",
              "status": 406,
              "detail": "A requisição não é suportada por este endereço.",
              "instance": "/api/v1/sectors/5c8d2e4f-6a1b-4c3d-9e7f-0a2b4c6d8e33/daily-reports"
            }""";

    public static final String LIST_500 =
            """
            {
              "code": "INTERNAL_ERROR",
              "title": "Erro inesperado",
              "status": 500,
              "detail": "Não foi possível concluir a operação. Tente novamente em instantes.",
              "instance": "/api/v1/sectors/5c8d2e4f-6a1b-4c3d-9e7f-0a2b4c6d8e33/daily-reports"
            }""";

    public static final String OPEN_REQUEST_RELATORIO_DE_HOJE =
            """
            {
              "collectionDate": "2026-09-25",
              "collectionTime": "06:42",
              "openingBirdCount": 96,
              "flockAge": 20,
              "note": "Temperatura alta no fim da tarde de ontem."
            }""";

    public static final String OPEN_201_RELATORIO_ABERTO =
            """
            {
              "id": "8e2a4c6e-0a2c-4e6a-8c0e-2a4c6e8a0c99",
              "sector": {
                "id": "5c8d2e4f-6a1b-4c3d-9e7f-0a2b4c6d8e33",
                "name": "Codornas — Galpão 4",
                "status": "ACTIVE"
              },
              "collectionDate": "2026-09-25",
              "collectionTime": "06:42",
              "openingBirdCount": 96,
              "flockAge": 20,
              "note": "Temperatura alta no fim da tarde de ontem.",
              "noMortalityConfirmed": false,
              "openedBy": {
                "id": "1e3a5c7b-9d1f-4b3d-8e5a-7c9e1a3b5d77",
                "name": "Marina Alves"
              },
              "openedAt": "2026-09-25T09:44:03Z",
              "production": {
                "status": "PENDING",
                "pendingCages": 2,
                "collectedEggs": 0,
                "standardEggs": 0,
                "unsellableEggs": 0,
                "layingRate": 0.0
              },
              "mortality": {
                "status": "PENDING",
                "deaths": 0,
                "culls": 0,
                "removalRate": 0.0,
                "closingBirdCount": 96
              },
              "cages": [
                {
                  "cageId": "2a4c6e8a-0b1d-4f3a-9c5e-7a9b1d3f5a66",
                  "code": "A-01",
                  "battery": "A",
                  "number": 1,
                  "birdCount": 47
                },
                {
                  "cageId": "9d2e4f6a-1b3c-4d5e-8f7a-2b4c6d8e0f44",
                  "code": "B-07",
                  "battery": "B",
                  "number": 7,
                  "birdCount": 49
                }
              ]
            }""";

    public static final String OPEN_400_CAMPOS_INVALIDOS =
            """
            {
              "code": "VALIDATION_FAILED",
              "title": "Dados inválidos",
              "status": 400,
              "detail": "Dados inválidos.",
              "instance": "/api/v1/sectors/5c8d2e4f-6a1b-4c3d-9e7f-0a2b4c6d8e33/daily-reports",
              "details": {
                "collectionDate": "A data da coleta não pode ser futura.",
                "openingBirdCount": "As aves do início do dia devem ficar entre 1 e 1.000.000.",
                "flockAge": "A idade do lote deve ficar entre 1 e 150 semanas."
              }
            }""";

    public static final String OPEN_401_SEM_SESSAO =
            """
            {
              "code": "UNAUTHENTICATED",
              "title": "Não autenticado",
              "status": 401,
              "detail": "Sua sessão expirou. Entre novamente para continuar.",
              "instance": "/api/v1/sectors/5c8d2e4f-6a1b-4c3d-9e7f-0a2b4c6d8e33/daily-reports"
            }""";

    public static final String OPEN_403_TOKEN_CSRF_AUSENTE =
            """
            {
              "code": "CSRF_TOKEN_INVALID",
              "title": "Proteção da requisição ausente",
              "status": 403,
              "detail": "O token de proteção da requisição está ausente ou expirou. Recarregue a página e tente novamente.",
              "instance": "/api/v1/sectors/5c8d2e4f-6a1b-4c3d-9e7f-0a2b4c6d8e33/daily-reports"
            }""";

    public static final String OPEN_403_TROCA_DE_SENHA_PENDENTE =
            """
            {
              "code": "PASSWORD_CHANGE_REQUIRED",
              "title": "Troca de senha obrigatória",
              "status": 403,
              "detail": "É necessário trocar a senha antes de executar qualquer outra operação.",
              "instance": "/api/v1/sectors/5c8d2e4f-6a1b-4c3d-9e7f-0a2b4c6d8e33/daily-reports"
            }""";

    public static final String OPEN_404_SETOR_NAO_ENCONTRADO =
            """
            {
              "code": "SECTOR_NOT_FOUND",
              "title": "Não encontrado",
              "status": 404,
              "detail": "Setor não encontrado.",
              "instance": "/api/v1/sectors/galpao-9/daily-reports"
            }""";

    public static final String OPEN_406 =
            """
            {
              "code": "REQUEST_NOT_ACCEPTABLE",
              "title": "Requisição não suportada",
              "status": 406,
              "detail": "A requisição não é suportada por este endereço.",
              "instance": "/api/v1/sectors/5c8d2e4f-6a1b-4c3d-9e7f-0a2b4c6d8e33/daily-reports"
            }""";

    public static final String OPEN_409_RELATORIO_DO_DIA_EXISTE =
            """
            {
              "code": "DAILY_REPORT_ALREADY_EXISTS",
              "title": "Operação recusada",
              "status": 409,
              "detail": "Já existe o relatório de 24/09/2026 neste setor.",
              "instance": "/api/v1/sectors/5c8d2e4f-6a1b-4c3d-9e7f-0a2b4c6d8e33/daily-reports",
              "details": {
                "collectionDate": "Já existe o relatório de 24/09/2026 neste setor."
              }
            }""";

    public static final String OPEN_409_SETOR_INATIVO =
            """
            {
              "code": "SECTOR_INACTIVE",
              "title": "Operação recusada",
              "status": 409,
              "detail": "O setor está inativo; os relatórios dele são só para consulta.",
              "instance": "/api/v1/sectors/5c8d2e4f-6a1b-4c3d-9e7f-0a2b4c6d8e33/daily-reports"
            }""";

    public static final String OPEN_409_SETOR_SEM_GAIOLA =
            """
            {
              "code": "SECTOR_WITHOUT_ACTIVE_CAGES",
              "title": "Operação recusada",
              "status": 409,
              "detail": "O setor não tem gaiola ativa. Cadastre as gaiolas antes de abrir o relatório.",
              "instance": "/api/v1/sectors/5c8d2e4f-6a1b-4c3d-9e7f-0a2b4c6d8e33/daily-reports"
            }""";

    public static final String OPEN_415 =
            """
            {
              "code": "REQUEST_NOT_ACCEPTABLE",
              "title": "Requisição não suportada",
              "status": 415,
              "detail": "A requisição não é suportada por este endereço.",
              "instance": "/api/v1/sectors/5c8d2e4f-6a1b-4c3d-9e7f-0a2b4c6d8e33/daily-reports"
            }""";

    public static final String OPEN_500 =
            """
            {
              "code": "INTERNAL_ERROR",
              "title": "Erro inesperado",
              "status": 500,
              "detail": "Não foi possível concluir a operação. Tente novamente em instantes.",
              "instance": "/api/v1/sectors/5c8d2e4f-6a1b-4c3d-9e7f-0a2b4c6d8e33/daily-reports"
            }""";

    public static final String SUGGEST_200_SUGESTAO_PELO_ANTERIOR =
            """
            {
              "collectionDate": "2026-09-25",
              "collectionTime": "06:42",
              "openingBirdCount": 96,
              "flockAge": 20
            }""";

    public static final String SUGGEST_200_PRIMEIRO_RELATORIO =
            """
            {
              "collectionDate": "2026-09-25",
              "collectionTime": "06:42",
              "openingBirdCount": 98
            }""";

    public static final String SUGGEST_401_SEM_SESSAO =
            """
            {
              "code": "UNAUTHENTICATED",
              "title": "Não autenticado",
              "status": 401,
              "detail": "Sua sessão expirou. Entre novamente para continuar.",
              "instance": "/api/v1/sectors/5c8d2e4f-6a1b-4c3d-9e7f-0a2b4c6d8e33/daily-reports/suggestion"
            }""";

    public static final String SUGGEST_403_TROCA_DE_SENHA_PENDENTE =
            """
            {
              "code": "PASSWORD_CHANGE_REQUIRED",
              "title": "Troca de senha obrigatória",
              "status": 403,
              "detail": "É necessário trocar a senha antes de executar qualquer outra operação.",
              "instance": "/api/v1/sectors/5c8d2e4f-6a1b-4c3d-9e7f-0a2b4c6d8e33/daily-reports/suggestion"
            }""";

    public static final String SUGGEST_404_SETOR_NAO_ENCONTRADO =
            """
            {
              "code": "SECTOR_NOT_FOUND",
              "title": "Não encontrado",
              "status": 404,
              "detail": "Setor não encontrado.",
              "instance": "/api/v1/sectors/galpao-9/daily-reports/suggestion"
            }""";

    public static final String SUGGEST_406 =
            """
            {
              "code": "REQUEST_NOT_ACCEPTABLE",
              "title": "Requisição não suportada",
              "status": 406,
              "detail": "A requisição não é suportada por este endereço.",
              "instance": "/api/v1/sectors/5c8d2e4f-6a1b-4c3d-9e7f-0a2b4c6d8e33/daily-reports/suggestion"
            }""";

    public static final String SUGGEST_500 =
            """
            {
              "code": "INTERNAL_ERROR",
              "title": "Erro inesperado",
              "status": 500,
              "detail": "Não foi possível concluir a operação. Tente novamente em instantes.",
              "instance": "/api/v1/sectors/5c8d2e4f-6a1b-4c3d-9e7f-0a2b4c6d8e33/daily-reports/suggestion"
            }""";

    public static final String FIND_200_RELATORIO_COMPLETO =
            """
            {
              "id": "6b1d3f5a-7c9e-4a2b-8d4f-1e3a5c7b9d55",
              "sector": {
                "id": "5c8d2e4f-6a1b-4c3d-9e7f-0a2b4c6d8e33",
                "name": "Codornas — Galpão 4",
                "status": "ACTIVE"
              },
              "collectionDate": "2026-09-24",
              "collectionTime": "06:30",
              "openingBirdCount": 98,
              "flockAge": 20,
              "note": "Bebedouro da bateria B trocado.",
              "noMortalityConfirmed": false,
              "openedBy": {
                "id": "1e3a5c7b-9d1f-4b3d-8e5a-7c9e1a3b5d77",
                "name": "Marina Alves"
              },
              "openedAt": "2026-09-24T09:31:40Z",
              "lastCorrectedBy": {
                "id": "5d7f9b1d-3f5a-4c7e-9a1b-3d5f7a9c1e88",
                "name": "João Pereira"
              },
              "lastCorrectedAt": "2026-09-24T11:05:12Z",
              "production": {
                "status": "COMPLETE",
                "pendingCages": 0,
                "collectedEggs": 89,
                "standardEggs": 79,
                "unsellableEggs": 4,
                "layingRate": 90.82
              },
              "mortality": {
                "status": "RECORDED",
                "deaths": 1,
                "culls": 1,
                "removalRate": 2.04,
                "closingBirdCount": 96
              },
              "cages": [
                {
                  "cageId": "2a4c6e8a-0b1d-4f3a-9c5e-7a9b1d3f5a66",
                  "code": "A-01",
                  "battery": "A",
                  "number": 1,
                  "birdCount": 48,
                  "production": {
                    "eggs": 44,
                    "small": 1,
                    "jumbo": 2,
                    "dirty": 1,
                    "cracked": 1,
                    "bloodSpot": 0,
                    "abnormal": 0
                  }
                },
                {
                  "cageId": "9d2e4f6a-1b3c-4d5e-8f7a-2b4c6d8e0f44",
                  "code": "B-07",
                  "battery": "B",
                  "number": 7,
                  "birdCount": 50,
                  "production": {
                    "eggs": 45,
                    "small": 0,
                    "jumbo": 1,
                    "dirty": 1,
                    "cracked": 2,
                    "bloodSpot": 1,
                    "abnormal": 0
                  },
                  "mortality": {
                    "deaths": 1,
                    "culls": 1,
                    "note": "Prostração e penas eriçadas; uma ave separada para necropsia."
                  }
                }
              ]
            }""";

    public static final String FIND_401_SEM_SESSAO =
            """
            {
              "code": "UNAUTHENTICATED",
              "title": "Não autenticado",
              "status": 401,
              "detail": "Sua sessão expirou. Entre novamente para continuar.",
              "instance": "/api/v1/sectors/5c8d2e4f-6a1b-4c3d-9e7f-0a2b4c6d8e33/daily-reports/6b1d3f5a-7c9e-4a2b-8d4f-1e3a5c7b9d55"
            }""";

    public static final String FIND_403_TROCA_DE_SENHA_PENDENTE =
            """
            {
              "code": "PASSWORD_CHANGE_REQUIRED",
              "title": "Troca de senha obrigatória",
              "status": 403,
              "detail": "É necessário trocar a senha antes de executar qualquer outra operação.",
              "instance": "/api/v1/sectors/5c8d2e4f-6a1b-4c3d-9e7f-0a2b4c6d8e33/daily-reports/6b1d3f5a-7c9e-4a2b-8d4f-1e3a5c7b9d55"
            }""";

    public static final String FIND_404_RELATORIO_NAO_ENCONTRADO =
            """
            {
              "code": "DAILY_REPORT_NOT_FOUND",
              "title": "Não encontrado",
              "status": 404,
              "detail": "Relatório não encontrado.",
              "instance": "/api/v1/sectors/5c8d2e4f-6a1b-4c3d-9e7f-0a2b4c6d8e33/daily-reports/24-09"
            }""";

    public static final String FIND_406 =
            """
            {
              "code": "REQUEST_NOT_ACCEPTABLE",
              "title": "Requisição não suportada",
              "status": 406,
              "detail": "A requisição não é suportada por este endereço.",
              "instance": "/api/v1/sectors/5c8d2e4f-6a1b-4c3d-9e7f-0a2b4c6d8e33/daily-reports/6b1d3f5a-7c9e-4a2b-8d4f-1e3a5c7b9d55"
            }""";

    public static final String FIND_500 =
            """
            {
              "code": "INTERNAL_ERROR",
              "title": "Erro inesperado",
              "status": 500,
              "detail": "Não foi possível concluir a operação. Tente novamente em instantes.",
              "instance": "/api/v1/sectors/5c8d2e4f-6a1b-4c3d-9e7f-0a2b4c6d8e33/daily-reports/6b1d3f5a-7c9e-4a2b-8d4f-1e3a5c7b9d55"
            }""";

    public static final String CORRECT_REQUEST_IDADE_CORRIGIDA =
            """
            {
              "collectionDate": "2026-09-24",
              "collectionTime": "06:30",
              "openingBirdCount": 98,
              "flockAge": 21,
              "note": "Bebedouro da bateria B trocado."
            }""";

    public static final String CORRECT_200_RELATORIO_CORRIGIDO =
            """
            {
              "id": "6b1d3f5a-7c9e-4a2b-8d4f-1e3a5c7b9d55",
              "sector": {
                "id": "5c8d2e4f-6a1b-4c3d-9e7f-0a2b4c6d8e33",
                "name": "Codornas — Galpão 4",
                "status": "ACTIVE"
              },
              "collectionDate": "2026-09-24",
              "collectionTime": "06:30",
              "openingBirdCount": 98,
              "flockAge": 21,
              "note": "Bebedouro da bateria B trocado.",
              "noMortalityConfirmed": false,
              "openedBy": {
                "id": "1e3a5c7b-9d1f-4b3d-8e5a-7c9e1a3b5d77",
                "name": "Marina Alves"
              },
              "openedAt": "2026-09-24T09:31:40Z",
              "lastCorrectedBy": {
                "id": "5d7f9b1d-3f5a-4c7e-9a1b-3d5f7a9c1e88",
                "name": "João Pereira"
              },
              "lastCorrectedAt": "2026-09-25T10:12:08Z",
              "production": {
                "status": "COMPLETE",
                "pendingCages": 0,
                "collectedEggs": 89,
                "standardEggs": 79,
                "unsellableEggs": 4,
                "layingRate": 90.82
              },
              "mortality": {
                "status": "RECORDED",
                "deaths": 1,
                "culls": 1,
                "removalRate": 2.04,
                "closingBirdCount": 96
              },
              "cages": [
                {
                  "cageId": "2a4c6e8a-0b1d-4f3a-9c5e-7a9b1d3f5a66",
                  "code": "A-01",
                  "battery": "A",
                  "number": 1,
                  "birdCount": 48,
                  "production": {
                    "eggs": 44,
                    "small": 1,
                    "jumbo": 2,
                    "dirty": 1,
                    "cracked": 1,
                    "bloodSpot": 0,
                    "abnormal": 0
                  }
                },
                {
                  "cageId": "9d2e4f6a-1b3c-4d5e-8f7a-2b4c6d8e0f44",
                  "code": "B-07",
                  "battery": "B",
                  "number": 7,
                  "birdCount": 50,
                  "production": {
                    "eggs": 45,
                    "small": 0,
                    "jumbo": 1,
                    "dirty": 1,
                    "cracked": 2,
                    "bloodSpot": 1,
                    "abnormal": 0
                  },
                  "mortality": {
                    "deaths": 1,
                    "culls": 1,
                    "note": "Prostração e penas eriçadas; uma ave separada para necropsia."
                  }
                }
              ]
            }""";

    public static final String CORRECT_400_AVES_ABAIXO_DAS_REMOVIDAS =
            """
            {
              "code": "VALIDATION_FAILED",
              "title": "Dados inválidos",
              "status": 400,
              "detail": "Dados inválidos.",
              "instance": "/api/v1/sectors/5c8d2e4f-6a1b-4c3d-9e7f-0a2b4c6d8e33/daily-reports/6b1d3f5a-7c9e-4a2b-8d4f-1e3a5c7b9d55",
              "details": {
                "openingBirdCount": "O relatório já tem 2 aves removidas; as aves do início do dia não podem ficar abaixo disso."
              }
            }""";

    public static final String CORRECT_401_SEM_SESSAO =
            """
            {
              "code": "UNAUTHENTICATED",
              "title": "Não autenticado",
              "status": 401,
              "detail": "Sua sessão expirou. Entre novamente para continuar.",
              "instance": "/api/v1/sectors/5c8d2e4f-6a1b-4c3d-9e7f-0a2b4c6d8e33/daily-reports/6b1d3f5a-7c9e-4a2b-8d4f-1e3a5c7b9d55"
            }""";

    public static final String CORRECT_403_TOKEN_CSRF_AUSENTE =
            """
            {
              "code": "CSRF_TOKEN_INVALID",
              "title": "Proteção da requisição ausente",
              "status": 403,
              "detail": "O token de proteção da requisição está ausente ou expirou. Recarregue a página e tente novamente.",
              "instance": "/api/v1/sectors/5c8d2e4f-6a1b-4c3d-9e7f-0a2b4c6d8e33/daily-reports/6b1d3f5a-7c9e-4a2b-8d4f-1e3a5c7b9d55"
            }""";

    public static final String CORRECT_403_TROCA_DE_SENHA_PENDENTE =
            """
            {
              "code": "PASSWORD_CHANGE_REQUIRED",
              "title": "Troca de senha obrigatória",
              "status": 403,
              "detail": "É necessário trocar a senha antes de executar qualquer outra operação.",
              "instance": "/api/v1/sectors/5c8d2e4f-6a1b-4c3d-9e7f-0a2b4c6d8e33/daily-reports/6b1d3f5a-7c9e-4a2b-8d4f-1e3a5c7b9d55"
            }""";

    public static final String CORRECT_404_RELATORIO_NAO_ENCONTRADO =
            """
            {
              "code": "DAILY_REPORT_NOT_FOUND",
              "title": "Não encontrado",
              "status": 404,
              "detail": "Relatório não encontrado.",
              "instance": "/api/v1/sectors/5c8d2e4f-6a1b-4c3d-9e7f-0a2b4c6d8e33/daily-reports/24-09"
            }""";

    public static final String CORRECT_406 =
            """
            {
              "code": "REQUEST_NOT_ACCEPTABLE",
              "title": "Requisição não suportada",
              "status": 406,
              "detail": "A requisição não é suportada por este endereço.",
              "instance": "/api/v1/sectors/5c8d2e4f-6a1b-4c3d-9e7f-0a2b4c6d8e33/daily-reports/6b1d3f5a-7c9e-4a2b-8d4f-1e3a5c7b9d55"
            }""";

    public static final String CORRECT_409_DATA_DE_OUTRO_RELATORIO =
            """
            {
              "code": "DAILY_REPORT_ALREADY_EXISTS",
              "title": "Operação recusada",
              "status": 409,
              "detail": "Já existe o relatório de 23/09/2026 neste setor.",
              "instance": "/api/v1/sectors/5c8d2e4f-6a1b-4c3d-9e7f-0a2b4c6d8e33/daily-reports/6b1d3f5a-7c9e-4a2b-8d4f-1e3a5c7b9d55",
              "details": {
                "collectionDate": "Já existe o relatório de 23/09/2026 neste setor."
              }
            }""";

    public static final String CORRECT_415 =
            """
            {
              "code": "REQUEST_NOT_ACCEPTABLE",
              "title": "Requisição não suportada",
              "status": 415,
              "detail": "A requisição não é suportada por este endereço.",
              "instance": "/api/v1/sectors/5c8d2e4f-6a1b-4c3d-9e7f-0a2b4c6d8e33/daily-reports/6b1d3f5a-7c9e-4a2b-8d4f-1e3a5c7b9d55"
            }""";

    public static final String CORRECT_500 =
            """
            {
              "code": "INTERNAL_ERROR",
              "title": "Erro inesperado",
              "status": 500,
              "detail": "Não foi possível concluir a operação. Tente novamente em instantes.",
              "instance": "/api/v1/sectors/5c8d2e4f-6a1b-4c3d-9e7f-0a2b4c6d8e33/daily-reports/6b1d3f5a-7c9e-4a2b-8d4f-1e3a5c7b9d55"
            }""";

    public static final String FIND_CAGE_200_GAIOLA_COM_OCORRENCIA =
            """
            {
              "cageId": "9d2e4f6a-1b3c-4d5e-8f7a-2b4c6d8e0f44",
              "code": "B-07",
              "battery": "B",
              "number": 7,
              "birdCount": 50,
              "production": {
                "eggs": 45,
                "small": 0,
                "jumbo": 1,
                "dirty": 1,
                "cracked": 2,
                "bloodSpot": 1,
                "abnormal": 0
              },
              "mortality": {
                "deaths": 1,
                "culls": 1,
                "note": "Prostração e penas eriçadas; uma ave separada para necropsia."
              }
            }""";

    public static final String FIND_CAGE_401_SEM_SESSAO =
            """
            {
              "code": "UNAUTHENTICATED",
              "title": "Não autenticado",
              "status": 401,
              "detail": "Sua sessão expirou. Entre novamente para continuar.",
              "instance": "/api/v1/sectors/5c8d2e4f-6a1b-4c3d-9e7f-0a2b4c6d8e33/daily-reports/6b1d3f5a-7c9e-4a2b-8d4f-1e3a5c7b9d55/cages/9d2e4f6a-1b3c-4d5e-8f7a-2b4c6d8e0f44"
            }""";

    public static final String FIND_CAGE_403_TROCA_DE_SENHA_PENDENTE =
            """
            {
              "code": "PASSWORD_CHANGE_REQUIRED",
              "title": "Troca de senha obrigatória",
              "status": 403,
              "detail": "É necessário trocar a senha antes de executar qualquer outra operação.",
              "instance": "/api/v1/sectors/5c8d2e4f-6a1b-4c3d-9e7f-0a2b4c6d8e33/daily-reports/6b1d3f5a-7c9e-4a2b-8d4f-1e3a5c7b9d55/cages/9d2e4f6a-1b3c-4d5e-8f7a-2b4c6d8e0f44"
            }""";

    public static final String FIND_CAGE_404_GAIOLA_FORA_DO_RELATORIO =
            """
            {
              "code": "CAGE_NOT_FOUND",
              "title": "Não encontrado",
              "status": 404,
              "detail": "Gaiola não encontrada neste relatório.",
              "instance": "/api/v1/sectors/5c8d2e4f-6a1b-4c3d-9e7f-0a2b4c6d8e33/daily-reports/6b1d3f5a-7c9e-4a2b-8d4f-1e3a5c7b9d55/cages/3b5d7f9b-1d3f-4b5d-8f9b-1d3f5b7d9f00"
            }""";

    public static final String FIND_CAGE_406 =
            """
            {
              "code": "REQUEST_NOT_ACCEPTABLE",
              "title": "Requisição não suportada",
              "status": 406,
              "detail": "A requisição não é suportada por este endereço.",
              "instance": "/api/v1/sectors/5c8d2e4f-6a1b-4c3d-9e7f-0a2b4c6d8e33/daily-reports/6b1d3f5a-7c9e-4a2b-8d4f-1e3a5c7b9d55/cages/9d2e4f6a-1b3c-4d5e-8f7a-2b4c6d8e0f44"
            }""";

    public static final String FIND_CAGE_500 =
            """
            {
              "code": "INTERNAL_ERROR",
              "title": "Erro inesperado",
              "status": 500,
              "detail": "Não foi possível concluir a operação. Tente novamente em instantes.",
              "instance": "/api/v1/sectors/5c8d2e4f-6a1b-4c3d-9e7f-0a2b4c6d8e33/daily-reports/6b1d3f5a-7c9e-4a2b-8d4f-1e3a5c7b9d55/cages/9d2e4f6a-1b3c-4d5e-8f7a-2b4c6d8e0f44"
            }""";

    public static final String PRODUCTION_REQUEST_PRODUCAO_DA_GAIOLA =
            """
            {
              "eggs": 45,
              "jumbo": 1,
              "dirty": 1,
              "cracked": 2,
              "bloodSpot": 1
            }""";

    public static final String PRODUCTION_200_PRODUCAO_LANCADA =
            """
            {
              "cageId": "9d2e4f6a-1b3c-4d5e-8f7a-2b4c6d8e0f44",
              "code": "B-07",
              "battery": "B",
              "number": 7,
              "birdCount": 50,
              "production": {
                "eggs": 45,
                "small": 0,
                "jumbo": 1,
                "dirty": 1,
                "cracked": 2,
                "bloodSpot": 1,
                "abnormal": 0
              }
            }""";

    public static final String PRODUCTION_400_CLASSIFICACAO_ACIMA_DOS_OVOS =
            """
            {
              "code": "VALIDATION_FAILED",
              "title": "Dados inválidos",
              "status": 400,
              "detail": "Dados inválidos.",
              "instance": "/api/v1/sectors/5c8d2e4f-6a1b-4c3d-9e7f-0a2b4c6d8e33/daily-reports/6b1d3f5a-7c9e-4a2b-8d4f-1e3a5c7b9d55/cages/9d2e4f6a-1b3c-4d5e-8f7a-2b4c6d8e0f44/production",
              "details": {
                "eggs": "As classificações somam 34, mais que os 30 ovos coletados."
              }
            }""";

    public static final String PRODUCTION_400_CAMPOS_INVALIDOS =
            """
            {
              "code": "VALIDATION_FAILED",
              "title": "Dados inválidos",
              "status": 400,
              "detail": "Dados inválidos.",
              "instance": "/api/v1/sectors/5c8d2e4f-6a1b-4c3d-9e7f-0a2b4c6d8e33/daily-reports/6b1d3f5a-7c9e-4a2b-8d4f-1e3a5c7b9d55/cages/9d2e4f6a-1b3c-4d5e-8f7a-2b4c6d8e0f44/production",
              "details": {
                "eggs": "Informe os ovos coletados.",
                "cracked": "A quantidade de trincados deve ser um número inteiro."
              }
            }""";

    public static final String PRODUCTION_401_SEM_SESSAO =
            """
            {
              "code": "UNAUTHENTICATED",
              "title": "Não autenticado",
              "status": 401,
              "detail": "Sua sessão expirou. Entre novamente para continuar.",
              "instance": "/api/v1/sectors/5c8d2e4f-6a1b-4c3d-9e7f-0a2b4c6d8e33/daily-reports/6b1d3f5a-7c9e-4a2b-8d4f-1e3a5c7b9d55/cages/9d2e4f6a-1b3c-4d5e-8f7a-2b4c6d8e0f44/production"
            }""";

    public static final String PRODUCTION_403_TOKEN_CSRF_AUSENTE =
            """
            {
              "code": "CSRF_TOKEN_INVALID",
              "title": "Proteção da requisição ausente",
              "status": 403,
              "detail": "O token de proteção da requisição está ausente ou expirou. Recarregue a página e tente novamente.",
              "instance": "/api/v1/sectors/5c8d2e4f-6a1b-4c3d-9e7f-0a2b4c6d8e33/daily-reports/6b1d3f5a-7c9e-4a2b-8d4f-1e3a5c7b9d55/cages/9d2e4f6a-1b3c-4d5e-8f7a-2b4c6d8e0f44/production"
            }""";

    public static final String PRODUCTION_403_TROCA_DE_SENHA_PENDENTE =
            """
            {
              "code": "PASSWORD_CHANGE_REQUIRED",
              "title": "Troca de senha obrigatória",
              "status": 403,
              "detail": "É necessário trocar a senha antes de executar qualquer outra operação.",
              "instance": "/api/v1/sectors/5c8d2e4f-6a1b-4c3d-9e7f-0a2b4c6d8e33/daily-reports/6b1d3f5a-7c9e-4a2b-8d4f-1e3a5c7b9d55/cages/9d2e4f6a-1b3c-4d5e-8f7a-2b4c6d8e0f44/production"
            }""";

    public static final String PRODUCTION_404_GAIOLA_FORA_DO_RELATORIO =
            """
            {
              "code": "CAGE_NOT_FOUND",
              "title": "Não encontrado",
              "status": 404,
              "detail": "Gaiola não encontrada neste relatório.",
              "instance": "/api/v1/sectors/5c8d2e4f-6a1b-4c3d-9e7f-0a2b4c6d8e33/daily-reports/6b1d3f5a-7c9e-4a2b-8d4f-1e3a5c7b9d55/cages/3b5d7f9b-1d3f-4b5d-8f9b-1d3f5b7d9f00/production"
            }""";

    public static final String PRODUCTION_406 =
            """
            {
              "code": "REQUEST_NOT_ACCEPTABLE",
              "title": "Requisição não suportada",
              "status": 406,
              "detail": "A requisição não é suportada por este endereço.",
              "instance": "/api/v1/sectors/5c8d2e4f-6a1b-4c3d-9e7f-0a2b4c6d8e33/daily-reports/6b1d3f5a-7c9e-4a2b-8d4f-1e3a5c7b9d55/cages/9d2e4f6a-1b3c-4d5e-8f7a-2b4c6d8e0f44/production"
            }""";

    public static final String PRODUCTION_409_SETOR_INATIVO =
            """
            {
              "code": "SECTOR_INACTIVE",
              "title": "Operação recusada",
              "status": 409,
              "detail": "O setor está inativo; os relatórios dele são só para consulta.",
              "instance": "/api/v1/sectors/5c8d2e4f-6a1b-4c3d-9e7f-0a2b4c6d8e33/daily-reports/6b1d3f5a-7c9e-4a2b-8d4f-1e3a5c7b9d55/cages/9d2e4f6a-1b3c-4d5e-8f7a-2b4c6d8e0f44/production"
            }""";

    public static final String PRODUCTION_415 =
            """
            {
              "code": "REQUEST_NOT_ACCEPTABLE",
              "title": "Requisição não suportada",
              "status": 415,
              "detail": "A requisição não é suportada por este endereço.",
              "instance": "/api/v1/sectors/5c8d2e4f-6a1b-4c3d-9e7f-0a2b4c6d8e33/daily-reports/6b1d3f5a-7c9e-4a2b-8d4f-1e3a5c7b9d55/cages/9d2e4f6a-1b3c-4d5e-8f7a-2b4c6d8e0f44/production"
            }""";

    public static final String PRODUCTION_500 =
            """
            {
              "code": "INTERNAL_ERROR",
              "title": "Erro inesperado",
              "status": 500,
              "detail": "Não foi possível concluir a operação. Tente novamente em instantes.",
              "instance": "/api/v1/sectors/5c8d2e4f-6a1b-4c3d-9e7f-0a2b4c6d8e33/daily-reports/6b1d3f5a-7c9e-4a2b-8d4f-1e3a5c7b9d55/cages/9d2e4f6a-1b3c-4d5e-8f7a-2b4c6d8e0f44/production"
            }""";

    public static final String MORTALITY_REQUEST_MORTALIDADE_DA_GAIOLA =
            """
            {
              "deaths": 1,
              "culls": 1,
              "note": "Prostração e penas eriçadas; uma ave separada para necropsia."
            }""";

    public static final String MORTALITY_200_MORTALIDADE_LANCADA =
            """
            {
              "cageId": "9d2e4f6a-1b3c-4d5e-8f7a-2b4c6d8e0f44",
              "code": "B-07",
              "battery": "B",
              "number": 7,
              "birdCount": 50,
              "production": {
                "eggs": 45,
                "small": 0,
                "jumbo": 1,
                "dirty": 1,
                "cracked": 2,
                "bloodSpot": 1,
                "abnormal": 0
              },
              "mortality": {
                "deaths": 1,
                "culls": 1,
                "note": "Prostração e penas eriçadas; uma ave separada para necropsia."
              }
            }""";

    public static final String MORTALITY_400_ACIMA_DAS_AVES_DA_GAIOLA =
            """
            {
              "code": "VALIDATION_FAILED",
              "title": "Dados inválidos",
              "status": 400,
              "detail": "Dados inválidos.",
              "instance": "/api/v1/sectors/5c8d2e4f-6a1b-4c3d-9e7f-0a2b4c6d8e33/daily-reports/6b1d3f5a-7c9e-4a2b-8d4f-1e3a5c7b9d55/cages/9d2e4f6a-1b3c-4d5e-8f7a-2b4c6d8e0f44/mortality",
              "details": {
                "deaths": "A gaiola tem 50 aves; mortes e descartes somam 55."
              }
            }""";

    public static final String MORTALITY_400_CAMPOS_INVALIDOS =
            """
            {
              "code": "VALIDATION_FAILED",
              "title": "Dados inválidos",
              "status": 400,
              "detail": "Dados inválidos.",
              "instance": "/api/v1/sectors/5c8d2e4f-6a1b-4c3d-9e7f-0a2b4c6d8e33/daily-reports/6b1d3f5a-7c9e-4a2b-8d4f-1e3a5c7b9d55/cages/9d2e4f6a-1b3c-4d5e-8f7a-2b4c6d8e0f44/mortality",
              "details": {
                "deaths": "As mortes devem ficar entre 0 e 1.000.",
                "culls": "Os descartes devem ser um número inteiro."
              }
            }""";

    public static final String MORTALITY_401_SEM_SESSAO =
            """
            {
              "code": "UNAUTHENTICATED",
              "title": "Não autenticado",
              "status": 401,
              "detail": "Sua sessão expirou. Entre novamente para continuar.",
              "instance": "/api/v1/sectors/5c8d2e4f-6a1b-4c3d-9e7f-0a2b4c6d8e33/daily-reports/6b1d3f5a-7c9e-4a2b-8d4f-1e3a5c7b9d55/cages/9d2e4f6a-1b3c-4d5e-8f7a-2b4c6d8e0f44/mortality"
            }""";

    public static final String MORTALITY_403_TOKEN_CSRF_AUSENTE =
            """
            {
              "code": "CSRF_TOKEN_INVALID",
              "title": "Proteção da requisição ausente",
              "status": 403,
              "detail": "O token de proteção da requisição está ausente ou expirou. Recarregue a página e tente novamente.",
              "instance": "/api/v1/sectors/5c8d2e4f-6a1b-4c3d-9e7f-0a2b4c6d8e33/daily-reports/6b1d3f5a-7c9e-4a2b-8d4f-1e3a5c7b9d55/cages/9d2e4f6a-1b3c-4d5e-8f7a-2b4c6d8e0f44/mortality"
            }""";

    public static final String MORTALITY_403_TROCA_DE_SENHA_PENDENTE =
            """
            {
              "code": "PASSWORD_CHANGE_REQUIRED",
              "title": "Troca de senha obrigatória",
              "status": 403,
              "detail": "É necessário trocar a senha antes de executar qualquer outra operação.",
              "instance": "/api/v1/sectors/5c8d2e4f-6a1b-4c3d-9e7f-0a2b4c6d8e33/daily-reports/6b1d3f5a-7c9e-4a2b-8d4f-1e3a5c7b9d55/cages/9d2e4f6a-1b3c-4d5e-8f7a-2b4c6d8e0f44/mortality"
            }""";

    public static final String MORTALITY_404_GAIOLA_FORA_DO_RELATORIO =
            """
            {
              "code": "CAGE_NOT_FOUND",
              "title": "Não encontrado",
              "status": 404,
              "detail": "Gaiola não encontrada neste relatório.",
              "instance": "/api/v1/sectors/5c8d2e4f-6a1b-4c3d-9e7f-0a2b4c6d8e33/daily-reports/6b1d3f5a-7c9e-4a2b-8d4f-1e3a5c7b9d55/cages/3b5d7f9b-1d3f-4b5d-8f9b-1d3f5b7d9f00/mortality"
            }""";

    public static final String MORTALITY_406 =
            """
            {
              "code": "REQUEST_NOT_ACCEPTABLE",
              "title": "Requisição não suportada",
              "status": 406,
              "detail": "A requisição não é suportada por este endereço.",
              "instance": "/api/v1/sectors/5c8d2e4f-6a1b-4c3d-9e7f-0a2b4c6d8e33/daily-reports/6b1d3f5a-7c9e-4a2b-8d4f-1e3a5c7b9d55/cages/9d2e4f6a-1b3c-4d5e-8f7a-2b4c6d8e0f44/mortality"
            }""";

    public static final String MORTALITY_409_SETOR_INATIVO =
            """
            {
              "code": "SECTOR_INACTIVE",
              "title": "Operação recusada",
              "status": 409,
              "detail": "O setor está inativo; os relatórios dele são só para consulta.",
              "instance": "/api/v1/sectors/5c8d2e4f-6a1b-4c3d-9e7f-0a2b4c6d8e33/daily-reports/6b1d3f5a-7c9e-4a2b-8d4f-1e3a5c7b9d55/cages/9d2e4f6a-1b3c-4d5e-8f7a-2b4c6d8e0f44/mortality"
            }""";

    public static final String MORTALITY_415 =
            """
            {
              "code": "REQUEST_NOT_ACCEPTABLE",
              "title": "Requisição não suportada",
              "status": 415,
              "detail": "A requisição não é suportada por este endereço.",
              "instance": "/api/v1/sectors/5c8d2e4f-6a1b-4c3d-9e7f-0a2b4c6d8e33/daily-reports/6b1d3f5a-7c9e-4a2b-8d4f-1e3a5c7b9d55/cages/9d2e4f6a-1b3c-4d5e-8f7a-2b4c6d8e0f44/mortality"
            }""";

    public static final String MORTALITY_500 =
            """
            {
              "code": "INTERNAL_ERROR",
              "title": "Erro inesperado",
              "status": 500,
              "detail": "Não foi possível concluir a operação. Tente novamente em instantes.",
              "instance": "/api/v1/sectors/5c8d2e4f-6a1b-4c3d-9e7f-0a2b4c6d8e33/daily-reports/6b1d3f5a-7c9e-4a2b-8d4f-1e3a5c7b9d55/cages/9d2e4f6a-1b3c-4d5e-8f7a-2b4c6d8e0f44/mortality"
            }""";

    public static final String CONFIRM_200_DIA_SEM_OCORRENCIA =
            """
            {
              "id": "4f6b8d0f-2b4d-4f6b-9d1f-3b5d7f9b1d11",
              "sector": {
                "id": "5c8d2e4f-6a1b-4c3d-9e7f-0a2b4c6d8e33",
                "name": "Codornas — Galpão 4",
                "status": "ACTIVE"
              },
              "collectionDate": "2026-09-23",
              "collectionTime": "06:35",
              "openingBirdCount": 98,
              "flockAge": 20,
              "noMortalityConfirmed": true,
              "openedBy": {
                "id": "5d7f9b1d-3f5a-4c7e-9a1b-3d5f7a9c1e88",
                "name": "João Pereira"
              },
              "openedAt": "2026-09-23T09:36:18Z",
              "lastCorrectedBy": {
                "id": "5d7f9b1d-3f5a-4c7e-9a1b-3d5f7a9c1e88",
                "name": "João Pereira"
              },
              "lastCorrectedAt": "2026-09-23T10:02:44Z",
              "production": {
                "status": "COMPLETE",
                "pendingCages": 0,
                "collectedEggs": 91,
                "standardEggs": 84,
                "unsellableEggs": 3,
                "layingRate": 92.86
              },
              "mortality": {
                "status": "RECORDED",
                "deaths": 0,
                "culls": 0,
                "removalRate": 0.0,
                "closingBirdCount": 98
              },
              "cages": [
                {
                  "cageId": "2a4c6e8a-0b1d-4f3a-9c5e-7a9b1d3f5a66",
                  "code": "A-01",
                  "battery": "A",
                  "number": 1,
                  "birdCount": 48,
                  "production": {
                    "eggs": 45,
                    "small": 1,
                    "jumbo": 1,
                    "dirty": 1,
                    "cracked": 1,
                    "bloodSpot": 0,
                    "abnormal": 0
                  }
                },
                {
                  "cageId": "9d2e4f6a-1b3c-4d5e-8f7a-2b4c6d8e0f44",
                  "code": "B-07",
                  "battery": "B",
                  "number": 7,
                  "birdCount": 50,
                  "production": {
                    "eggs": 46,
                    "small": 0,
                    "jumbo": 1,
                    "dirty": 0,
                    "cracked": 1,
                    "bloodSpot": 1,
                    "abnormal": 0
                  }
                }
              ]
            }""";

    public static final String CONFIRM_401_SEM_SESSAO =
            """
            {
              "code": "UNAUTHENTICATED",
              "title": "Não autenticado",
              "status": 401,
              "detail": "Sua sessão expirou. Entre novamente para continuar.",
              "instance": "/api/v1/sectors/5c8d2e4f-6a1b-4c3d-9e7f-0a2b4c6d8e33/daily-reports/6b1d3f5a-7c9e-4a2b-8d4f-1e3a5c7b9d55/mortality-confirmation"
            }""";

    public static final String CONFIRM_403_TOKEN_CSRF_AUSENTE =
            """
            {
              "code": "CSRF_TOKEN_INVALID",
              "title": "Proteção da requisição ausente",
              "status": 403,
              "detail": "O token de proteção da requisição está ausente ou expirou. Recarregue a página e tente novamente.",
              "instance": "/api/v1/sectors/5c8d2e4f-6a1b-4c3d-9e7f-0a2b4c6d8e33/daily-reports/6b1d3f5a-7c9e-4a2b-8d4f-1e3a5c7b9d55/mortality-confirmation"
            }""";

    public static final String CONFIRM_403_TROCA_DE_SENHA_PENDENTE =
            """
            {
              "code": "PASSWORD_CHANGE_REQUIRED",
              "title": "Troca de senha obrigatória",
              "status": 403,
              "detail": "É necessário trocar a senha antes de executar qualquer outra operação.",
              "instance": "/api/v1/sectors/5c8d2e4f-6a1b-4c3d-9e7f-0a2b4c6d8e33/daily-reports/6b1d3f5a-7c9e-4a2b-8d4f-1e3a5c7b9d55/mortality-confirmation"
            }""";

    public static final String CONFIRM_404_RELATORIO_NAO_ENCONTRADO =
            """
            {
              "code": "DAILY_REPORT_NOT_FOUND",
              "title": "Não encontrado",
              "status": 404,
              "detail": "Relatório não encontrado.",
              "instance": "/api/v1/sectors/5c8d2e4f-6a1b-4c3d-9e7f-0a2b4c6d8e33/daily-reports/24-09/mortality-confirmation"
            }""";

    public static final String CONFIRM_406 =
            """
            {
              "code": "REQUEST_NOT_ACCEPTABLE",
              "title": "Requisição não suportada",
              "status": 406,
              "detail": "A requisição não é suportada por este endereço.",
              "instance": "/api/v1/sectors/5c8d2e4f-6a1b-4c3d-9e7f-0a2b4c6d8e33/daily-reports/6b1d3f5a-7c9e-4a2b-8d4f-1e3a5c7b9d55/mortality-confirmation"
            }""";

    public static final String CONFIRM_409_OCORRENCIA_JA_LANCADA =
            """
            {
              "code": "MORTALITY_ALREADY_RECORDED",
              "title": "Operação recusada",
              "status": 409,
              "detail": "O relatório já tem mortes ou descartes lançados.",
              "instance": "/api/v1/sectors/5c8d2e4f-6a1b-4c3d-9e7f-0a2b4c6d8e33/daily-reports/6b1d3f5a-7c9e-4a2b-8d4f-1e3a5c7b9d55/mortality-confirmation"
            }""";

    public static final String CONFIRM_500 =
            """
            {
              "code": "INTERNAL_ERROR",
              "title": "Erro inesperado",
              "status": 500,
              "detail": "Não foi possível concluir a operação. Tente novamente em instantes.",
              "instance": "/api/v1/sectors/5c8d2e4f-6a1b-4c3d-9e7f-0a2b4c6d8e33/daily-reports/6b1d3f5a-7c9e-4a2b-8d4f-1e3a5c7b9d55/mortality-confirmation"
            }""";
}
