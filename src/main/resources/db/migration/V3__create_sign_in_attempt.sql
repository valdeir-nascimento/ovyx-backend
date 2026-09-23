-- Contencao de tentativas repetidas de autenticacao (FR-023).
--
-- Janela deslizante por combinacao de identificador tentado e origem. Persistida em banco,
-- e nao em memoria, para sobreviver a reinicio da aplicacao e funcionar com mais de uma
-- instancia.
--
-- Importante: ao estourar o limite, a resposta ao cliente continua sendo a mesma mensagem
-- generica de credencial invalida. O autor da tentativa nunca fica sabendo que atingiu um
-- limite, porque isso confirmaria a existencia do identificador e quebraria FR-002.
CREATE TABLE sign_in_attempt (
    attempted_identifier varchar(254) NOT NULL,
    origin               varchar(60)  NOT NULL,
    failure_count        integer      NOT NULL DEFAULT 0,
    window_started_at    timestamptz  NOT NULL,
    blocked_until        timestamptz  NULL,
    CONSTRAINT pk_sign_in_attempt PRIMARY KEY (attempted_identifier, origin),
    CONSTRAINT ck_sign_in_attempt_failure_count CHECK (failure_count >= 0)
);

CREATE INDEX ix_sign_in_attempt_blocked_until ON sign_in_attempt (blocked_until)
    WHERE blocked_until IS NOT NULL;

COMMENT ON TABLE sign_in_attempt IS 'Janela deslizante de falhas de autenticacao por identificador e origem';
COMMENT ON COLUMN sign_in_attempt.failure_count IS 'Zerado em autenticacao bem-sucedida';
COMMENT ON COLUMN sign_in_attempt.blocked_until IS 'Preenchido ao estourar o limite; invisivel para quem tenta';
