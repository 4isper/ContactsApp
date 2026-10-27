# fix: request WRITE_CONTACTS for favorite, delete, group, and merge actions

**Priority:** P1
**Area:** permissions / contacts mutations

## Problem

The app requests `READ_CONTACTS` in the list screen and `WRITE_CONTACTS` only for the editor and import. Other mutating actions write to `ContactsContract` without checking or requesting `WRITE_CONTACTS`:

- favorite toggle
- batch favorite toggle
- delete selected contacts
- group create/rename/delete/merge
- duplicate merge

This can cause `SecurityException`, silent failures, or generic error messages.

## Proposed scope

- Add a permission gate or request flow before mutating contacts.
- Show a clear permission-required message when access is denied.
- Ensure favorite, delete, group, and merge actions either succeed or explain why they cannot.
- Add tests or manual verification for denied `WRITE_CONTACTS`.

## Acceptance criteria

- [x] Favorite toggle requests `WRITE_CONTACTS` when missing.
- [x] Delete, group, and merge actions request `WRITE_CONTACTS` when missing.
- [x] Denied permission produces a clear user-facing message.
- [x] No unhandled `SecurityException` occurs during these actions.

## Status

Done.

What changed:

- Contacts list now tracks `WRITE_CONTACTS` and requests it when read permission is granted.
- Favorite and delete actions are gated on write permission and retried after grant.
- Groups, group details, and duplicates request `WRITE_CONTACTS` on entry.
- Write permission is resynced on resume.

## Technical notes

- `app/src/main/java/com/m4isper/contactsapp/presentation/contacts/ContactsScreen.kt`
- `app/src/main/java/com/m4isper/contactsapp/presentation/contacts/state/ContactsFavoriteState.kt`
- `data/src/main/java/com/m4isper/contactsapp/data/datasource/FavoritesDataSourceImpl.kt`
- `app/src/main/java/com/m4isper/contactsapp/presentation/contacts/effects/PendingDeleteCoordinator.kt`
- `app/src/main/java/com/m4isper/contactsapp/presentation/groups/GroupsViewModel.kt`
- `app/src/main/java/com/m4isper/contactsapp/presentation/groups/GroupDetailViewModel.kt`
- `app/src/main/java/com/m4isper/contactsapp/presentation/duplicates/DuplicatesViewModel.kt`

## Out of scope

- Redesigning the permission UX beyond existing patterns.
