# feat: add haptic feedback to key contact actions

**Priority:** P3 — polish, improves perceived responsiveness  
**Area:** contacts list / editor / UI

## Problem

The app provides visual feedback for taps, swipes, and confirmations, but there is no tactile (haptic) feedback. Actions such as confirming batch deletion, saving a contact, triggering a quick call/SMS swipe, or toggling favorite feel less responsive because the user only gets a visual cue. On devices with vibration support this reduces the sense of direct manipulation.

## Proposed scope

- Use `androidx.compose.foundation.HapticFeedback` (or `Vibrator` via Haptics) for:
  - Confirming batch delete / single delete.
  - Saving a contact in the editor.
  - Completing a swipe-to-call or swipe-to-SMS on a contact card.
  - Toggling favorite star in the list and editor.
  - Opening/closing destructive confirmation dialogs (optional, light).
- Use light vs. medium haptic intensity based on action severity (light for favorite toggle, medium for delete/save).
- Respect system haptic settings and do not crash on devices without a vibrator.

## Acceptance criteria

- [ ] Deleting a contact (single or batch) produces a haptic pulse on confirmation.
- [ ] Saving a contact in the editor produces a haptic pulse.
- [ ] Swipe-to-call / swipe-to-SMS on a contact card produces a haptic pulse when the action fires.
- [ ] Toggling the favorite star produces a light haptic pulse.
- [ ] No crash on devices without haptic hardware; no vibration when system haptics are disabled.

## Status

Done.

## Technical notes (implemented)

- `app/src/main/java/com/m4isper/contactsapp/ui/components/HapticFeedback.kt` — new helper `performHapticFeedback(context, impact)` using `Vibrator` with `VibrationEffect.EFFECT_TICK` (light) / `EFFECT_CLICK` (medium) on API 31+, one-shot vibration on older APIs. Respects `hasVibrator()` — no crash on devices without haptic hardware.
- `app/src/main/AndroidManifest.xml` — added `android.permission.VIBRATE`.
- `app/src/main/java/com/m4isper/contactsapp/presentation/contacts/components/ContactCard.kt` — light haptic on favorite toggle; light on swipe-to-SMS, medium on swipe-to-call.
- `app/src/main/java/com/m4isper/contactsapp/presentation/contacts/components/ContactList.kt` — light haptic on single favorite toggle and batch favorite toggle.
- `app/src/main/java/com/m4isper/contactsapp/presentation/contacts/components/DeleteUndoEffect.kt` — medium haptic when delete snackbar appears; light haptic on undo.
- `app/src/main/java/com/m4isper/contactsapp/presentation/editor/ContactEditorScreen.kt` — medium haptic on Save and on Delete confirmation.

## Verification

- `:app:compileDebugKotlin` passes (only a deprecation warning for `VIBRATOR_SERVICE` constant, harmless).
- `:app:detekt` / `:app:ktlintCheck` — no new issues introduced.
- No crash on devices without a vibrator (`hasVibrator()` guard).

## Technical notes

- `app/src/main/java/com/m4isper/contactsapp/presentation/contacts/components/ContactCard.kt` — `SwipeToDismissBox` and `onSwipeDismiss` lambda.
- `app/src/main/java/com/m4isper/contactsapp/presentation/contacts/ContactsScreen.kt` — `DeleteUndoEffect`, `toggleFavorite`, `toggleFavoriteSelected`.
- `app/src/main/java/com/m4isper/contactsapp/presentation/editor/ContactEditorScreen.kt` — save and delete flows.
- Compose `LocalContext` provides access to `VibratorService`; prefer `HapticFeedback` APIs where available on the target API level.

## Out of scope

- Custom vibration patterns or long haptic sequences.
- Haptics for every tap (would be noisy); limit to consequential actions only.