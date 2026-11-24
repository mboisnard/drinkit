--liquibase formatted sql
--changeset mboisnard:01-starters logicalFilePath:fixed runInTransaction:false runOnChange:true

SET lock_timeout = '5s';
SET statement_timeout = '1min';

-- Starters Schema

CREATE SCHEMA IF NOT EXISTS drinkit_starters;

-- Configuration

CREATE TABLE IF NOT EXISTS drinkit_starters.configuration
(
    key        VARCHAR(255) NOT NULL PRIMARY KEY,
    value      JSONB        NOT NULL,
    modified   TIMESTAMP WITH TIME ZONE NOT NULL
);

-- Search index rebuilds, one row per running rebuild, which any instance marks its batches done in

CREATE TABLE IF NOT EXISTS drinkit_starters.search_index_rebuild
(
    index_name     TEXT                     NOT NULL PRIMARY KEY,
    rebuild_id     UUID                     NOT NULL,
    total_batches  BIGINT                   NOT NULL,
    done_batches   BIGINT[]                 NOT NULL,
    started        TIMESTAMP WITH TIME ZONE NOT NULL
);

-- Idempotency keys, one row per key an entry point took, forgotten after the retention of idempotency-starter

CREATE TABLE IF NOT EXISTS drinkit_starters.idempotency_key
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
    ON drinkit_starters.idempotency_key (taken_at);

-- Rate limits, one row per rule, key and limit of rate-limit-starter, forgotten once its theoretical arrival passed

CREATE TABLE IF NOT EXISTS drinkit_starters.rate_limit
(
    rule                 TEXT                     NOT NULL,
    key_hash             TEXT                     NOT NULL,
    period               TEXT                     NOT NULL,
    theoretical_arrival  TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (rule, key_hash, period)
);

CREATE INDEX CONCURRENTLY IF NOT EXISTS rate_limit_theoretical_arrival_idx
    ON drinkit_starters.rate_limit (theoretical_arrival);

-- Stored files, one row per file uploaded through file-storage-starter: its object lives in the configured storage

CREATE TABLE IF NOT EXISTS drinkit_starters.stored_file
(
    id            VARCHAR(26)              PRIMARY KEY,
    usage         TEXT                     NOT NULL,
    owner_id      TEXT                     NOT NULL,
    status        TEXT                     NOT NULL,
    content_type  TEXT                     NOT NULL,
    size          BIGINT                   NOT NULL,
    name          TEXT                     NOT NULL,
    uploaded_at   TIMESTAMP WITH TIME ZONE NOT NULL,
    resource_type TEXT,
    resource_id   TEXT
);

CREATE INDEX CONCURRENTLY IF NOT EXISTS stored_file_uploaded_at_idx
    ON drinkit_starters.stored_file (uploaded_at) WHERE status <> 'KEPT';

-- Grants

-- The rows to applications, never the schema itself: ON ALL TABLES covers the tables above, the default privileges
-- those a later file creates
GRANT USAGE ON SCHEMA drinkit_starters TO applications;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA drinkit_starters TO applications;
ALTER DEFAULT PRIVILEGES IN SCHEMA drinkit_starters GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO applications;
ALTER DEFAULT PRIVILEGES IN SCHEMA drinkit_starters GRANT USAGE, SELECT ON SEQUENCES TO applications;
