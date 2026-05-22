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
