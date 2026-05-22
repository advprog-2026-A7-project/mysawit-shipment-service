# mysawit-shipment-service

Spring Boot (Java + Gradle) microservice for MySawit.

## Run (local)
```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
./gradlew bootRun
```

Runs at: http://localhost:8084

## Health
- GET /actuator/health

Quick check:
```bash
curl http://localhost:8084/actuator/health
```

## Shipment-Only FE Test
For temporary local testing from `mysawit-web` without running Identity, Harvest,
Plantation, Payroll, or Notification services:

1. Set shipment `.env`:

```bash
SPRING_PROFILES_ACTIVE=local
SPRING_DATASOURCE_URL=jdbc:postgresql://db.ewfaurlyroqvwrtdyfot.supabase.co:5432/postgres?sslmode=require
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=PASTE_SUPABASE_PASSWORD_HERE
SHIPMENT_EVENTS_ENABLED=false
```

2. Start shipment once so the local profile can create/update the shipment tables:

```bash
./gradlew bootRun
```

3. Seed the local shipment read models:

```bash
PGPASSWORD="$SPRING_DATASOURCE_PASSWORD" psql \
  "host=db.ewfaurlyroqvwrtdyfot.supabase.co port=5432 dbname=postgres user=postgres sslmode=require" \
  -f dev/shipment-local-seed.sql
```

The seed IDs match the dev role buttons in the frontend shipment page.

## OpenAPI Skeleton
- File: `src/main/resources/openapi/shipment-api.yaml`
- Contains milestone-level shipment endpoint list and minimal request/response schemas.

## Profiling Report

Profiling ini mencakup semua fitur utama shipment service: API sinkron,
authorization path, database read/write path, event publisher, event consumer,
dan health/observability. Full-feature profiling lokal sudah tersedia lewat
integration test berikut:

```bash
./gradlew test --tests com.mysawit.shipment.profiling.ShipmentFeatureProfilingTest --info
```

Latest generated report artifacts:

- Markdown: `build/reports/profiling/shipment-feature-profiling-report.md`
- HTML: `build/reports/profiling/shipment-feature-profiling-report.html`
- Screenshot: `build/reports/profiling/shipment-feature-profiling-report.png`

Latest local run result: `25/25` scenarios passed. Angka runtime lokal dipakai
untuk regression comparison, bukan production SLO, karena test berjalan di H2.
Replica consumer handler diprofil dengan mocked replica services karena H2 tidak
mengeksekusi SQL upsert PostgreSQL-specific yang dipakai replica services di
production.

### Coverage Summary

| Area | Feature | Entry Point | Profiling Focus | Current Evidence |
| --- | --- | --- | --- | --- |
| Health | Service health | `GET /api/shipments/health` | Basic HTTP latency and availability | Locally profiled |
| Health | Actuator health | `GET /actuator/health` | Spring health availability | Locally profiled |
| Security | JWT validation | `ShipmentAccessFilter` | Missing token and wrong-role paths | Locally profiled |
| Security | Role authorization | SUPIR, MANDOR, ADMIN endpoints | 403 path and role branch cost | Locally profiled |
| Security | Ownership validation | Detail/status/list endpoints | Owner vs non-owner path | Covered by security tests |
| Read API | List shipments as SUPIR | `GET /api/shipments` | `supir_user_id`, date, status filters | Locally profiled |
| Read API | List shipments as MANDOR | `GET /api/shipments` | `mandor_user_id`, `supir_user_id`, supir name, date, status filters | Locally profiled |
| Read API | List shipments as ADMIN | `GET /api/shipments` | default `MANDOR_APPROVED`, mandor name, date, status filters | Locally profiled |
| Read API | Shipment detail | `GET /api/shipments/{id}` | Primary-key lookup and DTO mapping | Locally profiled |
| Read API | Available supirs | `GET /api/shipments/available-supirs` | plantation assignment lookup and optional name filter | Locally profiled |
| Write API | Create shipment | `POST /api/shipments` | validation, duplicate harvest check, weight validation, insert shipment/items | Locally profiled |
| Write API | Supir status update | `PATCH /api/shipments/{id}/status` | `MEMUAT -> MENGIRIM -> TIBA`, owner check, completed event publish | Locally profiled |
| Write API | Mandor approval | `PATCH /api/shipments/{id}/mandor-approval` | approve/reject decisions and event publish | Locally profiled |
| Write API | Admin approval | `PATCH /api/shipments/{id}/admin-approval` | approve/reject/partial reject decisions and event publish | Locally profiled |
| Events | Harvest replica update | `HarvestEventConsumer` | consumer handler dispatch | Locally profiled with mocked replica service |
| Events | User replica update | user registered/assignment/updated/deleted consumers | consumer handler dispatch | Locally profiled with mocked replica service |
| Events | Plantation assignment replica | `PlantationAssignmentEventConsumer` and listener | replica handler and worker assignment listener | Locally profiled |
| Events | Shipment outbound events | `ShipmentEventPublisher` | completed/approved/rejected event publish calls | Locally profiled with mocked RabbitTemplate |

### Local Database Profiling Result

The local profiling test focuses on the busiest read path,
`ShipmentRepository.findWithFilters(...)`, using 10.000 H2 rows in PostgreSQL
mode and 100 matching rows for the target `supir_user_id`.

```bash
./gradlew test --tests com.mysawit.shipment.profiling.ShipmentDatabaseProfilingTest --info
```

| Scenario | Index State | Result Rows | Runtime |
| --- | --- | ---: | ---: |
| SUPIR list filter by `supir_user_id` | With JPA-created indexes | 100 | 13 ms |
| SUPIR list filter by `supir_user_id` | After dropping selected indexes | 100 | 7 ms |

The local H2 result proves functional equivalence but does not prove production
latency improvement. The unindexed query was faster in this run because the test
uses a small in-memory dataset and warmed database cache. Production decisions
must use Supabase PostgreSQL with realistic row counts and `EXPLAIN ANALYZE`.

### Per-Feature Profiling Scenarios

| Feature | Scenario | Suggested Metric | Expected Target |
| --- | --- | --- | --- |
| Health | Call `/api/shipments/health` and `/actuator/health/readiness` during idle and during load | p95 latency, success rate | p95 < 100 ms, 0 errors |
| JWT validation | Send valid, missing, malformed, expired, and wrong-role tokens | p95 latency for 401/403/2xx paths | auth overhead remains small vs DB time |
| SUPIR shipment list | Query owned shipments with no filter, status filter, date filter, and combined filter | p95 latency, SQL plan, rows scanned | p95 < 500 ms, index-backed plan |
| MANDOR shipment list | Query created shipments by supir id, supir name, date, and status | p95 latency, SQL plan, rows scanned | p95 < 500 ms, bounded scan |
| ADMIN shipment list | Query default `MANDOR_APPROVED`, mandor name search, date filter, and status override | p95 latency, SQL plan, rows scanned | p95 < 500 ms, bounded scan |
| Shipment detail | Fetch existing, forbidden, and missing shipment IDs | p95 latency, DB lookup count | p95 < 300 ms |
| Available supirs | Query same-plantation supirs with and without name filter | p95 latency, rows scanned | p95 < 300 ms |
| Create shipment | Create shipment with 1, 5, and 20 harvest items | p95 latency, transaction duration, duplicate check count | p95 < 700 ms |
| Supir status update | Run `MEMUAT -> MENGIRIM -> TIBA` and invalid transition attempts | p95 latency, event publish latency | p95 < 500 ms |
| Mandor approval | Run approve and reject decisions after `TIBA` | p95 latency, event publish latency | p95 < 500 ms |
| Admin approval | Run approve, reject, and partial reject after `MANDOR_APPROVED` | p95 latency, event publish latency | p95 < 500 ms |
| Harvest events | Consume create/update harvest events in batches | consumer processing time, failed deliveries | no unbounded backlog |
| User events | Consume registered, assignment, updated, and deleted events in batches | consumer processing time, failed deliveries | no unbounded backlog |
| Plantation events | Consume plantation assignment events in batches | consumer processing time, failed deliveries | no unbounded backlog |
| Outbound events | Publish shipment completed, Mandor approval, and Admin approval events | RabbitMQ publish latency, failures | publish succeeds within request budget |

### Database Indexes Under Profiling

`Shipment` currently declares the following indexes for the main read path:

- `idx_shipments_supir_user_id` on `supir_user_id`
- `idx_shipments_mandor_user_id` on `mandor_user_id`
- `idx_shipments_status` on `status`
- `idx_shipments_created_at` on `created_at`

These indexes match owner scoping, role-specific listing, status filtering, and
date range filtering. The query also supports `mandor_name` and `supir_name`
search with `ILIKE`; those filters are not covered by the current B-tree indexes
and should be checked with PostgreSQL `pg_trgm`/GIN only if staging profiling
shows name search becoming a bottleneck.

### Load Test Scenario

A k6 scenario is available at `tests/load/k6-shipment-load-test.js` for
concurrent `GET /api/shipments` reads:

```bash
API_URL=http://localhost:8084 JWT_TOKEN=<valid-jwt> k6 run tests/load/k6-shipment-load-test.js
```

The configured target is 50 virtual users with these thresholds:

- `p95` request duration below 500 ms
- request failure rate below 1%

For all-feature load profiling, run separate k6 scenarios per role because the
write endpoints require ordered state setup:

- MANDOR creates shipment.
- SUPIR moves status from `MEMUAT` to `MENGIRIM` and then `TIBA`.
- MANDOR approves or rejects after `TIBA`.
- ADMIN approves, rejects, or partially rejects after `MANDOR_APPROVED`.

### Laporan Profiling JMeter (Before/After)

Laporan lengkap profiling 1000 request via JMeter ada di
[`tests/load/jmeter/README.md`](tests/load/jmeter/README.md). Isinya:

- Perbandingan metrik before vs after untuk tiga run (baseline, tuning P0,
  refactor P1+P2)
- Breakdown error dan latensi per endpoint untuk SUPIR, MANDOR, dan ADMIN
- Analisis akar masalah error rate 78% di run awal (Hikari pool exhaustion)
- Daftar lengkap perubahan yang diterapkan — sizing Hikari, OSIV off, batch
  fetch, read-only transaction, JPA Specifications, cache Caffeine, dan seed
  data worker assignment
- Cara reproduce profiling, termasuk JMX plan, generator JWT, dan runner script

Ringkasan hasil: error rate turun dari **78.1%** ke **0.2%** pada beban yang
sama (1000 request), throughput naik dari 27.7 ke 32.1 req/s. Sisa latensi
sekarang didominasi RTT jaringan Indonesia → Supabase Tokyo. Langkah
high-impact berikutnya adalah migrasi region Supabase (mis. ke
`ap-southeast-1` Singapore).

### Observability

Runtime profiling can be supported through Actuator and Prometheus:

- `GET /actuator/metrics`
- `GET /actuator/prometheus`
- HTTP server request histograms are enabled through
  `management.metrics.distribution.percentiles-histogram.http.server.requests=true`
- Prometheus and Grafana assets are stored under `monitoring/`

Recommended dashboards for complete profiling:

- HTTP p50/p95/p99 latency grouped by `uri`, `method`, and `status`
- request throughput and error rate
- Hikari active/idle connections and acquisition time
- JVM heap, GC pause, and thread count
- RabbitMQ publish/consume failures and queue depth from broker metrics
- database query plan snapshots for list/search endpoints

### Follow-Up Profiling Checklist

- Run `EXPLAIN ANALYZE` for SUPIR, MANDOR, and ADMIN list queries in Supabase
  using production-like row counts.
- Compare owner-only, status-only, date-range, name-search, and combined-filter
  plans.
- Profile create shipment with increasing harvest item counts because it loops
  through harvest replica validation and duplicate checks.
- Profile status update and approval endpoints with RabbitMQ enabled because
  successful terminal transitions publish events.
- Run k6 read scenarios for SUPIR, MANDOR, and ADMIN tokens separately.
- Run write-path k6 scenarios with seeded shipment IDs and reset data between
  runs to keep state transitions valid.
- Keep `./gradlew clean check` green after any profiling-related schema, query,
  or event-flow change.

## Environment Config
- Example env file: `.env.example`
- Main variables:
  - `SERVER_PORT`
  - `SPRING_PROFILES_ACTIVE`
  - `SPRING_DATASOURCE_URL`
  - `SPRING_DATASOURCE_USERNAME`
  - `SPRING_DATASOURCE_PASSWORD`

## Deployment Baseline
- CD runs on GitHub-hosted Actions, builds the JAR and Docker image on the runner, then pushes both `azzelll/mysawit-shipment:<sha12>` and `azzelll/mysawit-shipment:latest` to Docker Hub.
- The EC2 host pulls the image from Docker Hub and runs the container with `--network host` so outbound Supabase Direct Connection traffic uses the instance's native dual-stack networking instead of Docker bridge IPv6.
- Public app access stays on the EC2 Elastic IP over IPv4 at port `8084`.
- Required GitHub repository variables:
  - `SHIPMENT_HOST_PORT`
- Optional GitHub repository variables:
  - `STAGING_PORT` defaults to `22`
  - `REMOTE_APP_DIR` defaults to `/home/<staging-user>/apps/mysawit-shipment-service`
  - `JWT_EXPIRATION` defaults to `86400000`
  - `JAVA_OPTS` defaults to `-Xms256m -Xmx768m`
  - `JAVA_TOOL_OPTIONS` defaults to `-Djava.net.preferIPv6Addresses=true -Djava.net.preferIPv4Stack=false -Dsun.net.inetaddr.ttl=60`
- Required GitHub repository secrets:
  - `DOCKERHUB_USERNAME` — Docker Hub account that owns the image repo (e.g. `azzelll`).
  - `DOCKERHUB_TOKEN` — Docker Hub access token with push permission (created at https://hub.docker.com/settings/security).
  - `STAGING_HOST`
  - `STAGING_USER`
  - `STAGING_SSH_KEY`
  - `SPRING_DATASOURCE_URL` for the Supabase Direct Connection JDBC URL over IPv6
  - `SPRING_DATASOURCE_USERNAME`
  - `SPRING_DATASOURCE_PASSWORD`
  - `JWT_SECRET`
  - `CORS_ORIGINS`
  - `CLOUDAMQP_URL`
- EC2 host prerequisites:
  - Ubuntu host with global IPv6 enabled on the instance
  - Docker installed and usable by the deploy user or through `sudo`
  - Outbound HTTPS (TCP 443) allowed to `registry-1.docker.io` so EC2 can pull the image
  - Port `8084` open on the EC2 security group for inbound IPv4 traffic
  - Outbound IPv6 allowed to the Supabase direct database endpoint on TCP `5432`

## Deployment Checks
- Host IPv6 presence is verified before the container is restarted.
- The Supabase hostname is resolved on the EC2 host and must return at least one IPv6 address.
- A TCP connection test to the Supabase hostname on port `5432` runs from EC2 before the new container starts.
- Spring readiness is checked at `http://127.0.0.1:8084/actuator/health/readiness` after deploy, and rollback to the previous image is attempted if startup fails.

## Manual Rollback

To roll back to a previous image tag (the `<sha12>` of an older deployment), SSH to the EC2 host and run the existing deploy script with the older image reference. Image tags older than the last few deployments may have been pruned locally; the script will pull them again from Docker Hub.

```bash
ssh ec2-host
APP_HOME=~/apps/mysawit-shipment-service \
RELEASE_DIR=~/apps/mysawit-shipment-service/releases/<old-sha> \
IMAGE_REF=azzelll/mysawit-shipment:<old-sha> \
IMAGE_TAG=<old-sha> \
bash ~/apps/mysawit-shipment-service/releases/<old-sha>/scripts/deploy-ec2.sh
```

The script reuses the env file already present at `~/apps/mysawit-shipment-service/shared/app.env`, performs the same IPv6 checks, pulls the image, restarts the container, and runs the readiness probe before declaring success.
