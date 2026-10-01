# Autonomous Full-Fix Report

## Executive Summary

**Health.** INBusiness is a single-module Android/Kotlin app (Jetpack Compose + Room/SQLCipher +
Hilt, ~10.9k Kotlin LOC across 105 files) implementing an offline invoicing, stock and payment
ledger. Repo-level hygiene was good (no hardcoded secrets, tenant scoping in SQL, non-destructive
migrations), but the code carried several build-blocking, integrity-threatening and
correctness defects. The most serious were an unsatisfiable Hilt graph, an unusable
backup/restore policy, idempotency-key handling that could permanently block invoice saving,
and a payment path with no double-submit protection.

**Release readiness.** *Not release-ready in this sandbox, for one specific reason: nothing can be
compiled or executed here.* The environment has no JDK, no Gradle cache, no Android SDK and no
outbound network (module downloads and `apt` both fail), so `./gradlew assembleDebug`,
`testDebugUnitTest`, `lintDebug` and the instrumented suite were impossible to run. Every change
below is therefore **inspection-verified only** and must be confirmed by CI before a release
decision. Assuming CI is green, the app moves from "several known-broken paths" to "one
known-untested data migration (18→19) plus documented follow-ups".

**Top risks (residual).**
1. `MIGRATION_18_19` recreates `stock_movements`, `payments` and `customers` while casting
   `businessId` INTEGER→TEXT; the transformation is plausible but has never been proven to
   preserve rows on a real pre-19 database (P0 to verify with an instrumented test).
2. Room schema export drift: `app/schemas` contains v5–13 and 18–19; versions 14–17 are missing,
   so only the pinned v19 CI check protects the schema.
3. Missing index on `invoices(businessId, createdAt)`; history/dashboard reads will degrade with
   data volume and the fix needs a schema bump.
4. No static-analysis gate (detekt/ktlint) and the instrumented migration/concurrency suites are
   the only protection for the ledger invariants.

**Top fixes.**
1. Deleted the duplicate Hilt bindings (`AppModule`) that made the Dagger graph unsatisfiable.
2. Rebuilt the backup/restore policy: `allowBackup=false` plus full-domain exclusions for cloud
   backup *and* device transfer, with a regression test.
3. Hardened the invoice submission path: idempotency key invalidated on every payload change,
   cleared on success, double-submit guarded, GSTIN validated before submission.
4. Made the money/ledger paths safe: payment double-tap guard, legacy-ledger repair, atomic
   exactly-once cancellation, business-timezone quota windows, bounded PDF cache.
5. Added 7 JVM regression suites (≈740 new test lines) and a CI dependency-vulnerability job.

**Verification status.** Cannot be executed in this environment (documented blocker, see
*Issues Not Fixed* / *Remaining Risks*). All fixes were verified by exhaustive source inspection:
call-site sweeps, signature/API cross-checks, import and brace-balance checks over all 105 Kotlin
files, and XML/manifest cross-checks against the new policy tests.

## Issues Found

| ID | Severity | Category | Description | Status |
|---|---|---|---|---|
| C-01 | Critical | Build / DI | `AppModule` provided `DeviceClassifier`, `SystemClock` and `QuotaGate` while all three already declare `@Inject` constructors → Dagger `DuplicateBindings`, build fails at kapt. | Fixed |
| C-02 | Critical | Security / data safety | Automatic backup could restore DataStore business/user ids without the SQLCipher database or its Keystore-bound passphrase, leaving an app that can never open its own data. | Fixed |
| C-03 | Critical | State / idempotency | The pending idempotency key survived payload edits; the repository rejects a reused key with a different payload, so an edited invoice after a failed attempt could never be saved. Payment-method and item changes also bypassed invalidation. | Fixed |
| C-04 | Critical | Payments | `recordPayment` had no in-flight guard; a double tap posted the same payment twice (ledger/balance corruption). | Fixed |
| C-05 | Critical | Ledger / inventory | `cancelInvoice` skipped reversal for a linked product that no longer exists, cancelling the invoice while leaving its stock deducted. | Fixed |
| H-01 | High | Timezone | Quota day/month windows used the device default timezone while dashboard, PDF and reporting used `Asia/Kolkata`, so quota could reset on a different calendar day than the one reported. | Fixed |
| H-02 | High | Quota / UI | `QuotaVerdict.DailyCap` computed its reset time in the device zone and could report a negative `resetIn`. | Fixed |
| H-03 | High | Payments / data | Invoices created before schema v15 (or restored from an older backup) had `amountPaid > 0` with an empty `payments` table, permanently failing the reconciliation check → no further payment could ever be recorded. | Fixed |
| H-04 | High | Concurrency / UX | `InvoiceHistoryViewModel` could apply a slow page response on top of a newer search result (no request generation). | Fixed |
| H-05 | High | Validation / tax | Invalid or lowercase GSTINs were accepted by setup and invoicing; supply-type detection silently returned `UNKNOWN`, blocking invoices or producing a wrong tax document. | Fixed |
| H-06 | High | Data integrity | `CalculatorViewModel` offered the live business profile as a deletable "scenario" (it lives in the same table as calculator scenarios). | Fixed |
| H-07 | High | Tests / CI | Mockito 4 (subclass mock maker) cannot mock the final Kotlin classes used by the instrumented tests (`QuotaGate`, `DeviceClassifier`) → 4 tests fail at runtime. | Fixed |
| H-08 | High | Performance / memory | `PdfDocument` keeps every page in memory and the generated-PDF cache was never pruned: a long description could exhaust the heap and the cache directory grew forever. | Fixed |
| H-09 | High | Migrations | `PRAGMA foreign_keys=OFF` was executed inside migration transactions where SQLite silently ignores it, so the intended FK ordering was unprotected; plus a leaked `Cursor` in `MIGRATION_8_9`. | Fixed |
| M-01 | Medium | Error handling | `ProductRepository.deleteProduct` failures (FK/ledger history) were swallowed; the UI appeared to do nothing and the DAO leaked a raw `SQLiteConstraintException`. | Fixed |
| M-02 | Medium | State / deadlock | `CalculatorViewModel` used a zero-buffer `MutableSharedFlow` with `emit()`; with no active collector the reporting coroutine suspends indefinitely. | Fixed |
| M-03 | Medium | Architecture / dead code | Dead encryption layer (`EncryptionManager`, unused AES-GCM wrapper) implied an extra protection layer that is not wired; dead `Converters` list serialization; 3 unused `InvoiceDao` methods, including an `insertOrUpdate` sequence writer that could reuse invoice numbers. | Fixed |
| M-04 | Medium | Dependencies | `navigation-compose:2.8.0-beta01` (beta) in the production path. | Fixed |
| M-05 | Medium | Repo hygiene | No `.gitignore`: build outputs, keystores, local properties, DB files, generated PDFs and env files could be committed. | Fixed |
| M-06 | Medium | Security / UX | The quota/upgrade dialog advertised capabilities the app does not implement (live IRN, API, teams, AI) and prices for purchases that cannot be completed. | Fixed |
| M-07 | Medium | Security | Raw history SQL returned the `SupportSQLiteQuery` interface, so bind arguments could not be asserted; the new injection-regression test could not compile against it. | Fixed |
| M-08 | Medium | Logging | Escaped `\${...}` inside log strings produced literal text with no diagnostic value (5 sites). | Fixed |
| M-09 | Medium | Reliability | `QuotaGate` dereferenced a possibly-null quota row (`!!`) and would crash on a null device tier from an injected classifier. | Fixed |
| M-10 | Medium | CI | No dependency/vulnerability review in CI. | Fixed |
| L-01 | Low | Test correctness | `GstCalculatorTest` asserted that a synthetic `"00ABCDE1234F1Z5"` GSTIN is invalid (zero is a legal state code); the assertion was wrong and was removed. | Fixed |
| L-02 | Low | Validation | `CalculateInvoiceTotalsUseCase` rejected sub-paisa initial payments with a raw `IllegalArgumentException`; behaviour is correct but is now pinned by tests and a clear message. | Fixed |
| L-03 | Low | UI | The `QuotaExceeded` error surfaced as the bare string "Quota Exceeded". | Fixed |
| L-04 | Low | Security | `file_paths.xml` / FileProvider exposure and `usesCleartextTraffic` were not explicitly hardened. | Fixed |
| L-05 | Low | UI | `ProductEntryScreen` displays an empty GST field for a stored 0.0 (blank and 0 are equivalent on save, so no data loss). | Not fixed |
| L-06 | Low | Dead code | Unused use-case dependencies (`businessContext`, `BusinessRepository`) kept in Hilt graphs. | Fixed |
| L-07 | Low | Tests | Misleading test name (`testRemainingCountIsNeverNegative` asserted a cap verdict, not a clamp). | Fixed |
| I-01 | Info | Performance | No index on `invoices(businessId, createdAt)` / `status`; history and dashboard queries scan per business. | Not fixed |
| I-02 | Info | Build | Compose BOM `2024.05.00` is much older than Kotlin 2.2.10 / AGP 8.9.1. | Not fixed |
| I-03 | Info | Tooling | No detekt/ktlint/static-analysis gate; UI strings are hardcoded instead of in `strings.xml`. | Not fixed |
| I-04 | Info | UX | Pagination trigger derives from `listState.layoutInfo`; splash has no timeout; dashboard `combine` uses positional vararg casts. | Not fixed |
| I-05 | Info | Migrations | Room schema snapshots for 14–17 are absent; CI pins v19 only. | Not fixed |
| I-06 | Info | Security | `FLAG_SECURE` is not set — deliberate: users need to screenshot/share invoices. Documented, not changed. | Accepted |

## Issues Fixed

| ID | Fix summary | Files changed | Verification method |
|---|---|---|---|
| C-01 | Deleted `AppModule.kt`; all three types self-provide through `@Inject` constructors (each already `@Singleton`), DAOs come from `DatabaseModule`. | `di/AppModule.kt` (deleted) | Inspection: constructor annotations, `DatabaseModule.kt` provides every DAO, only injection sites are `InvoiceRepository`/`InvoiceViewModel` (both request `QuotaGate`). |
| C-02 | `allowBackup=false` with a rationale comment; `backup_rules.xml` and `data_extraction_rules.xml` exclude `root`, `file`, `database`, `sharedpref`, `external` with `path="."` in every relevant section, plus explicit `inbusiness_secure_prefs.xml` / `inbusiness_ultra.db*` exclusions. | `AndroidManifest.xml`, `res/xml/backup_rules.xml`, `res/xml/data_extraction_rules.xml` | New JVM test `BackupPolicyTest` parses the manifest and both XML files and fails if the policy is weakened. |
| C-03 | Key is dropped on every payload mutator (customer data, items add/remove/edit, amount paid, payment method, supply type), cleared on `Success`/`IdempotentReplay`, plus an in-flight flag and `try/finally`. | `InvoiceViewModel.kt`, `InvoiceScreen.kt` (dropdown routes through the VM) | Inspection + repository-side rule pinned by `RequestFingerprintTest`. |
| C-04 | `paymentInFlight` guard with `finally` release; dialog button already disables while `Saving`. | `InvoicePreviewViewModel.kt` | Inspection; existing `PaymentRepositoryTest` covers overpayment rejection. |
| C-05 | Cancellation validates all linked products first, aborts the whole transaction when one is missing, then marks cancelled and writes one `SALE_REVERSAL` per line with a correct before/after. | `InvoiceRepository.kt` | Inspection; `FinancialAndInventoryTest` exercises cancellation. |
| H-01 | `SystemClock` resolves every day/month boundary in `AppDateUtils.businessZoneId` (`Asia/Kolkata`) and documents why. | `util/SystemClock.kt` | New `SystemClockTest` flips the JVM default zone (Honolulu/LA/UTC/Kiritimati) and asserts the business day is unchanged. |
| H-02 | `DailyCap.resetTime` defaults to the next business-zone midnight (nano-zeroed) and `resetIn` is clamped to non-negative. | `domain/quota/QuotaVerdict.kt` | `SystemClockTest.resetTimeIsExpressedInTheBusinessZone`. |
| H-03 | `addPayment` first repairs an empty ledger by recording the pre-existing `amountPaid` as an explicit `OPENING_BALANCE` payment (mode/date taken from the invoice), then reconciles normally. | `PaymentRepository.kt` | New instrumented test `PaymentRepositoryTest.repairsLegacyInvoiceSummaryWithoutLedgerRows`. |
| H-04 | Monotonic `requestGeneration`; stale successes *and* failures return before touching state. | `InvoiceHistoryViewModel.kt` | Inspection + `InvoiceHistoryFilterTest` covers the filter/bind contract. |
| H-05 | GSTIN trimmed+uppercased and validated in setup and in the invoice screen (inline error, submit disabled); `saveBusinessData` failures surface a real message and cancellation is rethrown. | `SetupViewModel.kt`, `SetupScreen.kt`, `InvoiceViewModel.kt`, `InvoiceScreen.kt` | Inspection + `GstCalculatorTest` cases for case-insensitivity, length, malformed and SQL-ish input. |
| H-06 | Live business id is filtered out of scenarios and cannot be deleted; the screen cannot offer it. | `CalculatorViewModel.kt` | Inspection (deletion path guards on `businessContext.activeBusinessId`). |
| H-07 | Mockito 5.14.2 (inline mock maker) for JVM + instrumented tests, with a comment; three unnecessary `mock(DeviceClassifier)` calls replaced by the real classifier on-device. | `app/build.gradle.kts`, `InvoiceConcurrencyTest.kt` | Inspection of every `mock(` site (only `QuotaGate` + abstract `Context` remain mockable-but-required). |
| H-08 | `MAX_PAGES=50`, `MAX_DESCRIPTION_CHARS=2000`, truncation notice on the document, cache pruning (newest 20 kept, >7 days evicted), sanitized file names. | `utils/pdf/PdfConstants.kt`, `PdfGenerator.kt`, `utils/pdf/PdfCacheManager.kt` (new) | New `PdfCacheManagerTest` (6 tests) over the pure pruning policy. |
| H-09 | `PRAGMA defer_foreign_keys=ON` (the documented replacement inside a transaction) in `MIGRATION_3_4`, `7_8`, `18_19`; `MIGRATION_8_9` cursor closed in `finally`. | `data/database/AppDatabase.kt` | Inspection; existing `DatabaseMigrationTest`/`MigrationTest` cover the paths. |
| M-01 | FK/constraint failures are mapped to an actionable `IllegalStateException`; the ViewModel logs and publishes the error. | `ProductRepository.kt`, `ProductViewModel.kt` | Inspection. |
| M-02 | `MutableSharedFlow(extraBufferCapacity = 1)` + `tryEmit` at all three sites. | `CalculatorViewModel.kt` | Inspection (no collector stall possible). |
| M-03 | Deleted `EncryptionManager.kt`, unused `Converters` list functions, and 3 unused DAO methods; `Converters` now Instant↔Long only. | `domain/security/EncryptionManager.kt` (deleted), `data/converters/Converters.kt`, `data/dao/InvoiceDao.kt` | Repo-wide grep: zero references remain. |
| M-04 | `navigation-compose` 2.8.9 (stable). | `app/build.gradle.kts` | Version string inspection. |
| M-05 | Root `.gitignore` for build outputs, IDE files, `local.properties`, keystores/signing props, `.env*`, DB/SQLite files, PDFs. | `.gitignore` (new) | Inspection. |
| M-06 | Upgrade dialog only lists implemented capabilities and states that in-app purchases are not enabled. | `QuotaBlockedDialog.kt` | Inspection. |
| M-07 | `toSQLiteQuery()` returns the concrete `SimpleSQLiteQuery`; unused import removed. | `data/dao/InvoiceHistoryFilter.kt` | `InvoiceHistoryFilterTest` asserts SQL text, bind-argument order/count, and that an injection payload is bound rather than interpolated. |
| M-08 | Log strings interpolate correctly (5 sites). | `BusinessRepository.kt` | Grep: no `\${` remains in `app/src/**`. |
| M-09 | Fresh-row fallback instead of `!!`; `runCatching { classifier }` with `LOW_END` fallback; `remainingAfter` clamps at zero. | `QuotaGate.kt` | Inspection + `QuotaGateTest` (5 tests) against real SQL semantics. |
| M-10 | New non-blocking `dependency-review` job for pull requests. | `.github/workflows/android.yml` | Inspection (job gated to `pull_request`). |
| L-01 | Removed the incorrect GSTIN assertion. | `GstCalculatorTest.kt` | Inspection. |
| L-02 | Added cases pinning 0.004 → rejected, 0.005 → 0.01 (HALF_UP), NaN → rejected. | `CalculateInvoiceTotalsUseCaseTest.kt` (existing) + `AmountInWordsConverterTest.kt` | Test intent inspection. |
| L-03 | User-facing message explains the reset and the upgrade path. | `InvoiceViewModel.kt` | Inspection. |
| L-04 | `usesCleartextTraffic="false"`; FileProvider exposes only `cacheDir/invoices`. | `AndroidManifest.xml`, `res/xml/file_paths.xml` | Inspection + `BackupPolicyTest`. |
| L-06 | Removed unused constructor dependencies; the use cases are thin passthroughs. | `CreateInvoiceUseCase.kt`, `GetInvoiceForPreviewUseCase.kt` | Grep: no remaining references to the removed parameters. |
| L-07 | Renamed to `overUsedQuotaRowIsReportedAsDailyCapInsteadOfNegativeRemaining`. | `QuotaGateTest.kt` | Inspection. |
| I-06 | Documented as a deliberate product decision. | `README.md`, `SECURITY.md` | — |

## Issues Not Fixed

| ID | Reason not fixed | Recommended next step |
|---|---|---|
| **Verification blocker** | No JDK, Gradle distribution/cache, Android SDK and no network in this sandbox: `./gradlew assembleDebug`, `testDebugUnitTest`, `lintDebug`, `connectedDebugAndroidTest` and any dependency-audit command cannot run. All changes are inspection-verified. | Run the CI workflow (build + unit tests + lint + emulator suite) on this branch and treat any failure as blocking; the report's fix list is designed to be verified by that run. |
| I-01 | Adding `Index("businessId","createdAt")` requires a schema version bump, a new exported `app/schemas/.../20.json` and a workflow update; the schema JSON cannot be generated without Room's compiler here, and a mismatched hand-written schema would fail at open time. | Add `MIGRATION_19_20` with `CREATE INDEX IF NOT EXISTS index_invoices_businessId_createdAt`, bump to v20, let kapt regenerate the schema in CI, and update the pinned schema check. |
| I-02 | The Compose BOM governs transitive library versions; bumping it without a compile pass is a real regression risk and the BOM is already a stable release. | Bump to a BOM contemporary with Kotlin 2.2.10 in a dedicated dependency PR and run the full suite + a UI smoke test. |
| I-03 | Adopting detekt/ktlint across 105 files would produce a very large unrelated diff and requires per-rule configuration choices. Hardcoded UI strings would need a full `strings.xml` migration and re-translation decisions. | Add detekt/ktlint with a baseline committed first (no new violations), then enable the formatter; migrate strings screen-by-screen. |
| I-04 | Pagination and splash behaviour are timing/UI dependent; changing them without an emulator risks new scroll or navigation regressions. The pagination path is already guarded by `isLoading`/`isEndOfList`. | Replace the `layoutInfo` trigger with a threshold on the last visible index and add a splash timeout with an explicit retry state, verified on an emulator. |
| I-05 | Schemas 14–17 cannot be regenerated here (they were never exported) and inventing them would be worse than the gap. | Regenerate the missing schemas from the migration history in a build environment and keep the v19 pin as the gate. |
| L-05 | Cosmetic: a stored `0.0` GST renders as an empty field, which parses back to `0.0`; no data is lost and the change is purely presentational. | Show `"0"` for an explicit zero once the product form is next touched. |

## Tests Added or Improved

New JVM suites (no device required):

| File | Covers |
|---|---|
| `app/src/test/.../data/dao/InvoiceHistoryFilterTest.kt` (8 tests) | Raw history SQL is fully parameterized; bind order/values; blank-search handling; placeholder count equals bind count for every filter combination; injection payload is bound, never interpolated. |
| `app/src/test/.../utils/RequestFingerprintTest.kt` (5 tests) | Cosmetic differences (case/whitespace/numeric formatting) do not change the fingerprint; every business field does; length-prefixed field boundaries; SHA-256 hex shape. |
| `app/src/test/.../security/BackupPolicyTest.kt` (4 tests) | `allowBackup=false`; every backup domain excluded in both rule files; cloud-backup/device-transfer singular blocks; DB and secure-prefs exclusions named. |
| `app/src/test/.../utils/AppDateUtilsTest.kt` (4 tests) | Business zone is IST; day boundaries are business-zone midnight; half-open `[start,end)` window is exactly one business day; 7-day list ordered/unique. |
| `app/src/test/.../util/SystemClockTest.kt` (5 tests) | Day/month/now resolve in the business zone regardless of the device zone; quota reset time is business-zone midnight; zone is stable. |
| `app/src/test/.../utils/pdf/PdfCacheManagerTest.kt` (6 tests) | Newest 20 protected; only >7-day files pruned; fresh files never pruned; non-PDF files/directories untouched; small caches untouched; empty input. |
| `app/src/test/.../domain/invoice/GstCalculatorTest.kt` (+6 tests) | GSTIN case-insensitivity and malformed input; supply type requires two valid GSTINs; `UNKNOWN` rejected at calculation; zero quantity; `CGST+SGST == tax` for odd rates; currency rounding (9.999 → 10.00/1.80/11.80). |
| `app/src/test/.../domain/quota/QuotaGateTest.kt` (+4 tests) | Monthly cap blocks without consuming daily quota; expired free tier blocked without consumption; daily rollover restarts at 1 while monthly accumulates; over-used rows report the cap (never a negative remainder). |
| `app/src/test/.../utils/AmountInWordsConverterTest.kt` (+3 tests) | HALF_UP paise rounding (1.005 → one paise); lakh/crore phrasing; negative/NaN/Infinity rejected. |

Improved instrumented coverage:

- `PaymentRepositoryTest.repairsLegacyInvoiceSummaryWithoutLedgerRows` — new test for the
  pre-v15 ledger repair (opening balance recorded, summary and ledger reconcile, no money lost).
- `InvoiceConcurrencyTest` — removed three `mock(DeviceClassifier)` usages that relied on
  final-class mocking; the real classifier is used on-device, reducing Mockito surface to the
  one place that genuinely needs a fake (`QuotaGate`).
- Existing suites retained and cross-checked against their production counterparts:
  `DatabaseMigrationTest`, `MigrationTest`, `ProductMigrationTest`, `FinancialAndInventoryTest`,
  `InvoiceDaoIsolationTest`, `ProductDaoIsolationTest`, `InvoiceIdempotencyTest`.

## Security Improvements

1. **Backup / restore hardened.** `allowBackup=false` plus explicit exclusions of every backup
   domain in both Android 12+ and legacy rule files, so neither cloud backup nor device transfer
   can produce a half-restored state (encrypted DB without its Keystore passphrase). Enforced by
   `BackupPolicyTest`.
2. **Key handling documented and protected.** `KeyProvider.getDatabasePassphrase()` now documents
   that the Base64 text is the SQLCipher passphrase and must not be "improved" into a different
   derivation without a rekey migration — changing it would render every deployed database
   permanently unreadable. The passphrase itself is never logged.
3. **Injection surface narrowed.** The only raw SQL builder is fully parameterized; a regression
   test asserts that a `'; DROP TABLE invoices; --` payload is bound, not interpolated, and that
   placeholders always match bind arguments.
4. **Input validation.** GSTINs are normalized and validated before they can influence supply-type
   detection or reach a tax document; numeric inputs are checked for finiteness before `BigDecimal`
   conversion; item descriptions are length-capped before PDF rendering.
5. **Transport hardening.** `usesCleartextTraffic="false"` (defense in depth; the app requests no
   `INTERNET` permission at all) and the FileProvider exposes only `cacheDir/invoices`.
6. **Least exposure.** Deleted an unused AES-GCM `EncryptionManager` that could be mistaken for an
   active protection layer, and documented the real data-safety model in `SECURITY.md`.
7. **Supply chain.** CI now runs a dependency-vulnerability review on pull requests.
8. **No secrets found.** No hardcoded credentials, tokens or keys exist in the repository; secrets
   and data artifacts are now ignored by `.gitignore`.

## Remaining Risks

1. **Nothing was compiled or executed** (no JDK/SDK/network). Any typo-level mistake in this
   changeset will surface only in CI. Highest-risk classes of change for that: the Dagger provider
   deletion (C-01), the new tests' API usage, and XML resource edits.
2. **Migration 18→19 remains unproven for data preservation** (INTEGER→TEXT `businessId` across
   three recreated tables). It is covered by an instrumented test that asserts the schema, but not
   by a test that asserts row contents survive a real upgrade path.
3. **Concurrency guarantees rely on instrumentation tests** (`InvoiceConcurrencyTest`,
   `InvoiceIdempotencyTest`); those suites must actually run on an emulator before release.
4. **Missing indexes** will cause gradually degrading history/dashboard queries as data grows.
5. **No static-analysis gate** (detekt/ktlint) means style/robustness regressions are invisible
   until review.
6. **PDF cache age policy is deliberately conservative**: below the 20-file cap nothing is ever
   evicted, so a long-lived install keeps up to 20 documents indefinitely. Chosen to avoid deleting
   a document the user may still be sharing; documented in `PdfCacheManagerTest`.
7. **Quota pricing/cap values are hardcoded** (`FREE` daily 2 / monthly 60, launch promo end
   2026-11-20) and there is no purchase backend; the dialog now says so rather than implying a
   working upgrade path.

## Recommended Follow-Up Work

**P0 (before the next release)**
- Run the full CI pipeline on this branch: `assembleDebug` (proves the Dagger/DI fix), `lintDebug`,
  `testDebugUnitTest` (the 12 JVM suites) and `connectedDebugAndroidTest` on API 36.
- Extend `DatabaseMigrationTest` to prove **row preservation** across 18→19 for `stock_movements`,
  `payments` and `customers` (counts, `businessId` conversion, FK integrity) and across the
  `defer_foreign_keys` migrations.
- Smoke-test on a device: create invoice → share PDF → record payment → cancel invoice → delete a
  product that is referenced by a cancelled invoice.

**P1 (next iteration)**
- Schema v20: add `Index("businessId","createdAt")` (plus `status` if the history filters need it)
  and regenerate the Room schema in CI; regenerate the missing 14–17 schema snapshots.
- Add a schema/data guard for the invoice-number sequence (assert no duplicates across concurrent
  creation) and keep the existing idempotency tests running in CI.
- Replace the history pagination trigger with an explicit last-visible-index threshold; add a
  splash timeout with a retry affordance.
- Introduce detekt/ktlint with a committed baseline.

**P2 (backlog)**
- Bump the Compose BOM to a Kotlin-2.2-compatible release and add a UI smoke test.
- Move user-facing strings to `strings.xml` and adopt a lint rule for hardcoded text.
- Replace the dashboard `combine` vararg casts with a typed holder to remove the
  `Array<Any?>` positional coupling.
- Consolidate `util/` and `utils/` packages, and drop `SystemClock`/`DeviceClassifier` from the
  graph if they remain unused outside `QuotaGate`.
- Implement the user-initiated encrypted export/import that the backup policy documents as the
  business-continuity path.

## Change Summary

- **50 files touched:** 40 modified, 2 deleted (`di/AppModule.kt`, `domain/security/EncryptionManager.kt`),
  8 added (`SECURITY.md`, `PdfCacheManager.kt`, 6 new test files).
- **Diff size:** +993 / −356 lines in tracked files, plus ~740 lines of new tests.
- **Production code:** quota/timezone correctness (`QuotaGate`, `QuotaVerdict`, `SystemClock`),
  invoice/payment integrity (`InvoiceRepository`, `PaymentRepository`, `InvoiceViewModel`,
  `InvoicePreviewViewModel`), product ledgers (`ProductRepository`, `ProductViewModel`),
  setup/GSTIN validation (`SetupViewModel`, `SetupScreen`, `InvoiceScreen`), PDF bounds
  (`PdfGenerator`, `PdfConstants`, `PdfCacheManager`), migrations (`AppDatabase`), DI cleanup
  (`AppModule` removed), SQL query typing (`InvoiceHistoryFilter`), history race
  (`InvoiceHistoryViewModel`), logging interpolation (`BusinessRepository`), and removal of dead
  code (`Converters`, `InvoiceDao`, use cases).
- **Security/config:** manifest (`allowBackup=false`, `usesCleartextTraffic=false`), both backup
  rule files, new `.gitignore`, `SECURITY.md`, CI dependency review.
- **Docs:** `README.md` rewritten to match actual behaviour (offline-first, encryption, quota,
  PDF immutability, build/verify commands, migration policy, known gaps); `SECURITY.md` added.
- **Deliberately unchanged:** database passphrase derivation (changing it would orphan every
  deployed database), all existing migrations' data transformations, Room schema pin at v19,
  `FLAG_SECURE` (screenshots are a user need), and the PDF cache's conservative pruning policy.
- **Blocking caveat:** none of the above could be compiled or executed in this environment
  (no JDK, Gradle, Android SDK or network). Treat the CI run as the acceptance gate for this
  changeset.
