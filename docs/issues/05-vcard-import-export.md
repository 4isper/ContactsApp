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

- [ ] Exported vCard includes photo and categories when present.
- [ ] Importing a vCard with photo/categories preserves them (or clearly reports what was skipped).
- [ ] Structured addresses survive export/import.
- [ ] Import preview shows duplicates/invalid entries before writing.
- [ ] Selected contacts can be exported to a file.
- [ ] Tests cover round-trip fidelity.

## Technical notes

- `data/src/main/java/com/m4isper/contactsapp/data/vcard/VCardBuilder.kt`, `VCardParser.kt`, `VCardValues.kt`.
- `app/src/main/java/com/m4isper/contactsapp/presentation/contacts/ContactsFileActions.kt`, `ContactsLaunchers.kt`.
- `domain/src/main/java/com/m4isper/contactsapp/domain/usecase/ImportContactsUseCase.kt`.
- `app/src/main/java/com/m4isper/contactsapp/presentation/contacts/SelectionTopBar.kt` — export-selected entry point.

## Out of scope

- CSV (follow-up).
- Cloud backup.
