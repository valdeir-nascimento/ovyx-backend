-- Tabelas de sessao do Spring Session JDBC.
--
-- DDL oficial, extraido de spring-session-jdbc-4.1.1.jar
-- (org/springframework/session/jdbc/schema-postgresql.sql). Vem por migracao versionada, e nao
-- por initialize-schema, porque NFR-005 exige que todo schema seja criado e evoluido por
-- migracao -- inclusive o de terceiros. Em application.yml,
-- spring.session.jdbc.initialize-schema: never.
--
-- A sessao vive no servidor por exigencia de FR-004: no logout a credencial anterior precisa
-- deixar de ser aceita imediatamente, o que um token stateless nao entrega. Ver R-002.
CREATE TABLE SPRING_SESSION (
    PRIMARY_ID            CHAR(36) NOT NULL,
    SESSION_ID            CHAR(36) NOT NULL,
    CREATION_TIME         BIGINT NOT NULL,
    LAST_ACCESS_TIME      BIGINT NOT NULL,
    MAX_INACTIVE_INTERVAL INT NOT NULL,
    EXPIRY_TIME           BIGINT NOT NULL,
    PRINCIPAL_NAME        VARCHAR(100),
    CONSTRAINT SPRING_SESSION_PK PRIMARY KEY (PRIMARY_ID)
);

CREATE UNIQUE INDEX SPRING_SESSION_IX1 ON SPRING_SESSION (SESSION_ID);
CREATE INDEX SPRING_SESSION_IX2 ON SPRING_SESSION (EXPIRY_TIME);
CREATE INDEX SPRING_SESSION_IX3 ON SPRING_SESSION (PRINCIPAL_NAME);

CREATE TABLE SPRING_SESSION_ATTRIBUTES (
    SESSION_PRIMARY_ID CHAR(36) NOT NULL,
    ATTRIBUTE_NAME     VARCHAR(200) NOT NULL,
    ATTRIBUTE_BYTES    BYTEA NOT NULL,
    CONSTRAINT SPRING_SESSION_ATTRIBUTES_PK PRIMARY KEY (SESSION_PRIMARY_ID, ATTRIBUTE_NAME),
    CONSTRAINT SPRING_SESSION_ATTRIBUTES_FK FOREIGN KEY (SESSION_PRIMARY_ID)
        REFERENCES SPRING_SESSION (PRIMARY_ID) ON DELETE CASCADE
);
