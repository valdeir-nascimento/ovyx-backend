package io.github.ovyx.farm.presentation.weighing;

/**
 * Exemplos publicados das operacoes de pesagem, gerados do contrato ({@code contracts/farm-api.yaml} da
 * 005): o nome de cada constante e a operacao, o status e o nome do exemplo no contrato. Os textos sao os
 * que o sistema devolve de fato.
 */
public final class WeighingExamples {

    private WeighingExamples() {}

    public static final String OVERVIEW_200_DENTRO_DA_FAIXA =
            """
            {
              "cage": {
                "id": "2a4c6e8a-0b1d-4f3a-9c5e-7a9b1d3f5a66",
                "code": "A-01",
                "battery": "A",
                "number": 1,
                "birdCount": 48,
                "status": "ACTIVE"
              },
              "sector": {
                "id": "3f6c2b1a-8d4e-4c7f-9a2b-1e5d7c9f0a11",
                "name": "Codornas — Galpão 1",
                "status": "ACTIVE",
                "referenceWeight": {
                  "minimum": 155,
                  "maximum": 175
                }
              },
              "latest": {
                "id": "7b9d1f3a-5c7e-4a9b-8d1f-3a5c7e9b1d77",
                "weighedOn": "2026-09-24",
                "averageWeight": 161.0
              },
              "fourWeekChange": {
                "change": 11.0,
                "since": "2026-08-27"
              },
              "rangeStatus": "WITHIN",
              "schedule": {
                "situation": "UP_TO_DATE",
                "nextOn": "2026-10-01"
              },
              "chart": [
                {
                  "weighedOn": "2026-08-27",
                  "averageWeight": 150.0
                },
                {
                  "weighedOn": "2026-09-03",
                  "averageWeight": 153.0
                },
                {
                  "weighedOn": "2026-09-10",
                  "averageWeight": 156.0
                },
                {
                  "weighedOn": "2026-09-17",
                  "averageWeight": 158.0
                },
                {
                  "weighedOn": "2026-09-24",
                  "averageWeight": 161.0
                }
              ],
              "history": [
                {
                  "id": "7b9d1f3a-5c7e-4a9b-8d1f-3a5c7e9b1d77",
                  "weighedOn": "2026-09-24",
                  "averageWeight": 161.0,
                  "change": 3.0,
                  "recordedBy": {
                    "id": "5e7a9c1e-3b5d-4f7a-9c1e-3b5d7f9a1c22",
                    "name": "Marina Alves"
                  }
                },
                {
                  "id": "6a8c0e2a-4b6d-4f8a-8c0e-2a4b6d8f0a66",
                  "weighedOn": "2026-09-17",
                  "averageWeight": 158.0,
                  "change": 2.0,
                  "recordedBy": {
                    "id": "5e7a9c1e-3b5d-4f7a-9c1e-3b5d7f9a1c22",
                    "name": "Marina Alves"
                  },
                  "lastCorrectedBy": {
                    "id": "1c3e5a7c-9d1f-4b3d-8e5a-7c9d1f3b5e88",
                    "name": "Administrador"
                  }
                },
                {
                  "id": "5f7b9d1f-3a5c-4e7b-9d1f-3a5c7e9b1d55",
                  "weighedOn": "2026-09-10",
                  "averageWeight": 156.0,
                  "change": 3.0,
                  "recordedBy": {
                    "id": "5e7a9c1e-3b5d-4f7a-9c1e-3b5d7f9a1c22",
                    "name": "Marina Alves"
                  }
                },
                {
                  "id": "4e6a8c0e-2b4d-4f6a-8c0e-2b4d6f8a0c44",
                  "weighedOn": "2026-09-03",
                  "averageWeight": 153.0,
                  "change": 3.0,
                  "recordedBy": {
                    "id": "5e7a9c1e-3b5d-4f7a-9c1e-3b5d7f9a1c22",
                    "name": "Marina Alves"
                  }
                },
                {
                  "id": "3d5f7b9d-1a3c-4e5f-8b9d-1a3c5e7f9b33",
                  "weighedOn": "2026-08-27",
                  "averageWeight": 150.0,
                  "recordedBy": {
                    "id": "5e7a9c1e-3b5d-4f7a-9c1e-3b5d7f9a1c22",
                    "name": "Marina Alves"
                  }
                }
              ]
            }""";

    public static final String OVERVIEW_200_SEM_PESAGEM =
            """
            {
              "cage": {
                "id": "9d2e4f6a-1b3c-4d5e-8f7a-2b4c6d8e0f44",
                "code": "B-07",
                "battery": "B",
                "number": 7,
                "birdCount": 50,
                "status": "ACTIVE"
              },
              "sector": {
                "id": "3f6c2b1a-8d4e-4c7f-9a2b-1e5d7c9f0a11",
                "name": "Codornas — Galpão 1",
                "status": "ACTIVE"
              },
              "schedule": {
                "situation": "NEVER_WEIGHED"
              },
              "chart": [],
              "history": []
            }""";

    public static final String OVERVIEW_401_SEM_SESSAO =
            """
            {
              "code": "UNAUTHENTICATED",
              "title": "Não autenticado",
              "status": 401,
              "detail": "Sua sessão expirou. Entre novamente para continuar.",
              "instance": "/api/v1/sectors/3f6c2b1a-8d4e-4c7f-9a2b-1e5d7c9f0a11/cages/2a4c6e8a-0b1d-4f3a-9c5e-7a9b1d3f5a66/weighings"
            }""";

    public static final String OVERVIEW_403_TROCA_DE_SENHA_PENDENTE =
            """
            {
              "code": "PASSWORD_CHANGE_REQUIRED",
              "title": "Troca de senha obrigatória",
              "status": 403,
              "detail": "É necessário trocar a senha antes de executar qualquer outra operação.",
              "instance": "/api/v1/sectors/3f6c2b1a-8d4e-4c7f-9a2b-1e5d7c9f0a11/cages/2a4c6e8a-0b1d-4f3a-9c5e-7a9b1d3f5a66/weighings"
            }""";

    public static final String OVERVIEW_404_GAIOLA_NAO_ENCONTRADA =
            """
            {
              "code": "CAGE_NOT_FOUND",
              "title": "Não encontrado",
              "status": 404,
              "detail": "Gaiola não encontrada.",
              "instance": "/api/v1/sectors/3f6c2b1a-8d4e-4c7f-9a2b-1e5d7c9f0a11/cages/b-07/weighings"
            }""";

    public static final String OVERVIEW_406 =
            """
            {
              "code": "REQUEST_NOT_ACCEPTABLE",
              "title": "Requisição não suportada",
              "status": 406,
              "detail": "A requisição não é suportada por este endereço.",
              "instance": "/api/v1/sectors/3f6c2b1a-8d4e-4c7f-9a2b-1e5d7c9f0a11/cages/2a4c6e8a-0b1d-4f3a-9c5e-7a9b1d3f5a66/weighings"
            }""";

    public static final String OVERVIEW_500 =
            """
            {
              "code": "INTERNAL_ERROR",
              "title": "Erro inesperado",
              "status": 500,
              "detail": "Não foi possível concluir a operação. Tente novamente em instantes.",
              "instance": "/api/v1/sectors/3f6c2b1a-8d4e-4c7f-9a2b-1e5d7c9f0a11/cages/2a4c6e8a-0b1d-4f3a-9c5e-7a9b1d3f5a66/weighings"
            }""";

    public static final String RECORD_REQUEST_PESAGEM_SEMANAL =
            """
            {
              "weighedOn": "2026-09-24",
              "averageWeight": "161,4"
            }""";

    public static final String RECORD_201_PESAGEM_REGISTRADA =
            """
            {
              "id": "7b9d1f3a-5c7e-4a9b-8d1f-3a5c7e9b1d77",
              "weighedOn": "2026-09-24",
              "averageWeight": 161.4,
              "recordedBy": {
                "id": "5e7a9c1e-3b5d-4f7a-9c1e-3b5d7f9a1c22",
                "name": "Marina Alves"
              },
              "recordedAt": "2026-09-24T10:12:40Z"
            }""";

    public static final String RECORD_400_CAMPOS_INVALIDOS =
            """
            {
              "code": "VALIDATION_FAILED",
              "title": "Dados inválidos",
              "status": 400,
              "detail": "Dados inválidos.",
              "instance": "/api/v1/sectors/3f6c2b1a-8d4e-4c7f-9a2b-1e5d7c9f0a11/cages/2a4c6e8a-0b1d-4f3a-9c5e-7a9b1d3f5a66/weighings",
              "details": {
                "weighedOn": "A data da pesagem não pode ser futura.",
                "averageWeight": "Informe o peso médio em gramas."
              }
            }""";

    public static final String RECORD_400_PESO_INVALIDO =
            """
            {
              "code": "VALIDATION_FAILED",
              "title": "Dados inválidos",
              "status": 400,
              "detail": "Dados inválidos.",
              "instance": "/api/v1/sectors/3f6c2b1a-8d4e-4c7f-9a2b-1e5d7c9f0a11/cages/2a4c6e8a-0b1d-4f3a-9c5e-7a9b1d3f5a66/weighings",
              "details": {
                "averageWeight": "Informe o peso em gramas, com até uma casa decimal."
              }
            }""";

    public static final String RECORD_401_SEM_SESSAO =
            """
            {
              "code": "UNAUTHENTICATED",
              "title": "Não autenticado",
              "status": 401,
              "detail": "Sua sessão expirou. Entre novamente para continuar.",
              "instance": "/api/v1/sectors/3f6c2b1a-8d4e-4c7f-9a2b-1e5d7c9f0a11/cages/2a4c6e8a-0b1d-4f3a-9c5e-7a9b1d3f5a66/weighings"
            }""";

    public static final String RECORD_403_TROCA_DE_SENHA_PENDENTE =
            """
            {
              "code": "PASSWORD_CHANGE_REQUIRED",
              "title": "Troca de senha obrigatória",
              "status": 403,
              "detail": "É necessário trocar a senha antes de executar qualquer outra operação.",
              "instance": "/api/v1/sectors/3f6c2b1a-8d4e-4c7f-9a2b-1e5d7c9f0a11/cages/2a4c6e8a-0b1d-4f3a-9c5e-7a9b1d3f5a66/weighings"
            }""";

    public static final String RECORD_404_GAIOLA_NAO_ENCONTRADA =
            """
            {
              "code": "CAGE_NOT_FOUND",
              "title": "Não encontrado",
              "status": 404,
              "detail": "Gaiola não encontrada.",
              "instance": "/api/v1/sectors/3f6c2b1a-8d4e-4c7f-9a2b-1e5d7c9f0a11/cages/b-07/weighings"
            }""";

    public static final String RECORD_406 =
            """
            {
              "code": "REQUEST_NOT_ACCEPTABLE",
              "title": "Requisição não suportada",
              "status": 406,
              "detail": "A requisição não é suportada por este endereço.",
              "instance": "/api/v1/sectors/3f6c2b1a-8d4e-4c7f-9a2b-1e5d7c9f0a11/cages/2a4c6e8a-0b1d-4f3a-9c5e-7a9b1d3f5a66/weighings"
            }""";

    public static final String RECORD_409_DATA_OCUPADA =
            """
            {
              "code": "WEIGHING_DATE_IN_USE",
              "title": "Operação recusada",
              "status": 409,
              "detail": "A gaiola já tem pesagem em 24/09/2026. Corrija a pesagem desse dia.",
              "instance": "/api/v1/sectors/3f6c2b1a-8d4e-4c7f-9a2b-1e5d7c9f0a11/cages/2a4c6e8a-0b1d-4f3a-9c5e-7a9b1d3f5a66/weighings",
              "details": {
                "weighedOn": "A gaiola já tem pesagem em 24/09/2026. Corrija a pesagem desse dia."
              }
            }""";

    public static final String RECORD_409_GAIOLA_INATIVA =
            """
            {
              "code": "CAGE_INACTIVE",
              "title": "Operação recusada",
              "status": 409,
              "detail": "A gaiola está inativa; as pesagens dela são só para consulta.",
              "instance": "/api/v1/sectors/3f6c2b1a-8d4e-4c7f-9a2b-1e5d7c9f0a11/cages/2a4c6e8a-0b1d-4f3a-9c5e-7a9b1d3f5a66/weighings"
            }""";

    public static final String RECORD_415 =
            """
            {
              "code": "REQUEST_NOT_ACCEPTABLE",
              "title": "Requisição não suportada",
              "status": 415,
              "detail": "A requisição não é suportada por este endereço.",
              "instance": "/api/v1/sectors/3f6c2b1a-8d4e-4c7f-9a2b-1e5d7c9f0a11/cages/2a4c6e8a-0b1d-4f3a-9c5e-7a9b1d3f5a66/weighings"
            }""";

    public static final String RECORD_500 =
            """
            {
              "code": "INTERNAL_ERROR",
              "title": "Erro inesperado",
              "status": 500,
              "detail": "Não foi possível concluir a operação. Tente novamente em instantes.",
              "instance": "/api/v1/sectors/3f6c2b1a-8d4e-4c7f-9a2b-1e5d7c9f0a11/cages/2a4c6e8a-0b1d-4f3a-9c5e-7a9b1d3f5a66/weighings"
            }""";

    public static final String FIND_200_PESAGEM_CORRIGIDA =
            """
            {
              "id": "6a8c0e2a-4b6d-4f8a-8c0e-2a4b6d8f0a66",
              "weighedOn": "2026-09-17",
              "averageWeight": 158.0,
              "recordedBy": {
                "id": "5e7a9c1e-3b5d-4f7a-9c1e-3b5d7f9a1c22",
                "name": "Marina Alves"
              },
              "recordedAt": "2026-09-17T09:40:05Z",
              "lastCorrectedBy": {
                "id": "1c3e5a7c-9d1f-4b3d-8e5a-7c9d1f3b5e88",
                "name": "Administrador"
              },
              "lastCorrectedAt": "2026-09-18T11:02:37Z"
            }""";

    public static final String FIND_401_SEM_SESSAO =
            """
            {
              "code": "UNAUTHENTICATED",
              "title": "Não autenticado",
              "status": 401,
              "detail": "Sua sessão expirou. Entre novamente para continuar.",
              "instance": "/api/v1/sectors/3f6c2b1a-8d4e-4c7f-9a2b-1e5d7c9f0a11/cages/2a4c6e8a-0b1d-4f3a-9c5e-7a9b1d3f5a66/weighings/7b9d1f3a-5c7e-4a9b-8d1f-3a5c7e9b1d77"
            }""";

    public static final String FIND_403_TROCA_DE_SENHA_PENDENTE =
            """
            {
              "code": "PASSWORD_CHANGE_REQUIRED",
              "title": "Troca de senha obrigatória",
              "status": 403,
              "detail": "É necessário trocar a senha antes de executar qualquer outra operação.",
              "instance": "/api/v1/sectors/3f6c2b1a-8d4e-4c7f-9a2b-1e5d7c9f0a11/cages/2a4c6e8a-0b1d-4f3a-9c5e-7a9b1d3f5a66/weighings/7b9d1f3a-5c7e-4a9b-8d1f-3a5c7e9b1d77"
            }""";

    public static final String FIND_404_PESAGEM_NAO_ENCONTRADA =
            """
            {
              "code": "WEIGHING_NOT_FOUND",
              "title": "Não encontrado",
              "status": 404,
              "detail": "Pesagem não encontrada.",
              "instance": "/api/v1/sectors/3f6c2b1a-8d4e-4c7f-9a2b-1e5d7c9f0a11/cages/2a4c6e8a-0b1d-4f3a-9c5e-7a9b1d3f5a66/weighings/7b9d1f3a-5c7e-4a9b-8d1f-3a5c7e9b1d77"
            }""";

    public static final String FIND_406 =
            """
            {
              "code": "REQUEST_NOT_ACCEPTABLE",
              "title": "Requisição não suportada",
              "status": 406,
              "detail": "A requisição não é suportada por este endereço.",
              "instance": "/api/v1/sectors/3f6c2b1a-8d4e-4c7f-9a2b-1e5d7c9f0a11/cages/2a4c6e8a-0b1d-4f3a-9c5e-7a9b1d3f5a66/weighings/7b9d1f3a-5c7e-4a9b-8d1f-3a5c7e9b1d77"
            }""";

    public static final String FIND_500 =
            """
            {
              "code": "INTERNAL_ERROR",
              "title": "Erro inesperado",
              "status": 500,
              "detail": "Não foi possível concluir a operação. Tente novamente em instantes.",
              "instance": "/api/v1/sectors/3f6c2b1a-8d4e-4c7f-9a2b-1e5d7c9f0a11/cages/2a4c6e8a-0b1d-4f3a-9c5e-7a9b1d3f5a66/weighings/7b9d1f3a-5c7e-4a9b-8d1f-3a5c7e9b1d77"
            }""";

    public static final String CORRECT_REQUEST_PESO_CORRIGIDO =
            """
            {
              "weighedOn": "2026-09-24",
              "averageWeight": 161
            }""";

    public static final String CORRECT_200_PESAGEM_CORRIGIDA =
            """
            {
              "id": "7b9d1f3a-5c7e-4a9b-8d1f-3a5c7e9b1d77",
              "weighedOn": "2026-09-24",
              "averageWeight": 161.0,
              "recordedBy": {
                "id": "5e7a9c1e-3b5d-4f7a-9c1e-3b5d7f9a1c22",
                "name": "Marina Alves"
              },
              "recordedAt": "2026-09-24T10:12:40Z",
              "lastCorrectedBy": {
                "id": "5e7a9c1e-3b5d-4f7a-9c1e-3b5d7f9a1c22",
                "name": "Marina Alves"
              },
              "lastCorrectedAt": "2026-09-24T10:15:02Z"
            }""";

    public static final String CORRECT_400_PESO_FORA_DA_FAIXA =
            """
            {
              "code": "VALIDATION_FAILED",
              "title": "Dados inválidos",
              "status": 400,
              "detail": "Dados inválidos.",
              "instance": "/api/v1/sectors/3f6c2b1a-8d4e-4c7f-9a2b-1e5d7c9f0a11/cages/2a4c6e8a-0b1d-4f3a-9c5e-7a9b1d3f5a66/weighings/7b9d1f3a-5c7e-4a9b-8d1f-3a5c7e9b1d77",
              "details": {
                "averageWeight": "O peso médio deve ficar entre 1 e 10.000 gramas."
              }
            }""";

    public static final String CORRECT_401_SEM_SESSAO =
            """
            {
              "code": "UNAUTHENTICATED",
              "title": "Não autenticado",
              "status": 401,
              "detail": "Sua sessão expirou. Entre novamente para continuar.",
              "instance": "/api/v1/sectors/3f6c2b1a-8d4e-4c7f-9a2b-1e5d7c9f0a11/cages/2a4c6e8a-0b1d-4f3a-9c5e-7a9b1d3f5a66/weighings/7b9d1f3a-5c7e-4a9b-8d1f-3a5c7e9b1d77"
            }""";

    public static final String CORRECT_403_TROCA_DE_SENHA_PENDENTE =
            """
            {
              "code": "PASSWORD_CHANGE_REQUIRED",
              "title": "Troca de senha obrigatória",
              "status": 403,
              "detail": "É necessário trocar a senha antes de executar qualquer outra operação.",
              "instance": "/api/v1/sectors/3f6c2b1a-8d4e-4c7f-9a2b-1e5d7c9f0a11/cages/2a4c6e8a-0b1d-4f3a-9c5e-7a9b1d3f5a66/weighings/7b9d1f3a-5c7e-4a9b-8d1f-3a5c7e9b1d77"
            }""";

    public static final String CORRECT_404_PESAGEM_NAO_ENCONTRADA =
            """
            {
              "code": "WEIGHING_NOT_FOUND",
              "title": "Não encontrado",
              "status": 404,
              "detail": "Pesagem não encontrada.",
              "instance": "/api/v1/sectors/3f6c2b1a-8d4e-4c7f-9a2b-1e5d7c9f0a11/cages/2a4c6e8a-0b1d-4f3a-9c5e-7a9b1d3f5a66/weighings/7b9d1f3a-5c7e-4a9b-8d1f-3a5c7e9b1d77"
            }""";

    public static final String CORRECT_406 =
            """
            {
              "code": "REQUEST_NOT_ACCEPTABLE",
              "title": "Requisição não suportada",
              "status": 406,
              "detail": "A requisição não é suportada por este endereço.",
              "instance": "/api/v1/sectors/3f6c2b1a-8d4e-4c7f-9a2b-1e5d7c9f0a11/cages/2a4c6e8a-0b1d-4f3a-9c5e-7a9b1d3f5a66/weighings/7b9d1f3a-5c7e-4a9b-8d1f-3a5c7e9b1d77"
            }""";

    public static final String CORRECT_409_SETOR_INATIVO =
            """
            {
              "code": "SECTOR_INACTIVE",
              "title": "Operação recusada",
              "status": 409,
              "detail": "O setor está inativo; as pesagens dele são só para consulta.",
              "instance": "/api/v1/sectors/3f6c2b1a-8d4e-4c7f-9a2b-1e5d7c9f0a11/cages/2a4c6e8a-0b1d-4f3a-9c5e-7a9b1d3f5a66/weighings/7b9d1f3a-5c7e-4a9b-8d1f-3a5c7e9b1d77"
            }""";

    public static final String CORRECT_415 =
            """
            {
              "code": "REQUEST_NOT_ACCEPTABLE",
              "title": "Requisição não suportada",
              "status": 415,
              "detail": "A requisição não é suportada por este endereço.",
              "instance": "/api/v1/sectors/3f6c2b1a-8d4e-4c7f-9a2b-1e5d7c9f0a11/cages/2a4c6e8a-0b1d-4f3a-9c5e-7a9b1d3f5a66/weighings/7b9d1f3a-5c7e-4a9b-8d1f-3a5c7e9b1d77"
            }""";

    public static final String CORRECT_500 =
            """
            {
              "code": "INTERNAL_ERROR",
              "title": "Erro inesperado",
              "status": 500,
              "detail": "Não foi possível concluir a operação. Tente novamente em instantes.",
              "instance": "/api/v1/sectors/3f6c2b1a-8d4e-4c7f-9a2b-1e5d7c9f0a11/cages/2a4c6e8a-0b1d-4f3a-9c5e-7a9b1d3f5a66/weighings/7b9d1f3a-5c7e-4a9b-8d1f-3a5c7e9b1d77"
            }""";

    public static final String VOID_401_SEM_SESSAO =
            """
            {
              "code": "UNAUTHENTICATED",
              "title": "Não autenticado",
              "status": 401,
              "detail": "Sua sessão expirou. Entre novamente para continuar.",
              "instance": "/api/v1/sectors/3f6c2b1a-8d4e-4c7f-9a2b-1e5d7c9f0a11/cages/2a4c6e8a-0b1d-4f3a-9c5e-7a9b1d3f5a66/weighings/7b9d1f3a-5c7e-4a9b-8d1f-3a5c7e9b1d77/voiding"
            }""";

    public static final String VOID_403_TROCA_DE_SENHA_PENDENTE =
            """
            {
              "code": "PASSWORD_CHANGE_REQUIRED",
              "title": "Troca de senha obrigatória",
              "status": 403,
              "detail": "É necessário trocar a senha antes de executar qualquer outra operação.",
              "instance": "/api/v1/sectors/3f6c2b1a-8d4e-4c7f-9a2b-1e5d7c9f0a11/cages/2a4c6e8a-0b1d-4f3a-9c5e-7a9b1d3f5a66/weighings/7b9d1f3a-5c7e-4a9b-8d1f-3a5c7e9b1d77/voiding"
            }""";

    public static final String VOID_404_PESAGEM_NAO_ENCONTRADA =
            """
            {
              "code": "WEIGHING_NOT_FOUND",
              "title": "Não encontrado",
              "status": 404,
              "detail": "Pesagem não encontrada.",
              "instance": "/api/v1/sectors/3f6c2b1a-8d4e-4c7f-9a2b-1e5d7c9f0a11/cages/2a4c6e8a-0b1d-4f3a-9c5e-7a9b1d3f5a66/weighings/pesagem-24-09/voiding"
            }""";

    public static final String VOID_406 =
            """
            {
              "code": "REQUEST_NOT_ACCEPTABLE",
              "title": "Requisição não suportada",
              "status": 406,
              "detail": "A requisição não é suportada por este endereço.",
              "instance": "/api/v1/sectors/3f6c2b1a-8d4e-4c7f-9a2b-1e5d7c9f0a11/cages/2a4c6e8a-0b1d-4f3a-9c5e-7a9b1d3f5a66/weighings/7b9d1f3a-5c7e-4a9b-8d1f-3a5c7e9b1d77/voiding"
            }""";

    public static final String VOID_409_GAIOLA_INATIVA =
            """
            {
              "code": "CAGE_INACTIVE",
              "title": "Operação recusada",
              "status": 409,
              "detail": "A gaiola está inativa; as pesagens dela são só para consulta.",
              "instance": "/api/v1/sectors/3f6c2b1a-8d4e-4c7f-9a2b-1e5d7c9f0a11/cages/2a4c6e8a-0b1d-4f3a-9c5e-7a9b1d3f5a66/weighings/7b9d1f3a-5c7e-4a9b-8d1f-3a5c7e9b1d77/voiding"
            }""";

    public static final String VOID_500 =
            """
            {
              "code": "INTERNAL_ERROR",
              "title": "Erro inesperado",
              "status": 500,
              "detail": "Não foi possível concluir a operação. Tente novamente em instantes.",
              "instance": "/api/v1/sectors/3f6c2b1a-8d4e-4c7f-9a2b-1e5d7c9f0a11/cages/2a4c6e8a-0b1d-4f3a-9c5e-7a9b1d3f5a66/weighings/7b9d1f3a-5c7e-4a9b-8d1f-3a5c7e9b1d77/voiding"
            }""";
}
