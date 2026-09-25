-- Setor (Sector): agrupamento da estrutura produtiva da granja -- um galpao, uma especie ou um
-- lote acompanhado separadamente. Raiz de agregado do contexto farm, com as gaiolas dentro (R-003).
-- Nunca e removido fisicamente: as features seguintes lancam dados por setor e por gaiola (FR-012).
CREATE TABLE sector (
    id          uuid         NOT NULL,
    name        varchar(80)  NOT NULL,
    description varchar(500),
    status      varchar(10)  NOT NULL,
    version     bigint       NOT NULL DEFAULT 0,
    created_at  timestamptz  NOT NULL,
    updated_at  timestamptz  NOT NULL,
    CONSTRAINT pk_sector PRIMARY KEY (id),
    CONSTRAINT ck_sector_name_length CHECK (char_length(name) BETWEEN 2 AND 80),
    CONSTRAINT ck_sector_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

-- FR-002: o nome e unico entre os setores ATIVOS, sem distinguir maiusculas. O nome ja chega
-- aparado do dominio. O indice e a rede de seguranca da corrida entre dois cadastros; a regra mora
-- no agregado (R-005).
CREATE UNIQUE INDEX ux_sector_name_active ON sector (lower(name)) WHERE status = 'ACTIVE';

-- Gaiola (Cage): unidade de alojamento das aves dentro de um setor, onde a producao, a racao, a
-- mortalidade e o peso serao lancados. Pertence ao mesmo setor para sempre.
CREATE TABLE cage (
    id                      uuid        NOT NULL,
    sector_id               uuid        NOT NULL,
    battery                 varchar(3)  NOT NULL,
    number                  smallint    NOT NULL,
    bird_count              integer     NOT NULL,
    status                  varchar(10) NOT NULL,
    deactivated_with_sector boolean     NOT NULL DEFAULT false,
    created_at              timestamptz NOT NULL,
    updated_at              timestamptz NOT NULL,
    CONSTRAINT pk_cage PRIMARY KEY (id),
    CONSTRAINT fk_cage_sector FOREIGN KEY (sector_id) REFERENCES sector (id),
    CONSTRAINT ck_cage_battery CHECK (battery ~ '^[A-Z0-9]{1,3}$'),
    CONSTRAINT ck_cage_number CHECK (number BETWEEN 1 AND 999),
    CONSTRAINT ck_cage_bird_count CHECK (bird_count BETWEEN 0 AND 1000),
    CONSTRAINT ck_cage_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

-- FR-007: bateria e numero unicos entre as gaiolas ATIVAS do mesmo setor. Setores diferentes
-- repetem a combinacao, e uma gaiola inativa nao impede uma nova com o mesmo codigo.
CREATE UNIQUE INDEX ux_cage_battery_number_active ON cage (sector_id, battery, number) WHERE status = 'ACTIVE';

-- A lista paginada das gaiolas de um setor filtra pela situacao e ordena por bateria e numero (R-006).
CREATE INDEX ix_cage_sector_status_battery_number ON cage (sector_id, status, battery, number);

COMMENT ON TABLE sector IS 'Setor da granja: galpao, especie ou lote acompanhado separadamente';
COMMENT ON COLUMN sector.version IS 'Controle otimista: muda a cada escrita no setor ou numa gaiola dele';
COMMENT ON TABLE cage IS 'Gaiola de um setor; o codigo exibido e bateria-numero, derivado e nao gravado';
COMMENT ON COLUMN cage.deactivated_with_sector IS 'Verdadeiro so quando a inativacao do setor inativou a gaiola; a reativacao do setor traz de volta exatamente estas (R-004)';
