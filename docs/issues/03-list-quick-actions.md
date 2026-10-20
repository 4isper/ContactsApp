# feat: add list quick actions, pinned favorites, and recents

**Priority:** P1  
**Area:** contacts list / navigation / favorites

## Problem

The list row only supports click (open details) and long-press (selection). Calling, texting, or favoriting a contact requires opening the contact. Favorites are only a filter chip: there is no dedicated favorites screen, no pinning, and no recents. Batch selection supports only delete, share, and add-to-group.

## Proposed scope

- Add row quick actions (trailing icon or overflow): call, SMS, favorite/unfavorite. Hide call/SMS when the contact has no phone.
- Add swipe actions with accessible alternatives (for example call/SMS/favorite), using Material 3 `SwipeToDismissBox` or a custom row.
- Allow favorite toggling from the list and batch favorite/unfavorite in the selection top bar.
- Add a Favorites section or screen, or pin favorites to the top when no filter is active; add a "Favorites first" setting.
- Add a Recents screen or section based on `Contacts.LAST_TIME_CONTACTED`/`TIMES_CONTACTED` or a local recent-contacts store. Define whether it is device-wide or app-only.
- Add UI tests for quick actions, favorite toggling, and recents.

## Acceptance criteria

- [ ] A user can call, SMS, or favorite a contact without opening details.
- [ ] Favorites are visible without applying the filter (pinned section or dedicated screen).
- [ ] Batch favorite/unfavorite works in selection mode.
- [ ] Recents shows recently contacted contacts and handles the empty state.
- [ ] Actions respect the existing call confirmation and permission handling.

## Technical notes

- `app/src/main/java/com/m4isper/contactsapp/presentation/contacts/ContactCard.kt`, `ContactList.kt`, `SelectionTopBar.kt`.
- `app/src/main/java/com/m4isper/contactsapp/presentation/navigation/ContactsNavHost.kt` — new Favorites/Recents destination.
- `data/src/main/java/com/m4isper/contactsapp/data/datasource/read/ContactsDataProjection.kt` — last-contacted fields.
- `app/src/main/java/com/m4isper/contactsapp/presentation/common/call/` — call handling.

## Out of scope

- A full call-log UI.
- Widgets.
