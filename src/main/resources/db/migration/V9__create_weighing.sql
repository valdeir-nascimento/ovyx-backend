-- Faixa de peso de referencia do setor (ReferenceWeight, R-006 da 005): o peso minimo e o maximo esperados
-- das aves, em gramas. Opcional: os dois limites vao juntos, e o minimo fica abaixo do maximo.
ALTER TABLE sector
    ADD COLUMN reference_weight_min integer,
    ADD COLUMN reference_weight_max integer,
    ADD CONSTRAINT ck_sector_reference_weight CHECK (
        (reference_weight_min IS NULL AND reference_weight_max IS NULL)
        -- Os IS NOT NULL sao explicitos: com um limite nulo, o BETWEEN daria nulo, e o CHECK aceitaria.
        OR (reference_weight_min IS NOT NULL AND reference_weight_max IS NOT NULL
            AND reference_weight_min BETWEEN 1 AND 10000
            AND reference_weight_max BETWEEN 1 AND 10000
            AND reference_weight_min < reference_weight_max));

-- Pesagem (Weighing): o peso medio de uma amostra de aves de uma gaiola numa data. Raiz de agregado do
-- contexto farm (R-001 e R-003 da 005). Nunca e removida fisicamente: a excluida fica anulada (VOIDED),
-- com quem a anulou e quando.
CREATE TABLE weighing (
    id                     uuid          NOT NULL,
    sector_id              uuid          NOT NULL,
    cage_id                uuid          NOT NULL,
    weighed_on             date          NOT NULL,
    average_weight         numeric(6, 1) NOT NULL,
    status                 varchar(16)   NOT NULL,
    recorded_by_id         uuid          NOT NULL,
    recorded_by_name       varchar(120)  NOT NULL,
    recorded_at            timestamptz   NOT NULL,
    last_corrected_by_id   uuid,
    last_corrected_by_name varchar(120),
    last_corrected_at      timestamptz,
    voided_by_id           uuid,
    voided_by_name         varchar(120),
    voided_at              timestamptz,
    version                bigint        NOT NULL DEFAULT 0,
    CONSTRAINT pk_weighing PRIMARY KEY (id),
    CONSTRAINT fk_weighing_sector FOREIGN KEY (sector_id) REFERENCES sector (id),
    CONSTRAINT fk_weighing_cage FOREIGN KEY (cage_id) REFERENCES cage (id),
    CONSTRAINT ck_weighing_average_weight CHECK (average_weight BETWEEN 1 AND 10000),
    CONSTRAINT ck_weighing_status CHECK (status IN ('VALID', 'VOIDED')),
    -- A correcao e marcada inteira: quem e quando.
    CONSTRAINT ck_weighing_correction CHECK (
        (last_corrected_by_id IS NULL AND last_corrected_by_name IS NULL AND last_corrected_at IS NULL)
        OR (last_corrected_by_id IS NOT NULL AND last_corrected_by_name IS NOT NULL
            AND last_corrected_at IS NOT NULL)),
    -- Quem anulou e quando, so na anulada, e sempre nela.
    CONSTRAINT ck_weighing_voiding CHECK (
        (status = 'VALID' AND voided_by_id IS NULL AND voided_by_name IS NULL AND voided_at IS NULL)
        OR (status = 'VOIDED' AND voided_by_id IS NOT NULL AND voided_by_name IS NOT NULL
            AND voided_at IS NOT NULL))
);

-- FR-004 e R-005 da 005: uma pesagem valida por gaiola e data. A anulada sai do indice, e a data dela fica
-- livre. O indice e a rede de seguranca da corrida entre dois registros; a regra mora no agregado. Serve
-- tambem a busca da ultima pesagem de cada gaiola (R-009).
CREATE UNIQUE INDEX ux_weighing_cage_day ON weighing (cage_id, weighed_on) WHERE status = 'VALID';

COMMENT ON COLUMN sector.reference_weight_min IS 'Peso minimo de referencia das aves do setor, em gramas';
COMMENT ON COLUMN sector.reference_weight_max IS 'Peso maximo de referencia das aves do setor, em gramas';
COMMENT ON TABLE weighing IS 'Pesagem de uma gaiola: o peso medio de uma amostra de aves numa data';
COMMENT ON COLUMN weighing.average_weight IS 'Peso medio da amostra, em gramas, com uma casa decimal';
COMMENT ON COLUMN weighing.status IS 'VALID ou VOIDED; a anulada fica guardada e sai das leituras';
COMMENT ON COLUMN weighing.recorded_by_name IS 'Nome de quem registrou, como estava na sessao';
COMMENT ON COLUMN weighing.version IS 'Controle otimista: muda a cada escrita na pesagem';
