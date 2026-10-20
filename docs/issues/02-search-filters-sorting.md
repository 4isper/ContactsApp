# feat: extend contact search, account filters, and sorting

**Priority:** P1  
**Area:** contacts list / search / data projection

## Problem

`filterContacts()` only matches name (including phonetic parts), phone, email, nickname, and relation names. Organization, job title, postal address, website, note, group name, and custom labels are not searchable. Phone matching uses a raw substring, so formatted numbers are missed. The list has no account filter and only two sort orders.

## Proposed scope

- Normalize phone search: compare digits only, strip `+`, spaces, dashes, and parentheses; support matching by the last 7–10 digits and country-code variants.
- Add search over company/title, addresses, websites, notes, group names, and custom labels.
  - Decide whether to extend the lightweight list projection in `ContactsDataSourceImpl.loadContacts()` or to query matching IDs first; keep list rendering fast.
- Add an account filter (All / Local / specific account) in the filter header, consistent with the group filter UX.
- Add sort options: company, recently added, and A–Z/Z–A; persist the chosen sort in settings. Keep the existing name/family sort.
- Add tests for normalized phone matching, new fields, account filter, and sort options.

## Acceptance criteria

- [ ] `999123` finds `+7 (999) 123-45-67` (and reasonable country-code variants).
- [ ] Searching by company, address, website, note, or group name returns matching contacts.
- [ ] The account filter limits the list and shows an empty state when nothing matches.
- [ ] The sort choice survives an app restart.
- [ ] Unit tests cover search, filter, and sort logic.

## Technical notes

- `app/src/main/java/com/m4isper/contactsapp/presentation/contacts/ContactSearch.kt` — current matching logic.
- `app/src/main/java/com/m4isper/contactsapp/presentation/contacts/ContactsViewModel.kt` — filter/sort state.
- `data/src/main/java/com/m4isper/contactsapp/data/datasource/read/ContactsDataProjection.kt` and `ContactsDataSourceImpl.kt` — current lightweight projection.
- `app/src/main/java/com/m4isper/contactsapp/presentation/contacts/ContactsFilterHeader.kt` — filter UI.
- `domain/src/main/java/com/m4isper/contactsapp/domain/model/AppSettings.kt` — sort settings.

## Out of scope

- T9/dialer search.
- Search history.
