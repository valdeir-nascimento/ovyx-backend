-- Relatorio diario (DailyReport): o registro de um dia de coleta de um setor. Raiz de agregado do
-- contexto production, com as gaiolas do dia dentro (R-003). Um por setor e data (R-005). Nunca e
-- removido fisicamente (FR-021).
CREATE TABLE daily_report (
    id                     uuid         NOT NULL,
    sector_id              uuid         NOT NULL,
    collection_date        date         NOT NULL,
    collection_time        time         NOT NULL,
    opening_bird_count     integer      NOT NULL,
    flock_age              smallint     NOT NULL,
    note                   varchar(500),
    no_mortality_confirmed boolean      NOT NULL DEFAULT false,
    opened_by_id           uuid         NOT NULL,
    opened_by_name         varchar(120) NOT NULL,
    opened_at              timestamptz  NOT NULL,
    last_corrected_by_id   uuid,
    last_corrected_by_name varchar(120),
    last_corrected_at      timestamptz,
    version                bigint       NOT NULL DEFAULT 0,
    CONSTRAINT pk_daily_report PRIMARY KEY (id),
    -- O production le a estrutura da granja pelas tabelas do farm (R-004); o farm nunca apaga setor.
    CONSTRAINT fk_daily_report_sector FOREIGN KEY (sector_id) REFERENCES sector (id),
    CONSTRAINT ck_daily_report_opening_bird_count CHECK (opening_bird_count BETWEEN 1 AND 1000000),
    CONSTRAINT ck_daily_report_flock_age CHECK (flock_age BETWEEN 1 AND 150)
);

-- FR-002: um relatorio por setor e data. O indice e a rede de seguranca da corrida entre duas
-- aberturas; a regra mora no agregado (R-005). Ele tambem serve a lista, do dia mais recente para o mais
-- antigo (R-008): o PostgreSQL o percorre ao contrario.
CREATE UNIQUE INDEX ux_daily_report_sector_date ON daily_report (sector_id, collection_date);

-- Gaiola do relatorio (ReportCage): a gaiola como estava na abertura, com os lancamentos do dia.
-- Producao e mortalidade ficam nulas enquanto nao lancadas.
CREATE TABLE report_cage (
    report_id      uuid         NOT NULL,
    cage_id        uuid         NOT NULL,
    battery        varchar(3)   NOT NULL,
    number         smallint     NOT NULL,
    bird_count     integer      NOT NULL,
    eggs           integer,
    small          integer,
    jumbo          integer,
    dirty          integer,
    cracked        integer,
    blood_spot     integer,
    abnormal       integer,
    deaths         integer,
    culls          integer,
    mortality_note varchar(500),
    CONSTRAINT pk_report_cage PRIMARY KEY (report_id, cage_id),
    CONSTRAINT fk_report_cage_report FOREIGN KEY (report_id) REFERENCES daily_report (id),
    CONSTRAINT fk_report_cage_cage FOREIGN KEY (cage_id) REFERENCES cage (id),
    CONSTRAINT ck_report_cage_bird_count CHECK (bird_count BETWEEN 0 AND 1000),
    CONSTRAINT ck_report_cage_production_ranges CHECK (
        eggs BETWEEN 0 AND 1000
        AND small BETWEEN 0 AND 1000
        AND jumbo BETWEEN 0 AND 1000
        AND dirty BETWEEN 0 AND 1000
        AND cracked BETWEEN 0 AND 1000
        AND blood_spot BETWEEN 0 AND 1000
        AND abnormal BETWEEN 0 AND 1000),
    -- A producao e lancada inteira ou nao e lancada; a classificacao nao passa dos ovos (FR-008).
    CONSTRAINT ck_report_cage_production CHECK (
        (eggs IS NULL AND small IS NULL AND jumbo IS NULL AND dirty IS NULL
            AND cracked IS NULL AND blood_spot IS NULL AND abnormal IS NULL)
        OR (eggs IS NOT NULL AND small IS NOT NULL AND jumbo IS NOT NULL AND dirty IS NOT NULL
            AND cracked IS NOT NULL AND blood_spot IS NOT NULL AND abnormal IS NOT NULL
            AND small + jumbo + dirty + cracked + blood_spot + abnormal <= eggs)),
    CONSTRAINT ck_report_cage_mortality_ranges CHECK (
        deaths BETWEEN 0 AND 1000
        AND culls BETWEEN 0 AND 1000),
    -- A mortalidade e lancada inteira ou nao e lancada, e nao passa das aves da gaiola (FR-011).
    CONSTRAINT ck_report_cage_mortality CHECK (
        (deaths IS NULL AND culls IS NULL)
        OR (deaths IS NOT NULL AND culls IS NOT NULL AND deaths + culls <= bird_count))
);

COMMENT ON TABLE daily_report IS 'Relatorio diario de um setor: um por setor e data de coleta';
COMMENT ON COLUMN daily_report.collection_date IS 'Dia da coleta no fuso da granja (ovyx.production.time-zone), e nao um instante (R-006)';
COMMENT ON COLUMN daily_report.opened_by_name IS 'Nome de quem abriu, como estava na sessao (R-007)';
COMMENT ON COLUMN daily_report.version IS 'Controle otimista: muda a cada escrita no relatorio ou numa gaiola dele';
COMMENT ON TABLE report_cage IS 'Gaiola do relatorio como estava na abertura, com a producao e a mortalidade do dia';
