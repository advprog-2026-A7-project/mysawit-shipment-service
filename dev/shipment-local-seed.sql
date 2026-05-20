-- Local shipment-only test seed.
-- Run after the shipment service has started once with SPRING_PROFILES_ACTIVE=local,
-- so Hibernate can create/update the local shipment tables.

DELETE FROM shipment_items
WHERE shipment_id IN (
  SELECT id
  FROM shipments
  WHERE mandor_user_id = 'aaaaaaaa-1111-1111-1111-111111111111'
     OR supir_user_id = 'bbbbbbbb-2222-2222-2222-222222222222'
)
   OR harvest_id IN (
     '11111111-1111-1111-1111-111111111111',
     '22222222-2222-2222-2222-222222222222',
     '33333333-3333-3333-3333-333333333333'
   );

DELETE FROM shipments
WHERE mandor_user_id = 'aaaaaaaa-1111-1111-1111-111111111111'
   OR supir_user_id = 'bbbbbbbb-2222-2222-2222-222222222222';

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

INSERT INTO shipment_harvest_replicas (
  id,
  event_id,
  harvester_id,
  foreman_id,
  plantation_id,
  weight_kg,
  status,
  approved_at,
  created_at,
  updated_at
) VALUES
  (
    '11111111-1111-1111-1111-111111111111',
    'local-seed-harvest-1',
    'cccccccc-3333-3333-3333-333333333333',
    'aaaaaaaa-1111-1111-1111-111111111111',
    'plantation-local-1',
    120.0,
    'APPROVED',
    now(),
    now(),
    now()
  ),
  (
    '22222222-2222-2222-2222-222222222222',
    'local-seed-harvest-2',
    'dddddddd-4444-4444-4444-444444444444',
    'aaaaaaaa-1111-1111-1111-111111111111',
    'plantation-local-1',
    180.0,
    'APPROVED',
    now(),
    now(),
    now()
  ),
  (
    '33333333-3333-3333-3333-333333333333',
    'local-seed-harvest-3',
    'eeeeeeee-5555-5555-5555-555555555555',
    'aaaaaaaa-1111-1111-1111-111111111111',
    'plantation-local-1',
    90.0,
    'APPROVED',
    now(),
    now(),
    now()
  )
ON CONFLICT (id) DO UPDATE SET
  event_id = EXCLUDED.event_id,
  harvester_id = EXCLUDED.harvester_id,
  foreman_id = EXCLUDED.foreman_id,
  plantation_id = EXCLUDED.plantation_id,
  weight_kg = EXCLUDED.weight_kg,
  status = EXCLUDED.status,
  approved_at = EXCLUDED.approved_at,
  updated_at = EXCLUDED.updated_at;
