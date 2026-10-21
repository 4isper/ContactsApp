# feat: improve vCard import/export fidelity

**Priority:** P1  
**Area:** vCard / import / export

## Problem

The vCard codec covers names, phones, emails, IM, addresses, websites, organization, note, birthday, nickname, relations, and events, but:

- Export omits `PHOTO` and `CATEGORIES`, so photos and group memberships are lost.
- Parsing explicitly does not support `PHOTO` or quoted-printable.
- `ADR` is flattened to a single string and loses structured parts.
- Import always creates new contacts, has no preview/merge, and silently skips unreadable entries.
- Export always includes all contacts; there is no "export selected" to a file.
- No CSV or other interchange format.

## Proposed scope

- Export/import `PHOTO` (base64) with size limits and sensible failure behavior.
- Export group membership as `CATEGORIES`; import categories into groups where the account allows it (or report skipped categories).
- Preserve structured `ADR` parts after the structured-fields item; support multi-line/quoted-printable input if feasible.
- Add an import preview: count, duplicates, invalid entries, target account, and a merge/skip/create choice.
- Report per-contact import errors instead of only a final count.
- Add "Export selected" from selection mode to a `.vcf` file.
- Evaluate CSV import/export as a separate follow-up (not required for this item).
- Add codec and import tests with realistic Google/Apple/Android vCards.

## Acceptance criteria

- [x] Exported vCard includes photo and categories when present.
- [x] Importing a vCard with photo/categories preserves them (or clearly reports what was skipped).
- [x] Structured addresses survive export/import.
- [x] Import preview shows duplicates/invalid entries before writing.
- [x] Selected contacts can be exported to a file.
- [x] Tests cover round-trip fidelity.

## Status

Done. The exported card carries the photo and the categories, the importer keeps both and reports what it could not read, structured addresses survive the round trip, and values in quoted-printable are decoded. Import shows a preview with the duplicates and the invalid entries before anything is written, and the selected contacts can be exported to a file. Round-trip fidelity is covered by unit tests.

## Technical notes

- `data/src/main/java/com/m4isper/contactsapp/data/vcard/VCardBuilder.kt`, `VCardParser.kt`, `VCardValues.kt`.
- `app/src/main/java/com/m4isper/contactsapp/presentation/contacts/ContactsFileActions.kt`, `ContactsLaunchers.kt`.
- `domain/src/main/java/com/m4isper/contactsapp/domain/usecase/ImportContactsUseCase.kt`.
- `app/src/main/java/com/m4isper/contactsapp/presentation/contacts/SelectionTopBar.kt` — export-selected entry point.

## Out of scope

- CSV (follow-up).
- Cloud backup.
