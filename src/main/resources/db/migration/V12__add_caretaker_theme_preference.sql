-- Feature 011: a preferencia de tema de cada responsavel (R-002). Todos comecam igual ao sistema, o
-- comportamento de antes da feature (FR-006).
ALTER TABLE caretaker
    ADD COLUMN theme_preference varchar(6) NOT NULL DEFAULT 'SYSTEM',
    ADD CONSTRAINT ck_caretaker_theme_preference CHECK (theme_preference IN ('LIGHT', 'DARK', 'SYSTEM'));

COMMENT ON COLUMN caretaker.theme_preference IS
    'Preferencia de tema do responsavel: claro, escuro ou igual ao sistema';
