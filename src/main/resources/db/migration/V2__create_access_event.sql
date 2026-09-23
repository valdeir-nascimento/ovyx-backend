-- Trilha auditavel de tentativas de autenticacao e encerramentos de sessao (FR-006).
-- Somente inclusao: nunca alterada, nunca removida.
--
-- Invariante critica: a senha informada NAO pode aparecer em nenhuma coluna desta tabela
-- (FR-021). O que se registra e o identificador tentado, nao a credencial.
CREATE TABLE access_event (
    id                   uuid         NOT NULL,
    attempted_identifier varchar(254) NOT NULL,
    caretaker_id         uuid         NULL,
    outcome              varchar(30)  NOT NULL,
    origin               varchar(60)  NOT NULL,
    occurred_at          timestamptz  NOT NULL,
    CONSTRAINT pk_access_event PRIMARY KEY (id),
    CONSTRAINT fk_access_event_caretaker FOREIGN KEY (caretaker_id) REFERENCES caretaker (id),
    CONSTRAINT ck_access_event_outcome CHECK (
        outcome IN ('GRANTED', 'INVALID_CREDENTIALS', 'INACTIVE_CARETAKER', 'THROTTLED', 'SIGNED_OUT')
    )
);

CREATE INDEX ix_access_event_occurred_at ON access_event (occurred_at DESC);

COMMENT ON TABLE access_event IS 'Auditoria de acesso; jamais contem senha';
COMMENT ON COLUMN access_event.attempted_identifier IS 'E-mail ou celular informado, como digitado';
COMMENT ON COLUMN access_event.caretaker_id IS 'Preenchido apenas quando o identificador corresponde a alguem';
COMMENT ON COLUMN access_event.outcome IS 'Causa real da falha; visivel apenas aqui, nunca na resposta ao cliente (FR-002)';
