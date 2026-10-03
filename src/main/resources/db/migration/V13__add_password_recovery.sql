-- Feature 012: a recuperacao da senha pelo e-mail (R-001, R-004, R-005 e R-007).
--
-- O link pendente mora no proprio responsavel: no maximo um por conta, e o pedido novo o
-- substitui. O codigo do link NUNCA e guardado, so o resumo SHA-256 dele: quem le o banco nao
-- tem como montar o link (FR-009).
ALTER TABLE caretaker
    ADD COLUMN recovery_token_hash char(64) NULL,
    ADD COLUMN recovery_expires_at timestamptz NULL,
    ADD COLUMN recovery_window_started_at timestamptz NULL,
    ADD COLUMN recovery_requests_in_window smallint NOT NULL DEFAULT 0,
    ADD COLUMN session_generation integer NOT NULL DEFAULT 0,
    ADD CONSTRAINT ck_caretaker_recovery_link CHECK (
        (recovery_token_hash IS NULL) = (recovery_expires_at IS NULL)
    ),
    ADD CONSTRAINT ck_caretaker_recovery_requests CHECK (recovery_requests_in_window BETWEEN 0 AND 3),
    ADD CONSTRAINT ck_caretaker_session_generation CHECK (session_generation >= 0);

CREATE UNIQUE INDEX ux_caretaker_recovery_token_hash ON caretaker (recovery_token_hash)
    WHERE recovery_token_hash IS NOT NULL;

COMMENT ON COLUMN caretaker.recovery_token_hash IS
    'Resumo SHA-256 do codigo do link de recuperacao pendente; o codigo nunca e guardado';
COMMENT ON COLUMN caretaker.recovery_expires_at IS
    'Fim da validade do link pendente, 30 minutos depois do pedido; nulo junto com o resumo';
COMMENT ON COLUMN caretaker.recovery_window_started_at IS
    'Inicio da hora em que os links emitidos sao contados para o limite de 3 (FR-014)';
COMMENT ON COLUMN caretaker.recovery_requests_in_window IS
    'Links emitidos na hora contada; nunca passa de 3';
COMMENT ON COLUMN caretaker.session_generation IS
    'Soma 1 a cada redefinicao pelo link; a sessao aberta com outra geracao e encerrada';

-- Contencao dos pedidos de recuperacao e das tentativas com link invalido, por origem (FR-015).
-- Como a sign_in_attempt da V3: persistida em banco para sobreviver a reinicio e funcionar com
-- mais de uma instancia. Ao estourar o limite, a resposta nao muda: quem tenta nao fica sabendo.
CREATE TABLE recovery_attempt (
    origin            varchar(60) NOT NULL,
    attempt_count     integer     NOT NULL DEFAULT 0,
    window_started_at timestamptz NOT NULL,
    blocked_until     timestamptz NULL,
    CONSTRAINT pk_recovery_attempt PRIMARY KEY (origin),
    CONSTRAINT ck_recovery_attempt_count CHECK (attempt_count >= 0)
);

CREATE INDEX ix_recovery_attempt_blocked_until ON recovery_attempt (blocked_until)
    WHERE blocked_until IS NOT NULL;

COMMENT ON TABLE recovery_attempt IS 'Janela de tentativas de recuperacao de senha por origem';
COMMENT ON COLUMN recovery_attempt.attempt_count IS 'Pedidos e links invalidos na janela de 15 minutos';
COMMENT ON COLUMN recovery_attempt.blocked_until IS 'Preenchido ao estourar o limite; invisivel para quem tenta';

-- A auditoria passa a registrar a recuperacao (R-007). O link e a senha continuam sem coluna.
ALTER TABLE access_event DROP CONSTRAINT ck_access_event_outcome;
ALTER TABLE access_event ADD CONSTRAINT ck_access_event_outcome CHECK (
    outcome IN (
        'GRANTED', 'INVALID_CREDENTIALS', 'INACTIVE_CARETAKER', 'THROTTLED', 'SIGNED_OUT',
        'RECOVERY_LINK_SENT', 'RECOVERY_UNKNOWN_EMAIL', 'RECOVERY_INACTIVE', 'RECOVERY_LIMITED',
        'RECOVERY_THROTTLED', 'RECOVERY_DELIVERY_FAILED', 'PASSWORD_RECOVERED', 'RECOVERY_LINK_REFUSED'
    )
);
