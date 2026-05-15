-- Local shipment-only test seed.
-- Run after the shipment service has started once with SPRING_PROFILES_ACTIVE=local,
-- so Hibernate can create/update the local shipment tables.

INSERT INTO worker_plantation_assignments (
  user_id,
  role,
  name,
  plantation_id,
  last_event_id,
  updated_at
) VALUES
  ('aaaaaaaa-1111-1111-1111-111111111111', 'MANDOR', 'Mandor Local', 'plantation-local-1', 'local-seed-mandor', now()),
  ('bbbbbbbb-2222-2222-2222-222222222222', 'SUPIR', 'Supir Local', 'plantation-local-1', 'local-seed-supir', now())
ON CONFLICT (user_id) DO UPDATE SET
  role = EXCLUDED.role,
  name = EXCLUDED.name,
  plantation_id = EXCLUDED.plantation_id,
  last_event_id = EXCLUDED.last_event_id,
  updated_at = EXCLUDED.updated_at;

INSERT INTO harvest_read_models (
  harvest_id,
  mandor_user_id,
  plantation_id,
  status,
  weight_kg,
  last_event_id,
  updated_at
) VALUES
  ('11111111-1111-1111-1111-111111111111', 'aaaaaaaa-1111-1111-1111-111111111111', 'plantation-local-1', 'APPROVED', 120.0, 'local-seed-harvest-1', now()),
  ('22222222-2222-2222-2222-222222222222', 'aaaaaaaa-1111-1111-1111-111111111111', 'plantation-local-1', 'APPROVED', 180.0, 'local-seed-harvest-2', now()),
  ('33333333-3333-3333-3333-333333333333', 'aaaaaaaa-1111-1111-1111-111111111111', 'plantation-local-1', 'APPROVED', 90.0, 'local-seed-harvest-3', now())
ON CONFLICT (harvest_id) DO UPDATE SET
  mandor_user_id = EXCLUDED.mandor_user_id,
  plantation_id = EXCLUDED.plantation_id,
  status = EXCLUDED.status,
  weight_kg = EXCLUDED.weight_kg,
  last_event_id = EXCLUDED.last_event_id,
  updated_at = EXCLUDED.updated_at;
