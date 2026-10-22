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

- [x] `ContactsScreen` exposes one action/event API instead of dozens of callbacks.
- [x] Composables do not call `Activity` lifecycle methods directly.
- [x] Missing contact details render an error/empty state instead of crashing.
- [x] UI tests cover the extracted actions and the details error path.

## Status

Done.

- The list has one contract instead of thirty callbacks: `ContactsUiAction` with three groups -
  the state of the list, the files and the deletion, the transitions to the other screens. The
  screen parses the action in one place, the leaves keep their own lambdas.
- The transitions belong to the host: the navigation actions come to the navigation host as
  `ContactsNavigationAction`, and it opens the screen. The screen of the list does not know the
  back stack.
- The screen of the settings does not touch the lifecycle of the activity any more: it reports
  that the window has to be recreated, and the activity does it.
- The card of the contact does not fall on `requireNotNull`: the sharing of the text and of the
  QR code checks the data and shows the error of the sharing.
- The UI tests of the list go through the actions (the click on the contact, the star, the
  search, the share, the groups), and the card of the contact keeps the test of the unknown
  contact.

## Technical notes

- `app/src/main/java/com/m4isper/contactsapp/presentation/contacts/ContactsScreen.kt`
- `app/src/main/java/com/m4isper/contactsapp/presentation/contacts/ContactList.kt`
- `app/src/main/java/com/m4isper/contactsapp/presentation/settings/SettingsScreen.kt`
- `app/src/main/java/com/m4isper/contactsapp/presentation/details/ContactDetailsScreen.kt`

## Out of scope

- Visual redesign.
- Migrating the whole app to a different state-management library.
