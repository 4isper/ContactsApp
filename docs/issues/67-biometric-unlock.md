# feat: unlock the app with biometrics

**Priority:** P2 — app lock exists but asks for the PIN on every unlock; biometrics are the platform-standard middle ground between typing a PIN each time and turning the lock off
**Area:** security / app lock

## Problem

The app lock supports only manual PIN entry:

- The gate in `MainActivity` shows `AppLockScreen` whenever `settings.appLockEnabled && !unlocked`; the only way past it is typing the PIN and hashing it against the stored PBKDF2 hash (issue 12).
- There is no fingerprint/face path anywhere in the codebase, and no `androidx.biometric` dependency.
- Issue 12 explicitly deferred biometric authentication to an issue of its own; this is that issue.

Users today face a binary choice: accept the friction of the PIN at every return to the app, or disable the lock entirely. A biometric prompt removes the friction without lowering the bar — the PIN stays as the fallback and remains the root of trust.

## Proposed scope

- Add `androidx.biometric:biometric` to `gradle/libs.versions.toml` and the app module.
- `MainActivity` must extend `FragmentActivity` (a drop-in supertype of `ComponentActivity`; Compose is fully compatible) because `androidx.biometric.BiometricPrompt` is constructed from a `FragmentActivity`.
- Lock screen: when `BiometricManager.canAuthenticate(BIOMETRIC_WEAK)` reports success, show the system prompt when the lock appears (cold start and re-lock after the 1.5 s background grace) and offer a "use PIN" fallback into the existing text field. With no enrolled biometrics or absent hardware, the screen behaves exactly as today.
- `AppLockViewModel`: add a biometric unlock path that sets `unlocked` and resets the attempt counter on success. Biometric failures and cancellations do **not** consume the PIN attempt budget (`AppLockAttempts` semantics stay PIN-only); `BiometricPrompt` has its own system-side lockout.
- Add an "unlock with biometrics" switch under the app-lock section in settings (default: on when the device reports available biometrics), so users can opt for PIN only. Without the switch, hide the prompt and fall back to the current behavior.
- Keep the lock lifecycle untouched: `ON_STOP` + 1.5 s grace (issue 22), no re-lock on configuration changes or external activities, and no contact content behind the gate until the prompt resolves.
- Unit tests: biometric success/failure/cancel in the view model, attempt-counter interaction, and the settings toggle round-trip. Manual device pass with `adb -e emu finger touch` on the emulator and a real device for the system prompt.

## Acceptance criteria

- [ ] With biometrics enrolled and the toggle on, returning to the locked app shows the system BiometricPrompt; a successful fingerprint/face opens the app without typing the PIN.
- [ ] With no enrolled biometrics (or unavailable hardware), the lock screen is byte-for-byte the current PIN-only experience.
- [ ] "Use PIN" fallback opens the existing PIN flow with its throttle and backoff intact.
- [ ] Biometric cancel/failure returns to the lock screen without consuming PIN attempts and without unlocking.
- [ ] No contact content renders before the prompt resolves; the lock gate, the 1.5 s grace, and the configuration-change exclusion behave as before.
- [ ] The settings switch turns the biometric path off and on without disabling app lock itself.
- [ ] Unit tests cover the view model's biometric paths and the toggle; a manual device pass is recorded in the Status section.

## Technical notes

- `app/src/main/java/com/m4isper/contactsapp/MainActivity.kt` — the gate (`if (settings.appLockEnabled && !unlocked)` around `AppLockScreen`), the `ON_STOP` observer with `APP_LOCK_DELAY_MILLIS = 1500`, and the activity base class.
- `app/src/main/java/com/m4isper/contactsapp/presentation/settings/AppLockViewModel.kt` — `unlocked` state, `AppLockAttempts` throttle, legacy-hash migration on PIN unlock.
- `app/src/main/java/com/m4isper/contactsapp/presentation/settings/AppLockScreen.kt` — add the prompt trigger and the fallback entry here; keep the TalkBack/IME work from issue 41 intact.
- `domain/src/main/java/com/m4isper/contactsapp/domain/security/AppLockAttempts.kt` — semantics unchanged; the counter remains PIN-only by design.
- `domain/src/main/java/com/m4isper/contactsapp/domain/model/settings/AppSettings.kt` — the new toggle lives here next to `appLockEnabled` / `appLockPinHash`.
- No secret material is introduced: the prompt authenticates against platform-enrolled biometrics, and the PIN hash remains the only stored credential.

## Out of scope

- A Keystore-bound `BiometricPrompt.CryptoObject`: it adds enrollment-invalidation and recovery complexity and protects no stored secret — the threat model from issue 12 (offline guessing of the settings file) does not change.
- Biometric confirmation for individual actions (deletes, merges) — this is the app-wide lock only.
- Root detection or biometric-spoof hardening.
