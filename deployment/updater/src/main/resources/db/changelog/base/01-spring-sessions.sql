--liquibase formatted sql
--changeset mboisnard:01-spring-sessions logicalFilePath:fixed splitStatements:false runInTransaction:false runOnChange:true

-- Session Schema

CREATE SCHEMA IF NOT EXISTS drinkit_session;

-- Session

CREATE TABLE IF NOT EXISTS drinkit_session.spring_session
(
    primary_id            CHAR(36) NOT NULL PRIMARY KEY,
    session_id            CHAR(36) NOT NULL,
    creation_time         BIGINT   NOT NULL,
    last_access_time      BIGINT   NOT NULL,
    max_inactive_interval INT      NOT NULL,
    expiry_time           BIGINT   NOT NULL,
    principal_name        VARCHAR(100)
);

CREATE UNIQUE INDEX IF NOT EXISTS spring_session_session_id_idx
    ON drinkit_session.spring_session (session_id);

CREATE INDEX IF NOT EXISTS spring_session_expiry_time_idx
    ON drinkit_session.spring_session (expiry_time);

CREATE INDEX IF NOT EXISTS spring_session_principal_name_idx
    ON drinkit_session.spring_session (principal_name);

CREATE TABLE IF NOT EXISTS drinkit_session.spring_session_attributes
(
    session_primary_id CHAR(36)     NOT NULL,
    attribute_name     VARCHAR(200) NOT NULL,
    attribute_bytes    BYTEA        NOT NULL,
    CONSTRAINT spring_session_attributes_pk PRIMARY KEY (session_primary_id, attribute_name),
    CONSTRAINT spring_session_attributes_fk FOREIGN KEY (session_primary_id)
        REFERENCES drinkit_session.spring_session (primary_id) ON DELETE CASCADE
);