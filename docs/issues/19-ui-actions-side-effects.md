# refactor: replace callback-heavy UI APIs and move side effects out of composition

**Priority:** P2  
**Area:** Compose UI

## Problem

Several Compose screens expose too much behavior through ad-hoc parameters and perform lifecycle work directly:

- `ContactsScreen`/`ContactList` pass a large number of callbacks, which bloats the composable API, makes previews/tests noisy, and increases the chance of unnecessary recomposition.
- `SettingsScreen` calls `Activity.recreate()` directly from a composable lambda. That is a lifecycle side effect in the UI layer and can lose state.
- `ContactDetailsScreen` uses `requireNotNull` after an existence check, which can crash if the state changes between the check and use.

## Proposed scope

- Replace the callback list with one `ContactsUiAction` sealed interface or an event sink; hoist state and split the list into smaller composables.
- Move activity-level effects (language change/recreate) to an effect handler at the host/Activity level; consider `AppCompatDelegate.setApplicationLocales` instead of manual recreation.
- Represent missing details as an explicit error/empty state instead of `requireNotNull`.
- Add Compose UI tests for the main actions and state transitions.

## Acceptance criteria

- [ ] `ContactsScreen` exposes one action/event API instead of dozens of callbacks.
- [ ] Composables do not call `Activity` lifecycle methods directly.
- [ ] Missing contact details render an error/empty state instead of crashing.
- [ ] UI tests cover the extracted actions and the details error path.

## Technical notes

- `app/src/main/java/com/m4isper/contactsapp/presentation/contacts/ContactsScreen.kt`
- `app/src/main/java/com/m4isper/contactsapp/presentation/contacts/ContactList.kt`
- `app/src/main/java/com/m4isper/contactsapp/presentation/settings/SettingsScreen.kt`
- `app/src/main/java/com/m4isper/contactsapp/presentation/details/ContactDetailsScreen.kt`

## Out of scope

- Visual redesign.
- Migrating the whole app to a different state-management library.
