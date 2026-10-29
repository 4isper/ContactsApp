# feat: unlock the app with biometrics

**Priority:** P2 — app lock exists but asks for the PIN on every unlock; biometrics are the platform-standard middle ground between typing a PIN each time and turning the lock off
**Area:** security / app lock

## Status

Done.

What changed:

- `androidx.biometric:biometric` added; `MainActivity` extends `FragmentActivity` (a drop-in supertype of `ComponentActivity`).
- `AppLockScreen` offers two branches: with the toggle on and biometrics available on a `FragmentActivity` host, the system `BiometricPrompt` is shown on every entry into the lock (cold start, settings landing under the closed gate, and re-lock after the background grace) with a fingerprint retry button; otherwise the screen is the previous PIN-only experience byte for byte. The prompt's negative button ("Use PIN") and any cancellation/timeout/error reveal the PIN field.
- `AppLockViewModel.unlockBiometric()` sets the unlocked state and resets the PIN attempt budget — biometric cancellations never consume the PIN budget; the legacy-hash migration stays PIN-path only.
- Settings gained an "Unlock with biometrics" switch (default on, shown when the device reports available biometrics), persisted via `PrivacySettings.setAppLockBiometrics` and reset with the rest.
- The PIN hash remains the root of trust; no secret material is stored for biometrics and no `CryptoObject` is used (per the issue's out-of-scope note).
- Follow-up fix found during the manual pass: the lock gate renders fail-closed before DataStore settings load (issue 29), which made the auto-shown prompt flash on every cold start of an app whose lock is disabled. `AppUiSettings` now reports `settingsLoaded`; the gate keeps its fail-closed behavior while the biometric offer waits for real settings, and the prompt effect is keyed on the effective availability so it also fires when settings land under an already-closed gate.

Verification:

- JVM: `AppLockViewModelTest` (biometric unlock, lockout reset, budget cleared for the next PIN), `SettingsDataSourceTest` (toggle round-trip, on by default); a Robolectric availability test was dropped as impractical (no `shadowOf` overload for the platform `BiometricManager`) — the real prompt is covered by the device pass. Full suites, detekt, ktlint, and `lintDebug` green.
- Connected: `SettingsUiTest` 11/11 and `AppLockScreenUiTest` 5/5 on the fold AVD — the lock screen is unchanged without biometrics.
- Manual on the fold AVD (Android 17), fingerprint enrolled through the system settings: cold start shows the prompt and `adb emu finger touch 1` unlocks; background for 3 s and return re-shows the prompt; "Use PIN" reveals the PIN field and the PIN is accepted on the first try (budget untouched); with the toggle off, re-locking shows the PIN field immediately with no prompt; with the lock disabled there is no prompt flash on cold start.

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
