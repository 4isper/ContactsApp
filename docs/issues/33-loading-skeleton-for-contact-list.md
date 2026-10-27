# feat: add skeleton placeholders while the contact list loads

**Priority:** P2 — visual polish, reduces perceived load time  
**Area:** contacts list / UI

## Problem

When the contact list loads (initial load, pull-to-refresh, or filter change), `ContactListBody` renders `LoadingState()` — a simple spinner/placeholder — and then the full list of cards appears all at once. On larger contact bases this creates a visible "jump" from empty to populated and gives no sense of how many items are coming. There are no skeleton/shimmer placeholders to pre-emptively show the list structure.

## Proposed scope

- Add a skeleton state (`LoadingState` variant or new `ListSkeleton` composable) that renders 4–6 gray placeholder cards shaped like real contact cards (avatar box, title line, subtitle line).
- Show the skeleton while `uiState.content` is `Loadable.Loading` and the list is not yet populated.
- Animate a subtle shimmer or pulse on the placeholder bars to indicate activity.
- Transition smoothly to the real `GroupedContacts` list once `Loadable.Content` arrives (crossfade or simple swap).
- Keep the existing spinner for very short loads if desired, or replace with skeleton entirely.

## Acceptance criteria

- [ ] While the list is loading, 4–6 placeholder cards appear instead of a blank spinner.
- [ ] Placeholder cards mimic the real card layout (avatar, name, subtitle).
- [ ] Placeholders have a subtle shimmer/pulse animation.
- [ ] When loading finishes, placeholders are replaced by actual contacts without layout jump.
- [ ] Error and empty states are unaffected.

## Status

Done.

## Technical notes (implemented)

- `app/src/main/java/com/m4isper/contactsapp/ui/components/States.kt` — new `ListSkeleton(itemCount, modifier)` composable: renders 5 skeleton cards (by default) matching the real `ContactCard` layout (circle avatar + two text lines). A subtle opacity pulse (`animateFloatAsState` + `infiniteRepeatable`) draws attention without being distracting.
- `app/src/main/java/com/m4isper/contactsapp/ui/components/States.kt` — private `SkeletonLine` helper for the name/subtitle placeholder bars.
- `app/src/main/java/com/m4isper/contactsapp/presentation/contacts/components/ContactListBody.kt` — `Loadable.Loading` now renders `ListSkeleton()` instead of `LoadingState()` spinner.

## Verification

- `:app:compileDebugKotlin` passes.
- `:app:detekt` / `:app:ktlintCheck` — no new issues introduced (only 2 pre-existing in DuplicatesScreen/SettingsScreen).
- Skeleton cards match the real card shape (`shapes.medium`) and spacing (`spacing.m` / `spacing.xs`).
- When loading finishes, `ListSkeleton` is replaced by `GroupedContacts` without layout jump.

## Technical notes

- `app/src/main/java/com/m4isper/contactsapp/presentation/contacts/components/ContactListBody.kt` — lines ~40–67 handle `Loadable.Loading` / `Empty` / `Error` / `Content`.
- `app/src/main/java/com/m4isper/contactsapp/ui/components/LoadingState.kt` — existing loading placeholder.
- `app/src/main/java/com/m4isper/contactsapp/presentation/contacts/components/ContactCard.kt` — reference layout for the skeleton shape.
- Compose `animate*` APIs or `rememberUpdatedState` can drive the shimmer.

## Out of scope

- Skeleton states for the editor or details screens (out of scope for this issue).
- Paged skeleton (issue 28 covers paging separately).