# Portable recovery contract

Updated 6 October 2026. Envelope `STDYBK01` is retained for compatibility. Payload
schema 2 expands eligible organiser records; schema 1 remains readable.

## Cryptography

JCA PBKDF2-HMAC-SHA256 (600,000 iterations, 256-bit key) and AES-256-GCM
(128-bit tag). Each archive has a fresh SecureRandom 16-byte salt and 12-byte nonce.
Passphrases are 12–1,024 characters, never persisted; mutable password/key buffers
are cleared. No cryptographic primitive is implemented manually.

The 44-byte big-endian header contains eight ASCII bytes `STDYBK01`, 32-bit
iteration count, salt, nonce and 32-bit ciphertext length. The header is GCM AAD.
Ciphertext includes the tag. Unsupported headers/parameters, incorrect lengths,
wrong passwords, modified data and truncation fail before import.

BackupService retains the original envelope maximum of 50 MiB plaintext. The
stricter PortableCodec entry point bounds all payload schemas at 25 MiB and
100,000 total records. No UI import bypasses that stricter bound. Parsing rejects
unknown/duplicate fields, incorrect primitive types, invalid identifiers,
references, dates, enums, lengths and ranges; it never truncates collections.

## Eligible data

Schema 2 has exactly `schema`, `legacy` and `organiser`. `legacy` preserves the
schema-1 daily plans, reviews and timers. The organiser allowlist includes tasks,
habit definitions/versions/occurrences/logs, subjects/topics, activity sessions
and actual segments, eligible scratchpads, workout sets/templates, water/sleep,
food ideas/meals, user-entered care instructions/logs, reflections/captures,
voluntary interruptions, sourced observations/estimates, day settings and profile.

Private Safety, private contacts, editing drafts, keys, bootstrap authentication
settings, live rest/alarm tokens and reminder receipts are structurally excluded.
Static interval configurations remain with the session. Route geometry is excluded
by default and requires its own explicit preview/scope choice. Private Safety
DAOs are never queried by the portability code.

## Restore

Read bounded input, authenticate/decrypt in memory, validate, preview counts/date
range/scope, require explicit replace confirmation, then replace eligible records
in one organiser Room transaction. Destination Safety is untouched. Imported
active sessions become interrupted, with no active clock anchors or automatic
restart. Schedules are cancelled/reconciled after commit; stale callbacks cannot
create duplicate history.

A legacy schema-1 import replaces only its declared legacy tables, preserves newer
expanded records, and interrupts any expanded active session. The preview discloses
that narrow scope. Missing legacy categories cannot be reconstructed.

Cancellation, provider/read/write failure, wrong password or validation failure
preserves existing records. No plaintext staging file, passphrase or private Safety
text is written to logs. Outside file providers may synchronize user-chosen files.
Markdown has date/category/note selection, exact preview and verbatim UTF-8 note
content; internal drafts, interval configuration and delivery tokens are absent.

## Evidence and limits

See r6/r7 evidence for encrypted round-trip, tamper/refusal, Safety preservation,
legacy interruption and scoped scratchpad checks. Provider integration, interrupted
UI process recovery and all release accessibility/delivery gates still require
verification. A passing codec test alone is not SAF acceptance.
