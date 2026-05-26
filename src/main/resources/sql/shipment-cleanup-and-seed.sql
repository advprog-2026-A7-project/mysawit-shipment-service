-- Shipment schema cleanup and dummy data for the current staging branch.
--
-- Current backend uses:
-- public.shipments:
--   id, mandor_user_id, supir_user_id, destination, total_kg, status,
--   created_at, updated_at
-- public.shipment_items:
--   id, shipment_id, harvest_id, weight_kg
--
-- Replace these two UUIDs with real user ids when you want rows returned for
-- a specific token. SUPIR GET /api/shipments only returns rows whose
-- supir_user_id matches the token subject.

begin;

create extension if not exists pgcrypto;

create temporary table _shipment_seed_params (
    mandor_user_id uuid not null,
    supir_user_id uuid not null
) on commit drop;

insert into _shipment_seed_params (mandor_user_id, supir_user_id)
values (
    'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa'::uuid,
    'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb'::uuid
);

-- Replicated worker plantation assignments. Required so that the
-- MANDOR-only endpoints (/available-supirs, POST /api/shipments) can be
-- profiled against a non-empty result set.
create table if not exists public.worker_plantation_assignments (
    user_id uuid primary key,
    role varchar(16) not null,
    name varchar(100),
    plantation_id varchar(64) not null,
    last_event_id varchar(80),
    updated_at timestamptz
);

with seed_params as (
    select mandor_user_id, supir_user_id from _shipment_seed_params
)
insert into public.worker_plantation_assignments
    (user_id, role, name, plantation_id, last_event_id, updated_at)
select mandor_user_id, 'MANDOR', 'Mandor Seed', 'dummy-plantation', 'seed-mandor', now()
from seed_params
union all
select supir_user_id, 'SUPIR', 'Supir Seed', 'dummy-plantation', 'seed-supir', now()
from seed_params
on conflict (user_id) do update
set
    role = excluded.role,
    name = excluded.name,
    plantation_id = excluded.plantation_id,
    last_event_id = excluded.last_event_id,
    updated_at = excluded.updated_at;

-- Make sure the tables/columns required by the current entities exist.
create table if not exists public.shipments (
    id uuid primary key default gen_random_uuid()
);

alter table public.shipments
    add column if not exists mandor_user_id uuid,
    add column if not exists mandor_name text,
    add column if not exists supir_user_id uuid,
    add column if not exists supir_name text,
    add column if not exists plantation_id varchar(64),
    add column if not exists destination varchar(255),
    add column if not exists total_kg double precision,
    add column if not exists kg_accepted double precision,
    add column if not exists rejection_reason text,
    add column if not exists status varchar(255),
    add column if not exists created_at timestamptz,
    add column if not exists updated_at timestamptz,
    add column if not exists mandor_reviewed_at timestamptz,
    add column if not exists admin_reviewed_at timestamptz;

create table if not exists public.shipment_items (
    id uuid primary key default gen_random_uuid()
);

alter table public.shipment_items
    add column if not exists shipment_id uuid,
    add column if not exists harvest_id uuid,
    add column if not exists weight_kg double precision;

-- Backfill old/partial shipment rows so NOT NULL and enum validation do not fail.
update public.shipments
set
    mandor_user_id = coalesce(mandor_user_id, (select mandor_user_id from _shipment_seed_params)),
    supir_user_id = coalesce(supir_user_id, (select supir_user_id from _shipment_seed_params)),
    plantation_id = coalesce(nullif(plantation_id, ''), 'dummy-plantation'),
    destination = coalesce(nullif(destination, ''), 'Pabrik Sawit Dummy'),
    total_kg = coalesce(total_kg, 1),
    status = case
        when status in (
            'MEMUAT',
            'MENGIRIM',
            'TIBA',
            'MANDOR_APPROVED',
            'MANDOR_REJECTED',
            'ADMIN_APPROVED',
            'ADMIN_REJECTED',
            'PARTIALLY_REJECTED'
        ) then status
        when status = 'IN_TRANSIT' then 'MENGIRIM'
        when status = 'DELIVERED' then 'TIBA'
        else 'MEMUAT'
    end,
    created_at = coalesce(created_at, now()),
    updated_at = coalesce(updated_at, now());

alter table public.shipments
    alter column mandor_user_id set not null,
    alter column supir_user_id set not null,
    alter column plantation_id set not null,
    alter column destination set not null,
    alter column total_kg set not null,
    alter column status set not null;

alter table public.shipments
    drop constraint if exists shipments_status_check;

alter table public.shipments
    add constraint shipments_status_check
    check (status in (
        'MEMUAT',
        'MENGIRIM',
        'TIBA',
        'MANDOR_APPROVED',
        'MANDOR_REJECTED',
        'ADMIN_APPROVED',
        'ADMIN_REJECTED',
        'PARTIALLY_REJECTED'
    ));

-- Legacy single-harvest column from older demos; current backend stores harvests
-- through shipment_items.
alter table public.shipments
    drop column if exists harvest_id;

-- Dummy rows for GET /api/shipments and GET /api/shipments/{id}.
with seed_params as (
    select mandor_user_id, supir_user_id from _shipment_seed_params
),
seed_shipments (
    id,
    destination,
    total_kg,
    status,
    created_at,
    updated_at
) as (
    values
        (
            '11111111-1111-1111-1111-111111111111'::uuid,
            'Pabrik Sawit Cikupa',
            125.50::double precision,
            'MEMUAT',
            now() - interval '2 days',
            now() - interval '2 days'
        ),
        (
            '22222222-2222-2222-2222-222222222222'::uuid,
            'Pabrik Sawit Karawang',
            210.00::double precision,
            'MENGIRIM',
            now() - interval '1 day',
            now() - interval '4 hours'
        ),
        (
            '33333333-3333-3333-3333-333333333333'::uuid,
            'Pabrik Sawit Bekasi',
            98.75::double precision,
            'TIBA',
            now() - interval '6 hours',
            now() - interval '1 hour'
        )
)
insert into public.shipments (
    id,
    mandor_user_id,
    supir_user_id,
    destination,
    total_kg,
    status,
    created_at,
    updated_at
)
select
    seed_shipments.id,
    seed_params.mandor_user_id,
    seed_params.supir_user_id,
    seed_shipments.destination,
    seed_shipments.total_kg,
    seed_shipments.status,
    seed_shipments.created_at,
    seed_shipments.updated_at
from seed_shipments
cross join seed_params
on conflict (id) do update
set
    mandor_user_id = excluded.mandor_user_id,
    supir_user_id = excluded.supir_user_id,
    destination = excluded.destination,
    total_kg = excluded.total_kg,
    status = excluded.status,
    created_at = excluded.created_at,
    updated_at = excluded.updated_at;

-- Backfill/clean shipment item rows to match the entity relationship.
update public.shipment_items
set
    shipment_id = coalesce(shipment_id, '11111111-1111-1111-1111-111111111111'::uuid),
    harvest_id = coalesce(harvest_id, gen_random_uuid()),
    weight_kg = coalesce(weight_kg, 1);

delete from public.shipment_items item
where not exists (
    select 1
    from public.shipments shipment
    where shipment.id = item.shipment_id
);

with ranked_items as (
    select
        ctid,
        row_number() over (partition by harvest_id order by id) as duplicate_rank
    from public.shipment_items
)
delete from public.shipment_items item
using ranked_items ranked
where item.ctid = ranked.ctid
  and ranked.duplicate_rank > 1;

alter table public.shipment_items
    alter column shipment_id set not null,
    alter column harvest_id set not null,
    alter column weight_kg set not null;

alter table public.shipment_items
    drop constraint if exists shipment_items_shipment_id_fkey,
    drop constraint if exists uk_shipment_items_harvest_id;

alter table public.shipment_items
    add constraint shipment_items_shipment_id_fkey
    foreign key (shipment_id) references public.shipments(id) on delete cascade,
    add constraint uk_shipment_items_harvest_id unique (harvest_id);

with seed_items (
    id,
    shipment_id,
    harvest_id,
    weight_kg
) as (
    values
        (
            '44444444-1111-1111-1111-111111111111'::uuid,
            '11111111-1111-1111-1111-111111111111'::uuid,
            '99999999-1111-1111-1111-111111111111'::uuid,
            125.50::double precision
        ),
        (
            '44444444-2222-2222-2222-222222222222'::uuid,
            '22222222-2222-2222-2222-222222222222'::uuid,
            '99999999-2222-2222-2222-222222222222'::uuid,
            120.00::double precision
        ),
        (
            '44444444-3333-3333-3333-333333333333'::uuid,
            '22222222-2222-2222-2222-222222222222'::uuid,
            '99999999-3333-3333-3333-333333333333'::uuid,
            90.00::double precision
        ),
        (
            '44444444-4444-4444-4444-444444444444'::uuid,
            '33333333-3333-3333-3333-333333333333'::uuid,
            '99999999-4444-4444-4444-444444444444'::uuid,
            98.75::double precision
        )
)
insert into public.shipment_items (
    id,
    shipment_id,
    harvest_id,
    weight_kg
)
select
    id,
    shipment_id,
    harvest_id,
    weight_kg
from seed_items
on conflict (id) do update
set
    shipment_id = excluded.shipment_id,
    harvest_id = excluded.harvest_id,
    weight_kg = excluded.weight_kg;

commit;
