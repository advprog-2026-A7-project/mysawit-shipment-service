# Software Design Report - MySawit Shipment Service

## Claimed Score

**Software Design: 4/4**

Shipment service tidak hanya memakai beberapa design pattern, tetapi juga menunjukkan perbaikan desain berbasis non-functional requirements: maintainability, testability, performance, reliability, security, dan observability. Bukti utamanya ada pada refactor bertahap dari milestone awal sampai optimasi final, disertai test dan profiling.

## Ringkasan Bukti Commit

| Area | Commit | Bukti Perubahan |
| --- | --- | --- |
| Status policy | [ce70ef9](https://github.com/advprog-2026-A7-project/mysawit-shipment-service/commit/ce70ef9d3d66c6f4cbc7e98b5a49642ae3824abf) | Centralize status transition rule ke `ShipmentStatusTransitionPolicy`. |
| DTO boundary | [a766bca](https://github.com/advprog-2026-A7-project/mysawit-shipment-service/commit/a766bca5f7acb224ee242fcebe37ff953de98139) | Memisahkan JPA entity dari API contract dengan DTO response/request. |
| Exception mapping | [5296a81](https://github.com/advprog-2026-A7-project/mysawit-shipment-service/commit/5296a81f8ddc272330e1b4ba705be344f59bd27a) | Memindahkan error mapping ke `@RestControllerAdvice`. |
| Guard clauses | [3a456eb](https://github.com/advprog-2026-A7-project/mysawit-shipment-service/commit/3a456ebe0421828d81ebd311d6d3179719a7195b) | Extract ownership dan transition guard dari status update flow. |
| Performance-driven design | [2456c9b](https://github.com/advprog-2026-A7-project/mysawit-shipment-service/commit/2456c9b6b12c34b9fa36b52151a708f92f243fef) | Batch fetch, read-only transaction, Caffeine cache, dan JPA Specifications. |
| Runtime tuning | [ebe8dcb](https://github.com/advprog-2026-A7-project/mysawit-shipment-service/commit/ebe8dcbfc0cca653c0e1d1fe3add32a3a514adcd) | Hikari pool tuning, disable open-in-view, batch fetch config. |
| Monitoring support | [5902e2f](https://github.com/advprog-2026-A7-project/mysawit-shipment-service/commit/5902e2fd2ee1033256c92fbe86b9adec3100b178) | Prometheus dan Grafana monitoring untuk observability. |
| Profiling evidence | [4e5e135](https://github.com/advprog-2026-A7-project/mysawit-shipment-service/commit/4e5e135240c2f7ca9aa4222965b1d9fc8043626c) | JMeter profiling tooling untuk shipment endpoints. |

## Design Patterns dan Justifikasi

| Pattern | Implementasi | Justifikasi |
| --- | --- | --- |
| Policy Object | `ShipmentStatusTransitionPolicy` | Aturan transisi `MEMUAT -> MENGIRIM -> TIBA -> approval` dipisahkan dari service, sehingga perubahan workflow tidak menyebar ke controller atau persistence. |
| Repository Pattern | `ShipmentRepository`, `WorkerPlantationAssignmentRepository` | Business service tidak bergantung pada SQL langsung; query dan persistence disembunyikan di layer repository. |
| Specification Pattern | `ShipmentSpecifications` | Filter list shipment dibuat dinamis per predicate. Ini menghindari query monolitik dengan banyak nullable parameter dan lebih mudah diperluas. |
| DTO Pattern | `CreateShipmentRequest`, `ShipmentResponse`, `ShipmentItemResponse`, approval request DTO | API contract dipisah dari entity JPA agar field internal dan relasi database tidak bocor ke client. |
| Domain Event Publisher | `ShipmentEventPublisher` | Shipment tidak memanggil Payroll/Notification secara langsung. Event dikirim async lewat RabbitMQ. |
| Event-Driven Replica | `HarvestEventConsumer`, user/assignment consumers, replica services | Shipment membaca data harvest/user/assignment lokal hasil sinkronisasi event. Ini mengurangi synchronous coupling antar service. |
| Guard Clause | `ensureOwnedByRequester`, `ensureValidDriverStatusTransition`, `ensureValidAdminDecision`, `ensureReasonPresent` | Validasi domain menjadi eksplisit dan mudah dites per skenario. |
| Centralized Exception Handler | `ShipmentExceptionHandler` | Mapping exception ke HTTP response konsisten dan controller tidak dipenuhi logic error handling. |
| Cached Lookup Boundary | `WorkerAssignmentLookupService` | Assignment mandor/supir adalah hot path saat create shipment, sehingga lookup dibungkus service cache dengan invalidation saat event assignment berubah. |

## Before vs After Design Improvement

### 1. Status Transition

Before:
- Aturan perpindahan status berisiko tersebar di service method.
- Sulit melihat keseluruhan state machine shipment dari satu tempat.

After:
- `ShipmentStatusTransitionPolicy` menjadi single source of truth untuk driver transition, Mandor decision, dan Admin decision.
- Test dapat memvalidasi policy tanpa perlu menyiapkan controller atau database.

NFR impact:
- **Maintainability:** perubahan workflow cukup dilakukan di policy.
- **Testability:** status rule bisa diuji sebagai pure domain logic.
- **Reliability:** invalid transition ditolak konsisten.

### 2. API Boundary

Before:
- Controller lebih dekat ke entity JPA, sehingga response API berpotensi mengikuti struktur persistence.

After:
- DTO request/response menjadi kontrak eksplisit.
- Entity tetap menjadi model internal database, bukan format publik.

NFR impact:
- **Security:** field internal tidak bocor ke client.
- **Maintainability:** perubahan database tidak otomatis memecahkan API.
- **Compatibility:** API lebih stabil untuk frontend.

### 3. Error Handling

Before:
- Error mapping rawan tersebar di controller.

After:
- `ShipmentExceptionHandler` menjadi pusat mapping exception ke HTTP status dan response body.

NFR impact:
- **Reliability:** response error konsisten.
- **Maintainability:** perubahan format error dilakukan di satu tempat.
- **Clean code:** controller tetap fokus pada request orchestration.

### 4. Query Design dan Performance

Before:
- Query filter shipment memakai bentuk legacy `findWithFilters(...)`.
- Banyak parameter optional berpotensi membuat query sulit dirawat dan bermasalah dengan null binding pada Supabase/Supavisor.

After:
- `ShipmentSpecifications` menyusun predicate hanya ketika filter tersedia.
- `@Transactional(readOnly = true)` dipakai untuk read path.
- `@BatchSize` dan `EntityGraph` mengurangi risiko N+1 saat membaca item shipment.
- `findClaimedHarvestIds(Collection)` mengganti pengecekan duplicate harvest satu per satu.

NFR impact:
- **Performance:** jumlah query dan overhead Hibernate berkurang.
- **Scalability:** filter baru dapat ditambah sebagai specification kecil.
- **Maintainability:** query tidak lagi menjadi satu blok besar yang sulit dibaca.

### 5. Assignment Lookup Cache

Before:
- Create shipment perlu lookup assignment Mandor dan Supir ke database setiap request.

After:
- `WorkerAssignmentLookupService` memakai Caffeine cache.
- Cache di-evict ketika event assignment plantation diterima.

NFR impact:
- **Performance:** hot lookup tidak selalu hit database.
- **Consistency:** invalidation tetap menjaga perubahan assignment.
- **Reliability:** create shipment tetap valid karena cache boundary ada di satu service.

### 6. Asynchronous Event Boundary

Before:
- Shipment berpotensi harus bergantung pada service lain secara sinkron untuk payroll, notification, harvest validation, dan assignment state.

After:
- Shipment memakai RabbitMQ event untuk inbound replica dan outbound payroll/notification event.
- Local replica membuat create shipment tidak memerlukan REST call langsung ke Harvest pada runtime utama.

NFR impact:
- **Availability:** Shipment tidak langsung gagal hanya karena downstream payroll/notification lambat.
- **Scalability:** consumer dan service dapat diskalakan terpisah.
- **Architecture clarity:** bounded context shipment tetap jelas.

## Keterkaitan dengan Profiling dan Monitoring

Desain final tidak berhenti di pattern, tetapi ditindaklanjuti dengan pengukuran:

- JMeter profiling menunjukkan error rate turun dari **78.1%** ke **0.2%** setelah tuning dan refactor performa.
- Runtime metrics diekspos lewat `/actuator/prometheus`.
- Grafana dashboard memantau uptime, request rate, 5xx ratio, p95 latency, JVM memory, dan Hikari database connections.

Ini mendukung klaim rubrik 4/4 karena design improvement dikaitkan langsung dengan non-functional requirements, bukan hanya penggunaan pattern secara kosmetik.

## Quality Gate Pendukung

Quality gate terakhir:

```bash
./gradlew clean check
```

Hasil lokal terakhir:

- Build: **SUCCESSFUL**
- Test: **PASS**
- PMD main/test: **PASS**
- JaCoCo line coverage: **100.00%**
- JaCoCo branch coverage: **97.56%**
- JaCoCo method coverage: **100.00%**
- JaCoCo class coverage: **100.00%**

## Kesimpulan

Shipment service layak diklaim **Software Design 4/4** karena:

1. Menggunakan lebih dari tiga design pattern secara tepat.
2. Menunjukkan before-after design improvement melalui commit refactor dan performance-driven design.
3. Mengaitkan desain dengan NFR konkret: maintainability, performance, security, reliability, testability, dan observability.
4. Memiliki bukti quality gate dan profiling untuk mendukung bahwa desain tersebut bukan sekadar struktur kode, tetapi berdampak pada kualitas sistem.
