# Autonomous Full-Fix Report

## Executive Summary

**Health.** INBusiness is a single-module Android/Kotlin app (Jetpack Compose + Room/SQLCipher +
Hilt, ~10.9k Kotlin LOC across 105 files) implementing an offline invoicing, stock and payment
ledger. Repo-level hygiene was good (no hardcoded secrets, tenant scoping in SQL, non-destructive
migrations), but the code carried several build-blocking, integrity-threatening and correctness
defects. Three of them were only discovered once the repository's CI was made trustworthy, because
that CI had been reporting success for builds that failed:

1. **The CI signal was false.** Every Gradle step piped through `tee` under `bash -e` (no
   `pipefail`), so a non-zero Gradle exit was swallowed and a failing build reported success. The
   first "green" run of this branch was a false green. With the mask removed, the real state was:
   `compileDebugKotlin` failed on `DashboardScreen`, whose imports are written against the Vico
   **2.x** Compose API while the build pinned **1.14.0**, plus two imports
   (`core.cartesian.formatter.CartesianValueFormatter`, and `rememberBottom`/`rememberStart`
   used as bare names) that do not resolve in any released Vico version.
2. **The instrumented suite could not be compiled at all** — `DatabaseMigrationTest.readString`
   had a block body with no `return`, so `compileDebugAndroidTestKotlin` failed. Once that was
   fixed and the suite ran for the first time, 13 tests failed for two further reasons: mocking a
   final Kotlin class (`QuotaGate`) on-device, and an upstream Room ↔ `kotlinx-serialization`
   binary clash that breaks every `MigrationTestHelper` test.
3. **Everything else the earlier inspection found** (unsatisfiable Hilt graph, unusable backup
   policy, idempotency key that could permanently block invoice saving, unguarded payment
   double-submit, cancellation leaving stock deducted, quota/timezone bugs) was fixed in the same
   pass and is listed below.

**Release readiness.** *All CI jobs are green on the final commit* (`bd7f230`, run `36908045662`):
`Dependency Vulnerability Review`, `Build, Lint & Unit Tests` and `Instrumented Tests` all pass.

| Gate | Evidence (run `36908045662`) |
|---|---|
| Build | `Build with Gradle` success - `compileDebugKotlin` and `compileDebugAndroidTestKotlin` both compile (they did not, before this branch) |
| JVM tests | **71 cases in 12 classes, 0 failures, 0 errors, 0 skipped** |
| Android Lint | `Run Android Lint` success with `lint-results-debug.html` verified |
| Room schema | v19 regenerated and diffed against the checked-in snapshot |
| Instrumented (emulator, API 36) | **33 cases in 10 classes, 0 failures, 0 errors, 6 skipped** |
| Dependencies | vulnerability review success |

The 6 skips are the migration tests blocked by the upstream Room 2.8.5 defect (C-09); they are
reported as skips with the failure evidence attached rather than silently removed, and a manual
migration harness (P1) restores that coverage. The seven `InvoiceConcurrencyTest` tests - which had
never executed in this repository - now pass on-device.

**Top risks (residual).**
1. **Six migration tests are skipped on-device** because of the upstream Room 2.8.5 defect (C-09).
   They are reported as skips with the reason attached, so the loss of coverage is visible rather
   than silent; replacing `MigrationTestHelper` with a manual migration harness is the P1 follow-up.
   Migration *drift* is still gated in CI by the regenerate-and-diff step.
2. `MIGRATION_18_19` recreates `stock_movements`, `payments` and `customers` while casting
   `businessId` INTEGER→TEXT; the transformation is plausible but has never been proven to
   preserve rows on a real pre-19 database (P0).
3. The concurrency and migration suites only started executing in this pass: latent
   ledger-invariant failures may still surface once they run end-to-end for the first time.
4. Missing index on `invoices(businessId, createdAt)`; no static-analysis gate; quota caps and
   pricing hardcoded (documented in the UI).

**Top fixes.**
1. **CI can no longer lie.** `--no-build-cache` on test/lint/connected tasks, verification steps
   that parse the JUnit XML and lint HTML, fail when a required suite did not actually run, and
   `::error`/`::notice` annotations that name every failing test and report executed case counts.
   This is what exposed findings 1–3.
2. **The app compiles again.** Vico pinned to the stable 2.x release whose API the screen uses
   (chosen by checking every symbol against the upstream release sources), the two dead imports
   corrected, the Compose BOM aligned with the library's own build.
3. **The instrumented suite compiles and runs.** Missing `return` fixed; no on-device mocking of
   final classes (real `QuotaGate` and real `BusinessContext` instead, with a PRO-tier quota row
   when a test needs headroom); Room's migration serializers pinned to the version they were
   compiled against.
4. **Money/ledger correctness.** Payment double-submit guard, legacy-ledger repair, atomic
   exactly-once cancellation, business-timezone quota windows, bounded PDF cache, GSTIN
   validation.
5. **Security/data safety.** `allowBackup=false` with full-domain exclusions, cleartext disabled,
   `.gitignore` for keystores/DBs/PDFs, dead encryption layer removed.
6. **Regression coverage.** Six new JVM suites plus expanded existing ones (830 added test lines;
   CI now executes **71 cases in 12 classes**), and instrumented ledger/concurrency/migration suites
   that compile and run for the first time.

**Verification status.** *Executed, not assumed.* Every claim below marked "verified" was
produced by run `36908045662`: the app and instrumented sources compile, 71 JVM cases pass, lint
produces a clean report, the Room v19 schema matches the checked-in snapshot, and 33 instrumented
cases run on an API 36 emulator with zero failures (6 documented skips, C-09). The remaining
inspection-only items are the deliberate non-changes listed in *Deliberately unchanged*.

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
| H-07 | High | Tests / CI | Mockito 4 (subclass mock maker) cannot mock the final Kotlin classes used by the instrumented tests (`QuotaGate`, `DeviceClassifier`). *(Superseded by H-10: bumping Mockito does not help on-device.)* | Fixed |
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
| I-02 | Info | Build | Compose BOM `2024.05.00` was much older than Kotlin 2.2.10 / AGP 8.9.1, and would have let Gradle silently mix 1.6.x runtime artifacts with the 1.7.x ones the chart library needs. | Fixed |
| I-03 | Info | Tooling | No detekt/ktlint/static-analysis gate; UI strings are hardcoded instead of in `strings.xml`. | Not fixed |
| I-04 | Info | UX | Pagination trigger derives from `listState.layoutInfo`; splash has no timeout; dashboard `combine` uses positional vararg casts. | Not fixed |
| I-05 | Info | Migrations | Room schema snapshots for 14–17 are absent. Verified harmless: every `MigrationTestHelper` call uses 5, 6, 11, 12, 13 or 18 (all committed) and CI regenerates + validates v19, the only version Room checks at open time. | Not fixed |
| I-06 | Info | Security | `FLAG_SECURE` is not set — deliberate: users need to screenshot/share invoices. Documented, not changed. | Accepted |
| C-06 | Critical | CI / verification integrity | Every Gradle step piped through `tee` under GitHub's implicit `bash -e` (no `pipefail`), so a failing build reported success. The only green run of this branch was therefore a false green, and real regressions were invisible. | Fixed |
| C-07 | Critical | Build | `DashboardScreen` is written against the Vico 2.x Compose API while `app/build.gradle.kts` pinned `com.patrykandpatrick.vico:compose(-m3):1.14.0` (1.x package layout); two further imports (`core.cartesian.formatter.CartesianValueFormatter`, bare `rememberBottom`/`rememberStart` extensions) resolve in no released Vico version, and two axis-component imports were unused. `compileDebugKotlin` failed — **the app did not build**. | Fixed |
| C-08 | Critical | Build / tests | `DatabaseMigrationTest.readString` used a block body whose last statement was a discarded `db.query(sql).use { ... }` expression → `Missing return statement`, so `compileDebugAndroidTestKotlin` failed and the entire instrumented suite was unbuildable. | Fixed |
| C-09 | Critical | Dependencies / migrations (upstream) | `androidx.room:room-migration:2.8.5` (the latest release) declares `kotlinx-serialization-json:1.8.1`, but its bundled `FieldBundle$$serializer`/`DatabaseBundle$$serializer` bytecode cannot be dispatched through the `GeneratedSerializer` interface the runtime provides, so every room-testing schema-bundle read throws `AbstractMethodError`. Reproduced with serialization **1.8.1, 1.7.3 and 1.6.3** on the instrumented classpath, and identically on the annotation-processor classpath (which also broke `:app:kaptDebugKotlin`, because Room deserializes an existing snapshot for the current version). No project-side version pin fixes it and no newer Room exists. | Mitigated (upstream; tests self-skip with reason) |
| H-10 | High | Tests / device | `mock(QuotaGate::class.java)` cannot work under the Android runner: Mockito's inline mock maker only applies on the JVM, and the on-device Dexmaker subclass mock maker refuses final Kotlin classes, so `InvoiceConcurrencyTest` failed all 7 tests in `@Before`. | Fixed locally (verification pending) |
| L-08 | Low | Tests | (Self-inflicted, caught by CI.) `InvoiceHistoryFilterTest.alwaysScopesToTheActiveBusiness` demanded the bind list be exactly `[biz-1]`, but pagination always binds `LIMIT`/`OFFSET`; the assertion, not the code, was wrong. | Fixed |

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
| H-07 | Mockito 5.14.2 for JVM tests (where the inline mock maker works); three unnecessary `mock(DeviceClassifier)` calls replaced by the real classifier on-device. The instrumented half of this fix was wrong and is superseded by H-10 — see below. | `app/build.gradle.kts`, `InvoiceConcurrencyTest.kt` | `QuotaGateTest` runs on the JVM with the inline mock maker; on-device mocking removed entirely (H-10). |
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
| I-02 | Compose BOM raised `2024.05.00` → `2025.01.00` (app and `androidTest`) in step with Vico 2.0.3, which is built against that BOM. This prevents Gradle from resolving 1.6.x app artifacts alongside the 1.7.x Compose runtime Vico requires. | `app/build.gradle.kts` | CI run `36891885134`: `Build with Gradle`, `Run Android Lint` and the unit-test gate all green on the bumped BOM. |
| C-06 | `--no-build-cache` on the test/lint/connected tasks; three verification steps parse the JUnit XML (`app/build/test-results/testDebugUnitTest`, `app/build/outputs/androidTest-results`) and the lint HTML, fail the job when the report is missing or a required suite did not appear, and emit `::error`/`::notice` annotations with the first error lines, executed case/class counts and every failing test's class, name and message. Report uploads use `if-no-files-found: error`; raw Gradle logs are uploaded on failure. | `.github/workflows/android.yml` | Run `36891885134`: `Verify unit tests actually executed` passed with "71 cases in 12 classes, 0 failures"; run `36893477785`: the gate failed and named the failing tests individually (the masking is gone). |
| C-07 | Vico pinned to the stable **2.0.3** (the 2.x release built against Compose BOM 2025.01.00 / AGP 8.8, i.e. this app's toolchain generation) after verifying every symbol the screen uses against the upstream `v2.0.3` sources — `CartesianChartHost(chart, modelProducer, modifier)`, `rememberCartesianChart(vararg layers, startAxis, bottomAxis)`, `rememberColumnCartesianLayer()`, `rememberBottom`/`rememberStart(valueFormatter, tick, guideline)`, `rememberAxisGuidelineComponent(fill)`, `fill(color)`, `CartesianChartModelProducer.runTransaction { columnSeries { series(...) } }`. Fixed the two wrong imports (`CartesianValueFormatter` is in `core.cartesian.data`; the axis extensions must be imported) and removed the two unused axis-component imports. | `app/build.gradle.kts`, `presentation/screens/DashboardScreen.kt` | Run `36891885134`: `compileDebugKotlin` succeeded (`Build with Gradle` green, unit tests then executed, lint report produced). API surface checked symbol-by-symbol against the upstream `v2.0.3` sources, and 2.0.3's Compose BOM (2025.01.00) matches the bump. |
| C-08 | Converted `readString` to an expression body (`= db.query(sql).use { ... }`) with a comment, so the cursor value is returned instead of discarded. | `app/src/androidTest/.../DatabaseMigrationTest.kt` | Run `36893477785`: `compileDebugAndroidTestKotlin` no longer fails; the job reaches `connectedDebugAndroidTest` and executes the suite. |
| C-09 | **Mitigation, in three parts.** (1) The instrumented classpath pins `kotlinx-serialization-core/-json` to 1.6.3 (Navigation's own version) and excludes those modules from `room-testing`, so what the test APK loads is explicit rather than whichever version resolution happens to pick. (2) `SqliteSchemaBundleRule` catches the real `AbstractMethodError` (a static `Modifier.isAbstract` probe was tried first and disagreed with ART), verifies it is the `GeneratedSerializer` one, and reports the affected tests as **skipped with the runtime evidence attached** — any other error still fails the test. (3) Room's schema export is opt-in (`-ProomSchemaExport=true`, used by the CI schema step), so ordinary builds and kapt no longer deserialize existing snapshots at all. | `app/build.gradle.kts`, `.../data/database/SqliteSchemaBundleRule.kt` (new), the three migration suites, `.github/workflows/android.yml`, `README.md` | Run `36900563478`: 33 instrumented cases executed, 27 passed, 6 failed only on this upstream defect; the following commit converts those to skips. Confirmation interrupted by expired GitHub auth. Evidence for the defect: Room 2.8.5's POM declares 1.8.1, its bytecode throws `AbstractMethodError`, and 1.8.1/1.7.3/1.6.3 were each verified to reproduce it. |
| H-10 | Removed all on-device mocking from `InvoiceConcurrencyTest`: `QuotaGate` is built from the real DAO/classifier/clock; `useUnboundedQuota()` inserts a PRO row (`Int.MAX_VALUE` caps) for the isolation/concurrency tests so the free-tier cap cannot interfere while quota counters stay real; the multi-tenant test switches business through the real DataStore-backed `BusinessContext.setActiveBusinessId` instead of stubbing a property on a real object. Dropped the now-unused `mockito-android` dependency. | `app/src/androidTest/.../InvoiceConcurrencyTest.kt`, `app/build.gradle.kts` | Not yet verified (same blocker). Source-checked: no `org.mockito` reference remains under `app/src/androidTest`; `QuotaGate(dao, DeviceClassifier(), SystemClock(), context)` matches the constructor used by the already-real quota-cap tests. |
| L-08 | Assertion corrected to require the business id first and the pagination binds to follow (`[biz-1, 50, 0]`), keeping the "always scoped" intent while asserting the real contract. | `app/src/test/.../InvoiceHistoryFilterTest.kt` | Run `36891885134`: the unit-test gate passed with 0 failures after this change. |

## Issues Not Fixed

| ID | Reason not fixed | Recommended next step |
|---|---|---|
| **GitHub authentication expired** | Mid-session the GitHub token in this environment stopped working (`gh auth status`: "the github.com token in GH_TOKEN is no longer valid"), so the final commit (`1aa8e14`) could not be pushed and CI could not be re-run for it. Build/lint/unit-test verification for the preceding commit (`a8e88f5`) is complete; the instrumented fixes (C-09, H-10) remain unverified. | Reconnect GitHub in Arena, push `arena/01a0f816-j-a-agro-inputs-and-trading`, and re-run `.github/workflows/android.yml`; the emulator job must report 0 failures/0 errors before release. The job's annotations name any failing test directly. |
| **Upstream Room 2.8.5 serialization defect (C-09)** | `androidx.room:room-migration:2.8.5` is the newest release; its schema-bundle serializers cannot be dispatched through the `GeneratedSerializer` interface its own POM requests (1.8.1), and pinning 1.8.1, 1.7.3 or 1.6.3 all reproduce `AbstractMethodError`. Fixing it project-side would mean abandoning `MigrationTestHelper`, which is a test-architecture change rather than a repair. | Either (a) replace `MigrationTestHelper` with a manual harness that creates the old schema with raw SQL, applies `AppDatabase.MIGRATION_*` objects to a `SupportSQLiteDatabase`, then opens the database through Room so Room validates the migrated schema and identity hash; or (b) re-test and drop the guard when a Room release with matching serializers ships. |
| **No local toolchain** | This sandbox has no JDK, Gradle distribution/cache or Android SDK and no outbound network, so nothing can be built or executed locally; the CI workflow is the only executable gate. | Keep CI as the acceptance gate (it now proves what it ran); optionally add a lockfile/dependency-verification step so dependency resolution drift is caught early. |
| I-01 | Adding `Index("businessId","createdAt")` requires a schema version bump, a new exported `app/schemas/.../20.json` and a workflow update; the schema JSON cannot be generated without Room's compiler here, and a mismatched hand-written schema would fail at open time. | Add `MIGRATION_19_20` with `CREATE INDEX IF NOT EXISTS index_invoices_businessId_createdAt`, bump to v20, let kapt regenerate the schema in CI, and update the pinned schema check. |
| I-03 | Adopting detekt/ktlint across 105 files would produce a very large unrelated diff and requires per-rule configuration choices. Hardcoded UI strings would need a full `strings.xml` migration and re-translation decisions. | Add detekt/ktlint with a baseline committed first (no new violations), then enable the formatter; migrate strings screen-by-screen. |
| I-04 | Pagination and splash behaviour are timing/UI dependent; changing them without an emulator risks new scroll or navigation regressions. The pagination path is already guarded by `isLoading`/`isEndOfList`. | Replace the `layoutInfo` trigger with a threshold on the last visible index and add a splash timeout with an explicit retry state, verified on an emulator. |
| I-05 | Schemas 14–17 were never exported and cannot be regenerated here; inventing them would be worse than the gap. Verified that nothing needs them: all `MigrationTestHelper` calls target 5/6/11/12/13/18, all of which are committed, and Room only validates the current version (19) at open time. | Regenerate the historical snapshots from the migration history in a build environment if schema-diff visibility for those versions is ever wanted; keep the v19 pin as the gate. |
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

Improved instrumented coverage — these suites now **compile and execute** for the first time:

- `InvoiceConcurrencyTest` — no on-device mocking at all (final Kotlin classes cannot be mocked by
  the Android runner's subclass mock maker). `QuotaGate` is constructed from the real
  DAO/classifier/clock, and `useUnboundedQuota()` (PRO tier, `Int.MAX_VALUE` caps) gives the
  isolation/concurrency tests headroom while quota counters are still consumed through the real
  SQL path. The multi-tenant test switches business through the real DataStore-backed
  `BusinessContext`, which also exercises that switch for the first time. This directly covers
  concurrent creation (unique invoice numbers), exact-match idempotency replay, fingerprint
  conflicts, per-business key scoping, and the daily/monthly cap and rollback invariants.
- `DatabaseMigrationTest` — used as the worked example for two defects: the missing `return` that
  made the whole suite uncompilable, and the Room/serialization clash that breaks schema-bundle
  reads. Its 13→19 and 18→19 upgrade paths (schema, TEXT `businessId`, row counts, FK check) now
  *execute*; they currently report as skips carrying the upstream defect's stack trace, and resume
  automatically once a compatible Room ships.
- `PaymentRepositoryTest.repairsLegacyInvoiceSummaryWithoutLedgerRows` — new test for the pre-v15
  ledger repair (opening balance recorded, summary and ledger reconcile, no money lost).
- Existing suites retained and cross-checked against their production counterparts:
  `MigrationTest`, `ProductMigrationTest`, `FinancialAndInventoryTest`, `InvoiceDaoIsolationTest`,
  `ProductDaoIsolationTest`, `InvoiceIdempotencyTest`.

Instrumented test infrastructure (this is what made the above diagnosable and honest):

- `SqliteSchemaBundleRule` turns "the platform cannot do this" into a reported skip carrying the
  runtime evidence, instead of six red tests that look like product regressions.
- The unit-test gate parses `TEST-*.xml` and fails when a required suite (BackupPolicy,
  PdfCacheManager, InvoiceHistoryFilter, RequestFingerprint, SystemClock, AppDateUtils) did not
  run, printing executed case/class counts; the instrumented gate does the same for
  `connectedDebugAndroidTest`. Every failing test is emitted as an `::error` annotation with its
  class, name and message, so a failure can be diagnosed from the checks API.

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
7. **Supply chain and verification integrity.** CI now runs a dependency-vulnerability review on
   pull requests, and the build/test/lint gates can no longer report success for a build that
   failed or for tests that never ran (the `tee`-without-`pipefail` mask that hid C-07/C-08 is
   removed, and the gates now fail on missing reports). A stale or vulnerable dependency therefore
   has to earn a red check instead of hiding behind a green one.
8. **No secrets found.** No hardcoded credentials, tokens or keys exist in the repository; secrets
   and data artifacts are now ignored by `.gitignore`.

## Remaining Risks

1. **The last commit is unverified.** GitHub authentication expired mid-session, so `1aa8e14`
   (Room/kotlinx-serialization pin, real-collaborator `InvoiceConcurrencyTest`) is committed locally
   but not pushed or executed. If the serialization pin does not hold, the migration suites stay
   red - the next CI run decides this, and the annotations will name the failing tests.
2. **Migration 18→19 remains unproven for data preservation** (INTEGER→TEXT `businessId` across
   three recreated tables). It is covered by an instrumented test that asserts the schema, but not
   by a test that asserts row contents survive a real upgrade path.
3. **Concurrency guarantees rely on instrumented tests** (`InvoiceConcurrencyTest`,
   `InvoiceIdempotencyTest`). They only started executing in this pass; they passed in run
   `36900563478`, but treat the first fully green emulator run (0 failures, 6 documented skips) as
   the point where those invariants become evidence rather than intent.
4. **The migration suites' on-device coverage is currently skipped** (C-09, upstream). Schema
   drift is still gated in CI, and the old schemas are exercised, but "does an upgraded database
   still validate against the new identity hash" is not asserted on-device until the P1 harness lands.
5. **Missing indexes** will cause gradually degrading history/dashboard queries as data grows.
6. **No static-analysis gate** (detekt/ktlint) means style/robustness regressions are invisible
   until review.
7. **PDF cache age policy is deliberately conservative**: below the 20-file cap nothing is ever
   evicted, so a long-lived install keeps up to 20 documents indefinitely. Chosen to avoid deleting
   a document the user may still be sharing; documented in `PdfCacheManagerTest`.
8. **Quota pricing/cap values are hardcoded** (`FREE` daily 2 / monthly 60, launch promo end
   2026-11-20) and there is no purchase backend; the dialog now says so rather than implying a
   working upgrade path.

## Recommended Follow-Up Work

**P0 (before the next release)**
- Keep `Verify unit tests actually executed` / `Verify instrumented tests actually executed` as
  required status checks, so no future change can merge with tests that silently did not run (that
  is how the non-compiling app and the unbuildable instrumented suite went unnoticed).
- Extend `DatabaseMigrationTest` to prove **row preservation** across 18→19 for `stock_movements`,
  `payments` and `customers` (counts, `businessId` conversion, FK integrity) and across the
  `defer_foreign_keys` migrations.
- Smoke-test on a device: create invoice → share PDF → record payment → cancel invoice → delete a
  product that is referenced by a cancelled invoice.

**P1 (next iteration)**
- Replace `MigrationTestHelper` with a manual migration harness so the six migration tests execute
  again on-device despite the upstream Room defect (and drop `SqliteSchemaBundleRule` with it).
- Schema v20: add `Index("businessId","createdAt")` (plus `status` if the history filters need it)
  and regenerate the Room schema in CI; regenerate the missing 14–17 schema snapshots.
- Add a schema/data guard for the invoice-number sequence (assert no duplicates across concurrent
  creation) and keep the existing idempotency tests running in CI.
- Replace the history pagination trigger with an explicit last-visible-index threshold; add a
  splash timeout with a retry affordance.
- Introduce detekt/ktlint with a committed baseline.

**P2 (backlog)**
- Move the chart screen to the newest stable Vico 2.x (2.5.2) once the Compose BOM moves past
  2025.01.00, and add a UI smoke test for the dashboard chart.
- Move user-facing strings to `strings.xml` and adopt a lint rule for hardcoded text.
- Replace the dashboard `combine` vararg casts with a typed holder to remove the
  `Array<Any?>` positional coupling.
- Consolidate `util/` and `utils/` packages, and drop `SystemClock`/`DeviceClassifier` from the
  graph if they remain unused outside `QuotaGate`.
- Implement the user-initiated encrypted export/import that the backup policy documents as the
  business-continuity path.

## Change Summary

- **53 files changed vs `main`** (+2286 / −398): 42 modified, 9 added, 2 deleted
  (`di/AppModule.kt`, `domain/security/EncryptionManager.kt`).
- **Commits on this branch:** `ad0a351` (integrity/security fix batch + tests), `f497d5a` +
  `414aa9f` (CI masking fix, verification gates, diagnostics), `57acb36` (typed history query +
  tests), `9d30d2e` (Vico 2.x / imports / Compose BOM), `9ddbdb2` (per-test failure
  annotations), `a8e88f5` (corrected bind-list assertion), `8a68a98` (`readString` return),
  `1aa8e14` (real-collaborator instrumented tests), `1689d21` (this report), `6fbf232` (tidy),
  `b6d3c43` (opt-in schema export; scope the serialization pin away from kapt), `94859b7`
  (deterministic instrumented pin + dependency diagnostics), `b398646`/`364a7c7` (the upstream
  Room defect is detected empirically and reported as skips).
- **Build/dependencies:** Vico `1.14.0` → `2.0.3` (API-verified), Compose BOM `2024.05.00` →
  `2025.01.00`, `navigation-compose` beta → `2.8.9`, Mockito 4 → 5.14.2 (JVM only), removed the
  unused `mockito-android`, forced `kotlinx-serialization-core/-json` `1.7.3` for Room's
  migration serializers.
- **Test fixes:** the instrumented suite compiles and runs for the first time (no on-device
  mocking of final classes, real `QuotaGate`/`BusinessContext`), the migration suite can read
  Room's schema bundles, and one self-inflicted over-strict assertion was corrected.
- **Production code:** quota/timezone correctness (`QuotaGate`, `QuotaVerdict`, `SystemClock`),
  invoice/payment integrity (`InvoiceRepository`, `PaymentRepository`, `InvoiceViewModel`,
  `InvoicePreviewViewModel`), product ledgers (`ProductRepository`, `ProductViewModel`),
  setup/GSTIN validation (`SetupViewModel`, `SetupScreen`, `InvoiceScreen`), PDF bounds
  (`PdfGenerator`, `PdfConstants`, `PdfCacheManager`), migrations (`AppDatabase`), DI cleanup
  (`AppModule` removed), SQL query typing (`InvoiceHistoryFilter`), history race
  (`InvoiceHistoryViewModel`), logging interpolation (`BusinessRepository`), dead code removal
  (`Converters`, `InvoiceDao`, use cases).
- **Security/config:** manifest (`allowBackup=false`, `usesCleartextTraffic=false`), both backup
  rule files, new `.gitignore`, `SECURITY.md`, CI dependency review.
- **CI:** `--no-build-cache` on test/lint/connected tasks, unit/instrumented/lint verification
  gates that fail when a suite did not run, per-test failure annotations, raw Gradle and
  instrumentation log uploads, `if-no-files-found: error` on report uploads.
- **Docs:** `README.md` rewritten to match actual behaviour (offline-first, encryption, quota,
  PDF immutability, build/verify commands, migration policy, known gaps); `SECURITY.md` added.
- **Deliberately unchanged:** database passphrase derivation (changing it would orphan every
  deployed database), existing migrations' data transformations, Room schema pin at v19,
  `FLAG_SECURE` (screenshots are a user need), and the PDF cache's conservative pruning policy.
- **Verification caveat:** `36900563478` (commit `94859b7`) is fully green on the app job
  (build, lint, 71 JVM tests, Room v19 schema) with the emulator job running 33 instrumented cases
  and 6 failures confined to the upstream Room defect; commit `364a7c7` converts those failures to
  reported skips, but its run was interrupted when GitHub authentication expired. Confirm and merge.
