# Portable recovery format, version 1

Frozen for this finishing change, 6 October 2026. This extends the existing
BackupService/Room/SAF boundary; no database or project replacement is involved.

## Cryptography and envelope

Use standard JCA implementations: PBKDF2-HMAC-SHA256 (600,000 iterations,
256-bit output) and AES-256-GCM (128-bit authentication tag). Each archive gets a
fresh SecureRandom 16-byte salt and 12-byte nonce. The passphrase is 12–1,024
characters, is never saved, and mutable password/key buffers are cleared after use.
Phone derivation timing will be recorded; background work keeps the UI responsive.

The 44-byte authenticated header is big-endian: eight ASCII bytes `STDYBK01`,
32-bit iteration count, 16-byte salt, 12-byte nonce, 32-bit ciphertext length.
Ciphertext follows, including the GCM tag. All header bytes are GCM associated
data. Only this version and exact iteration count are accepted. JSON plaintext is
bounded at 8 MiB, archive size at 8 MiB + 60 bytes. Length mismatches, unknown
version, wrong passwords, modified data and truncation fail before import.

This is an archive framing format around standard crypto, not a custom cipher.
[Android cryptography guidance](https://developer.android.com/privacy-and-security/cryptography)
supports AES-GCM through JCA. No third-party crypto dependency is added.

## Payload and restore

Payload schema 1 contains exactly `schema`, `plans`, `reviews`, `timers`.
Explicit serializers enumerate eligible fields from those three existing entities;
they never query Safety DAOs, contacts, keys, authentication or private files.
Unknown fields, invalid enums/dates/zones, duplicate IDs/dates, overlong text,
invalid durations/boundaries and more than 10,000 records per collection are rejected.
Numbers and strings are checked by type rather than coerced.

Restore authenticates and validates entirely in bounded memory, previews counts
and date range, and requires explicit replace confirmation. A Room transaction
replaces only these three organiser tables. Destination Safety/contacts remain
untouched. Imported timers become cancelled with no active deadlines. Existing
alarms/notifications are cancelled after commit; stale receiver callbacks must
match persisted running state and generation. No timer auto-starts from an archive.

Picker cancellation and provider failures preserve existing records. Markdown is
previewed exactly before export; the chosen file provider may sync externally.
No plaintext archive or password is written to local staging files or logs.
