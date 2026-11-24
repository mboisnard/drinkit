--liquibase formatted sql
--changeset mboisnard:01-application logicalFilePath:fixed runInTransaction:false runOnChange:true

SET lock_timeout = '5s';
SET statement_timeout = '1min';

-- Application Schema

CREATE SCHEMA IF NOT EXISTS drinkit_application;

-- Cellar

CREATE TABLE IF NOT EXISTS drinkit_application.cellar
(
    id                    VARCHAR(26)              NOT NULL PRIMARY KEY,
    owner_id              VARCHAR(26)              NOT NULL,
    name                  VARCHAR(100)             NOT NULL,
    location              JSONB                    NOT NULL,
    rooms                 JSONB                    NOT NULL,
    modified              TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX CONCURRENTLY IF NOT EXISTS cellar_owner_id_idx
    ON drinkit_application.cellar (owner_id);

-- User Events

CREATE TABLE IF NOT EXISTS drinkit_application.user_event
(
    user_id        VARCHAR(26)              NOT NULL,
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
    id             VARCHAR(26)              NOT NULL PRIMARY KEY,
    firstname      VARCHAR(100),
    lastname       VARCHAR(100),
    birthdate      DATE,
    email          VARCHAR(255)             NOT NULL,
    lastConnection TIMESTAMP WITH TIME ZONE,
    status         VARCHAR(50)              NOT NULL,
    enabled        BOOLEAN                  NOT NULL,
    roles          VARCHAR(50) ARRAY        NOT NULL,
    preferences    JSONB                    NOT NULL,
    modified       TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT roles_check CHECK (roles::VARCHAR[] <@ ARRAY['ROLE_ADMIN'::VARCHAR, 'ROLE_USER'::VARCHAR, 'ROLE_REGISTRATION_IN_PROGRESS'::VARCHAR] AND cardinality(roles) > 0)
);

CREATE INDEX CONCURRENTLY IF NOT EXISTS user_email_idx
    ON drinkit_application.user (email);

CREATE INDEX CONCURRENTLY IF NOT EXISTS user_enabled_idx
    ON drinkit_application.user (enabled);

-- Exchange Rates

CREATE TABLE IF NOT EXISTS drinkit_application.exchange_rate
(
    source_currency   VARCHAR(3)       NOT NULL,
    target_currency   VARCHAR(3)       NOT NULL,
    rate              DECIMAL(18, 8)   NOT NULL,  -- Decimal Precision linked to EXCHANGE_RATE_SCALE_PRECISION
    modified          TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (source_currency, target_currency)
);

-- Grants

-- The rows to applications, never the schema itself: ON ALL TABLES covers the tables above, the default privileges
-- those a later file creates
GRANT USAGE ON SCHEMA drinkit_application TO applications;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA drinkit_application TO applications;
ALTER DEFAULT PRIVILEGES IN SCHEMA drinkit_application GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO applications;
ALTER DEFAULT PRIVILEGES IN SCHEMA drinkit_application GRANT USAGE, SELECT ON SEQUENCES TO applications;
