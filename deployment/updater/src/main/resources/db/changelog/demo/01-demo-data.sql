--liquibase formatted sql
--changeset demo:01-demo-data logicalFilePath:fixed runInTransaction:false runOnChange:true

-- demo@drinkit.test, password Drinkit-demo1: a verified admin with a cellar, as the API would have written them
SET lock_timeout = '5s';
SET statement_timeout = '1min';

INSERT INTO drinkit_application.user_event (user_id, sequence_id, event_name, date, author, payload)
VALUES
    ('64656d6f0000000000000001', 0, 'INITIALIZED', '2026-01-01T00:00:00Z',
     '{"type": "UNLOGGED", "correlationId": {"value": "64656d6f-0000-4000-8000-000000000001"}}',
     '{"email": {"value": "demo@drinkit.test"}, "roles": {"values": ["ROLE_REGISTRATION_IN_PROGRESS"]}, "password": {"value": "$2a$10$szm3KQoaxzefTnJm5IlhHumS79wo/SnDJ3mYygmGrXXnrRf8cHpVC"}, "preferredLocale": "fr-FR"}'),
    ('64656d6f0000000000000001', 10, 'VERIFIED', '2026-01-01T00:01:00Z',
     '{"type": "CONNECTED", "userId": "64656d6f0000000000000001"}',
     '{}'),
    ('64656d6f0000000000000001', 20, 'PROFILE_COMPLETED', '2026-01-01T00:02:00Z',
     '{"type": "CONNECTED", "userId": "64656d6f0000000000000001"}',
     '{"profile": {"firstName": {"value": "Demo"}, "lastName": {"value": "Drinkit"}, "birthDate": {"value": "1990-01-01"}}}'),
    ('64656d6f0000000000000001', 30, 'PROMOTED_AS_ADMIN', '2026-01-01T00:03:00Z',
     '{"type": "CONNECTED", "userId": "64656d6f0000000000000001"}',
     '{}')
ON CONFLICT DO NOTHING;

INSERT INTO drinkit_application.user
    (id, firstname, lastname, birthdate, email, password, status, verified, enabled, roles, modified)
VALUES
    ('64656d6f0000000000000001', 'Demo', 'Drinkit', '1990-01-01', 'demo@drinkit.test',
     '$2a$10$szm3KQoaxzefTnJm5IlhHumS79wo/SnDJ3mYygmGrXXnrRf8cHpVC', 'ACTIVE', TRUE, TRUE,
     '{ROLE_USER,ROLE_ADMIN}', '2026-01-01T00:03:00Z')
ON CONFLICT DO NOTHING;

INSERT INTO drinkit_application.cellar (id, owner_id, name, location, rooms, modified)
VALUES
    ('64656d6f0000000000000002', '64656d6f0000000000000001', 'Demo cellar',
     '{"city": "Bordeaux", "country": {"code": "FR", "name": "France"}, "point": {"latitude": 44.837789, "longitude": -0.57918}}',
     '["Main room"]', '2026-01-01T00:03:00Z')
ON CONFLICT DO NOTHING;
