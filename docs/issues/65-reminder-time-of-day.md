# feat: let the user pick the daily reminder time

**Priority:** P3 — reminders work end to end, but the notification always lands at a fixed 9:00 local; the time of day is the one thing a user would expect to control
**Area:** reminders / settings

## Problem

The daily birthday/event reminder is scheduled at a hardcoded hour:

- `BirthdayReminderScheduler` sets `REMINDER_HOUR = 9` and repeats every day at 09:00 local time.
- `AppSettings` carries only `reminderLeadDays`; the time of day is not a setting at all.
- The roadmap README ("Not covered by the roadmap") and issue 21's "Out of scope" both documented this gap.

Everything needed for a user-chosen time already exists: the receiver aggregates occasions at fire time (no data migration), the scheduler is a single entry point, and `ReminderSettingsCoordinator` already owns the save-then-schedule-then-rollback contract. Only the plumbing of the time value is missing.

## Proposed scope

- Add `reminderHour: Int = 9` and `reminderMinute: Int = 0` to `AppSettings` (domain) and persist them with the rest of the settings.
- Parameterize `ReminderScheduler.schedule()` with the time of day; update `BirthdayReminderScheduler` and keep the "already past → schedule for tomorrow" roll-forward.
- Extend `ReminderSettingsCoordinator.apply` (or add a `SetReminderTimeUseCase`) so changing the time saves it first and reschedules the alarm second, preserving the existing rollback on scheduling failure.
- `ReminderBootReceiver` reschedules with the stored time instead of the default.
- Settings UI: a "Reminder time" choice row in `RemindersSection` (below the lead-days row) that opens a value sheet with a Material 3 `TimePicker`, consistent with the choice-row pattern from issue 60.
- Update the reminder settings JVM tests for the new parameter; add a test for the next-trigger computation around midnight (time in the past rolls to tomorrow).

## Acceptance criteria

- [ ] The user can pick any time of day for the daily reminder; the default stays 9:00 for existing installs.
- [ ] Changing the time reschedules the single existing alarm — no duplicate or stale alarms.
- [ ] After reboot the reminder fires at the chosen time, not the default.
- [ ] If scheduling fails after a time change, settings roll back including the time and the error surfaces, matching the coordinator contract.
- [ ] Unit tests cover the next-trigger computation (before/after the chosen time, midnight boundary) and the coordinator rollback.

## Technical notes

- `app/src/main/java/com/m4isper/contactsapp/presentation/reminders/BirthdayReminderScheduler.kt` — `REMINDER_HOUR` lives in the private companion; `ReminderBootReceiver` constructs the scheduler directly and calls `schedule()`.
- `app/src/main/java/com/m4isper/contactsapp/presentation/settings/ReminderSettingsCoordinator.kt`
- `domain/src/main/java/com/m4isper/contactsapp/domain/model/settings/AppSettings.kt`
- `app/src/main/java/com/m4isper/contactsapp/presentation/settings/RemindersSection.kt`
- `app/src/main/java/com/m4isper/contactsapp/presentation/reminders/ReminderBootReceiver.kt` — it already reads settings before scheduling, so passing the time there is mechanical.

## Out of scope

- Exact alarms (`SCHEDULE_EXACT_ALARM` / `setExactAndAllowWhileIdle`): `setInexactRepeating` can shift the trigger within the doze window; for a daily digest this is acceptable.
- Per-occasion or per-contact reminder rules — the single daily digest stays.
- A separate "reminder today" notification in addition to the lead time.
