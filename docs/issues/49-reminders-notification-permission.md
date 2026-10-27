# fix: keep the reminders switch in sync with notification permission

**Priority:** P2 — reminders can appear enabled while the system silently drops every notification
**Area:** settings / reminders / permissions

## Problem

When the user turns reminders on, `SettingsScreen` launches the Android 13+ notification-permission request and immediately calls `viewModel.onRemindersChanged(true, ...)` without waiting for the result (`SettingsScreen.kt` lines ~270–290). The permission callback is an empty `/* no-op: receiver checks permission */`.

Consequences:

- Denying the permission leaves the reminders switch on and the lead-time options visible. The UI claims reminders are active, but `BirthdayReminderReceiver` checks `POST_NOTIFICATIONS` and skips `notify()` when it is missing (`BirthdayReminderReceiver.kt` lines ~64–73), so nothing is ever delivered.
- If the user selected "Don't ask again", the launcher returns denied immediately; there is no rationale, no explanation, and no path to system settings.
- The code only checks runtime permission. If the user has notifications disabled for the app in system settings (or the reminders channel is blocked), the switch still shows on. No code calls `NotificationManagerCompat.areNotificationsEnabled()` or checks the channel importance.
- The denial is not announced or surfaced in any way, so the failure is only discovered when a birthday passes without a notification.
- The switch's label says "Notify about upcoming birthdays and events", so an on state is a strong promise. This is a trust issue, not just a permission edge case.

## Proposed scope

- Make the switch reflect an effective state: reminders are on only when the preference is enabled and notifications can actually be delivered.
- When enabling:
  - On Android 13+, request `POST_NOTIFICATIONS` and update the preference only after a granted result.
  - If denied, keep the switch off and show an inline explanation. If `shouldShowRequestPermissionRationale` is true, show a short rationale before/after the system dialog; if permanently denied, offer "Open settings".
  - On older versions, check `NotificationManagerCompat.areNotificationsEnabled()` before enabling.
- Check the reminders notification channel as well. Create it at the right time and treat `IMPORTANCE_NONE` as blocked; show a channel-specific explanation with a settings action.
- Re-check the state in `LifecycleResumeEffect` so returning from system settings updates the switch and warning without restarting the screen.
- Remember a pending "user wants reminders" intent so that if they grant permission in Settings and return, the preference can be applied (or require one explicit toggle, but make that clear).
- Use a snackbar/dialog with an action rather than a toast: the user needs a route to fix the problem. Add all strings to English and Russian.
- Do not request notification permission on screen entry; only in response to the switch.

## Acceptance criteria

- [x] Denying the notification permission leaves the reminders switch off and shows an explanation with a settings action.
- [x] Granting the permission turns reminders on and persists the preference.
- [x] On Android 12 and below, disabling app notifications in system settings is reflected by the switch/warning on return to the screen.
- [x] A blocked reminders channel is detected and explained.
- [x] Returning from system settings refreshes the state automatically.
- [x] The user is never shown an "on" switch when notifications are guaranteed to be dropped.
- [x] All new text is localized in English and Russian.

## Technical notes

- `app/src/main/java/com/m4isper/contactsapp/presentation/settings/SettingsScreen.kt` lines ~263–302 — `RemindersSection`, empty permission callback.
- `app/src/main/java/com/m4isper/contactsapp/presentation/settings/SettingsViewModel.kt` / `ReminderSettingsCoordinator.kt` — where the reminder preference is persisted.
- `app/src/main/java/com/m4isper/contactsapp/presentation/reminders/BirthdayReminderReceiver.kt` lines ~64–73 — silently skips notification when permission is missing.
- `app/src/main/java/com/m4isper/contactsapp/presentation/common/permissions/` — `hasPermission`, `openAppSettings`.
- `androidx.core.app.NotificationManagerCompat.areNotificationsEnabled()` and `NotificationManager.getNotificationChannel(...)` for the channel check.
- `androidx.activity.compose.rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission())` callback must drive the preference, not be a no-op.
- Add a reminders-channel constant/ID shared with the receiver so the settings screen checks the same channel.
- `docs/issues/21-reminders-and-notifications.md` — data/scheduling correctness; this issue is the settings and permission UX on top of it.

## Out of scope

- Changing when reminders fire or how lead days are calculated.
- Adding notification categories or per-contact reminder controls.
- Requesting permission during onboarding.

## Verification

- Manual on API 33+ and API 30: deny, grant, permanently deny, disable app notifications, block the channel, and return from Settings.
- Confirm no notification is claimed as active while blocked.
- UI test for the permission callback and switch state.
- `./gradlew :app:compileDebugKotlin :app:testDebugUnitTest`.

## Status

Done, with the one-explicit-toggle choice for the pending intent.

### What was implemented

- The switch shows the effective state through `remindersEffectivelyOn(enabled, appNotificationsEnabled, channelBlocked)` — a pure function unit-tested in `RemindersDeliveryTest`. An "on" switch now guarantees the notification can actually be delivered.
- Enabling: on Android 13+ the `POST_NOTIFICATIONS` request drives the preference — the launcher callback applies `onRemindersChanged(true)` only after a grant; a denial shows an inline explanation with an "Open settings" action and leaves the switch off. The "pending intent" from the issue is resolved with the documented alternative: one explicit toggle after returning from settings.
- On any API level the screen also checks `NotificationManagerCompat.areNotificationsEnabled()` and the reminders channel importance (the channel id is now shared between the receiver and settings via `REMINDERS_CHANNEL_ID`); a blocked app or channel shows a dedicated warning with an "Open settings" action.
- `LifecycleResumeEffect` recomputes deliverability, so returning from system settings updates the switch and warnings without restarting the screen.
- No permission request happens on screen entry — the launcher fires only from the switch.
- The reminders section moved to its own `RemindersSection.kt` (the settings file hit the detekt function-count limit); warnings and lead options became separate composables to stay within the complexity budget.
- New strings in en/ru: `settings_reminders_permission`, `settings_reminders_blocked`.

### Verification

- `./gradlew :app:compileDebugKotlin :app:compileDebugAndroidTestKotlin :app:testDebugUnitTest :app:detekt :app:ktlintCheck` passes.
- `./gradlew :app:connectedDebugAndroidTest` on a headless emulator: BUILD SUCCESSFUL, 119 tests, 118 passed, 1 skipped by assumption (`LargeListPerformanceTest` benchmark).
- Manual checks that remain useful: deny / grant / permanently-deny / app-notifications-off / channel-blocked on API 33+ and API 30 devices, and the return-from-settings refresh on a real device.
