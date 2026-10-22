# refactor: centralize the composition root and settings side effects

**Priority:** P2  
**Area:** DI / lifecycle

## Problem

The DI graph is centralized in `AppGraph`, but two paths bypass it or mix responsibilities:

- `BirthdayReminderReceiver` and `ContactsWidgetProvider` manually construct use cases from `AppGraph`. This duplicates the composition root, makes entry points harder to test, and can create inconsistent object lifecycles.
- `SettingsViewModel` both persists reminder settings and schedules alarms. If saving succeeds but scheduling fails, the stored state and the scheduled alarm diverge. Scheduling is an application/infrastructure concern, not a ViewModel concern.
- `ReminderScheduler` and `AppLanguageStore` are app-level ports, but the ViewModel calls them directly instead of going through a coordinating use case.

## Proposed scope

- Make framework entry points obtain dependencies from one graph/entry point; do not hand-construct use cases.
- Introduce a coordinator/use case for reminder settings: persist first, then schedule, with explicit failure handling and retry/rollback semantics.
- Keep alarm scheduling out of `SettingsViewModel`; expose a result/effect instead.
- Add tests for receiver/widget entry points and for the save-then-schedule failure path.

## Acceptance criteria

- [ ] `BirthdayReminderReceiver` and `ContactsWidgetProvider` use the shared DI graph without manual use-case assembly.
- [ ] Saving reminder settings and scheduling the alarm cannot silently diverge.
- [ ] `SettingsViewModel` does not call `ReminderScheduler` directly.
- [ ] Entry points and the reminder-settings flow have focused tests.

## Technical notes

- `app/src/main/java/com/m4isper/contactsapp/di/AppGraph.kt`
- `app/src/main/java/com/m4isper/contactsapp/presentation/settings/SettingsViewModel.kt`
- `app/src/main/java/com/m4isper/contactsapp/reminders/BirthdayReminderReceiver.kt`
- `app/src/main/java/com/m4isper/contactsapp/widget/ContactsWidgetProvider.kt`

## Out of scope

- Migrating alarms to WorkManager.
- Changing Android background-execution policy.
