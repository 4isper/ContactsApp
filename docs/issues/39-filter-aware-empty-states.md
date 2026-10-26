# fix: make contact-list empty states aware of group/account filters

**Priority:** P1 — users can be told "No contacts yet" while their contacts are simply filtered out
**Area:** contacts list / empty states

## Problem

`EmptyPlaceholder` in `ContactListBody.kt` decides that the address book is completely empty with:

```kotlin
val isCompletelyEmpty = !showFavoritesOnly && !isSearching
```

It ignores `uiState.selectedGroupId` and `uiState.selectedAccount`. As a result, when a group or account has no contacts, the screen says "No contacts found" and offers "Create contact" and "Import from file". Creating or importing a contact does not necessarily put it into the selected group or account, so the empty screen remains and the suggested actions do not solve the problem. The user has no "Show all contacts" or "Clear filter" escape hatch.

Two related problems make the empty states weaker:

- The search-empty and favorites-empty states have no recovery action at all. The search field and chips are above the empty state, but the user is left to infer that they must clear them.
- `EmptyState`'s secondary action is implemented as a filled `Button` with `secondaryContainer` colors (`ui/components/States.kt` lines ~201–214). Issue 30 described it as a toned-down outlined action; in practice both actions look equally important, so the visual hierarchy is flat. Long labels on two stacked full-width buttons also crowd small screens.

## Proposed scope

- Pass the active `selectedGroup`, `selectedAccount`, `showFavoritesOnly`, and `searchQuery` (or their labels/booleans) plus clear callbacks into `ContactListBody`.
- Replace the single `isCompletelyEmpty` check with an explicit empty-reason model, for example:
  - `NoContacts` — the address book is genuinely empty → primary "Create contact", secondary "Import from file".
  - `SearchEmpty` → title "Nothing found for “{query}”", primary "Clear search", no import CTA.
  - `FavoritesEmpty` → primary "Show all contacts" (clears the favorites chip).
  - `GroupEmpty` → title "No contacts in {group}", primary "Show all contacts" (clears the group filter); optionally secondary "Manage group".
  - `AccountEmpty` → title "No contacts in {account}", primary "Show all contacts"; secondary "Import from file" only when the account can accept imports.
  - `CombinedFiltersEmpty` → title that names the active filters, primary "Clear filters".
- If search is active together with a group/account filter, clear only the search first or clear all filters with one clearly labelled action; do not offer "Create contact" as the primary fix.
- Change the secondary action to `OutlinedButton` or `TextButton` so the primary action is unmistakable. Keep both actions at least 48 dp high and allow labels to wrap rather than truncate.
- Add a content description/semantics for the empty state so TalkBack reads the reason and available actions together.
- Keep the existing behavior for a truly empty address book: create/import remain the right first-run actions.

## Acceptance criteria

- [ ] With a group filter active and zero matching contacts, the empty state says which group is empty and offers "Show all contacts".
- [ ] With an account filter active and zero matching contacts, the empty state says which account is empty and offers "Show all contacts" (and import only where relevant).
- [ ] Search with no results offers "Clear search" and does not suggest creating a contact as the main action.
- [ ] Favorites-only with no favorites offers "Show all contacts".
- [ ] Clearing the filter restores the full list without leaving the screen.
- [ ] Two CTAs have a clear primary/secondary hierarchy; the secondary action is not a filled button.
- [ ] Empty-state text and actions fit at 320 dp and 200% font scale.

## Technical notes

- `app/src/main/java/com/m4isper/contactsapp/presentation/contacts/components/ContactListBody.kt` lines ~75–111 — `EmptyPlaceholder`, current flags.
- `app/src/main/java/com/m4isper/contactsapp/presentation/contacts/components/ContactList.kt` lines ~146 and ~221–240 — has `uiState`; pass the filter data and clear callbacks down.
- `app/src/main/java/com/m4isper/contactsapp/presentation/contacts/ContactsViewModel.kt` lines ~42–71 — `selectedGroupId`, `selectedGroup`, `selectedAccount`; add/reset actions already exist (`GroupSelected`, `AccountSelected`, `FavoritesOnlyToggled`, `SearchQueryChanged`).
- `app/src/main/java/com/m4isper/contactsapp/ui/components/States.kt` lines ~160–217 — `EmptyState`; add a secondary style parameter or switch to `OutlinedButton`.
- `app/src/main/res/values/strings.xml` and `values-ru/strings.xml` — add filter-specific titles and clear-filter labels, including a format string for the group/account name.
- `docs/issues/30-empty-state-cta.md` — this is a follow-up, not a replacement: keep the first-run CTAs.

## Out of scope

- Onboarding or sample contacts.
- Changing how groups/accounts are filtered or synchronized.
- Empty states for groups, duplicates, and recents screens, which have their own context.

## Verification

- Manual matrix: no contacts, group with no members, account with no contacts, favorites empty, search empty, search + group filter.
- Rotate and use 200% font scale on a small phone.
- Add a Compose test or screenshot test for each empty reason.
- `./gradlew :app:compileDebugKotlin :app:testDebugUnitTest`.
