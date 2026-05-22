-- Seed worker_plantation_assignments for the dummy MANDOR/SUPIR used by
-- shipment seed and JMeter profiling tokens.
-- Idempotent: aman dijalankan ulang.

create table if not exists public.worker_plantation_assignments (
    user_id uuid primary key,
    role varchar(16) not null,
    name varchar(100),
    plantation_id varchar(64) not null,
    last_event_id varchar(80),
    updated_at timestamptz
);

insert into public.worker_plantation_assignments
    (user_id, role, name, plantation_id, last_event_id, updated_at)
values
    ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa'::uuid, 'MANDOR', 'Mandor Seed', 'dummy-plantation', 'seed-mandor', now()),
    ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb'::uuid, 'SUPIR',  'Supir Seed',  'dummy-plantation', 'seed-supir',  now())
on conflict (user_id) do update
set
    role          = excluded.role,
    name          = excluded.name,
    plantation_id = excluded.plantation_id,
    last_event_id = excluded.last_event_id,
    updated_at    = excluded.updated_at;

select user_id, role, plantation_id from public.worker_plantation_assignments
where user_id in (
    'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa'::uuid,
    'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb'::uuid
);
