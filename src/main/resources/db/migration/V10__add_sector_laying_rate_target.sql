-- Meta de produtividade do setor (feature 008; R-005): a porcentagem de ovos coletados sobre as aves
-- alojadas esperada por dia, de 1 a 100, com uma casa decimal. Obrigatoria em todo setor.
--
-- O DEFAULT so existe para preencher os setores que ja estavam cadastrados, com os 85% que o painel da
-- feature 006 usava para todos (FR-004). Ele sai no fim: dai em diante, quem grava a meta e sempre o
-- agregado, e a sugestao de 85% para o setor novo e so do formulario (R-004).
ALTER TABLE sector
    ADD COLUMN laying_rate_target numeric(4,1) NOT NULL DEFAULT 85.0;

ALTER TABLE sector
    ADD CONSTRAINT ck_sector_laying_rate_target CHECK (laying_rate_target BETWEEN 1 AND 100);

ALTER TABLE sector
    ALTER COLUMN laying_rate_target DROP DEFAULT;

COMMENT ON COLUMN sector.laying_rate_target IS
    'Meta de produtividade do setor: ovos coletados sobre aves alojadas esperados por dia, em porcentagem, com uma casa decimal';
