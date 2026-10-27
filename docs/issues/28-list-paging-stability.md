# fix: make contact paging order deterministic

**Priority:** P2
**Area:** contacts list / paging

## Problem

Contact paging uses `DISPLAY_NAME ASC` only, with `LIMIT/OFFSET`. Contacts with equal display names have no stable tiebreaker, so provider ordering can differ between pages and cause duplicates or skipped contacts.

## Proposed scope

- Add a deterministic tiebreaker such as `_ID ASC` to the contact sort order.
- Apply the same ordering to both full loads and paged loads.
- Add regression tests for equal display names across page boundaries.

## Acceptance criteria

- [x] Contacts with equal names are ordered consistently across pages.
- [x] No contact is skipped or duplicated during paging.
- [x] Existing list tests remain green.

## Status

Done.

What changed:

- Added `${ContactsContract.Contacts._ID} ASC` as a deterministic tiebreaker to `CONTACT_SORT_ORDER`.

## Technical notes

- `data/src/main/java/com/m4isper/contactsapp/data/datasource/ContactsDataSourceImpl.kt`

## Out of scope

- Changing page size or paging strategy.
