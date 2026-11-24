--liquibase formatted sql
--changeset mboisnard:00-roles logicalFilePath:fixed runInTransaction:false runOnChange:true runAlways:true splitStatements:false

-- Runs at each start, so that a new DRINKIT_BACKEND_PASSWORD reaches the role without editing this file. Liquibase reads
-- the environment as parameters, and the updater stops when one is missing

SET lock_timeout = '5s';
SET statement_timeout = '1min';

-- applications holds the privileges and cannot sign in; each application signs in with a role of its own, a member of
-- applications, so that its password, settings and activity stay its own. CREATE ROLE has no IF NOT EXISTS
DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'applications') THEN
        CREATE ROLE applications NOLOGIN;
    END IF;
    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'drinkit-backend') THEN
        CREATE ROLE "drinkit-backend";
    END IF;
END $$;

-- The role signs in, with the password of the environment: its access changes only when that password does
-- squawk-ignore ban-alter-role-options
ALTER ROLE "drinkit-backend" WITH LOGIN PASSWORD '${DRINKIT_BACKEND_PASSWORD}';
GRANT applications TO "drinkit-backend";
