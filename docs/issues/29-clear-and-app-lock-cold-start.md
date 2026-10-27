# fix: reset filters and avoid exposing contacts before app-lock settings load

**Priority:** P2
**Area:** contacts list / app lock / settings

## Problem

Two related state issues:

1. `ContactsViewModel.clear()` calls `filters.onToggleFavoritesOnly()`, which toggles the favorites filter instead of resetting it to false.
2. `AppSettingsViewModel` starts with `appLockEnabled = false` until DataStore emits. If app lock is enabled, contacts can briefly render before the setting loads.
3. `AppLockScreen` keeps the unlock button enabled during lockout; failed attempts are rejected only after tapping.

## Proposed scope

- Reset `favoritesOnly` to false in `clear()`.
- Avoid rendering protected content until app-lock settings have loaded, or default to a safe locked state.
- Disable the unlock button while lockout is active.
- Add tests for permission-loss reset and cold-start app lock.

## Acceptance criteria

- [x] Clearing the list resets the favorites filter to false.
- [x] App lock settings are loaded before protected content is shown.
- [x] The unlock button is disabled during lockout.
- [x] Regression tests cover both behaviors.

## Status

Done.

What changed:

- `ContactsViewModel.clear()` now resets `favoritesOnly` to false.
- `AppSettingsViewModel` starts from a safe locked state until settings load.
- `AppLockScreen` disables unlock during lockout.

## Technical notes

- `app/src/main/java/com/m4isper/contactsapp/presentation/contacts/ContactsViewModel.kt`
- `app/src/main/java/com/m4isper/contactsapp/presentation/contacts/state/ContactsFilterState.kt`
- `app/src/main/java/com/m4isper/contactsapp/presentation/settings/AppSettingsViewModel.kt`
- `app/src/main/java/com/m4isper/contactsapp/MainActivity.kt`
- `app/src/main/java/com/m4isper/contactsapp/presentation/settings/AppLockScreen.kt`

## Out of scope

- Redesigning the app-lock screen.
