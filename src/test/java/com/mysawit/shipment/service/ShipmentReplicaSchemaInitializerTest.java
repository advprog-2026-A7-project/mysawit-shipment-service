package com.mysawit.shipment.service;

import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

class ShipmentReplicaSchemaInitializerTest {

    @Test
    void runCreatesReplicaTablesAndIndexes() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        ShipmentReplicaSchemaInitializer initializer = new ShipmentReplicaSchemaInitializer(jdbcTemplate);

        initializer.run(null);

        verify(jdbcTemplate).execute(contains("create table if not exists public.shipment_user_replicas"));
        verify(jdbcTemplate).execute(contains("idx_shipment_user_replicas_role"));
        verify(jdbcTemplate).execute(contains("create table if not exists public.shipment_harvest_replicas"));
        verify(jdbcTemplate).execute(contains("idx_shipment_harvest_replicas_status"));
        verify(jdbcTemplate).execute(contains("idx_shipment_harvest_replicas_foreman_status"));
    }
}
