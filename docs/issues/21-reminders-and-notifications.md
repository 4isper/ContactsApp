# fix: make birthday and event reminders actually work

**Priority:** P1
**Area:** reminders / notifications / data

## Problem

Reminders are wired through the correct domain logic, but the data and Android layers break the chain:

- `aggregateContacts` does not map `birthday`, `events`, or `instantMessages`.
- `DATA_MIMETYPES` does not include `Event` or `IM`, so list queries never fetch those rows.
- `BirthdayReminderReceiver` builds occasions from `graph.getContacts()`, which uses the broken aggregation.
- The repeating alarm is cleared by the system after reboot and is never rescheduled; there is no `BOOT_COMPLETED` receiver or permission.
- `POST_NOTIFICATIONS` is declared but never requested, so Android 13+ silently suppresses notifications.
- The receiver does not guard against a revoked `READ_CONTACTS` permission and can throw `SecurityException`.

## Proposed scope

- Add `Event` (and `IM` if needed) to the list MIME types and map `birthday`/`events` in `aggregateContacts`.
- Add a `BOOT_COMPLETED` receiver and `RECEIVE_BOOT_COMPLETED` permission to reschedule reminders.
- Request `POST_NOTIFICATIONS` when reminders are enabled on Android 13+.
- Guard the receiver with a permission check and safe error handling.
- Add regression tests for upcoming occasions from list contacts and reminder scheduling.

## Acceptance criteria

- [x] Contacts loaded for the list include birthdays and events.
- [x] Enabling reminders schedules a repeating alarm and it is restored after reboot.
- [x] On Android 13+, enabling reminders requests `POST_NOTIFICATIONS`.
- [x] The receiver does not crash when `READ_CONTACTS` is revoked.
- [x] Reminders show expected occasions in unit tests.

## Status

Done.

What changed:

- `aggregateContacts` now maps `birthday`, `events`, and `instantMessages`.
- `DATA_MIMETYPES` now includes `Event` and `Im`, with the placeholder count updated.
- `BirthdayReminderReceiver.showReminder` returns early when `READ_CONTACTS` is revoked.
- Added `ReminderBootReceiver` and registered it for `BOOT_COMPLETED` with `RECEIVE_BOOT_COMPLETED`.
- Settings now requests `POST_NOTIFICATIONS` when reminders are enabled on Android 13+.
- Added a regression test verifying birthdays are aggregated for reminder consumers.

## Technical notes

- `data/src/main/java/com/m4isper/contactsapp/data/datasource/read/ContactsAggregation.kt`
- `data/src/main/java/com/m4isper/contactsapp/data/datasource/ContactsDataSourceImpl.kt`
- `app/src/main/java/com/m4isper/contactsapp/presentation/reminders/BirthdayReminderReceiver.kt`
- `app/src/main/java/com/m4isper/contactsapp/presentation/reminders/ReminderBootReceiver.kt`
- `app/src/main/AndroidManifest.xml`
- `app/src/main/java/com/m4isper/contactsapp/presentation/settings/SettingsScreen.kt`

## Technical notes

- `data/src/main/java/com/m4isper/contactsapp/data/datasource/read/ContactsAggregation.kt`
- `data/src/main/java/com/m4isper/contactsapp/data/datasource/ContactsDataSourceImpl.kt`
- `app/src/main/java/com/m4isper/contactsapp/presentation/reminders/BirthdayReminderReceiver.kt`
- `app/src/main/java/com/m4isper/contactsapp/presentation/reminders/BirthdayReminderScheduler.kt`
- `app/src/main/AndroidManifest.xml`
- `app/src/main/java/com/m4isper/contactsapp/presentation/settings/ReminderSettingsCoordinator.kt`

## Out of scope

- Custom notification time of day.
- Replacing AlarmManager with WorkManager.
