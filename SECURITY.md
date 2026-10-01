# Security & Data-Safety Model

This document describes how INBusiness protects local business data (invoices, payments,
stock ledger, customer records) and the guarantees the code relies on. It is written for
maintainers and for anyone assessing the app before a production rollout.

## Threat model in one paragraph

The app is offline-first and stores the entire book of accounts on the device. The
primary assets are the local database, the key material that decrypts it, generated PDFs
and the device backup surface. The primary threats are (a) another app or a lost/stolen
device reading the data, (b) partial restores that leave the app pointing at data it
cannot open, (c) logical corruption of financial invariants (duplicate invoices, ledger
drift, oversell), and (d) input-driven injection into raw SQL.

## Encryption at rest

| Layer | Mechanism |
|---|---|
| Database file | SQLCipher (`net.zetetic:sqlcipher-android`), AES-256, opened through `SupportOpenHelperFactory` |
| Database passphrase | 32 bytes from `SecureRandom` (256-bit), Base64-encoded, stored in `EncryptedSharedPreferences` (`KeyProvider`) |
| Preference master key | Android Keystore (`MasterKey`, AES256-GCM) |
| Exported PDFs | Written to `cacheDir/invoices`, shared only via a non-exported `FileProvider` (`${applicationId}.fileprovider`) |

**Do not change the passphrase derivation.** SQLCipher has always received the Base64 text
of the random value. Switching to "decoded bytes" or a different KDF without a rekey
migration would make every already deployed database permanently unreadable. There is no
key rotation API today; adding one requires `PRAGMA rekey` plus a migration path and
backup of the previous key.

## Backup & restore policy

Automatic backup and device transfer are **disabled**:

- `android:allowBackup="false"` in `AndroidManifest.xml`.
- `res/xml/data_extraction_rules.xml` (Android 12+) excludes every domain for both
  `cloud-backup` and `device-transfer`.
- `res/xml/backup_rules.xml` (Android 11 and below) does the same for `fullBackupContent`.

Rationale: the encrypted database cannot be restored onto another device, since the
Keystore master key is device-bound. Restoring only the surrounding state (the DataStore
holding the active user/business ids) produces an app that references a database it can
never open. `app/src/test/.../security/BackupPolicyTest.kt` fails the build if this policy
is weakened.

A future explicit export/import feature must be user-initiated, integrity-checked and
versioned; it must not be combined with automatic backup.

## Financial-integrity guarantees (and where they are enforced)

1. **Invoice creation is atomic and idempotent.** Quota consumption, sequence allocation,
   stock deduction, invoice/items insertion and the initial payment all happen inside one
   `withTransaction` block (`InvoiceRepository`). Replaying the same request (same
   idempotency key + identical `RequestFingerprint`) returns the original invoice and
   creates no new rows; reusing a key with a different payload is rejected.
2. **Stock never goes negative.** Deduction uses a conditional `UPDATE ... WHERE
   availableStock >= :quantity`; a zero-row result aborts the whole transaction.
3. **Inventory is double-entry.** Every stock change (opening stock, adjustment, sale,
   sale reversal, manual deduction) writes a `stock_movements` row.
4. **Payments reconcile with the invoice summary.** `Invoice.amountPaid` must equal
   `SUM(Payment.amount)` for `SUCCESS` payments; the invariant is verified before each new
   payment and overpayment is rejected. Invoices that predate the payments table are
   repaired by recording the pre-existing amount as a clearly-labelled `OPENING_BALANCE`
   entry rather than discarding it.
5. **Cancellation is exactly-once and total.** A cancelled invoice cannot be cancelled
   again, cannot be cancelled while payments exist, and the reversal aborts (rolling back)
   if a referenced product is missing instead of silently keeping stock deducted.
6. **Multi-tenant isolation.** Every read and write is scoped by `businessId` in SQL, and
   repositories re-validate the active business from `BusinessContext`.

## Input handling

- All user-supplied values in dynamic queries are bound parameters
  (`InvoiceHistoryFilter` → `SimpleSQLiteQuery`), never string-interpolated.
  `InvoiceHistoryFilterTest` asserts this and that bind counts match placeholders.
- GSTINs are validated (format) and normalised (trim + uppercase) before they can drive
  supply-type detection, so a malformed GSTIN cannot silently disable invoicing.
- Numeric inputs are validated for finiteness before being converted to `BigDecimal`;
  money math uses `BigDecimal` with `HALF_UP` at 2 decimals.
- PDF page/description sizes are capped to avoid memory exhaustion from pathological
  input, and file names derived from stored data are sanitised.

## Logging

Log statements intentionally never include key material, passphrases, or full business
records. Exceptions are logged with their message only. Do not add logging of DB
contents, `BusinessData`, or user identifiers to release builds.

## Reporting a vulnerability

Open a private security advisory on the repository (GitHub → Security → Report a
vulnerability) rather than a public issue. Include reproduction steps, the affected
version/commit, and whether the database or key material can be exposed. Please do not
attach real customer data.
