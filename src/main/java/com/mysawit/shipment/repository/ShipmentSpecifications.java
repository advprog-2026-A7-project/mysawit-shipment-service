package com.mysawit.shipment.repository;

import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.UUID;

import org.springframework.data.jpa.domain.Specification;

import com.mysawit.shipment.domain.ShipmentStatus;
import com.mysawit.shipment.model.Shipment;

import jakarta.persistence.criteria.JoinType;

/**
 * Dynamic predicate builder for shipment list queries. We use Specifications instead of a single
 * monster JPQL with `:p IS NULL OR ...` because Supabase Supavisor pooler runs with
 * `prepareThreshold=0` which prevents Postgres from inferring null parameter types.
 *
 * Specifications attach predicates only when values are present, so no null binds and no type
 * inference issues.
 */
public final class ShipmentSpecifications {

    private static final String FIELD_CREATED_AT = "createdAt";

    private ShipmentSpecifications() {
    }

    public static Specification<Shipment> bySupirUserId(UUID supirUserId) {
        return (root, query, cb) -> supirUserId == null ? null : cb.equal(root.get("supirUserId"), supirUserId);
    }

    public static Specification<Shipment> byMandorUserId(UUID mandorUserId) {
        return (root, query, cb) -> mandorUserId == null ? null : cb.equal(root.get("mandorUserId"), mandorUserId);
    }

    public static Specification<Shipment> byStatus(ShipmentStatus status) {
        return (root, query, cb) -> status == null ? null : cb.equal(root.get("status"), status);
    }

    public static Specification<Shipment> byMandorNameLike(String value) {
        return likePredicate("mandorName", value);
    }

    public static Specification<Shipment> bySupirNameLike(String value) {
        return likePredicate("supirName", value);
    }

    public static Specification<Shipment> createdBetween(OffsetDateTime from, OffsetDateTime to) {
        return (root, query, cb) -> {
            if (from == null && to == null) {
                return null;
            }
            if (from != null && to != null) {
                return cb.and(
                        cb.greaterThanOrEqualTo(root.get(FIELD_CREATED_AT), from),
                        cb.lessThan(root.get(FIELD_CREATED_AT), to)
                );
            }
            return from != null
                    ? cb.greaterThanOrEqualTo(root.get(FIELD_CREATED_AT), from)
                    : cb.lessThan(root.get(FIELD_CREATED_AT), to);
        };
    }

    /**
     * Eagerly fetch items so callers can read {@code shipment.getItems()} after the read tx ends.
     * Apply only on root queries (skip count queries) to avoid Hibernate fetch-join warnings.
     */
    public static Specification<Shipment> withItems() {
        return (root, query, cb) -> {
            if (query != null && query.getResultType() != Long.class && query.getResultType() != long.class) {
                root.fetch("items", JoinType.LEFT);
                query.distinct(true);
            }
            return null;
        };
    }

    private static Specification<Shipment> likePredicate(String field, String value) {
        return (root, query, cb) -> {
            if (value == null || value.isBlank()) {
                return null;
            }
            String pattern = "%" + value.toLowerCase(Locale.ROOT) + "%";
            return cb.like(cb.lower(root.get(field)), pattern);
        };
    }
}
