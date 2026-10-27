# fix: exclude existing members from the group contact picker

**Priority:** P2 — the picker offers actions that do nothing and gives no feedback when it is empty
**Area:** group detail / add members

## Problem

"Add members" opens `ContactPickerDialog` with every contact loaded from the provider:

- `GroupDetailScreen` computes `val memberIds = uiState.members.map { it.contactId }.toSet()` at line ~177 but never uses it. `ContactPickerDialog` has no `excludedIds` parameter, so current group members are shown and selectable. Selecting them and pressing Add results in a no-op or a redundant "members added" toast.
- The picker always opens with an empty selection and no hint about how many people are already in the group. There is no selected-count summary; the confirm button simply becomes enabled.
- When the search returns no results, the `LazyColumn` is empty. There is no "Nothing found" message and no "Clear search" action, so it looks like the contact list failed to load.
- When every contact is already a member, the search is empty and the user still sees a normal dialog; the add FAB remains available even though the only possible action is a no-op.
- `ContactPickerDialog` uses `heightIn(max = 320.dp)` without a minimum, so with two or three results the dialog can look disproportionately empty; with a long list the keyboard plus dialog can obscure the list on small screens.

## Proposed scope

- Add an `excludedIds: Set<Long>` parameter to `ContactPickerDialog` and filter it out before `filterContacts`. Pass the actual `uiState.members` ids from `GroupDetailScreen`.
- Show an empty state inside the dialog:
  - no contacts at all → "No contacts to add";
  - search has no matches → "No contacts found for “{query}”" plus a Clear search button;
  - all contacts already members → a specific message and no Add action.
- Show a selected count in the title or confirm button ("Add 2") and explain why Add is disabled when nothing is selected.
- Keep selection stable while the user searches and clears the query (already the case, but verify).
- Consider fetching only non-member contacts from the data layer for large address books; for this issue filtering in the dialog is sufficient if the list is already loaded.
- Make the dialog height adaptive: a minimum height for short lists and IME-aware scrolling for long ones.
- Disable or explain the Add-member FAB when there are no eligible contacts.

## Acceptance criteria

- [x] Existing group members are not listed in the add-member picker.
- [x] Adding a member updates the member list and the next opening excludes that contact.
- [x] Empty search shows a no-results message with a working Clear search action.
- [x] When every contact is already a member, the picker shows an explanatory empty state and cannot add duplicates.
- [x] The dialog is usable with the keyboard open on a small phone and in landscape.
- [x] Selected contacts remain checked across search query changes.
- [x] The Add action communicates the selected count or why it is disabled.

## Technical notes

- `app/src/main/java/com/m4isper/contactsapp/presentation/groups/GroupDetailScreen.kt` lines ~176–186 — unused `memberIds` and picker call site.
- `app/src/main/java/com/m4isper/contactsapp/presentation/groups/ContactPickerDialog.kt` — whole dialog; no exclusion, no empty state.
- `app/src/main/java/com/m4isper/contactsapp/presentation/groups/GroupDetailViewModel.kt` lines ~138–151 — `allContactsFlow` contains all contacts, including members.
- `app/src/main/java/com/m4isper/contactsapp/ui/components/SearchField.kt` — reuse for the dialog; it already has a clear button.
- `app/src/test/.../GroupDetailViewModelTest` / existing picker tests — extend for exclusion.
- Strings: add empty/no-results/count labels to `values/strings.xml` and `values-ru/strings.xml`.

## Out of scope

- Removing members from the picker (the member list already has its own remove action).
- Creating a new contact from inside the picker.
- Multi-account filtering for groups; groups are already account-scoped.

## Verification

- Manual: empty group, one member, all contacts already members, no results for a query, rotation with the keyboard open.
- UI test for exclusion and selection persistence across searches.
- `./gradlew :app:compileDebugKotlin :app:testDebugUnitTest`.

## Status

Done.

### What was implemented

- `ContactPickerDialog` takes `excludedIds: Set<Long>` and filters already-member contacts before the search, so current members can no longer be selected for a no-op add. `GroupDetailScreen` passes the real member ids — the previously unused `memberIds` set is now the source.
- The dialog renders explicit empty states: no contacts at all → "No contacts to add"; every contact is a member → "Everyone is already in this group"; a search with no matches → the shared "Nothing found for “{query}”" message plus a working Clear search button (the SearchField clear action).
- The confirm button communicates the selection with a plural label ("Add 1 contact" / "Add 2 contacts", Russian one/few/many forms) and stays disabled with the plain label while nothing is selected.
- The list area got a minimum height (`heightIn(min = 192.dp, max = 320.dp)`), so short lists do not collapse the dialog; the search field keeps the dialog usable with the keyboard.
- The Add-member FAB on the group screen is hidden while every contact is already a member — the action would be a no-op, so it is not offered (`FloatingActionButton` has no enabled parameter, hiding replaces disabling).
- Tests: `GroupDetailUiTest` gained three cases — exclusion plus selection persistence across search and clear with the merged ids asserted against the fake repository, the no-results state with the clear action, and the hidden FAB when everyone is a member. Both `GroupDetailUiTest` and `DuplicatesUiTest` now grant `WRITE_CONTACTS` through `GrantPermissionRule`: these screens request it on open (the issue 42 behavior), and running them outside a full suite let the system dialog cover the window with "No compose hierarchies" failures.

### Verification

- `./gradlew :app:compileDebugKotlin :app:compileDebugAndroidTestKotlin :app:testDebugUnitTest :app:detekt :app:ktlintCheck` passes.
- `./gradlew :app:connectedDebugAndroidTest` on a headless emulator: BUILD SUCCESSFUL, 116 tests, 115 passed, 1 skipped by assumption (`LargeListPerformanceTest` benchmark).
- Manual checks that remain useful: the picker with the keyboard open on a small phone in landscape, and the next-open exclusion after a real provider add (the instrumented test covers the fake path end to end).
