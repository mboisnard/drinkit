--liquibase formatted sql
--changeset mboisnard:01-initial-schema logicalFilePath:fixed splitStatements:false runInTransaction:false runOnChange:true

-- Application Schema

CREATE SCHEMA IF NOT EXISTS drinkit_application;

-- Cellar

CREATE TABLE IF NOT EXISTS drinkit_application.cellar
(
    id                    VARCHAR(24)      NOT NULL PRIMARY KEY,
    owner_id              VARCHAR(24)      NOT NULL,
    name                  VARCHAR(100)     NOT NULL,
    location              JSONB            NOT NULL,
    rooms                 JSONB            NOT NULL,
    modified              TIMESTAMP        NOT NULL
);

CREATE INDEX IF NOT EXISTS cellar_owner_id_idx
    ON drinkit_application.cellar (owner_id);

-- User Events

CREATE TABLE IF NOT EXISTS drinkit_application.user_event
(
    user_id        VARCHAR(24)              NOT NULL,
    sequence_id    BIGINT                   NOT NULL,
    event_name     VARCHAR(50)              NOT NULL,
    date           TIMESTAMP WITH TIME ZONE NOT NULL,
    author         JSONB                    NOT NULL,
    payload        JSONB                    NOT NULL,
    PRIMARY KEY (user_id, sequence_id)
);

-- User

CREATE TABLE IF NOT EXISTS drinkit_application.user
(
    id             VARCHAR(24)              NOT NULL PRIMARY KEY,
    firstname      VARCHAR(100),
    lastname       VARCHAR(100),
    birthdate      DATE,
    email          VARCHAR(255)             NOT NULL,
    password       VARCHAR(100)             NOT NULL,
    lastConnection TIMESTAMP WITH TIME ZONE,
    status         VARCHAR(50)              NOT NULL,
    verified       BOOLEAN                  NOT NULL,
    enabled        BOOLEAN                  NOT NULL,
    roles          VARCHAR(50) ARRAY        NOT NULL,
    modified       TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT roles_check CHECK (roles::VARCHAR[] && ARRAY['ROLE_ADMIN'::VARCHAR, 'ROLE_USER'::VARCHAR, 'ROLE_REGISTRATION_IN_PROGRESS'::VARCHAR])
);

CREATE INDEX IF NOT EXISTS user_email_idx
    ON drinkit_application.user (email);

CREATE INDEX IF NOT EXISTS user_enabled_idx
    ON drinkit_application.user (enabled);

-- Verification Token

CREATE TABLE IF NOT EXISTS drinkit_application.verification_token
(
    user_id     VARCHAR(24)                 NOT NULL PRIMARY KEY,
    token       VARCHAR(100)                NOT NULL,
    expiry_date TIMESTAMP WITH TIME ZONE    NOT NULL,
    CONSTRAINT fk_user FOREIGN KEY (user_id) REFERENCES drinkit_application.user (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS verification_token_user_id_token_expiry_date_idx
    ON drinkit_application.verification_token (user_id, token, expiry_date);
