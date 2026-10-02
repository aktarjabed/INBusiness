# CLAUDE.md

Project context for AI agents and reviewers. Facts here are verified against the code; where a
claim is unverified it says so. Keep this file in sync when you change a listed invariant.

## What this is

`com.aktarjabed.inbusiness` — an offline-first Android app for Indian agri-input traders:
GST invoicing (CGST/SGST/IGST), inventory with a stock-movement ledger, payments, a profitability
calculator, and PDF invoice export. Single-module app (`:app`).

Stack: Kotlin 2.2.10, AGP 8.9.1, Compose (BOM 2025.01.00), Room 2.8.5 + SQLCipher for Android
4.19.1, Hilt 2.58, DataStore Preferences, minSdk 24 / targetSdk 36.
**Nothing has shipped yet** (`versionCode = 1`), so schema/key changes are still cheap — but see
the passphrase note below before assuming any change to persisted data is safe.

Room schema version: **19**. Checked-in schemas live in
`app/schemas/com.aktarjabed.inbusiness.data.database.AppDatabase/`.

## Build and test

```bash
./gradlew assembleDebug                 # compile (CI gate)
./gradlew testDebugUnitTest             # JVM unit tests (CI gate, ~80 cases)
./gradlew :app:connectedDebugAndroidTest  # instrumented; needs a device/emulator
./gradlew :app:kaptDebugKotlin -ProomSchemaExport=true   # regenerate the Room schema (CI diffs it)
```

CI: `.github/workflows/android.yml` runs build → schema regen+diff → unit tests, and *fails* if the
required suites (`BackupPolicyTest`, `PdfCacheManagerTest`, `InvoiceHistoryFilterTest`,
`RequestFingerprintTest`, `SystemClockTest`, `AppDateUtilsTest`) or their JUnit XML are missing.
Lint is not wired into CI as a gate. Unit tests run on the JVM with `unitTests.isReturnDefaultValues
= true`; anything touching `android.util.Log` or Room needs an instrumented test instead.

## Invariants — do NOT "fix" these

Each of these looks wrong at a glance and is deliberate. Changing one will corrupt data or brick
existing installs.

1. **The SQLCipher passphrase is used as Base64 *text*, not as decoded bytes.**
   `KeyProvider.getDatabasePassphrase()` returns the Base64 string, and
   `buildDatabase` calls `passphrase.toByteArray(Charsets.UTF_8)`. The stored 32 random bytes are a
   1:1 encoding of the same 256 bits of entropy, so there is no weakness — but decoding it *now*
   would derive a different SQLCipher key and make every existing database unreadable. Changing
   this requires a `PRAGMA rekey` migration first. See the comment in `KeyProvider.kt`.
2. **Invoice sequence allocation is increment-then-read inside one `withTransaction`.** SQLite
   lets exactly one writer commit at a time, so this serializes correctly; there is no "read the
   value twice" race. `InvoiceConcurrencyTest.testSequenceConcurrency` hammers 20 concurrent
   creations and asserts unique numbers. Do not split the increment and the read.
3. **Quota is consumed through one cap-guarded conditional `UPDATE`**
   (`UserQuotaDao.consumeQuotaAtomic`), inside the same transaction as the invoice insert. The
   `UPDATE ... WHERE (dailyUsed < cap AND monthlyUsed < cap)` form is what makes it race-free —
   a read-then-write refactor would reintroduce a TOCTOU bypass.
4. **`MIGRATION_12_13`'s `PRAGMA table_info` guard is intentional**, not redundant with
   `MIGRATION_11_12`. It is idempotent so a half-applied 11→12 cannot crash-loop users at 12→13.
5. **`MIGRATION_18_19` converts `businessId` with `CAST(... AS TEXT)` and is lossless**:
   business ids are UUIDs (`SetupViewModel`), and SQLite's INTEGER affinity only coerces
   *numeric-looking* text, so UUIDs were already stored as TEXT. Do not add a "repair orphaned
   rows" UPDATE — there are no orphaned rows, and `business_data` has no `createdAt` column.
6. **`getHistoricalInvoiceItems` returns every line, newest-first
   (`ORDER BY inv.createdAt DESC, i.id DESC`)**, and `collapseHistoryToLatest()` in the Kotlin
   layer keeps the first row per product/description key. That ordering is load-bearing: the
   collapse assumes "first occurrence = most recent". The old correlated-subquery SQL was
   row-for-row equivalent but quadratic (measured 0.6 s at 1.5k lines, >100 s at 20k on real
   SQLite). Do not replace the Kotlin collapse with `GROUP BY MAX(id)` — item ids are UUIDs, so
   `MAX(id)` picks an arbitrary row and returns the wrong price for essentially every product.
7. **`RequestFingerprint` is payload-only** (no timestamp), so it is stable across retries and
   recomputable after a failed transaction. Idempotency keys are reused only while the payload is
   unchanged (`InvoiceViewModel.invalidatePendingIdempotencyKey`).
8. **`TransactionAbortException` is control flow**, carries the domain result, and skips stack
   capture on purpose. Expected rejections (quota, stock, replay) must not look like crashes.
9. **`ON CONFLICT IGNORE` (never REPLACE) for the invoice sequence row** — REPLACE would reset the
   counter and allow invoice numbers to be reused.
10. **`allowBackup="false"` + empty `backup_rules`/`data_extraction_rules`** must stay aligned:
    the encrypted database and its key in `EncryptedSharedPreferences` cannot be restored
    independently without losing data. `BackupPolicyTest` asserts the manifest/rules stay consistent.
11. **GSTIN validation includes the Luhn mod 36 check digit** (H-12). `isValidGstin` is a hard gate
    in Setup/Invoice screens and `determineSupplyType`; test GSTINs must therefore be
    checksum-valid (e.g. `29ABCDE1234F1ZW`, or real ones like `27AAPFU0939F1ZV`). Unregistered
    buyers leave the field blank; the invoice screen's manual Intra/Inter selector is the escape
    hatch when a real counterparty GSTIN still fails.
12. **Never query `business_data` without naming an id.** The table holds the live profile *and*
    the calculator's saved scenarios, and `insertBusinessData` uses `OnConflictStrategy.REPLACE`,
    which gives the profile a new rowid on every edit. A `SELECT * FROM business_data LIMIT 1`
    stops meaning "the business" after the first profile edit (verified in SQLite: it returns a
    scenario row). The two such DAO methods were removed for this reason; use
    `getBusinessDataById(activeBusinessId)`.
13. **Do not add `init { require(...) }` validation to Room entities.** Room calls the entity
    constructor when *reading* rows, so a `require` there turns any legacy/corrupt row into a
    crash on read (and old rows cannot be fixed by validation on the write path). Validate in the
    repository, where `PaymentRepository.addPayment`, `ProductRepository.saveProduct` and
    `StockMovementRepository.addMovement` already do.
14. **`StockMovementRepository.addMovement` deliberately does not enforce a sign-per-type table.**
    `STOCK_ADJUSTMENT` is legitimately positive or negative, so a fixed rule would reject valid
    adjustments. Type-independent invariants (finite, non-zero, non-blank type/reference) are
    enforced; the sign is set by the call site that knows the movement type.

## Known real gaps (verified, not yet fixed)

- `InvoiceViewModel` keeps the in-progress invoice in plain `MutableStateFlow`s with no
  `SavedStateHandle`, so process death in the background loses a half-composed invoice. The
  screen-level `rememberSaveable` conversions cover product/setup/search forms only.
- `app/schemas/.../{14,15,16,17}.json` are missing, so `MigrationTestHelper.createDatabase(name,
  14..17)` cannot open those versions. Tests currently start at 13 (which has a schema) and 18.
  Per-step validation of 14→18 therefore isn't possible until those snapshots are regenerated
  from the historical entity definitions (they can be back-filled but must be *correct*; do not
  fabricate them).
- `QuotaGate` hardcodes the device tier to `FREE` (`createFirstQuota`), so `dailyCap` is 2 (+1
  during the launch window) and `isLaunchPeriod()` ends 2026-11-20. There is no purchase flow;
  the "unlimited" tiers are unreachable in production today.
- `invoice_items` has no index on `productId`; the history query scans all of a business's lines.
- No `@Preview`s, no `FLAG_SECURE`, no Compose UI tests, no `testTag`s.
- The calculator lists the live business profile alongside saved scenarios (`getAllBusinessData`
  returns every row; only *deletion* of the active profile is guarded in `CalculatorViewModel`).
  Splitting profile from scenarios is the real fix; the `LIMIT 1` landmine is gone but the shared
  table remains.
- AndroidManifest has no `INTERNET` permission (deliberate: fully offline), so nothing may assume
  network access. FileProvider (`${applicationId}.fileprovider` -> `cache-path invoices/`) lines up
  with `PdfGenerator` (`cacheDir/invoices`) and the share intent sets
  `FLAG_GRANT_READ_URI_PERMISSION` — verified, do not "simplify" any of the three.
- Two catch blocks (the `SQLiteConstraintException` idempotency fallback in `createInvoice`, and
  `cancelInvoice`'s generic handler) would also catch a `CancellationException`; the inner guard
  usually throws first, but the handlers are not airtight.

## Working agreements

- **Verify before changing.** Every claim about this codebase (especially in a review or audit)
  must be checkable against the file, the checked-in schema, or a real SQLite engine. Several past
  "critical" findings were provably false — see the notes above and `AUDIT_REPORT.md`-style
  reports for examples. Prefer a 20-line reproduction (sqlite3 + Python works well) over reasoning
  about SQLite internals from memory.
- Keep money and quantities in `BigDecimal` with explicit `RoundingMode.HALF_UP` and 2-decimal
  scale; `GstCalculator` derives `sgst = taxAmount - cgst` so the halves always sum to the tax.
- Migrations: never `INSERT INTO new SELECT *`; list columns explicitly. Never rely on
  `PRAGMA foreign_keys=OFF` inside a migration — use `PRAGMA defer_foreign_keys=ON`.
- Write the *why* in comments when a construct looks wrong but isn't (as above). That is the house
  style and it is the only thing preventing a future "cleanup" from reintroducing these bugs.
