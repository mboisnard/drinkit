--liquibase formatted sql
--changeset mboisnard:01-initial-schema logicalFilePath:fixed splitStatements:false runInTransaction:false runOnChange:true

-- Cellar

CREATE TABLE IF NOT EXISTS public.cellar
(
    id                    VARCHAR(24)      NOT NULL PRIMARY KEY,
    owner_id              VARCHAR(24)      NOT NULL,
    name                  VARCHAR(100)     NOT NULL,
    location              JSONB            NOT NULL,
    rooms                 JSONB            NOT NULL,
    modified              TIMESTAMP        NOT NULL
);

CREATE INDEX IF NOT EXISTS cellar_owner_id_idx
    ON public.cellar (owner_id);


-- User

CREATE TABLE IF NOT EXISTS public.user
(
    id             VARCHAR(24)  NOT NULL PRIMARY KEY,
    firstname      VARCHAR(100),
    lastname       VARCHAR(100),
    birthdate      DATE,
    email          VARCHAR(255) NOT NULL,
    password       VARCHAR(100) NOT NULL,
    lastConnection TIMESTAMP,
    status         VARCHAR(50)  NOT NULL,
    completed      BOOLEAN      NOT NULL,
    enabled        BOOLEAN      NOT NULL,
    modified       TIMESTAMP    NOT NULL
);

CREATE INDEX IF NOT EXISTS user_email_idx
    ON public.user (email);

CREATE INDEX IF NOT EXISTS user_completed_idx
    ON public.user (completed);

CREATE INDEX IF NOT EXISTS user_enabled_idx
    ON public.user (enabled);

-- Role

CREATE TABLE IF NOT EXISTS public.role
(
    user_id   VARCHAR(24) NOT NULL,
    authority VARCHAR(50) NOT NULL,
    CONSTRAINT fk_user FOREIGN KEY (user_id) REFERENCES public.user (id) ON DELETE CASCADE
);

CREATE UNIQUE INDEX IF NOT EXISTS user_id_authority_idx
    ON public.role (user_id, authority);

-- Verification Token

CREATE TABLE IF NOT EXISTS public.verification_token
(
    user_id     VARCHAR(24)  NOT NULL PRIMARY KEY,
    token       VARCHAR(100) NOT NULL,
    expiry_date TIMESTAMP    NOT NULL,
    CONSTRAINT fk_user FOREIGN KEY (user_id) REFERENCES public.user (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS verification_token_user_id_token_expiry_date_idx
    ON public.verification_token (user_id, token, expiry_date);
