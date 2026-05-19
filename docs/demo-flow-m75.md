# Demo Flow Shipment Sampai Milestone 75%

Dokumen ini buat demo API shipment dari milestone skeleton sampai 75%.

## Data Yang Harus Diganti

Ganti data ini sebelum init/seed database:

| Placeholder | Isi Dengan | Dipakai Untuk |
| --- | --- | --- |
| `MANDOR_ID` | `id` dari akun role `MANDOR` di Identity | `shipments.mandor_user_id`, token create shipment |
| `SUPIR_ID` | `id` dari akun role `SUPIR` di Identity | `shipments.supir_user_id`, token list/update shipment |
| `ADMIN_ID` | `id` dari akun role `ADMIN` di Identity | token admin approval |
| `JWT_SECRET` | secret yang sama di Identity, Shipment, Harvest | validasi token lintas service |
| `SPRING_DATASOURCE_URL` | JDBC database service yang sedang didemo | koneksi DB |
| `SPRING_DATASOURCE_USERNAME` | username DB | koneksi DB |
| `SPRING_DATASOURCE_PASSWORD` | password DB | koneksi DB |
| `HARVEST_SERVICE_URL` | URL Harvest Service, biasanya `http://localhost:8083` | validasi harvest saat create shipment |
| `CLOUDAMQP_URL` | URL RabbitMQ/CloudAMQP, optional untuk demo event | publish `shipment.completed` |

Di file seed shipment:

```text
src/main/resources/sql/shipment-cleanup-and-seed.sql
```

ganti:

```sql
'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa'::uuid -- MANDOR_ID
'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb'::uuid -- SUPIR_ID
```

## Branch Milestone

| Milestone | Branch | Yang Didemo |
| --- | --- | --- |
| 25% / Skeleton | `feat-shandy/shipment-foundation-m25` | GET list/detail, status enum, ownership supir |
| 50% / Core Rules | `feat-shandy/shipment-foundation-m50` | create by mandor, max 400 kg, update status supir, event saat `TIBA` |
| 75% | `feat-shandy/shipment-m75` | admin approval, unique harvest claim, race-condition guard |

## 0. Start Services

Terminal 1, Identity:

```bash
cd mysawit-identity-service
export JWT_SECRET="secret-yang-sama-minimal-32-karakter"
./gradlew bootRun
```

Terminal 2, Harvest:

```bash
cd mysawit-harvest-service
export JWT_SECRET="secret-yang-sama-minimal-32-karakter"
./gradlew bootRun
```

Terminal 3, Shipment:

```bash
cd mysawit-shipment-service
git switch feat-shandy/shipment-m75
export JWT_SECRET="secret-yang-sama-minimal-32-karakter"
export HARVEST_SERVICE_URL="http://localhost:8083"
./gradlew bootRun
```

Base URL:

```bash
export IDENTITY_BASE="http://localhost:8081"
export HARVEST_BASE="http://localhost:8083"
export SHIPMENT_BASE="http://localhost:8084"
```

## 1. Buat Akun Demo Dan Token

Register mandor:

```bash
MANDOR_AUTH=$(curl -s -X POST "$IDENTITY_BASE/api/auth/register" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "mandor-demo",
    "email": "mandor-demo@mail.com",
    "password": "secret123",
    "role": "MANDOR",
    "certificationNumber": "CERT-DEMO-001"
  }')

export MANDOR_TOKEN=$(echo "$MANDOR_AUTH" | jq -r '.token')
export MANDOR_ID=$(echo "$MANDOR_AUTH" | jq -r '.id')
```

Register supir:

```bash
SUPIR_AUTH=$(curl -s -X POST "$IDENTITY_BASE/api/auth/register" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "supir-demo",
    "email": "supir-demo@mail.com",
    "password": "secret123",
    "role": "SUPIR",
    "kebunId": "kebun-demo-001"
  }')

export SUPIR_TOKEN=$(echo "$SUPIR_AUTH" | jq -r '.token')
export SUPIR_ID=$(echo "$SUPIR_AUTH" | jq -r '.id')
```

Kalau akun sudah ada, pakai login:

```bash
SUPIR_AUTH=$(curl -s -X POST "$IDENTITY_BASE/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"email":"supir-demo@mail.com","password":"secret123"}')

export SUPIR_TOKEN=$(echo "$SUPIR_AUTH" | jq -r '.token')
export SUPIR_ID=$(echo "$SUPIR_AUTH" | jq -r '.id')
```

Admin tidak bisa self-register. Pakai akun admin seed dari Identity, lalu login:

```bash
ADMIN_AUTH=$(curl -s -X POST "$IDENTITY_BASE/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@example.com","password":"secret123"}')

export ADMIN_TOKEN=$(echo "$ADMIN_AUTH" | jq -r '.token')
export ADMIN_ID=$(echo "$ADMIN_AUTH" | jq -r '.id')
```

Cek ID:

```bash
echo "MANDOR_ID=$MANDOR_ID"
echo "SUPIR_ID=$SUPIR_ID"
echo "ADMIN_ID=$ADMIN_ID"
```

## 2. Init Shipment Demo Data

Edit seed SQL:

```bash
open src/main/resources/sql/shipment-cleanup-and-seed.sql
```

Ganti:

```sql
'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa'::uuid
```

dengan `$MANDOR_ID`, lalu ganti:

```sql
'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb'::uuid
```

dengan `$SUPIR_ID`.

Run SQL itu di Supabase SQL Editor atau `psql`.

Data yang akan tersedia:

| Shipment ID | Status | Total |
| --- | --- | --- |
| `11111111-1111-1111-1111-111111111111` | `MEMUAT` | `125.50` |
| `22222222-2222-2222-2222-222222222222` | `MENGIRIM` | `210.00` |
| `33333333-3333-3333-3333-333333333333` | `TIBA` | `98.75` |

## 3. Milestone 25%: GET API Skeleton

Health:

```bash
curl -i "$SHIPMENT_BASE/api/shipments/health"
```

Supir list shipment miliknya:

```bash
curl -i "$SHIPMENT_BASE/api/shipments" \
  -H "Authorization: Bearer $SUPIR_TOKEN"
```

Expected:

```text
200 OK
```

Response berisi shipment dengan `supirUserId = SUPIR_ID`.

Detail shipment:

```bash
curl -i "$SHIPMENT_BASE/api/shipments/11111111-1111-1111-1111-111111111111" \
  -H "Authorization: Bearer $SUPIR_TOKEN"
```

Expected:

```text
200 OK
status MEMUAT
```

## 4. Milestone 50%: Status Flow Dan Create Guard

Update valid `MEMUAT -> MENGIRIM`:

```bash
curl -i -X PATCH "$SHIPMENT_BASE/api/shipments/11111111-1111-1111-1111-111111111111/status" \
  -H "Authorization: Bearer $SUPIR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"status":"MENGIRIM"}'
```

Expected:

```text
200 OK
status MENGIRIM
```

Update valid `MENGIRIM -> TIBA`:

```bash
curl -i -X PATCH "$SHIPMENT_BASE/api/shipments/11111111-1111-1111-1111-111111111111/status" \
  -H "Authorization: Bearer $SUPIR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"status":"TIBA"}'
```

Expected:

```text
200 OK
status TIBA
```

Saat status menjadi `TIBA`, service publish event:

```text
exchange: shipment.exchange
routing key: shipment.completed
payload: shipmentId, driverId, mandorId, totalKg, harvestIds, completedAt
```

Test invalid loncat status. Pakai shipment yang masih `MEMUAT`; kalau sudah berubah, reset status via seed dulu.

```bash
curl -i -X PATCH "$SHIPMENT_BASE/api/shipments/11111111-1111-1111-1111-111111111111/status" \
  -H "Authorization: Bearer $SUPIR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"status":"TIBA"}'
```

Expected:

```text
409 Conflict
Invalid status transition
```

Test create shipment `>400 kg`, ini tidak butuh Harvest Service valid karena ditolak sebelum validasi harvest:

```bash
curl -i -X POST "$SHIPMENT_BASE/api/shipments" \
  -H "Authorization: Bearer $MANDOR_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{
    \"supirUserId\":\"$SUPIR_ID\",
    \"destination\":\"Pabrik Sawit Cikupa\",
    \"items\":[
      {\"harvestId\":\"99999999-aaaa-aaaa-aaaa-aaaaaaaaaaaa\",\"weightKg\":401}
    ]
  }"
```

Expected:

```text
400 Bad Request
Total weight 401.0 kg exceeds maximum of 400 kg
```

## 5. Milestone 75%: Admin Approval Dan Duplicate Harvest Guard

Admin approve shipment yang sudah `TIBA`:

```bash
curl -i -X PATCH "$SHIPMENT_BASE/api/shipments/33333333-3333-3333-3333-333333333333/admin-approval" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"status":"ADMIN_APPROVED"}'
```

Expected:

```text
200 OK
status ADMIN_APPROVED
```

Admin approve shipment yang belum `TIBA`:

```bash
curl -i -X PATCH "$SHIPMENT_BASE/api/shipments/22222222-2222-2222-2222-222222222222/admin-approval" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"status":"ADMIN_APPROVED"}'
```

Expected:

```text
409 Conflict
Shipment must be TIBA before admin approval
```

Duplicate harvest guard bisa ditunjukkan dari seed DB:

```sql
select harvest_id, count(*)
from public.shipment_items
group by harvest_id
having count(*) > 1;
```

Expected:

```text
0 rows
```

Dan constraint yang menjaga race-condition:

```sql
select conname
from pg_constraint
where conname = 'uk_shipment_items_harvest_id';
```

Expected:

```text
uk_shipment_items_harvest_id
```

Kalau mau demo API duplicate harvest, create shipment lagi memakai `harvestId` yang sudah ada di `shipment_items`, misalnya:

```bash
curl -i -X POST "$SHIPMENT_BASE/api/shipments" \
  -H "Authorization: Bearer $MANDOR_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{
    \"supirUserId\":\"$SUPIR_ID\",
    \"destination\":\"Pabrik Sawit Cikupa\",
    \"items\":[
      {\"harvestId\":\"99999999-1111-1111-1111-111111111111\",\"weightKg\":100}
    ]
  }"
```

Expected kalau Harvest Service mengembalikan harvest tersebut sebagai `Approved`:

```text
409 Conflict
Harvest already claimed
```

Kalau Harvest Service belum siap atau harvest tidak ada, hasilnya bisa `404/503`. Untuk presentasi, sebutkan bahwa guard API-nya ada di service dan guard DB-nya ada di unique constraint `uk_shipment_items_harvest_id`.

## Narasi Singkat Demo

Milestone 25%:

```text
Shipment sudah punya entity dasar, enum status, list/detail API, dan filter ownership untuk SUPIR dari JWT.
```

Milestone 50%:

```text
Mandor bisa membuat shipment dari harvest approved, total kg dibatasi 400 kg, supir hanya bisa update shipment miliknya dengan state transition berurutan, dan event shipment.completed dipublish saat shipment TIBA.
```

Milestone 75%:

```text
Admin approval hanya boleh setelah shipment TIBA. Harvest tidak bisa diklaim dua shipment karena dicek di service dan diproteksi unique constraint di shipment_items.harvest_id untuk race-condition.
```
