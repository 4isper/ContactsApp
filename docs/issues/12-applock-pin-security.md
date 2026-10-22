# fix: harden app-lock PIN storage

**Priority:** P1  
**Area:** security / app lock

## Problem

`AppLock.hashAppLockPin` uses a single SHA-256 hash with a constant salt. This is not a password-hashing scheme:

- there is no key stretching, so an offline attacker can test PIN candidates very quickly;
- a 4-digit PIN has low entropy, which makes fast hashing especially dangerous;
- the constant salt allows precomputed tables and makes identical PINs produce identical hashes across installs;
- the README describes the scheme as a salted hash, but the implementation does not provide meaningful resistance to offline guessing.

The comparison path should also be constant-time, and failed attempts should be rate-limited.

## Proposed scope

- Replace the single SHA-256 with PBKDF2-HMAC-SHA256 (high iteration count) or Argon2.
- Generate a random per-install salt and store it with a versioned hash format.
- Use an Android Keystore-backed secret where appropriate, and compare hashes in constant time.
- Add attempt throttling/backoff or lockout after repeated failures.
- Provide migration for existing stored PIN hashes.
- Document the chosen parameters and the threat model.

## Acceptance criteria

- [x] No production path uses one SHA-256 with a constant salt for PIN storage.
- [x] Each install gets a random salt; the hash format is versioned and migration-safe.
- [x] Verification uses constant-time comparison.
- [x] Repeated failed attempts are throttled or locked out.
- [x] Tests cover hash/verify, migration, wrong PIN, and lockout behavior.

## Status

Done.

- Hash: PBKDF2-HMAC-SHA256, 210 000 iterations, 256-bit key, random 16-byte salt per hash.
  Format: `pbkdf2-sha256$<iterations>$<salt>$<hash>` (Base64 without padding).
- Verification: `MessageDigest.isEqual` (constant time). The legacy SHA-256 hash with a constant
  salt is only used to recognize an old installation; it is never created any more.
- Migration: after a successful unlock the old hash is rewritten in the new format
  (`needsAppLockHashMigration` + `AppLockViewModel.migrateLegacyHash`).
- Throttling: `AppLockAttempts` (domain) allows 5 attempts, then blocks the input for 30 s;
  every further failure doubles the pause up to 15 min. A successful unlock resets the counter.
- The PIN is checked on `Dispatchers.Default`: the KDF is slow and must not block the UI.

Keystore was deliberately not used: the hash is not a secret that needs hardware protection —
what matters is that an offline copy of the settings cannot be brute-forced quickly. PBKDF2 with
a per-hash salt and 210 000 iterations raises the cost of a 4-digit PIN guess to a level that
makes the Keystore secret (which can be extracted on a rooted device anyway) irrelevant.
The threat model is therefore: the device is not rooted, the settings file is not readable, and
the PIN has low entropy.

Tests: `AppLockTest` (format, verification, migration marker), `AppLockAttemptsTest` (throttling),
`AppLockViewModelTest` (unlock, lockout, migration of the legacy hash).

## Technical notes

- `domain/src/main/java/com/m4isper/contactsapp/domain/model/AppLock.kt`
- Settings storage used for the app-lock hash.
- `README.md` — the app-lock description should match the implementation.

## Out of scope

- Biometric authentication.
- Full device-security hardening or root detection.
