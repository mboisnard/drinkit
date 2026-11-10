--liquibase formatted sql
--changeset mboisnard:01-initial-schema logicalFilePath:fixed runInTransaction:false runOnChange:true

SET lock_timeout = '5s';
SET statement_timeout = '1min';

-- Application Schema

CREATE SCHEMA IF NOT EXISTS drinkit_application;

-- Cellar

CREATE TABLE IF NOT EXISTS drinkit_application.cellar
(
    id                    VARCHAR(24)              NOT NULL PRIMARY KEY,
    owner_id              VARCHAR(24)              NOT NULL,
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
    lastConnection TIMESTAMP WITH TIME ZONE,
    status         VARCHAR(50)              NOT NULL,
    enabled        BOOLEAN                  NOT NULL,
    roles          VARCHAR(50) ARRAY        NOT NULL,
    modified       TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT roles_check CHECK (roles::VARCHAR[] <@ ARRAY['ROLE_ADMIN'::VARCHAR, 'ROLE_USER'::VARCHAR, 'ROLE_REGISTRATION_IN_PROGRESS'::VARCHAR] AND cardinality(roles) > 0)
);

CREATE INDEX CONCURRENTLY IF NOT EXISTS user_email_idx
    ON drinkit_application.user (email);

CREATE INDEX CONCURRENTLY IF NOT EXISTS user_enabled_idx
    ON drinkit_application.user (enabled);

-- Configuration

CREATE TABLE IF NOT EXISTS drinkit_application.configuration
(
    key        VARCHAR(255) NOT NULL PRIMARY KEY,
    value      JSONB        NOT NULL,
    modified   TIMESTAMP WITH TIME ZONE NOT NULL
);

-- Exchange Rates

CREATE TABLE IF NOT EXISTS drinkit_application.exchange_rate
(
    source_currency   VARCHAR(3)       NOT NULL,
    target_currency   VARCHAR(3)       NOT NULL,
    rate              DECIMAL(18, 8)   NOT NULL,  -- Decimal Precision linked to EXCHANGE_RATE_SCALE_PRECISION
    modified          TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (source_currency, target_currency)
);

-- Search index rebuilds, one row per running rebuild, which any instance marks its batches done in

CREATE TABLE IF NOT EXISTS drinkit_application.search_index_rebuild
(
    index_name     TEXT                     NOT NULL PRIMARY KEY,
    rebuild_id     UUID                     NOT NULL,
    total_batches  BIGINT                   NOT NULL,
    done_batches   BIGINT[]                 NOT NULL,
    started        TIMESTAMP WITH TIME ZONE NOT NULL
);

-- Idempotency keys, one row per key an entry point took, forgotten after the retention of idempotency-starter

CREATE TABLE IF NOT EXISTS drinkit_application.idempotency_key
(
    scope         TEXT                     NOT NULL,
    key_hash      TEXT                     NOT NULL,
    owner         UUID                     NOT NULL,
    fingerprint   TEXT                     NOT NULL,
    taken_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    lease_until   TIMESTAMP WITH TIME ZONE NOT NULL,
    completed_at  TIMESTAMP WITH TIME ZONE,
    outcome       BYTEA,
    PRIMARY KEY (scope, key_hash)
);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idempotency_key_taken_at_idx
    ON drinkit_application.idempotency_key (taken_at);

-- Rate limits, one row per rule, key and limit of rate-limit-starter, forgotten once its theoretical arrival passed

CREATE TABLE IF NOT EXISTS drinkit_application.rate_limit
(
    rule                 TEXT                     NOT NULL,
    key_hash             TEXT                     NOT NULL,
    period               TEXT                     NOT NULL,
    theoretical_arrival  TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (rule, key_hash, period)
);

CREATE INDEX CONCURRENTLY IF NOT EXISTS rate_limit_theoretical_arrival_idx
    ON drinkit_application.rate_limit (theoretical_arrival);
