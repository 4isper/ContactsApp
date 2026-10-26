# fix: stop the selection app bar from overflowing on phones

**Priority:** P1 — the title disappears and actions become unclickable on common phone widths
**Area:** contacts list / selection mode

## Problem

`SelectionTopBar` puts six `IconButton`s, a close button, and a title into one Material 3 `TopAppBar` (`SelectionTopBar.kt` lines ~43–125). At the standard 48 dp touch target each action consumes 48 dp:

- close button: 48 dp
- favorite, select all, add to group, share, export, delete: 6 × 48 dp = 288 dp

That is 336 dp of fixed content before the title, on a 360 dp-wide phone. The title is measured last and collapses to almost zero width; on a 320 dp device even the action row cannot fit. The result is a bar where the selected count is unreadable or missing, and the last actions are clipped or pushed off-screen.

The actions are also visually indistinguishable in importance: the destructive "Delete" button sits next to "Export", both are icon-only, and "Export" uses a download icon that reads as "download" rather than "export".

## Proposed scope

- Keep the close button, the selected-count title, and at most two primary actions in the bar.
- Move secondary actions into a `MoreVert` overflow menu: add to group, share, export, and delete. Keep the destructive action in the overflow with an error-colored label and the existing confirmation dialog.
- Alternatively, use a bottom action bar on compact widths, but do not rely on horizontal scrolling for the primary selection action.
- Guarantee a minimum width for the title (for example `Modifier.widthIn(min = 96.dp)`), and let the count truncate only as a last resort.
- Keep the "select all / deselect all" action visible because it changes the selection scope; favorite can stay next to it if the width allows.
- Add a visible label or tooltip for the overflow and preserve all current test tags where the action still exists.
- Verify with large font scale and display size, not only the default 360 dp width.

## Acceptance criteria

- [ ] On a 320–360 dp phone at 200% font scale, the selected count is visible and every action is reachable.
- [ ] Delete is not adjacent to a non-destructive action without a visual separator, and still asks for confirmation.
- [ ] All selection actions remain available through the overflow menu.
- [ ] The bar does not clip or overlap with the status bar or navigation bar.
- [ ] Existing selection-mode tests are updated for the new action locations.

## Technical notes

- `app/src/main/java/com/m4isper/contactsapp/presentation/contacts/components/SelectionTopBar.kt` — current six actions.
- `app/src/main/java/com/m4isper/contactsapp/presentation/contacts/components/ContactList.kt` lines ~159–177 — call site and callbacks.
- `app/src/main/java/com/m4isper/contactsapp/presentation/contacts/ContactsViewModel.kt` — selection state and actions; no changes to behavior are expected.
- Consider Material 3 `TopAppBar` with `actions = { ... }` plus a `DropdownMenu`; do not replace it with a custom Row that ignores insets.
- Check `ContactsScreen` announce scaffolding when selection mode starts; the title is part of the feedback loop.

## Out of scope

- Redesigning selection mode itself (long-press to enter, tap to toggle).
- Adding new bulk actions.

## Verification

- Preview/compile at 320 dp, 360 dp, 411 dp, and tablet widths, in portrait and landscape.
- Run the selection-mode UI tests and add a width-constrained screenshot test if the project has screenshot infrastructure.
- `./gradlew :app:compileDebugKotlin :app:testDebugUnitTest`.
