-- Feature 010: o dia da semana da pesagem das aves do setor (R-003). Opcional: os setores existentes ficam
-- sem dia e seguem o prazo de 7 dias desde a ultima pesagem de cada gaiola (FR-002).
ALTER TABLE sector
    ADD COLUMN weighing_day varchar(9),
    ADD CONSTRAINT ck_sector_weighing_day CHECK (
        weighing_day IN ('MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY'));

COMMENT ON COLUMN sector.weighing_day IS
    'Dia da semana da pesagem das aves do setor; nulo segue o prazo de 7 dias desde a ultima pesagem';
