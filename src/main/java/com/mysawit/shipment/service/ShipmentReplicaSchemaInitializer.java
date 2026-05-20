package com.mysawit.shipment.service;

import java.util.List;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class ShipmentReplicaSchemaInitializer implements ApplicationRunner {

    private static final List<String> SCHEMA_STATEMENTS = List.of(
            """
            create table if not exists public.shipment_user_replicas (
                id uuid primary key,
                email varchar(255),
                username varchar(255),
                role varchar(50),
                mandor_id uuid,
                mandor_name varchar(255),
                plantation_id varchar(64),
                deleted boolean not null default false,
                created_at timestamptz not null default now(),
                updated_at timestamptz not null default now()
            )
            """,
            "create index if not exists idx_shipment_user_replicas_role on public.shipment_user_replicas (role)",
            """
            create table if not exists public.shipment_harvest_replicas (
                id uuid primary key,
                event_id varchar(64),
                harvester_id uuid,
                foreman_id uuid,
                plantation_id varchar(64),
                weight_kg double precision,
                status varchar(32),
                approved_at timestamptz,
                created_at timestamptz not null default now(),
                updated_at timestamptz not null default now()
            )
            """,
            "create index if not exists idx_shipment_harvest_replicas_status on public.shipment_harvest_replicas (status)",
            """
            create index if not exists idx_shipment_harvest_replicas_foreman_status
            on public.shipment_harvest_replicas (foreman_id, status)
            """
    );

    private final JdbcTemplate jdbcTemplate;

    public ShipmentReplicaSchemaInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        SCHEMA_STATEMENTS.forEach(jdbcTemplate::execute);
    }
}
