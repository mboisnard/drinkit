--liquibase formatted sql
--changeset demo:01-demo-data logicalFilePath:fixed runOnChange:true

-- demo@drinkit.test, who signs in with a code sent by email: an admin with a cellar, as the API would have written them
SET lock_timeout = '5s';
SET statement_timeout = '1min';

INSERT INTO drinkit_application.user_event (user_id, sequence_id, event_name, date, author, payload)
VALUES
    ('01kdvdna00e5mt007j31qpda6g', 0, 'INITIALIZED', '2026-01-01T00:00:00Z',
     '{"type": "UNLOGGED", "visitorId": "01kdvdn9w0e5mt007j31qpda6h"}',
     '{"email": {"value": "demo@drinkit.test"}, "roles": {"values": ["ROLE_REGISTRATION_IN_PROGRESS"]}, "preferences": {"language": "fr"}}'),
    ('01kdvdna00e5mt007j31qpda6g', 20, 'PROFILE_COMPLETED', '2026-01-01T00:02:00Z',
     '{"type": "CONNECTED", "userId": "01kdvdna00e5mt007j31qpda6g"}',
     '{"profile": {"firstName": {"value": "Demo"}, "lastName": {"value": "Drinkit"}, "birthDate": {"value": "1990-01-01"}}}'),
    ('01kdvdna00e5mt007j31qpda6g', 30, 'PROMOTED_AS_ADMIN', '2026-01-01T00:03:00Z',
     '{"type": "CONNECTED", "userId": "01kdvdna00e5mt007j31qpda6g"}',
     '{}')
ON CONFLICT DO NOTHING;

INSERT INTO drinkit_application.user
    (id, firstname, lastname, birthdate, email, status, enabled, roles, preferences, modified)
VALUES
    ('01kdvdna00e5mt007j31qpda6g', 'Demo', 'Drinkit', '1990-01-01', 'demo@drinkit.test', 'ACTIVE', TRUE,
     '{ROLE_USER,ROLE_ADMIN}', '{"language": "fr"}', '2026-01-01T00:03:00Z')
ON CONFLICT DO NOTHING;

INSERT INTO drinkit_application.cellar (id, owner_id, name, location, rooms, modified)
VALUES
    ('01kdvdq4k0e0ns0a08m1dthwn5', '01kdvdna00e5mt007j31qpda6g', 'Demo cellar',
     '{"city": "Bordeaux", "country": {"code": "FR", "name": "France"}, "point": {"latitude": 44.837789, "longitude": -0.57918}}',
     '["Main room"]', '2026-01-01T00:03:00Z')
ON CONFLICT DO NOTHING;
