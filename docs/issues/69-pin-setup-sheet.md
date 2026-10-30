# feat: set the PIN on the same numeric pad, in a bottom sheet

**Priority:** P3 — the unlock screen now rehearses a specific motor skill (issue 68's pad); setting the PIN on a different input (two IME text fields in a cramped dialog) breaks that skill at the moment it is being formed. Setup happens rarely, so this is polish — but the marginal cost is small now that the pad exists
**Area:** security / app lock / ux

## Status

Done.

What changed:

- `PinSetupDialog` is replaced by `PinSetupSheet` (`AppBottomSheet`, the issue 60 pattern): step 1 "Enter a PIN" with growing dots and a checkmark key, step 2 "Repeat the PIN" with a fixed dot target, the mismatch message (liveRegion) and a LongPress buzz, Done pinned at the bottom.
- The two-step machine lives in a plain `SetupPinEntry` class (FIRST/REPEAT stages, short-entry flash, mismatch at full length, done on a matching pair) — JVM-tested, the sheet only wires haptics (KeyboardTap on keys, LongPress on errors, Confirm on a saved pair).
- `LockPinPad`, `LockPinDots`, `PadKey`, and the PIN length constants moved into a shared `LockPinPad.kt` with a new optional checkmark slot — one pad implementation for both screens.
- `SettingsScreen` wires the sheet for both enable and change-PIN; the `onConfirm(pin)` contract and `SetAppLockUseCase` (hash + length in one write) are unchanged. Strings for all six locales (step headers, Next).
- Follow-up fix from the connected run: the sheet content exceeded the modal height and pad taps failed to inject — the reminder-sheet structure (fixed header, scrollable pad, pinned button) resolved it.

Verification:

- JVM: `SetupPinEntryTest` (7 cases: short flash, stage transition, done/mismatch/partials, delete semantics, cap at 8); full suites, detekt, ktlint, `lintDebug` green.
- Connected: `SettingsUiTest` 11/11 (the two dialog tests now drive the pad: keys, checkmark, confirm, and assert the saved `(enabled, hash, length 4)` triple) and `AppLockScreenUiTest` 7/7.
- Manual on the fold AVD: enabling the lock through the sheet (1234 → Next → 1234 → Done) closed the gate and stored the length; the lock screen then unlocked by auto-submit in the known mode. The sheet's pad sits higher on step 2 than on step 1, so a scripted pass with stale coordinates pressed the wrong keys first — fresh per-step coordinates behaved; real users see the screen. Haptics are not observable on the emulator; the API calls are code-reviewed.

## Problem

The PIN is set in `PinSetupDialog` — an `AlertDialog` with two IME-driven text fields (PIN, repeat). After issue 68 the asymmetry is noticeable:

- **Motor-memory mismatch.** The user types the PIN on a device keyboard, then unlocks it on the 3×4 pad grid. Spatial positions do not transfer; a user who "knows" their PIN fumbles on the unfamiliar layout — at the exact moment the muscle memory is being formed.
- **IME over a dialog is janky.** The keyboard covers and resizes the dialog, the digit layout is device-dependent (`KeyboardType.NumberPassword` is a request, not a guarantee), and focus juggling (Next → repeat field) exists only to serve the IME.
- **The dialog is too small for a pad.** Issue 60 already moved oversized dialogs to bottom sheets; a cramped pad inside `AlertDialog` would look cheap and hit the touch-target budget.

## Proposed scope

- Convert `PinSetupDialog` into a bottom sheet (`AppBottomSheet`, the issue 60 pattern) hosting the same `LockPinPad` and dots row the lock screen uses.
- Two-step flow, mirroring today's Next/Done contract:
  1. **Enter the PIN** — growing dots (the length is the user's choice; no auto-submit is possible), a checkmark key submits the first entry.
  2. **Repeat the PIN** — the same pad, the header switches to the repeat prompt; a mismatch flashes the dots and the existing mismatch message (liveRegion, TalkBack); a match enables Done.
- Haptics reuse the issue 68 pattern: KeyboardTap on keys, LongPress on a mismatch, Confirm on a successful pair.
- The step state machine stays testable outside Compose (a small class next to `LockPinEntry`, not inline in the composable).
- Keep the public behavior: the sheet is used for both enable and change-PIN (`showPinDialog` call sites in `SettingsScreen`), dismissal cancels without saving, `app_lock_pin_dialog` tag moves to the sheet, the existing instrumented tests are reworked to pad taps.
- New strings for all six shipped locales (step headers, checkmark key); the MissingTranslation gate enforces parity.

## Acceptance criteria

- [ ] Enabling the lock and changing the PIN both open the sheet with the pad; no IME appears anywhere in the PIN setup flow.
- [ ] Step 1 accepts any length 4–8 via the checkmark key; dots show the growing entry, and the chosen length is what step 2 requires.
- [ ] A mismatch in step 2 flashes the feedback (message + haptic) and lets the user correct or restart; a match enables Done and saves through `SetAppLockUseCase` (hash + length, one write).
- [ ] Dismissal at any step closes the sheet without changing the stored lock state.
- [ ] TalkBack announces the current step, the dots state, and the mismatch; pad keys and the checkmark key are labeled.
- [ ] The instrumented tests covering the dialog (`appLockAsksForPinBeforeEnabling`, `appLockStateShowsEnabledAndOffersChangePin`) are reworked to pad taps and stay green connected.

## Technical notes

- `app/src/main/java/com/m4isper/contactsapp/presentation/settings/PinSetupDialog.kt` → replaced by `PinSetupSheet.kt`; `MIN_PIN_LENGTH`/`MAX_PIN_LENGTH` stay the shared constants.
- `app/src/main/java/com/m4isper/contactsapp/presentation/settings/LockPinPad.kt` — extract `LockPinPad`/`PadKey`/the dots row from `AppLockScreen.kt` into a shared file so both screens consume one implementation (the Dryer rule: two copies of a pad rank well above the duplication threshold).
- The step machine is a sibling of `LockPinEntry` (e.g. `SetupPinEntry`): stage 1 grows the entry, the checkmark validates length ≥ 4 and moves to stage 2, stage 2 compares against stage 1 and reports mismatch — plain class, JVM-tested like `LockPinEntryTest`.
- Call sites: `SettingsScreen.kt` (`showPinDialog` state and the two `PinSetupDialog` usages) — replace with the sheet; the `onConfirm(pin)` contract is unchanged, so `SetAppLockUseCase` and the length persistence from issue 68 stay untouched.
- The sheet hosts scrollable content like `ReminderTimeSheet` does (the pad plus header must survive small screens and font scale 2.0 — same check as issues 60/67).

## Out of scope

- Auto-submit during setup (the length is chosen by the user; the checkmark key is the deliberate signal).
- Changes to the unlock pad, hashing, attempt throttling, or the fail-closed gate (issues 68, 12, 29 territory).
- A "show PIN" toggle in setup — the repeat step is the typo guard; dots keep the entry masked like on the lock screen.
