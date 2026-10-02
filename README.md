# INBusiness - Agro Inputs Invoicing & Financial Management

INBusiness is an offline-first Android application designed specifically for **J.A. Agro Inputs & Trading**. It provides a robust, encrypted, transactional framework for generating invoices, tracking stock, receiving payments, and managing customer ledgers. It supports standard accounting invariants and complies with regional tax/GST formatting without live IRP integration.

## Current Production Capabilities

- **Offline-First:** All data operations are local, powered by Room SQLite.
- **Security:** Entire database is fully encrypted with **SQLCipher** for Android. Keys are managed by Android Keystore. Automatic backup/restore is intentionally disabled because the encrypted database and its Keystore-bound passphrase cannot be restored onto another device (see [Security & Data Safety](#security--data-safety)).
- **Data Isolation:** Fully scoped architecture supporting multiple businesses internally. Transactions strictly validate the `businessId`.

## Implemented Modules

### Transactional Ledger (Phase 3 & 4)
- **Invoice Lifecycle:** Explicit `status` tracking (`COMPLETED`, `CANCELLED`). Atomic transactional creation. `documentType` is modelled and filterable, and every invoice created by the app is a `TAX_INVOICE`; no screen creates a `BILL_OF_SUPPLY` yet (see [Known Limitations](#known-limitations)).
- **Stock Movements:** Every inventory update enforces double-entry rules. Support for `SALE`, `SALE_REVERSAL`, etc. Editing products does not bypass movements.
- **Payment Invariants:** `amountPaid` and `balanceDue` strictly track multiple ledger payments (`amountPaid == SUM(valid Payment.amount)`). Legacy invoices that predate the payments table are repaired with an explicit `OPENING_BALANCE` ledger entry before new payments are accepted.
- **Cancellation:** Invoices can be cancelled exactly once from the invoice preview screen (only while no payment has been recorded), producing deterministic `SALE_REVERSAL` records. Cancellation is refused while payments exist, and it rolls back entirely if a referenced product is missing (no half-applied reversals).
- **Payments from the preview screen:** "Record Payment" writes through `PaymentRepository`, which reconciles the ledger, rejects overpayment and rejects payments on cancelled invoices.
- **Idempotency:** Replaying an identical invoice creation request generates no new side effects. The idempotency key is scoped to one submission attempt and is discarded whenever the invoice payload changes, so an edited invoice is never rejected as a "reused key".

### Dashboard & Analytics (Phase 5)
- **SQL-Backed:** In-database aggregation to prevent N+1 and unbounded memory allocations.
- **Timezone Aware:** `Asia/Kolkata` is the single authoritative business timezone for quota windows, dashboard ranges, ledger timestamps (history list, preview, PDF) and PDF dates. Interval handling uses safe `[start, end)` SQL queries.
- **7-Day Chart:** Always shows exactly the last 7 calendar days, zero-filling empty spots deterministically; the chart is reloaded whenever the dashboard is re-entered.
- **Insights:** Total Revenue, Today's Revenue, Pending Dues, Active Products, Low Stock.
- **Invoice History:** server-side (SQL) search and pagination with status, payment-status (`PAID`/`UNPAID`) and document-type filters.

### Immutable PDF Generation (Phase 6)
- **Truth at Transaction Time:** PDFs are generated based strictly on immutable snapshots recorded at the time of invoice creation (prices, GST, items, seller/buyer details). `Product` carries `hsnSac`/`uqc` columns for future use, but invoice lines do not capture them yet, so the PDF prints neither.
- **Semantically Accurate:** Cumulative payment fields accurately reflect `TOTAL AMOUNT PAID` rather than incorrectly claiming payments were received "today".
- **Bounded:** The generated-PDF cache is pruned (newest 20 documents kept, anything older than 7 days beyond that is evicted) and page/description sizes are capped so a pathological description cannot exhaust memory.
- *Note:* No live IRP, E-Invoice, or dynamic QR generation is claimed.

### Freemium Quota (Phase 7)
- Daily/monthly invoice counters are consumed with a single conditional SQL `UPDATE`, making the cap race-free under concurrent submissions.
- Quota day/month rollover uses the business timezone.
- The free tier is 2 invoices/day (plus a launch bonus while it lasts) and 60/month. The quota dialog/pricing is informational only: **no purchase can be completed in this build**, and the "Upgrade" entry points say so instead of doing nothing.

### Business Calculator
- Cost/revenue inputs (raw materials, supplier costs, GST in/out, unit price, quantity) with live gross/net margin, EBITDA, ROI, break-even and cash-flow metrics.
- Inputs keep their own text state, so typing "15" is not rewritten to "1.05" mid-entry, and money fields request a decimal keypad.
- Scenarios can be saved by name, loaded back and deleted; they are stored in the same table as the live profile, which is filtered out of the scenario list so it can never be deleted from this screen.

## Known Limitations

- **Document types:** only `TAX_INVOICE` is produced. A `BILL_OF_SUPPLY` flow (and HSN/SAC + UQC capture on invoice lines) is modelled in the schema but not implemented in the UI.
- **Customer master:** the `customers` table, DAO and repository exist, but there is no customer management screen, and `Invoice.customerId` is not populated (buyer details are snapshotted per invoice instead).
- **Stock-movement history:** every movement is ledgered, but no screen displays the movement history, and manual stock deduction is only reachable through `ProductRepository.deductStock` (used by tests).
- **History date range:** `InvoiceHistoryViewModel` and the SQL filter support a date range, but the filter panel does not expose date pickers yet.
- **Data portability:** everything is local to the device. Automatic backup is disabled on purpose (see [Security & Data Safety](#security--data-safety)); there is no encrypted export/import yet, so a lost device means lost data.

## Technical Architecture

- **API Level:** Targets Android API 36 / SDK 36, `minSdk` 24.
- **Tooling:** Kotlin 2.2.10, Android Gradle Plugin 8.9.1, Gradle 8.11.1, JDK 17.
- **UI:** 100% Jetpack Compose (Material 3).
- **DI:** Hilt 2.58.
- **Persistence:** Room 2.8.5 + SQLCipher 4.19.1 (AES-256).
- **Concurrency:** Kotlin Coroutines & Flow; `RoomDatabase.withTransaction` for atomic business operations.

## Build & Verification

Prerequisites:

- JDK 17 (`JAVA_HOME` set)
- Android SDK with platform 36 and build-tools (`ANDROID_HOME` / `ANDROID_SDK_ROOT`, or `local.properties` with `sdk.dir=...`)

```bash
# Debug build
./gradlew assembleDebug

# JVM unit tests (domain + security regression suites)
./gradlew testDebugUnitTest

# Android Lint (fails the build on errors)
./gradlew lintDebug

# Instrumented tests — require an emulator/device (API 36 recommended)
./gradlew connectedDebugAndroidTest

# Regenerate the Room schema JSON for the current version (opt-in; see Database & Migrations)
./gradlew :app:kaptDebugKotlin -ProomSchemaExport=true
```

CI (`.github/workflows/android.yml`) runs the debug build, unit tests, Android Lint, a Room schema-drift check, and the instrumented suite on an emulator. Every Gradle invocation runs under `pipefail` (or with an explicit exit-code hand-off for the emulator step), so a non-zero Gradle exit fails the job instead of being masked by `tee`; the test and lint steps additionally verify that results were actually produced before the job can pass.

## Database & Migrations

- Single `AppDatabase` (SQLCipher encrypted), currently at **schema version 19**.
- All migrations live in `AppDatabase.Companion` and are non-destructive: existing rows are copied forward, never dropped.
- Checked-in schema snapshots are in `app/schemas/.../`; the CI job fails if the generated schema drifts from the checked-in JSON.
- Schema export is **opt-in** (`-ProomSchemaExport=true`): an ordinary build does not re-export, because
  Room's processor deserializes the existing snapshot with the `kotlinx-serialization` classes on the
  annotation-processor classpath, and Room 2.8.5's bundled serializers predate the 1.8 interface
  (`AbstractMethodError: FieldBundle$$serializer ... typeParametersSerializers()`). Tests read the
  checked-in JSON directly, and CI regenerates and diffs it, so drift is still caught.
- Adding a column/index requires a new `Migration`, a bumped `@Database(version = ...)`, a regenerated schema JSON **and** an entry in the workflow's schema-verification step.
- Migrations that recreate tables run `PRAGMA defer_foreign_keys=ON` (the documented replacement for `PRAGMA foreign_keys=OFF`, which SQLite silently ignores inside a migration transaction).
- Checked-in snapshots exist for the versions the migration tests start from (`5`, `6`–`13`, `18`, `19`); the intermediate versions (`14`–`17`) are never used as a start point and are therefore not stored.
- Regression coverage: `DatabaseMigrationTest`, `ProductMigrationTest`, `MigrationTest`, plus the JVM suites under `app/src/test`.

## Tests

| Suite | Location | What it protects |
|---|---|---|
| Domain invariants | `app/src/test/.../domain` | GST rounding/splitting, GSTIN validation, invoice totals, quota caps |
| Security/regression | `app/src/test/.../security`, `.../utils` | Backup policy, request fingerprint, SQL filter binding, PDF cache bounds, business-timezone boundaries |
| Database/Room | `app/src/androidTest/.../database` | Migrations, schema drift, tenant isolation, concurrency, rollback |
| Repository | `app/src/androidTest/.../repository` | Payment ledger, stock ledger, idempotency, cancellation reversal exactly once |

## Security & Data Safety

- The database is encrypted with SQLCipher; the passphrase is a 256-bit random value stored in `EncryptedSharedPreferences`, which is backed by an Android Keystore master key.
- Automatic cloud backup and device-to-device transfer are disabled (`android:allowBackup="false"` plus full domain exclusions in `data_extraction_rules.xml` / `backup_rules.xml`). Restoring a partial copy of app state (the encrypted DB cannot be restored, and the Keystore key never leaves the device) previously left the app pointing at data it could not open.
- The invoice PDF cache is stored in `cacheDir/invoices` and exposed only through a non-exported `FileProvider`.
- See [SECURITY.md](SECURITY.md) for the full data-safety model and reporting process.

## Missing / Future Implementation

- **Data Backup / Restore:** Not implemented. Because automatic backup is disabled for integrity reasons, business continuity requires a future explicit, user-initiated encrypted export/import feature.
- **E-Invoice API:** Nullable placeholders exist in the schema, but live IRP/IRN submission is disabled.
- **Purchases / subscriptions:** The upgrade dialog is informational only; no billing integration is wired, and no purchase can be completed in-app.
- **Multi-business UI:** The data model and repositories are multi-tenant, but the UI currently exposes a single active business.
