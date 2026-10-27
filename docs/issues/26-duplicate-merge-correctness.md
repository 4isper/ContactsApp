# fix: treat null merge result as failure and merge all raw contacts

**Priority:** P2
**Area:** duplicates / merge

## Problem

`MergeContactsUseCase` returns `null` when the target or source raw contact is missing, but callers treat the operation as successful:

- `DuplicatesViewModel` emits `Merged` even when the result is null, and sets `lastLink = null`, making undo impossible.
- `ImportContactsUseCase.mergeInto` ignores the null result and counts the merge as successful.

In addition, merge links only the first raw contact per aggregated contact. Contacts with multiple raw contacts can remain partially unmerged.

## Proposed scope

- Check for null merge results in both duplicate merge and import merge.
- Link every raw contact of each source contact, not only the first one.
- Delete the temporary imported contact when merge fails.
- Emit `MergeFailed` and preserve undo state only on success.

## Acceptance criteria

- [x] A null merge result is reported as failure.
- [x] Undo is available only after a successful merge.
- [x] Imported contacts are cleaned up when merge fails.
- [x] Multi-raw source contacts are fully merged.

## Status

Done.

What changed:

- `DuplicatesViewModel` checks the merge result and emits `MergeFailed` on null.
- `ImportContactsUseCase.mergeInto` returns whether the merge actually succeeded.
- `ContactsWriterDataSource` can load all raw contact IDs.
- Repository merge now links every raw contact of each source.

## Technical notes

- `app/src/main/java/com/m4isper/contactsapp/presentation/duplicates/DuplicatesViewModel.kt`
- `domain/src/main/java/com/m4isper/contactsapp/domain/usecase/contacts/ImportContactsUseCase.kt`
- `data/src/main/java/com/m4isper/contactsapp/data/repository/ContactsRepositoryImpl.kt`
- `data/src/main/java/com/m4isper/contactsapp/data/datasource/ContactsWriterDataSourceImpl.kt`

## Out of scope

- Changing duplicate detection heuristics.
