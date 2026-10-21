# feat: add account management, system contact integration, and safe merge

**Priority:** P2  
**Area:** accounts / platform integration / contact lifecycle

## Problem

- Existing contacts show their account as read-only; there is no move/copy to another account.
- Only the last used account is remembered; there is no default account setting.
- Favorites live in DataStore instead of `ContactsContract.STARRED`, so they do not sync with the system or other apps and can become stale after external deletion.
- `ContactsContract.Profile` ("My profile") is not supported.
- The manifest has no contact intents (`ACTION_VIEW`, `ACTION_INSERT`, `ACTION_EDIT`, `ACTION_PICK`, `ACTION_SEND`/`ACTION_VIEW` for `.vcf`), so the app cannot act as a system contacts handler or receive shared vCards.
- Duplicate merge deletes source contacts without undo; there is no manual link/unlink of raw contacts or field-by-field merge preview.

## Proposed scope

- Add "Move/Copy to account" for existing contacts (copy + delete or provider-supported move), with clear warnings.
- Add a default account setting for new contacts, separate from last-used.
- Read/write `ContactsContract.STARRED` for favorites; migrate existing DataStore favorites on first launch and clean stale IDs.
- Add a "My profile" entry using `ContactsContract.Profile`.
- Add intent filters/handlers for viewing, inserting, editing, picking, and receiving vCards; test with the system Contacts app and a share sheet.
- Add merge preview (fields/conflicts), an undo path (at least a snackbar with delayed source deletion where feasible), and manual link/unlink via `AggregationExceptions`.
- Add tests for account copy/move, favorites migration, intent parsing, and merge safety.

## Acceptance criteria

- [x] A contact can be copied or moved to another account.
- [x] New contacts use the configured default account.
- [x] Favorites match `STARRED` and survive sync; old DataStore favorites migrate.
- [x] The app appears in "Open with"/"Share" for contacts and vCards.
- [x] Merge shows what will change and can be undone or safely cancelled.
- [x] Manual link/unlink is available for raw contacts.

## Status

Done. A contact can be copied or moved to another account, new contacts use the configured default account, and the favorites live in the `STARRED` rows of the provider (the old DataStore favorites are migrated once). The app registers itself for "Open with" and "Share" for contacts and vCards. Merging links the raw contacts instead of deleting the sources, so it can be undone from a snackbar; the manual link and unlink live in the duplicates screen. Covered by unit tests and by an instrumentation test for the merge.

## Technical notes

- `app/src/main/java/com/m4isper/contactsapp/presentation/editor/EditorForm.kt` — account row is read-only for existing contacts.
- `domain/src/main/java/com/m4isper/contactsapp/domain/model/AppSettings.kt` — `lastContactAccount`.
- `data/src/main/java/com/m4isper/contactsapp/data/datasource/FavoritesDataSourceImpl.kt`.
- `data/src/main/java/com/m4isper/contactsapp/data/datasource/ContactsWriterDataSourceImpl.kt`.
- `data/src/main/java/com/m4isper/contactsapp/data/datasource/write/MergePlans.kt`.
- `app/src/main/AndroidManifest.xml`.

## Out of scope

- Full contacts-app replacement role.
- Widgets.
