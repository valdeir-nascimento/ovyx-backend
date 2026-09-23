-- Responsavel (Caretaker): pessoa autorizada a usar o sistema e tambem ator de dominio,
-- responsavel por setores e gaiolas. Por isso nunca e removida fisicamente: registros das
-- features seguintes a referenciam (FR-018).
CREATE TABLE caretaker (
    id                   uuid         NOT NULL,
    full_name            varchar(120) NOT NULL,
    cpf                  char(11)     NOT NULL,
    email                varchar(254) NOT NULL,
    mobile_phone         varchar(11)  NOT NULL,
    password_hash        varchar(255) NOT NULL,
    role                 varchar(20)  NOT NULL,
    status               varchar(10)  NOT NULL,
    must_change_password boolean      NOT NULL DEFAULT false,
    created_at           timestamptz  NOT NULL,
    updated_at           timestamptz  NOT NULL,
    CONSTRAINT pk_caretaker PRIMARY KEY (id),
    CONSTRAINT ck_caretaker_role CHECK (role IN ('ADMINISTRATOR', 'USER')),
    CONSTRAINT ck_caretaker_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

-- FR-016: e-mail e celular sao identificadores de acesso e precisam ser unicos --
-- mas apenas entre responsaveis ATIVOS. O indice e parcial porque um responsavel
-- inativado preserva seu historico (FR-018) e nao pode impedir que um novo
-- responsavel use o mesmo e-mail.
CREATE UNIQUE INDEX ux_caretaker_email_active ON caretaker (email) WHERE status = 'ACTIVE';
CREATE UNIQUE INDEX ux_caretaker_mobile_active ON caretaker (mobile_phone) WHERE status = 'ACTIVE';

-- CPF e unico sem restricao de situacao: a mesma pessoa nao deve existir duas vezes
-- na base, ativa ou nao.
CREATE UNIQUE INDEX ux_caretaker_cpf ON caretaker (cpf);

-- Suporta a pesquisa por trecho do nome sem distincao entre maiusculas e minusculas
-- (cenario 5 da Historia 2).
CREATE INDEX ix_caretaker_full_name ON caretaker (lower(full_name));

COMMENT ON TABLE caretaker IS 'Responsavel pelo plantel e usuario do sistema';
COMMENT ON COLUMN caretaker.password_hash IS 'Hash com prefixo de algoritmo, ex.: {argon2}...; nunca texto claro';
COMMENT ON COLUMN caretaker.must_change_password IS 'Bloqueia toda operacao ate a troca da senha (FR-025)';
COMMENT ON COLUMN caretaker.status IS 'Inativacao substitui a exclusao fisica do legado';
