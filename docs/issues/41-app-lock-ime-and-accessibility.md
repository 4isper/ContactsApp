# fix: make the app-lock screens usable with the keyboard and TalkBack

**Priority:** P1 — the unlock button can be hidden by the IME, blocking access to the app
**Area:** app lock / PIN setup / accessibility

## Problem

`AppLockScreen` centers a fixed `Column` inside a `Surface` without scrolling or IME insets (`AppLockScreen.kt` lines ~56–125). On short screens, in landscape, or when the font scale is large, the keyboard covers the PIN field, the "Wrong PIN" message, and the Unlock button. The user can be locked out of the app by the input method.

Other problems in the same flow:

- `KeyboardOptions` sets `imeAction = ImeAction.Done`, but there is no `KeyboardActions(onDone = ...)`. Pressing Done on the keyboard does nothing; the user must reach the on-screen button.
- After a failed unlock, the entered PIN stays in the field. The user must manually clear it before trying again, and there is no automatic refocus.
- The PIN is always masked with `PasswordVisualTransformation`; there is no show/hide control. Users cannot verify a long PIN before submitting it.
- The "wrong PIN" and lockout countdown texts are plain `Text` nodes. They are not a live region, so TalkBack users may not hear that the attempt failed or how long the lockout lasts.
- The lockout countdown decrements by exactly one second and formats `remaining.seconds.toInt()`. Fractional starting durations are truncated, and the countdown text is recomposed every second without any accessibility announcement. The PIN field stays enabled while locked, which invites input that cannot be submitted.
- `PinSetupDialog` has the same IME issues: the "Next" action is not wired to the repeat field, "Done" is not wired to Save, and the mismatch message is not announced. The user can be left with a disabled Save button and no focus to fix the problem.

## Proposed scope

- Make the lock screen scrollable and IME-aware: `Modifier.verticalScroll(rememberScrollState())` plus `imePadding()` (or apply `WindowInsets.ime` through the content padding), so the field and button are always reachable. Use `BringIntoViewRequester` after an error if needed.
- Wire the IME Done action to the same validation path as the Unlock button. Disable it while checking or locked out.
- On a failed attempt: clear the PIN, move focus back to the field, and announce "Wrong PIN" through a `LiveRegionMode.Polite` semantics node or `View.announceForAccessibility`.
- Add a show/hide PIN button with a localized content description and an accessible toggle state.
- Keep the field enabled only when the user can actually submit; while locked out show the remaining time with a live region and use `Duration.seconds` rounded up. Do not clear the lockout message silently.
- Show a small progress indicator or label while `checking` is true so the disabled state has a reason.
- Apply the same IME and announcement fixes to `PinSetupDialog`: Next focuses the repeat field, Done submits when valid, mismatch is announced, and Save gets focus/feedback.
- Ensure autofill/password managers are not offered a PIN field; set `KeyboardType.NumberPassword` and mark the field as sensitive if needed.

## Acceptance criteria

- [ ] With the keyboard open on a 320×480 dp window and in landscape, the PIN field and Unlock button are both reachable by scrolling.
- [ ] Pressing IME Done triggers unlock exactly once when the PIN length is valid.
- [ ] A wrong PIN clears the field, refocuses it, and is announced by TalkBack.
- [ ] The user can reveal and hide the PIN; the toggle has a correct TalkBack state.
- [ ] The lockout countdown is announced, uses rounded-up seconds, and input is disabled until it ends.
- [ ] PIN setup: Next moves to the repeat field, Done submits when the PINs match, and the mismatch is announced.
- [ ] The unlock flow works at 200% font scale without clipping.

## Technical notes

- `app/src/main/java/com/m4isper/contactsapp/presentation/settings/AppLockScreen.kt` — whole composable; currently no scroll or IME insets.
- `app/src/main/java/com/m4isper/contactsapp/presentation/settings/PinSetupDialog.kt` — two fields, unwired `ImeAction` values.
- `app/src/main/java/com/m4isper/contactsapp/MainActivity.kt` lines ~147–165 — `AppLockScreen` is the root when locked; the activity uses `adjustResize`.
- `app/src/main/res/values/strings.xml` / `values-ru/strings.xml` — add content descriptions for show/hide and any lockout announcement text.
- Compose: `imePadding`, `verticalScroll`, `BringIntoViewRequester`, `KeyboardActions`, `LiveRegionMode.Polite`, `FocusRequester`.
- The countdown loop uses `COUNTDOWN_STEP_MILLIS = 1_000` in the same file.

## Out of scope

- Biometric unlock or a full lockout/attempt policy redesign (issue 12 covers PIN storage security).
- App-lock lifecycle behavior (issue 22).
- Changing the PIN length rules.

## Verification

- Manual with Gboard and a hardware keyboard; portrait, landscape, 200% font scale.
- TalkBack: fail an attempt and wait out a lockout; confirm both are announced.
- ESPRESSO/Compose test for the Done action and field clearing.
- `./gradlew :app:compileDebugKotlin :app:testDebugUnitTest`.
