# Feature Audit Report — 2026-10-02

Second full-feature audit of INBusiness (`J.A. Agro Inputs & Trading`), following the
autonomous fix pass recorded in [AUTONOMOUS_FIX_REPORT.md](AUTONOMOUS_FIX_REPORT.md).

## How this pass was performed (and its limits)

**Method:** line-by-line static review of every Kotlin source file under `app/src/main`
(82 files / ~8.3k LOC, including all DAOs, entities, migrations, repositories, use cases,
ViewModels and Compose screens), the 12 JVM test classes, the 10 instrumented test classes
(plus their shared `SqliteSchemaBundleRule`), the Room schema snapshots, the Gradle build
files and the CI workflow, followed by targeted fixes.

**Limit:** this sandbox has no JDK, no Android SDK and no network access, so nothing
could be compiled or executed *in the sandbox*. Every finding below is derived from
reading the code, and the fixes were pushed so that CI could compile and execute them —
see *Verification evidence* at the end of this report. Only the CI run proves the changes
build and pass.

## Verification evidence

Run [`36958995339`](https://github.com/aktarjabed/J.A.Agro_Inputs_And_Trading-/actions/runs/36958995339)
on commit `f83fe6a`, all jobs green:

| Job | Result | Evidence |
|---|---|---|
| Build, Lint & Unit Tests | **success** (3m18s) | `assembleDebug` compiles the modified screens/ViewModels; **71 cases in 12 classes, 0 failures, 0 errors, 0 skipped**; `lintDebug` passed (now a real gate via `pipefail`); the Room v19 regenerate-and-diff step passed. |
| Instrumented Tests | **success** (4m54s) | **34 cases in 10 classes, 0 failures, 0 errors, 6 skipped** — one more case than before this branch, i.e. the new `cancellingAnInvoiceReversesStockExactlyOnce` ran and passed. |
| Dependency Vulnerability Review | **success** | unchanged. |

The 6 skips are the pre-existing, documented migration tests blocked by the upstream
Room 2.8.5 ↔ `kotlinx-serialization` incompatibility (see *Carried over*, item 1).

## Findings and status

| # | Severity | Area | Finding | Status |
|---|---|---|---|---|
| F-01 | High | Feature reachability | `InvoiceRepository.cancelInvoice` — the documented cancellation feature with deterministic `SALE_REVERSAL` ledger entries — had **no UI entry point**. It was callable only from tests. | **Fixed**: cancel action in the invoice preview (top bar) with a confirmation dialog, in-flight guard, and result/error surfacing. New instrumented test covers success + "cannot cancel twice". |
| F-02 | High | CI trust | Only the Room-schema step ran Gradle under `pipefail`. `assembleDebug`, `testDebugUnitTest` and `lintDebug` piped into `tee` without it, so a non-zero Gradle exit was reported as success — the exact defect the previous pass claimed to have fixed. A lint error could never fail CI. | **Fixed**: `set -o pipefail` on all three steps; the emulator step now hands the Gradle exit code to the shell explicitly (no pipe). |
| F-03 | High | Calculator input | `CalculatorScreen` bound `Double.toString()` straight to its text fields. Typing `1` of `15` rewrote the field to `1.0`, so the next keystroke produced `1.05`. Money fields also used `KeyboardType.Number`, whose keypad has no decimal separator on most IMEs, making paise unenterable. | **Fixed**: local-text `NumberInputField` + `KeyboardType.Decimal`; external updates (scenario load) are still adopted. |
| F-04 | Medium | Feature reachability | Calculator scenarios (save/load/delete) existed in `CalculatorViewModel` but were unreachable from the UI; `errorMsg` and `savedScenarios` had no collector. | **Fixed**: scenario section (save dialog, load, delete) and a snackbar collector in `CalculatorScreen`. |
| F-04b | Medium | Silent failure | `BusinessRepository` returns `Result` instead of throwing, but `CalculatorViewModel.saveScenario`/`deleteScenario` only caught exceptions: a failed write closed the dialog and changed nothing, with no message. Each save/delete also started an extra permanent collector of the scenario flow. | **Fixed**: `Result` failures are checked and reported; the redundant reloads were removed (the Room flow is live). |
| F-05 | Medium | Feature reachability | History filters: `paymentStatus` reached the SQL builder but nothing in the UI could set it. | **Fixed**: Paid/Unpaid/All chips in the filter panel (server-side filtering, unchanged SQL). |
| F-06 | Medium | Dead UI | The "Upgrade"/"Upgrade Now" buttons routed to an empty `TODO` lambda — a button that visibly does nothing. | **Fixed**: a plan-limits dialog states the free caps and that in-app purchases are not enabled in this build; the fake navigation hook was removed. |
| F-07 | Medium | Error handling | Duplicate products (unique index over name/brand/category/unit/batch) surfaced the raw SQLite text `UNIQUE constraint failed: products...` in a snackbar. | **Fixed**: translated to an actionable message in `ProductRepository.saveProduct`. |
| F-08 | Low | Timezone consistency | The invoice-history list formatted timestamps in the device timezone while the preview, PDF, dashboard and quota use `Asia/Kolkata`, contradicting the documented single-timezone rule. | **Fixed**: history uses `AppDateUtils.businessZoneId`. |
| F-09 | Low | Stale data | The dashboard's live metric cards update themselves, but the 7-day chart was loaded only in the ViewModel constructor, so a new invoice did not appear in the chart on return. | **Fixed**: chart reloads when the screen is re-entered. |
| F-10 | Low | Documentation | README claimed HSN/SAC + UQC snapshots on invoice lines; the `InvoiceItem` entity has no such columns and no screen can set the product-level ones. It also implied a usable `BILL_OF_SUPPLY` flow. | **Fixed**: README corrected, with a new *Known Limitations* section. |

## Carried over from the previous report (still open, unchanged)

These were inspected again and remain deliberate, documented limitations rather than
regressions. They need product decisions, not code:

1. **Migration coverage is unproven on-device.** Six migration tests are reported as
   *skipped* because of the upstream Room 2.8.5 ↔ `kotlinx-serialization` clash
   (`SqliteSchemaBundleRule` documents and detects it). `MIGRATION_18_19`
   (INTEGER → TEXT `businessId` on three tables) has still never been proven to preserve
   rows on a real pre-19 database in this environment.
2. **Money is stored as `REAL`/`Double`.** Rounding is centralised in `BigDecimal` with
   `HALF_UP` and pinned by tests, but integer paise would be structurally safer.
3. **Customer master and stock-movement history have no UI** (see README *Known
   Limitations*), as do history date-range filters.
4. **`KeyProvider` intentionally keeps the legacy Base64 passphrase.** Changing the
   derivation would make deployed databases unreadable without a rekey migration; this is
   documented in the class and in `SECURITY.md`.
5. **No silent-loss data flow:** `firebase`-style cloud backup is disabled; there is still
   no encrypted export/import, so device loss means data loss.
6. **Theme/splash:** `Theme.InBusiness` inherits `Theme.SplashScreen` and does not set
   `postSplashScreenTheme`, so the activity keeps the splash theme after the splash
   animation. Not changed here because it cannot be visually verified without a build —
   flagged for a device check.
7. **Splash → Setup fallback creates a new business id** when the stored id resolves to a
   missing row, orphaning the previous business's rows. Rare (partial restore) but
   worth a recovery path.

## What was verified by reading (not by running)

- Ledger invariants: quota consumption, sequence allocation, stock deduction, invoice
  header/items, and the initial payment are all written inside one
  `RoomDatabase.withTransaction`; any abort rolls back all of it.
- Cancellation validates every linked product *before* mutating state and refuses while
  any successful payment exists.
- `PaymentRepository.addPayment` reconciles the ledger, repairs legacy
  summary-without-ledger invoices via an explicit `OPENING_BALANCE` entry, and rejects
  overpayment.
- The invoice-creation idempotency key is dropped whenever the payload changes, and
  replayed requests are answered from the persisted fingerprint.
- Business scoping: every read/write that touches tenant data binds
  `businessContext.activeBusinessId`; raw history SQL binds all user input as arguments
  and the placeholder/argument counts are pinned by tests.
- Backup policy: `allowBackup=false` plus full-domain exclusions in both
  `backup_rules.xml` and `data_extraction_rules.xml`, asserted by `BackupPolicyTest`.

## How to reproduce the verification

```bash
# 1. Everything must compile and the JVM suites must stay green
./gradlew assembleDebug testDebugUnitTest --no-build-cache

# 2. Lint (now a real gate in CI)
./gradlew lintDebug --no-build-cache

# 3. Instrumented suites — includes the new cancellation-reversal test
./gradlew connectedDebugAndroidTest --no-build-cache
```

CI runs the same three commands (plus the Room schema-drift check) and fails the job on
any non-zero Gradle exit.
