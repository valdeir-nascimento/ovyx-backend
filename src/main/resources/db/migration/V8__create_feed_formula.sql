-- Formula de racao (FeedFormula): a racao que a granja compra, com o preco por quilo e o consumo esperado
-- por ave ao dia. Raiz de agregado do contexto farm (R-001 da 004). Nunca e removida fisicamente: e
-- inativada e reativada.
CREATE TABLE feed_formula (
    id              uuid          NOT NULL,
    name            varchar(80)   NOT NULL,
    price_per_kg    numeric(7, 2) NOT NULL,
    expected_intake smallint      NOT NULL,
    description     varchar(500),
    status          varchar(10)   NOT NULL,
    created_at      timestamptz   NOT NULL,
    updated_at      timestamptz   NOT NULL,
    version         bigint        NOT NULL DEFAULT 0,
    CONSTRAINT pk_feed_formula PRIMARY KEY (id),
    CONSTRAINT ck_feed_formula_price_per_kg CHECK (price_per_kg BETWEEN 0.01 AND 1000.00),
    CONSTRAINT ck_feed_formula_expected_intake CHECK (expected_intake BETWEEN 1 AND 200),
    CONSTRAINT ck_feed_formula_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

-- R-011 da 004: o nome e unico entre todas as formulas, ativas e inativas, sem diferenciar maiusculas. O
-- indice e a rede de seguranca da corrida entre dois cadastros; a regra mora no agregado.
CREATE UNIQUE INDEX ux_feed_formula_name ON feed_formula (lower(name));

-- Racao da gaiola no relatorio (FeedEntry): a formula, o preco e o consumo esperado dela quando o
-- lancamento foi feito (R-005 da 004), e o consumo do dia em gramas.
ALTER TABLE report_cage
    ADD COLUMN feed_formula_id      uuid,
    ADD COLUMN feed_price_per_kg    numeric(7, 2),
    ADD COLUMN feed_expected_intake smallint,
    ADD COLUMN feed_consumption     integer,
    ADD CONSTRAINT fk_report_cage_feed_formula FOREIGN KEY (feed_formula_id) REFERENCES feed_formula (id),
    ADD CONSTRAINT ck_report_cage_feed_consumption CHECK (feed_consumption BETWEEN 0 AND 50000),
    -- A racao e lancada inteira ou nao e lancada.
    ADD CONSTRAINT ck_report_cage_feed CHECK (
        (feed_formula_id IS NULL AND feed_price_per_kg IS NULL AND feed_expected_intake IS NULL
            AND feed_consumption IS NULL)
        OR (feed_formula_id IS NOT NULL AND feed_price_per_kg IS NOT NULL AND feed_expected_intake IS NOT NULL
            AND feed_consumption IS NOT NULL));

COMMENT ON TABLE feed_formula IS 'Formula de racao da granja: preco por quilo e consumo esperado por ave ao dia';
COMMENT ON COLUMN feed_formula.price_per_kg IS 'Preco atual, em reais por quilo; os lancamentos guardam o preco da epoca';
COMMENT ON COLUMN feed_formula.expected_intake IS 'Gramas de racao por ave ao dia';
COMMENT ON COLUMN feed_formula.version IS 'Controle otimista: muda a cada escrita na formula';
COMMENT ON COLUMN report_cage.feed_price_per_kg IS 'Preco por quilo da formula quando a racao foi lancada (R-005 da 004)';
COMMENT ON COLUMN report_cage.feed_expected_intake IS 'Consumo esperado da formula quando a racao foi lancada (R-005 da 004)';
COMMENT ON COLUMN report_cage.feed_consumption IS 'Gramas de racao da gaiola no dia';
