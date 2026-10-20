# feat: observe contact provider changes for live updates

**Priority:** P0 — high impact, small/medium size  
**Area:** contacts list / data layer

## Problem

The contact list is loaded only by `ContactsViewModel.refresh()` on screen resume and pull-to-refresh. There is no `ContentObserver` anywhere in the project. If a contact is created, edited, deleted, or synced by another app while the list is open, the UI stays stale until the user refreshes manually.

## Proposed scope

- Add a provider observer (for example `ContactsObserver`) registered on `ContactsContract.Contacts.CONTENT_URI` and `ContactsContract.Data.CONTENT_URI`; consider `Groups.CONTENT_URI` for membership changes.
- Debounce bursts (300–500 ms) and trigger a refresh that preserves the search query, group/favorites filters, and selection where possible.
- Tie registration to the screen lifecycle (`DisposableEffect`, `repeatOnLifecycle`) or expose a repository flow with `SharingStarted.WhileSubscribed`; always unregister.
- Handle revoked `READ_CONTACTS` permission and provider errors gracefully.
- Prevent overlapping refreshes (`isLoading` already guards the ViewModel path, but an observer may bypass it).
- Add tests: a fake observer or an instrumentation test that changes a contact and asserts the list updates.

## Acceptance criteria

- [ ] A contact edited in the system Contacts app appears in the list within ~1 second without pull-to-refresh.
- [ ] Creating or deleting a contact updates the list automatically.
- [ ] Search/filter/selection state is not unexpectedly cleared by a background update.
- [ ] The observer is unregistered when the screen is not active; no leaked callbacks.
- [ ] Unit/UI tests cover the update path.

## Technical notes

- `app/src/main/java/com/m4isper/contactsapp/presentation/contacts/ContactsViewModel.kt` — `refresh()` is the only load path.
- `data/src/main/java/com/m4isper/contactsapp/data/datasource/ContactsDataSourceImpl.kt` — provider queries.
- Consider exposing a `Flow<List<ContactDetails>>` from the repository instead of manual refresh; this would simplify future reactive changes.

## Out of scope

- Paging (covered by the settings/performance item).
- Changes to the system Contacts app.
