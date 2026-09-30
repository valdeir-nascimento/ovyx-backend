package io.github.ovyx.production.presentation.dashboard;

/**
 * Exemplos publicados das operacoes do painel, gerados do contrato ({@code contracts/production-api.yaml}
 * da 006): o nome de cada constante e a operacao, o status e o nome do exemplo no contrato. Os textos sao
 * os que o sistema devolve de fato.
 */
public final class DashboardExamples {

    private DashboardExamples() {}

    public static final String OVERVIEW_200_GRANJA_COM_SETORES =
            """
            {
              "today": "2026-09-24",
              "partOfDay": "MORNING",
              "activeSectors": 3,
              "completeToday": 2,
              "sectors": [
                {
                  "id": "3f6c2b1a-8d4e-4c7f-9a2b-1e5d7c9f0a11",
                  "name": "Codornas — Galpão 1"
                },
                {
                  "id": "5c8d2e4f-6a1b-4c3d-9e7f-0a2b4c6d8e33",
                  "name": "Codornas — Galpão 4"
                },
                {
                  "id": "7e9a1c3e-5b7d-4f9a-8c1e-3b5d7f9a1c55",
                  "name": "Poedeiras — Galpão 2"
                }
              ]
            }""";

    public static final String OVERVIEW_200_GRANJA_SEM_RELATORIO =
            """
            {
              "today": "2026-09-24",
              "partOfDay": "AFTERNOON",
              "activeSectors": 1,
              "completeToday": 0,
              "sectors": []
            }""";

    public static final String OVERVIEW_401_SEM_SESSAO =
            """
            {
              "code": "UNAUTHENTICATED",
              "title": "Não autenticado",
              "status": 401,
              "detail": "Sua sessão expirou. Entre novamente para continuar.",
              "instance": "/api/v1/dashboard"
            }""";

    public static final String OVERVIEW_403_TROCA_DE_SENHA_PENDENTE =
            """
            {
              "code": "PASSWORD_CHANGE_REQUIRED",
              "title": "Troca de senha obrigatória",
              "status": 403,
              "detail": "É necessário trocar a senha antes de executar qualquer outra operação.",
              "instance": "/api/v1/dashboard"
            }""";

    public static final String OVERVIEW_406 =
            """
            {
              "code": "REQUEST_NOT_ACCEPTABLE",
              "title": "Requisição não suportada",
              "status": 406,
              "detail": "A requisição não é suportada por este endereço.",
              "instance": "/api/v1/dashboard"
            }""";

    public static final String OVERVIEW_500 =
            """
            {
              "code": "INTERNAL_ERROR",
              "title": "Erro inesperado",
              "status": 500,
              "detail": "Não foi possível concluir a operação. Tente novamente em instantes.",
              "instance": "/api/v1/dashboard"
            }""";

    public static final String SECTOR_200_SETOR_COM_ALERTAS =
            """
            {
              "sector": {
                "id": "3f6c2b1a-8d4e-4c7f-9a2b-1e5d7c9f0a11",
                "name": "Codornas — Galpão 1",
                "status": "ACTIVE"
              },
              "period": "TODAY",
              "from": "2026-09-24",
              "to": "2026-09-24",
              "todayReport": {
                "id": "6b1d3f5a-7c9e-4a2b-8d4f-1e3a5c7b9d55",
                "productionStatus": "COMPLETE",
                "feedStatus": "COMPLETE",
                "mortalityStatus": "RECORDED"
              },
              "indicators": {
                "production": {
                  "value": 1740,
                  "previous": 1700,
                  "change": 2.4,
                  "goodDirection": "UP",
                  "incompleteDays": 0
                },
                "layingRate": {
                  "value": 87.0,
                  "previous": 85.0,
                  "change": 2.0,
                  "goodDirection": "UP",
                  "incompleteDays": 0
                },
                "feedCost": {
                  "value": 159.6,
                  "previous": 157.25,
                  "change": 1.5,
                  "goodDirection": "DOWN",
                  "incompleteDays": 0
                },
                "costPerEgg": {
                  "value": 0.092,
                  "previous": 0.093,
                  "change": -1.1,
                  "goodDirection": "DOWN",
                  "incompleteDays": 0
                }
              },
              "trend": [
                {
                  "date": "2026-09-18",
                  "production": 1690,
                  "layingRate": 84.5,
                  "feedCost": 156.1,
                  "costPerEgg": 0.092
                },
                {
                  "date": "2026-09-19",
                  "production": 1702,
                  "layingRate": 85.1,
                  "feedCost": 157.02,
                  "costPerEgg": 0.092
                },
                {
                  "date": "2026-09-20",
                  "production": 1688,
                  "layingRate": 84.4,
                  "feedCost": 155.8,
                  "costPerEgg": 0.092
                },
                {
                  "date": "2026-09-21",
                  "production": 1715,
                  "layingRate": 85.75,
                  "feedCost": 158.3,
                  "costPerEgg": 0.092
                },
                {
                  "date": "2026-09-22",
                  "production": 1720,
                  "layingRate": 86.0,
                  "feedCost": 158.75,
                  "costPerEgg": 0.092
                },
                {
                  "date": "2026-09-23",
                  "production": 1700,
                  "layingRate": 85.0,
                  "feedCost": 157.25,
                  "costPerEgg": 0.093
                },
                {
                  "date": "2026-09-24",
                  "production": 1740,
                  "layingRate": 87.0,
                  "feedCost": 159.6,
                  "costPerEgg": 0.092
                }
              ],
              "target": 85.0,
              "targetStatus": "ABOVE",
              "grades": {
                "collected": 1740,
                "standard": {
                  "grade": "standard",
                  "count": 1650,
                  "percent": 94.8
                },
                "shares": [
                  {
                    "grade": "small",
                    "count": 30,
                    "percent": 1.7
                  },
                  {
                    "grade": "jumbo",
                    "count": 12,
                    "percent": 0.7
                  },
                  {
                    "grade": "dirty",
                    "count": 20,
                    "percent": 1.1
                  },
                  {
                    "grade": "cracked",
                    "count": 18,
                    "percent": 1.0
                  },
                  {
                    "grade": "bloodSpot",
                    "count": 6,
                    "percent": 0.3
                  },
                  {
                    "grade": "abnormal",
                    "count": 4,
                    "percent": 0.2
                  }
                ]
              },
              "alerts": [
                {
                  "kind": "HIGH_MORTALITY",
                  "tone": "WARNING",
                  "title": "Mortalidade acima da média na gaiola B-07",
                  "detail": "2 aves removidas hoje; a média do setor é 0,4 por gaiola ao dia.",
                  "target": {
                    "reportId": "6b1d3f5a-7c9e-4a2b-8d4f-1e3a5c7b9d55",
                    "cageId": "9d2e4f6a-1b3c-4d5e-8f7a-2b4c6d8e0f44",
                    "cageCode": "B-07"
                  }
                },
                {
                  "kind": "LOW_LAYING",
                  "tone": "WARNING",
                  "title": "Baixa postura na gaiola C-03",
                  "detail": "76,0% nos últimos 3 relatórios, contra 87,0% no setor hoje.",
                  "target": {
                    "cageId": "1c3e5a7c-9e1a-4c3e-8a5c-7e9a1c3e5a88",
                    "cageCode": "C-03"
                  }
                },
                {
                  "kind": "WEIGHT_OUT_OF_RANGE",
                  "tone": "WARNING",
                  "title": "Pesagem fora da faixa na gaiola A-02",
                  "detail": "150,8 g em 24/09/2026; a faixa do setor é 155–175 g.",
                  "target": {
                    "cageId": "4b6d8f0b-2c4e-4a6c-9e0b-2d4f6a8c0e99",
                    "cageCode": "A-02"
                  }
                }
              ],
              "openAlerts": 3,
              "latestReports": [
                {
                  "id": "6b1d3f5a-7c9e-4a2b-8d4f-1e3a5c7b9d55",
                  "collectionDate": "2026-09-24",
                  "collectionTime": "06:30",
                  "openedBy": {
                    "id": "1e3a5c7b-9d1f-4b3d-8e5a-7c9e1a3b5d77",
                    "name": "Marina Alves"
                  },
                  "collectedEggs": 1740,
                  "removedBirds": 3,
                  "productionStatus": "COMPLETE",
                  "feedStatus": "COMPLETE",
                  "mortalityStatus": "RECORDED"
                },
                {
                  "id": "8d2f4a6c-0e2a-4c6e-9a2c-4e6a8c0e2a11",
                  "collectionDate": "2026-09-23",
                  "collectionTime": "06:40",
                  "openedBy": {
                    "id": "1e3a5c7b-9d1f-4b3d-8e5a-7c9e1a3b5d77",
                    "name": "Marina Alves"
                  },
                  "collectedEggs": 1700,
                  "removedBirds": 1,
                  "productionStatus": "COMPLETE",
                  "feedStatus": "COMPLETE",
                  "mortalityStatus": "RECORDED"
                },
                {
                  "id": "2e4a6c8e-0a2c-4e6a-8c0e-2a4c6e8a0c22",
                  "collectionDate": "2026-09-22",
                  "collectionTime": "06:35",
                  "openedBy": {
                    "id": "5d7f9b1d-3f5a-4c7e-9a1b-3d5f7a9c1e88",
                    "name": "João Pereira"
                  },
                  "collectedEggs": 1720,
                  "removedBirds": 0,
                  "productionStatus": "COMPLETE",
                  "feedStatus": "COMPLETE",
                  "mortalityStatus": "RECORDED"
                },
                {
                  "id": "4a6c8e0a-2c4e-4a6c-8e0a-2c4e6a8c0e33",
                  "collectionDate": "2026-09-21",
                  "collectionTime": "06:45",
                  "openedBy": {
                    "id": "5d7f9b1d-3f5a-4c7e-9a1b-3d5f7a9c1e88",
                    "name": "João Pereira"
                  },
                  "collectedEggs": 1715,
                  "removedBirds": 2,
                  "productionStatus": "COMPLETE",
                  "feedStatus": "COMPLETE",
                  "mortalityStatus": "RECORDED"
                }
              ]
            }""";

    public static final String SECTOR_200_HOJE_SEM_RELATORIO =
            """
            {
              "sector": {
                "id": "7e9a1c3e-5b7d-4f9a-8c1e-3b5d7f9a1c55",
                "name": "Poedeiras — Galpão 2",
                "status": "ACTIVE"
              },
              "period": "TODAY",
              "from": "2026-09-24",
              "to": "2026-09-24",
              "indicators": {
                "production": {
                  "previous": 912,
                  "goodDirection": "UP",
                  "incompleteDays": 0
                },
                "layingRate": {
                  "previous": 91.2,
                  "goodDirection": "UP",
                  "incompleteDays": 0
                },
                "feedCost": {
                  "goodDirection": "DOWN",
                  "incompleteDays": 0
                },
                "costPerEgg": {
                  "goodDirection": "DOWN",
                  "incompleteDays": 0
                }
              },
              "trend": [
                {
                  "date": "2026-09-18"
                },
                {
                  "date": "2026-09-19"
                },
                {
                  "date": "2026-09-20"
                },
                {
                  "date": "2026-09-21"
                },
                {
                  "date": "2026-09-22"
                },
                {
                  "date": "2026-09-23",
                  "production": 912,
                  "layingRate": 91.2
                },
                {
                  "date": "2026-09-24"
                }
              ],
              "target": 85.0,
              "targetStatus": "ABOVE",
              "alerts": [
                {
                  "kind": "REPORT_NOT_OPENED",
                  "tone": "WARNING",
                  "title": "Relatório de hoje não aberto",
                  "detail": "Abra o relatório de 24/09 para lançar a produção, a ração e a mortalidade.",
                  "target": {}
                }
              ],
              "openAlerts": 1,
              "latestReports": [
                {
                  "id": "9f1b3d5f-7a9c-4e1b-8d3f-5a7c9e1b3d44",
                  "collectionDate": "2026-09-23",
                  "collectionTime": "07:05",
                  "openedBy": {
                    "id": "1e3a5c7b-9d1f-4b3d-8e5a-7c9e1a3b5d77",
                    "name": "Marina Alves"
                  },
                  "collectedEggs": 912,
                  "removedBirds": 0,
                  "productionStatus": "COMPLETE",
                  "feedStatus": "PENDING",
                  "mortalityStatus": "RECORDED"
                }
              ]
            }""";

    public static final String SECTOR_400_PERIODO_INVALIDO =
            """
            {
              "code": "VALIDATION_FAILED",
              "title": "Dados inválidos",
              "status": 400,
              "detail": "Valor inválido para o parâmetro 'period'.",
              "instance": "/api/v1/sectors/3f6c2b1a-8d4e-4c7f-9a2b-1e5d7c9f0a11/dashboard",
              "details": {
                "parameter": "period"
              }
            }""";

    public static final String SECTOR_401_SEM_SESSAO =
            """
            {
              "code": "UNAUTHENTICATED",
              "title": "Não autenticado",
              "status": 401,
              "detail": "Sua sessão expirou. Entre novamente para continuar.",
              "instance": "/api/v1/sectors/5c8d2e4f-6a1b-4c3d-9e7f-0a2b4c6d8e33/dashboard"
            }""";

    public static final String SECTOR_403_TROCA_DE_SENHA_PENDENTE =
            """
            {
              "code": "PASSWORD_CHANGE_REQUIRED",
              "title": "Troca de senha obrigatória",
              "status": 403,
              "detail": "É necessário trocar a senha antes de executar qualquer outra operação.",
              "instance": "/api/v1/sectors/5c8d2e4f-6a1b-4c3d-9e7f-0a2b4c6d8e33/dashboard"
            }""";

    public static final String SECTOR_404_SETOR_NAO_ENCONTRADO =
            """
            {
              "code": "SECTOR_NOT_FOUND",
              "title": "Não encontrado",
              "status": 404,
              "detail": "Setor não encontrado.",
              "instance": "/api/v1/sectors/galpao-9/dashboard"
            }""";

    public static final String SECTOR_406 =
            """
            {
              "code": "REQUEST_NOT_ACCEPTABLE",
              "title": "Requisição não suportada",
              "status": 406,
              "detail": "A requisição não é suportada por este endereço.",
              "instance": "/api/v1/sectors/3f6c2b1a-8d4e-4c7f-9a2b-1e5d7c9f0a11/dashboard"
            }""";

    public static final String SECTOR_500 =
            """
            {
              "code": "INTERNAL_ERROR",
              "title": "Erro inesperado",
              "status": 500,
              "detail": "Não foi possível concluir a operação. Tente novamente em instantes.",
              "instance": "/api/v1/sectors/3f6c2b1a-8d4e-4c7f-9a2b-1e5d7c9f0a11/dashboard"
            }""";

    // ---------------------------------------------------------------- exportSectorDashboard (007)

    public static final String EXPORT_400_PERIODO_INVALIDO =
            """
            {
              "code": "VALIDATION_FAILED",
              "title": "Dados inválidos",
              "status": 400,
              "detail": "Valor inválido para o parâmetro 'period'.",
              "instance": "/api/v1/sectors/3f6c2b1a-8d4e-4c7f-9a2b-1e5d7c9f0a11/dashboard/export",
              "details": {
                "parameter": "period"
              }
            }""";

    public static final String EXPORT_401_SEM_SESSAO =
            """
            {
              "code": "UNAUTHENTICATED",
              "title": "Não autenticado",
              "status": 401,
              "detail": "Sua sessão expirou. Entre novamente para continuar.",
              "instance": "/api/v1/sectors/3f6c2b1a-8d4e-4c7f-9a2b-1e5d7c9f0a11/dashboard/export"
            }""";

    public static final String EXPORT_403_TROCA_DE_SENHA_PENDENTE =
            """
            {
              "code": "PASSWORD_CHANGE_REQUIRED",
              "title": "Troca de senha obrigatória",
              "status": 403,
              "detail": "É necessário trocar a senha antes de executar qualquer outra operação.",
              "instance": "/api/v1/sectors/3f6c2b1a-8d4e-4c7f-9a2b-1e5d7c9f0a11/dashboard/export"
            }""";

    public static final String EXPORT_404_SETOR_NAO_ENCONTRADO =
            """
            {
              "code": "SECTOR_NOT_FOUND",
              "title": "Não encontrado",
              "status": 404,
              "detail": "Setor não encontrado.",
              "instance": "/api/v1/sectors/3f6c2b1a-8d4e-4c7f-9a2b-1e5d7c9f0a11/dashboard/export"
            }""";

    public static final String EXPORT_406 =
            """
            {
              "code": "REQUEST_NOT_ACCEPTABLE",
              "title": "Requisição não suportada",
              "status": 406,
              "detail": "A requisição não é suportada por este endereço.",
              "instance": "/api/v1/sectors/3f6c2b1a-8d4e-4c7f-9a2b-1e5d7c9f0a11/dashboard/export"
            }""";

    public static final String EXPORT_500 =
            """
            {
              "code": "INTERNAL_ERROR",
              "title": "Erro inesperado",
              "status": 500,
              "detail": "Não foi possível concluir a operação. Tente novamente em instantes.",
              "instance": "/api/v1/sectors/3f6c2b1a-8d4e-4c7f-9a2b-1e5d7c9f0a11/dashboard/export"
            }""";
}
