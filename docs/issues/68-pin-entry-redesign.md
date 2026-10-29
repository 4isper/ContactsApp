# feat: make the PIN entry a genre-standard numeric pad with explicit feedback

**Priority:** P3 — the lock screen is secure and accessible, but as a PIN input it is half a generation behind what banking apps and system screens consider a given; the cheapest items are pure feedback, the biggest one is the input paradigm
**Area:** security / app lock / ux

## Problem

The PIN entry is built as a text field, and several small frictions add up to a screen that behaves unlike any PIN screen the user knows:

- **Silent short PIN.** The unlock button is disabled below 4 digits (`AppLockScreen.kt` `LockPinEntry`, `pin.length >= MIN_PIN_LENGTH`), and IME Done is a silent no-op for the same reason. A user who forgot their PIN length types 4 digits and — nothing happens, with no explanation. The "4–8 digits" fact exists only in the settings state line (`settings_app_lock_state_on`), never on the lock screen.
- **IME instead of a pad.** The input rides the system keyboard: it takes half the screen, its layout is a request (`KeyboardType.NumberPassword`) rather than a guarantee, touch targets are keyboard-sized, and typing is slower than tapping a pad. Every PIN screen in the genre — banks, Google Wallet, system unlock — uses a custom numeric pad with dot indicators instead.
- **No auto-submit.** Unlocking always requires an explicit Done press or button tap. With a known PIN length the submission can happen on the last digit, removing one action from the most frequent unlock path.
- **No haptics.** Issue 32 added tactile feedback to row actions, undo, and the editor; the lock screen — the one place where error and success feedback matter most — was left out.
- **No "forgot PIN" story.** A forgotten PIN means clearing app data and losing all settings (contacts survive in the provider, but nothing warns about this anywhere). The biometric prompt now softens this by being the default path, but the trade-off is never communicated.

## Proposed scope

- **Explicit length feedback:** a supporting line under the entry stating the accepted length ("4–8 digits"), shown while the entry is below the minimum (and on a short-Done attempt). Silence on submit disappears.
- **Numeric pad instead of the IME:** a custom pad (1–9, 0, backspace; the biometric retry key can take the empty slot when biometrics are active), with entered digits shown as masked dots — dots grow as digits are typed, no fixed target row (the app knows only the hash). The IME disappears from the lock screen entirely: no `imePadding`, no `KeyboardOptions`, no focus juggling; `LockPinField` and its text-field machinery are replaced.
- **Auto-submit on the stored length:** persist the PIN length alongside the hash (length is not a secret worth protecting against an attacker who already holds the PBKDF2 hash with 210 000 iterations and a rate limiter — document this consciously). Dots then show a fixed target, and the check fires on the last digit. Backspace deletes; after a wrong attempt the dots clear.
- **Haptics:** key presses on the pad (tick), wrong PIN (error), successful unlock (confirm) — via the standard Compose `hapticFeedback` API, as issue 32 did elsewhere.
- **"Forgot PIN" note in settings:** a one-line note under the app-lock section explaining that a forgotten PIN can only be removed by resetting the app's data (contacts are untouched). No recovery path on the lock screen itself — that would invite abuse.
- New strings for all six shipped locales (the MissingTranslation gate enforces the full set).

## Acceptance criteria

- [ ] Entering fewer than 4 digits and pressing submit shows visible feedback about the required length instead of doing nothing.
- [ ] The lock screen never opens the IME; all input goes through the on-screen pad, including TalkBack (keys are labeled, backspace is labeled, dot state is announced).
- [ ] Unlock submits automatically when the stored PIN length is reached; a wrong attempt clears the dots and, after the attempt budget is exhausted, the existing countdown takes over.
- [ ] Wrong PIN and successful unlock produce distinct haptic feedback; pad presses tick.
- [ ] The settings app-lock section states the forgotten-PIN consequence; the lock screen offers no recovery link.
- [ ] The biometric flow keeps working end to end on top of the pad (prompt auto-show, "Use PIN" reveals the pad, retry key re-shows the prompt).
- [ ] `AppLockScreenUiTest` is reworked for the pad (key taps instead of `performTextInput`) and stays green connected; the biometric-off path keeps the 5/5 coverage.

## Technical notes

- `app/src/main/java/com/m4isper/contactsapp/presentation/settings/AppLockScreen.kt` — `LockPinField` (the whole IME path: `KeyboardOptions`, `imePadding`, `FocusRequester`) is replaced by `LockPinPad` + a dots row; the `unlock` closure's length guard moves into the pad (a tap on the last digit submits).
- `app/src/main/java/com/m4isper/contactsapp/presentation/settings/PinSetupDialog.kt` — `MIN_PIN_LENGTH`/`MAX_PIN_LENGTH` (4/8) stay the source of truth; setup gains nothing (the repeat field already guards typos).
- `domain/src/main/java/com/m4isper/contactsapp/domain/model/settings/AppSettings.kt` — new `appLockPinLength: Int` next to `appLockPinHash`; written by `SetAppLockUseCase` (0 = no lock), read by the pad; persisted through `PrivacySettings.setAppLock` in `SettingsDataSourceImpl` (one more key, cleared by reset).
- Haptics follow the issue 32 pattern (Compose `HapticFeedbackType` / `performHapticFeedback`), no new helper.
- TalkBack: pad keys are labeled buttons; the dots row gets a single state description ("3 of 6 digits") instead of per-dot noise — consistent with the issue 41 conventions.
- The dot indicator and the pad must respect the expressive design tokens from issue 54 (spacing, shapes, no bespoke colors).

## Out of scope

- A PIN recovery or reset path from the lock screen (deliberately — it would turn the throttle into decoration).
- Changing the hashing, attempt throttling, or the fail-closed gate (issue 12/29 territory; the length setting leaks nothing that matters against that threat model).
- Biometric changes — issue 67 owns that flow; the pad only feeds its "Use PIN" branch.
- Shake animation and other decorative motion.
