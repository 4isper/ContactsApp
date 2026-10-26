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

- [ ] Denying the notification permission leaves the reminders switch off and shows an explanation with a settings action.
- [ ] Granting the permission turns reminders on and persists the preference.
- [ ] On Android 12 and below, disabling app notifications in system settings is reflected by the switch/warning on return to the screen.
- [ ] A blocked reminders channel is detected and explained.
- [ ] Returning from system settings refreshes the state automatically.
- [ ] The user is never shown an "on" switch when notifications are guaranteed to be dropped.
- [ ] All new text is localized in English and Russian.

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
