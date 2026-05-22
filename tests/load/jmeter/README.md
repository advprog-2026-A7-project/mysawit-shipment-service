# JMeter Profiling — Shipment Service

Profiling 1000 request ke endpoint **GET** shipment-service. Default-nya 4 endpoint
× 50 thread × 5 loop = **1000 request**.

## Endpoint yang di-profile

| # | Method | Path | Role | Share |
|---|---|---|---|---|
| 1 | GET | `/api/shipments` | SUPIR | 250 |
| 2 | GET | `/api/shipments/{id}` | SUPIR | 250 |
| 3 | GET | `/api/shipments/available-supirs` | MANDOR | 250 |
| 4 | GET | `/api/shipments` | ADMIN | 250 |

> Sengaja semua GET. Endpoint POST/PATCH punya state machine satu arah, unique
> constraint `harvest_id`, dan butuh harvest replica → 1000 request hampir pasti
> banyak yang fail karena bisnis-rule, bukan karena performa. Kalau memang mau
> profile write path, lakukan terpisah dengan dataset yang fresh setiap run.

## Prasyarat

- `jmeter` (sudah ter-install: `brew install jmeter`)
- `node` (untuk generate JWT HS256)
- `mysawit-shipment-service/.env` berisi `JWT_SECRET` yang sama dengan service
  yang sedang jalan
- shipment-service running (lokal `http://localhost:8084` atau remote)
- Seed data sudah di-load: `src/main/resources/sql/shipment-cleanup-and-seed.sql`
  (UUID seed sudah selaras dengan token yang kita generate)

## Cara pakai

```bash
cd mysawit-shipment-service/tests/load/jmeter
./run.sh
```

Override target / beban:

```bash
BASE_URL=http://<ec2-host>:8084 ./run.sh           # ke EC2
THREADS=100 LOOPS_PER_ENDPOINT=2 ./run.sh           # 4 x 100 x 2 = 800 req
THREADS=25  LOOPS_PER_ENDPOINT=10 ./run.sh          # tetap 1000, lebih ringan
SHIPMENT_ID=<uuid> ./run.sh                         # ganti shipment id GET-by-id
```

Hasil tersimpan di:

```
tests/load/jmeter/results/<timestamp>/
  ├── jmeter.log
  ├── results.jtl       # raw sample
  └── report/index.html # dashboard HTML (latency, p95/p99, throughput, error rate)
```

Buka dashboard:

```bash
open results/<timestamp>/report/index.html
```

## Troubleshooting

- **401 di semua request** → `JWT_SECRET` di `.env` beda dengan yang dipakai
  service. Pastikan service & generator pakai secret yang sama.
- **403 di GET shipments (SUPIR)** → hanya akan kelihatan kalau filter role
  benar. Cek `ShipmentAccessFilter` masih aktif.
- **404 di GET by id** → pastikan shipment dengan UUID seed sudah ada di DB
  (jalankan seed SQL dulu) atau override via `SHIPMENT_ID=<uuid>`.
- **Connection refused** → service belum jalan di `BASE_URL`.

## Hasil Profiling: Before vs After

Skenario: 1000 request, 50 thread per endpoint, ramp-up 10 detik, target Supabase Tokyo (`ap-northeast-1`).

| Metrik | Before (run 15:47) | After P0 (run 15:59) | After P1+P2 (run 17:30) |
|---|---|---|---|
| Total request | 1000 | 1000 | 1000 |
| Error rate | **78.1%** | 25.0% (data) * | **0.2%** |
| HTTP 500 | 729 | 0 | 1 |
| Mean response | 4958 ms | 3487 ms | 4528 ms |
| Median | 5008 ms | 3657 ms | 4527 ms |
| p95 | 5395 ms | 4470 ms | 6353 ms |
| p99 | 5673 ms | 6162 ms | 7100 ms |
| Throughput | 27.7 req/s | 36.8 req/s | 32.1 req/s |

\* run P0 punya error 25% murni karena `worker_plantation_assignments` belum di-seed
untuk endpoint `/available-supirs` (semua HTTP 400 valid response, bukan timeout/500).

### Per endpoint — before vs after P1+P2

| Endpoint | Before err% | After err% | Before mean | After mean |
|---|---|---|---|---|
| GET `/api/shipments` (SUPIR) | 62.8% | 0.4% | 5071 ms | 4515 ms |
| GET `/api/shipments/{id}` | 69.6% | 0.0% | 4959 ms | 4513 ms |
| GET `/api/shipments` (ADMIN) | 80.0% | 0.0% | 4905 ms | 4542 ms |
| GET `/api/shipments/available-supirs` | 100% | 0.4% | 4898 ms | 4543 ms |

### Diagnosis akar masalah

**Run awal (78% error, semua 500 di ~5000 ms):** HikariCP pool exhaust.
- 200 thread JMeter bersamaan vs `maximum-pool-size=3`
- `connection-timeout=5000ms` cocok dengan latensi observasi (mean 4958 ms)
- Setiap request antri → timeout di 5 detik → controller balikin 500
- Diperparah `spring.jpa.open-in-view=true` (default) yang nahan koneksi
  sepanjang serialisasi response, bukan cuma saat query

**Bottleneck setelah tuning (~4500 ms mean):** dominan dari network RTT
ke Supabase Tokyo (`ap-northeast-1`) via Supavisor pooler. Dari Indonesia,
RTT 80–150 ms × beberapa query per request = baseline ~3 detik. Kalau ingin
turun di bawah 1 detik, opsi paling murah adalah migrasi region Supabase
ke Singapore (`ap-southeast-1`).

### Refactoring yang diterapkan

| Tier | Perubahan | Dampak |
|---|---|---|
| P0 | `maximum-pool-size=20`, `min-idle=5`, `connection-timeout=10s` (env) | Hilangkan pool exhaust |
| P0 | `spring.jpa.open-in-view=false` | Koneksi dilepas tepat setelah query |
| P0 | `spring.jpa.show-sql=false` | Hilangkan overhead logging |
| P1 | `@Transactional(readOnly = true)` di semua read methods | Skip dirty check Hibernate |
| P1 | `@BatchSize(50)` di `Shipment.items` | Mitigasi N+1 saat list shipment |
| P1 | `@EntityGraph` di `findBySupirUserId` & `findWithItemsById` | Eager fetch tanpa N+1 |
| P1 | `ShipmentSpecifications` dynamic predicate | Atasi Supabase pooler `prepareThreshold=0` |
| P1 | `findClaimedHarvestIds(Collection)` batch | Ganti loop `existsByItemsHarvestId` |
| P2 | Caffeine `@Cacheable` di `WorkerAssignmentLookupService` | Hot path lookup di-cache 60s |
| P2 | Cache evict di `PlantationAssignmentEventListener` | Konsistensi saat assignment berubah |
| Data | Seed `worker_plantation_assignments` ke Supabase | Endpoint `/available-supirs` jadi reachable |

### Re-run profiling

```bash
# 1. apply tuning + restart service
./gradlew bootRun

# 2. seed (sekali, idempotent)
./scripts/seed-supabase.sh

# 3. run JMeter
cd tests/load/jmeter
./run.sh
```

Folder `results/<timestamp>/report/index.html` punya breakdown lengkap
per request dan timeline grafik.

