--liquibase formatted sql
--changeset mboisnard:01-initial-schema logicalFilePath:fixed splitStatements:false runInTransaction:false runOnChange:true

CREATE TABLE IF NOT EXISTS public.cellar(
    id VARCHAR(24) NOT NULL PRIMARY KEY,
    owner_id VARCHAR(24) NOT NULL,
    name VARCHAR(100) NOT NULL,
    location_city VARCHAR(200) NOT NULL,
    location_country VARCHAR(100) NOT NULL,
    location_country_code VARCHAR(2) NOT NULL,
    location_latitude DOUBLE PRECISION NOT NULL,
    location_longitude DOUBLE PRECISION NOT NULL,
    modified TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS cellar_owner_id_idx
    ON public.cellar (owner_id);
