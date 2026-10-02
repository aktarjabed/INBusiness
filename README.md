# INBusiness — Agro Inputs Invoicing & Financial Management

INBusiness is an offline-first Android application built for **J.A. Agro Inputs & Trading**. It
creates GST-formatted tax invoices, keeps a double-entry stock ledger, records payments against
invoices, and reports revenue and dues — entirely on the device, with no network calls.

> **Scope of this document.** Every feature below is implemented and reachable in this
> codebase, and each row names the files that implement it so the claim can be checked.
> Anything that is modelled in the schema but *not* wired to the UI is listed only under
> [Not Implemented Yet](#not-implemented-yet).
>
> Unless stated otherwise, file paths are relative to
> `app/src/main/java/com/aktarjabed/inbusiness/`.

## Implemented Features

### App shell & data foundation

| Feature | Behaviour | Implementation |
|---|---|---|
| Offline-first storage | All reads/writes go to a local Room database; the app declares no `INTERNET` permission. | `data/database/AppDatabase.kt`, `di/DatabaseModule.kt` |
| Encrypted database | The DB is opened through SQLCipher with a 256-bit random passphrase held in `EncryptedSharedPreferences` behind an Android Keystore master key. | `security/KeyProvider.kt`, `AppDatabase.buildDatabase` |
| First-run setup | Name + address are required; the optional GSTIN is normalized to uppercase and validated before it is stored. Saving also activates the new business. | `presentation/screens/SetupScreen.kt`, `presentation/viewmodel/SetupViewModel.kt`, `data/repository/BusinessRepository.kt` |
| Startup routing | Decides between Dashboard and Setup, treating "no business yet" as a normal state, and re-routes to Setup (with a warning log) when the stored business id has no matching row. | `presentation/viewmodel/SplashViewModel.kt`, `presentation/screens/SplashScreen.kt` |
| Per-business data isolation | Every query that touches tenant data binds the active `businessId` from `BusinessContext`; the id flows through DataStore and is re-read on every operation. | `domain/context/BusinessContext.kt`, all DAOs, `di/DatabaseModule.kt` |
| Navigation | Single Compose `NavHost` with typed route constants for splash, setup, dashboard, invoice, preview, history, inventory, add/edit product and calculator. | `presentation/navigation/NavGraph.kt`, `presentation/navigation/NavigationRoutes.kt` |

### Invoicing

| Feature | Behaviour | Implementation |
|---|---|---|
| Atomic invoice creation | Header, line items, stock movements, quota counter, invoice number and the optional initial payment are written in one `RoomDatabase.withTransaction`; any failure rolls all of it back. | `data/repository/InvoiceRepository.createInvoice`, `domain/usecase/CreateInvoiceUseCase.kt` |
| Invoice number sequence | Per-business counter (`INV-00001`, …) incremented with a conditional `UPDATE` inside the same transaction, so numbers cannot repeat. | `data/dao/InvoiceDao.kt` (`incrementSequence`, `insertSequence`), `data/entities/InvoiceSequence.kt` |
| Seller/buyer snapshots | Seller name/address/GSTIN and buyer name/GSTIN/address are frozen onto the invoice at creation time; later profile edits do not change issued invoices. | `data/entities/Invoice.kt`, `InvoiceRepository.createInvoice` |
| GST calculation | Per-line subtotal and tax are computed with `BigDecimal` and `HALF_UP`; intra-state splits into CGST/SGST with the remainder assigned to SGST, inter-state produces IGST. `UNKNOWN` supply type is rejected at calculation time. | `domain/invoice/GstCalculator.kt`, `domain/invoice/CalculateInvoiceTotalsUseCase.kt` |
| GSTIN validation & supply-type detection | A 15-character GSTIN pattern is enforced in Setup and on the invoice screen; supply type is derived by comparing seller/buyer state codes, with a manual override. | `domain/invoice/GstCalculator.kt`, `domain/usecase/DetermineSupplyTypeUseCase.kt`, `SetupScreen.kt`, `presentation/screens/invoice/InvoiceScreen.kt` |
| Line items | Catalog-linked lines (product id, unit, price and GST prefilled from the product or its last sale) or ad-hoc lines; quantity > 0, price ≥ 0, GST ≥ 0 and a non-blank description are enforced. | `presentation/screens/invoice/InvoiceScreen.kt` (`AddItemDialog`), `domain/usecase/GetProductSuggestionsUseCase.kt` |
| Initial payment with the invoice | An optional amount paid + payment mode is validated against the total and written as a `Payment` row plus `amountPaid`/`balanceDue` on the invoice. | `CalculateInvoiceTotalsUseCase.kt`, `InvoiceRepository.createInvoice` |
| Idempotent submission | A submission key is sent with the request, stored with a SHA-256 payload fingerprint and validated inside the transaction: an identical retry replays the existing invoice, a different payload with the same key is rejected, and the key is discarded as soon as any input changes. | `utils/RequestFingerprint.kt`, `InvoiceRepository.createInvoice`, `presentation/screens/invoice/InvoiceViewModel.kt` |

### Inventory & stock ledger

| Feature | Behaviour | Implementation |
|---|---|---|
| Product catalogue | Create and edit products (name, brand, category, unit, price, stock, batch, wholesale flag, GST %, low-stock threshold) with field-level validation; duplicates on the same name/brand/category/unit/batch are rejected with an actionable message. | `presentation/screens/inventory/ProductEntryScreen.kt`, `presentation/viewmodel/ProductViewModel.kt`, `data/repository/ProductRepository.kt`, `data/entities/Product.kt` |
| Search & category filter | Name/brand search and category chips backed by SQL `LIKE` queries scoped to the business. | `data/dao/ProductDao.kt`, `presentation/screens/inventory/InventoryListScreen.kt` |
| Double-entry stock movements | Every stock change writes a movement with `stockBefore`/`stockAfter`: `OPENING_STOCK`, `STOCK_ADJUSTMENT`, `STOCK_DEDUCTION`, `SALE`, `SALE_REVERSAL`. | `data/entities/StockMovement.kt`, `data/dao/StockMovementDao.kt`, `ProductRepository.kt`, `InvoiceRepository.kt` |
| Race-free stock deduction | `UPDATE … WHERE availableStock >= :quantity` means a concurrent sale cannot drive stock negative; zero affected rows is reported as insufficient stock. | `data/dao/ProductDao.kt` (`deductStock`), `InvoiceRepository.createInvoice` |
| Deletion safety | Products referenced by the stock ledger cannot be deleted (RESTRICT foreign key); the raw constraint failure is translated into guidance to set stock to zero instead. | `data/entities/StockMovement.kt`, `ProductRepository.deleteProduct` |
| Sale/reversal symmetry | Stock moves only for catalog-linked lines: creating an invoice deducts and logs `SALE`, cancelling adds back and logs `SALE_REVERSAL`, each exactly once. | `InvoiceRepository.createInvoice`, `InvoiceRepository.cancelInvoice` |

### Payments & ledger reconciliation

| Feature | Behaviour | Implementation |
|---|---|---|
| Record payment | "Record Payment" on the invoice preview; the amount is rounded to paise and written with the invoice summary in one transaction. | `presentation/screens/invoice_preview/InvoicePreviewViewModel.kt`, `data/repository/PaymentRepository.kt` |
| Reconciliation invariant | A payment is refused unless `amountPaid` equals `SUM(SUCCESS payments)` for that invoice, and refused if it would exceed the invoice total. | `PaymentRepository.addPayment`, `data/dao/PaymentDao.kt` |
| Legacy ledger repair | Invoices predating the `payments` table carry a summary with no ledger rows; the first new payment records that amount as an explicit `OPENING_BALANCE` entry instead of permanently blocking the ledger. | `PaymentRepository.repairLegacyLedgerGap` |
| Cancelled-invoice guard | Payments cannot be recorded on a `CANCELLED` invoice, and an invoice with recorded payments cannot be cancelled. | `PaymentRepository.addPayment`, `InvoiceRepository.cancelInvoice` |
| Double-submit guard | In-flight flags in the view models stop a double tap from posting the same payment or cancellation twice. | `InvoicePreviewViewModel.kt` |

### Invoice preview, history & dashboard

| Feature | Behaviour | Implementation |
|---|---|---|
| Invoice preview | Loads the invoice snapshot + items (scoped to the active business), shows totals, GST split, paid/balance and a CANCELLED banner, and offers Share PDF, Record Payment and Cancel. | `presentation/screens/invoice_preview/InvoicePreviewScreen.kt`, `domain/usecase/GetInvoiceForPreviewUseCase.kt` |
| Cancellation | Cancel is offered only for a completed invoice with no payments, asks for confirmation, marks the invoice `CANCELLED`, restores stock and surfaces the repository's reason if it refuses. | `InvoicePreviewViewModel.cancelInvoice`, `InvoiceRepository.cancelInvoice` |
| Invoice history | SQL-backed list with debounced search (invoice number / customer name), infinite pagination, and status, payment-status (`PAID`/`UNPAID`) and document-type filters; result generation ids prevent out-of-order pages. | `presentation/screens/invoice_history/InvoiceHistoryViewModel.kt`, `data/dao/InvoiceHistoryFilter.kt`, `data/repository/InvoiceHistoryRepository.kt` |
| Injection-safe history query | Every user value is a bind argument; placeholder/argument counts are pinned by tests and the query is built per filter combination. | `data/dao/InvoiceHistoryFilter.kt`, `app/src/test/.../dao/InvoiceHistoryFilterTest.kt` |
| Dashboard metrics | Total revenue, today's revenue, pending dues, invoices today, active products and low-stock counts, all aggregated in SQL (completed invoices only). | `data/dao/DashboardDao.kt`, `data/repository/DashboardRepository.kt`, `presentation/screens/dashboard/DashboardViewModel.kt` |
| 7-day revenue chart | Exactly the last seven calendar days, zero-filled, rendered with Vico; reloads when the dashboard is re-entered. | `DashboardRepository.getSevenDayChartData`, `presentation/screens/DashboardScreen.kt` |
| Single business timezone | `Asia/Kolkata` is authoritative for quota windows, dashboard day windows, history/preview/PDF timestamps and reset times, using half-open `[start, end)` ranges. | `utils/AppDateUtils.kt`, `util/SystemClock.kt` |

### PDF generation & sharing

| Feature | Behaviour | Implementation |
|---|---|---|
| Snapshot-driven PDF | The document is rendered from the stored invoice snapshot (buyer/seller, prices, GST split, totals, paid/balance) — never from live product data. | `utils/pdf/PdfGenerator.kt` |
| Multi-page, bounded output | Long descriptions wrap and paginate; pages are capped (50) and each description is truncated (2000 chars) with an explicit note so a pathological input cannot exhaust memory or silently drop lines. | `utils/pdf/PdfGenerator.kt`, `utils/pdf/PdfConstants.kt` |
| Amount in words | Indian-numbering words (Rupees … Paise Only) computed through `BigDecimal` so the wording matches the printed total. | `utils/AmountInWordsConverter.kt` |
| Bounded PDF cache | Generated files go to `cacheDir/invoices`; the newest 20 are kept and anything older than the retention window beyond that is evicted, never touching non-PDF files. | `utils/pdf/PdfCacheManager.kt` |
| Safe sharing | Only the cache directory is exposed, through a non-exported `FileProvider`; sharing uses `ACTION_SEND` with a read-permission grant and a sanitized file name. | `AndroidManifest.xml`, `res/xml/file_paths.xml`, `InvoicePreviewScreen.kt` |

### Free-tier invoice quota

| Feature | Behaviour | Implementation |
|---|---|---|
| Race-free consumption | One conditional `UPDATE` both rolls the period over and increments the counters, so concurrent submissions can never exceed the cap. | `data/dao/UserQuotaDao.kt` (`consumeQuotaAtomic`), `domain/quota/QuotaGate.kt` |
| Limits | Free tier: 2 invoices/day (plus a launch bonus while it lasts) and 60/month; paid tiers modelled as unlimited. | `QuotaGate.getDailyLimit/getMonthlyLimit/getLaunchBonus` |
| Business-timezone rollover | Day and month boundaries come from the business clock, matching the dashboard and reports. | `util/SystemClock.kt`, `QuotaGate.assertQuota` |
| UI feedback | Warning banner as the allowance runs low, a blocked dialog that names the limit and its reset time (or expiry), and an informational upgrade dialog stating that in-app purchases are not enabled in this build. | `presentation/components/QuotaWarningBanner.kt`, `presentation/components/QuotaBlockedDialog.kt`, `presentation/screens/invoice/InvoiceScreen.kt` |

### Business calculator

| Feature | Behaviour | Implementation |
|---|---|---|
| Cost/revenue inputs | Raw materials, supplier costs, input/output GST, unit price and quantity. | `presentation/screens/CalculatorScreen.kt`, `presentation/viewmodel/CalculatorViewModel.kt` |
| Metrics | Gross profit, EBITDA, net profit, GST payable, break-even, cash flow, gross/net/operating margin and ROI, recomputed on every edit. | `data/repository/BusinessRepository.calculateFinancialMetrics`, `domain/models/FinancialMetrics.kt` |
| Typing-safe inputs | Fields keep their own text state (typing "15" is never rewritten to "1.05") and request a decimal keypad. | `CalculatorScreen.NumberInputField` |
| Scenarios | Save the current numbers under a name, load them back, delete them; the live business profile is filtered out so it can never be deleted from this screen, and save/delete failures are surfaced. | `CalculatorViewModel.kt`, `CalculatorScreen.kt`, `data/dao/BusinessDao.kt` |

### Database schema & migrations

| Feature | Behaviour | Implementation |
|---|---|---|
| Versioned schema | Single database at version 19 with ten entities and checked-in JSON snapshots used by the migration tests and the CI drift check. | `data/database/AppDatabase.kt`, `app/schemas/…` |
| Non-destructive migrations | Table rewrites copy rows forward and use `PRAGMA defer_foreign_keys=ON`, the documented replacement for `PRAGMA foreign_keys=OFF` (silently ignored inside a migration transaction). | `AppDatabase.MIGRATION_3_4` … `MIGRATION_18_19` |

## Not Implemented Yet

Modelled in the schema or repository layer, but **not reachable in the app** and therefore not
claimed as features:

- **Bill of Supply.** `Invoice.documentType` exists, defaults to `TAX_INVOICE` and can be
  filtered, but no screen ever writes any other value.
- **HSN/SAC and UQC.** `Product.hsnSac`/`uqc` columns exist; nothing sets them and the PDF does
  not print them.
- **Customer master.** The `customers` table, DAO and repository exist, but there is no customer
  screen, and `Invoice.customerId` is not populated (buyer details are snapshotted per invoice).
- **Stock-movement history.** Movements are written for every change, but no screen lists them;
  manual deduction is only reachable via `ProductRepository.deductStock`.
- **Date-range filtering in history.** The SQL filter and view model support it; the filter panel
  exposes no date pickers yet.
- **Multi-business UI.** Storage and repositories are multi-tenant and verified by tests, but the
  UI only ever uses the single active business — there is no switcher or "add business" flow.
- **Encrypted export/import.** No way to move data to a new device; combined with the disabled
  cloud backup, a lost device means lost data (see [Security & Data Safety](#security--data-safety)).
- **Live e-invoicing.** IRN/acknowledgement/QR fields exist on the invoice for future use; there
  is no IRP integration and no dynamic QR.
- **Billing.** The upgrade dialog is informational; no purchase can be completed in-app.
- **Print / save-to-file.** Sharing hands the PDF to the system share sheet; there is no direct
  print or document-picker flow.

## Technical Architecture

- **API level:** targets Android API 36 / SDK 36, `minSdk` 24.
- **Tooling:** Kotlin 2.2.10, Android Gradle Plugin 8.9.1, Gradle 8.11.1, JDK 17.
- **UI:** 100% Jetpack Compose (Material 3), single-activity.
- **DI:** Hilt 2.58.
- **Persistence:** Room 2.8.5 + SQLCipher 4.19.1 (AES-256).
- **Concurrency:** Kotlin coroutines/Flow; `RoomDatabase.withTransaction` for every multi-table
  business operation.

## Build & Verification

Prerequisites:

- JDK 17 (`JAVA_HOME` set)
- Android SDK with platform 36 and build-tools (`ANDROID_HOME` / `ANDROID_SDK_ROOT`, or
  `local.properties` with `sdk.dir=...`)

```bash
# Debug build
./gradlew assembleDebug

# JVM unit tests (domain, security and regression suites)
./gradlew testDebugUnitTest

# Android Lint (fails the build on errors)
./gradlew lintDebug

# Instrumented tests — require an emulator/device (API 36 recommended)
./gradlew connectedDebugAndroidTest

# Regenerate the Room schema JSON for the current version (opt-in; see below)
./gradlew :app:kaptDebugKotlin -ProomSchemaExport=true
```

CI (`.github/workflows/android.yml`) runs the debug build, unit tests, Android Lint, a Room
schema-drift check and the instrumented suite on an emulator. Every Gradle invocation runs under
`pipefail` (or hands its exit code over explicitly for the emulator step), so a non-zero Gradle
exit fails the job instead of being masked by `tee`; the test and lint steps additionally verify
that results were actually produced before the job can pass.

Latest verified run on `main`'s session branch:
[run `36959674425`](https://github.com/aktarjabed/J.A.Agro_Inputs_And_Trading-/actions/runs/36959674425)
— build + lint + **71 unit cases (12 classes, 0 failures)** and **34 instrumented cases
(10 classes, 0 failures, 6 documented skips)**, plus the Room v19 schema diff.

## Database & Migrations

- Single `AppDatabase` (SQLCipher encrypted), currently at **schema version 19**.
- All migrations live in `AppDatabase.Companion` and are non-destructive: existing rows are copied
  forward, never dropped.
- Checked-in schema snapshots are in `app/schemas/.../`; the CI job fails if the generated schema
  drifts from the checked-in JSON.
- Checked-in snapshots exist for the versions the migration tests start from (`5`, `6`–`13`, `18`,
  `19`); the intermediate versions (`14`–`17`) are never a start point and are not stored.
- Schema export is **opt-in** (`-ProomSchemaExport=true`): an ordinary build does not re-export,
  because Room's processor deserializes the existing snapshot with the `kotlinx-serialization`
  classes on the annotation-processor classpath, and Room 2.8.5's bundled serializers predate the
  1.8 interface (`AbstractMethodError: FieldBundle$$serializer ... typeParametersSerializers()`).
  Tests read the checked-in JSON directly and CI regenerates and diffs it, so drift is still caught.
- Adding a column/index requires a new `Migration`, a bumped `@Database(version = …)`, a regenerated
  schema JSON **and** an entry in the workflow's schema-verification step.
- Migrations that recreate tables run `PRAGMA defer_foreign_keys=ON`.

## Tests

| Suite | Location | What it protects |
|---|---|---|
| Domain invariants | `app/src/test/.../domain` | GST rounding/splitting, GSTIN validation, supply types, invoice totals, quota caps |
| Security/regression | `app/src/test/.../security`, `.../utils` | Backup policy, request fingerprint, SQL filter binding, PDF cache bounds, business-timezone boundaries |
| Database/Room | `app/src/androidTest/.../database` | Migrations, tenant isolation, concurrency, rollback, stock/payment ledger |
| Repository | `app/src/androidTest/.../repository` | Payment ledger reconciliation, idempotency, cancellation reversal exactly once |

Migration tests that depend on Room's exported-schema bundles are reported as *skipped* on-device
while the upstream Room 2.8.5 ↔ `kotlinx-serialization` incompatibility persists; the skip rule
(`app/src/androidTest/.../database/SqliteSchemaBundleRule.kt`) documents the defect and re-fails on
anything else.

## Security & Data Safety

- The database is encrypted with SQLCipher; the passphrase is a 256-bit random value stored in
  `EncryptedSharedPreferences`, backed by an Android Keystore master key.
- Automatic cloud backup and device-to-device transfer are disabled (`android:allowBackup="false"`
  plus full-domain exclusions in `data_extraction_rules.xml` / `backup_rules.xml`), because a
  partial restore would leave the app pointing at data it cannot open.
- Cleartext traffic is disabled, and generated PDFs are exposed only through the non-exported
  `FileProvider`.
- See [SECURITY.md](SECURITY.md) for the full data-safety model and the reporting process.
