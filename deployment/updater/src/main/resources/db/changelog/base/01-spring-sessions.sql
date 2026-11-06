--liquibase formatted sql
--changeset mboisnard:01-spring-sessions logicalFilePath:fixed runInTransaction:false runOnChange:true

-- The tables of Spring Session JDBC as its schema-postgresql.sql ships them, but for a session id of 43 characters
-- and attributes stored in JSON, and the remember-me logins of security-starter
-- squawk-ignore-file ban-char-field, prefer-bigint-over-int

SET lock_timeout = '5s';
SET statement_timeout = '1min';

-- Session Schema

CREATE SCHEMA IF NOT EXISTS drinkit_session;

-- Session

CREATE TABLE IF NOT EXISTS drinkit_session.spring_session
(
    primary_id            CHAR(36)    NOT NULL PRIMARY KEY,
    session_id            VARCHAR(64) NOT NULL,
    creation_time         BIGINT      NOT NULL,
    last_access_time      BIGINT      NOT NULL,
    max_inactive_interval INT         NOT NULL,
    expiry_time           BIGINT      NOT NULL,
    principal_name        VARCHAR(100)
);

CREATE UNIQUE INDEX CONCURRENTLY IF NOT EXISTS spring_session_session_id_idx
    ON drinkit_session.spring_session (session_id);

CREATE INDEX CONCURRENTLY IF NOT EXISTS spring_session_expiry_time_idx
    ON drinkit_session.spring_session (expiry_time);

CREATE INDEX CONCURRENTLY IF NOT EXISTS spring_session_principal_name_idx
    ON drinkit_session.spring_session (principal_name);

CREATE TABLE IF NOT EXISTS drinkit_session.spring_session_attributes
(
    session_primary_id CHAR(36)     NOT NULL,
    attribute_name     VARCHAR(200) NOT NULL,
    attribute_bytes    JSONB        NOT NULL,
    CONSTRAINT spring_session_attributes_pk PRIMARY KEY (session_primary_id, attribute_name),
    CONSTRAINT spring_session_attributes_fk FOREIGN KEY (session_primary_id)
        REFERENCES drinkit_session.spring_session (primary_id) ON DELETE CASCADE
);

-- Remember-me token: one series per remembered browser, whose token turns at each visit and is stored hashed, in
-- hexadecimal

CREATE TABLE IF NOT EXISTS drinkit_session.remember_me_token
(
    series              VARCHAR(64)              NOT NULL PRIMARY KEY,
    user_id             VARCHAR(100)             NOT NULL,
    token_hash          VARCHAR(64)              NOT NULL,
    previous_token_hash VARCHAR(64),
    rotated_at          TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL,
    user_agent          VARCHAR(256)
);

CREATE INDEX CONCURRENTLY IF NOT EXISTS remember_me_token_user_id_idx
    ON drinkit_session.remember_me_token (user_id);

-- Sign-in code: one live code per login, sent by email and stored as its hash in hexadecimal. A new request
-- replaces the code, but not the tries the login has left until attempts_reset_at, so that asking for code after
-- code gives no new tries. A row goes once its code has expired and its tries are reset

CREATE TABLE IF NOT EXISTS drinkit_session.sign_in_code
(
    login             VARCHAR(320)             NOT NULL PRIMARY KEY,
    code_hash         VARCHAR(64)              NOT NULL,
    expires_at        TIMESTAMP WITH TIME ZONE NOT NULL,
    attempts_left     INT                      NOT NULL,
    attempts_reset_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX CONCURRENTLY IF NOT EXISTS sign_in_code_attempts_reset_at_idx
    ON drinkit_session.sign_in_code (attempts_reset_at);
