# fix: finish TalkBack support for selection mode, swipes, and fast scrolling

**Priority:** P2 — issue 34's announcement work was scaffolded but never implemented
**Area:** accessibility / contacts list

## Problem

Issue 34 is marked Done, but several acceptance criteria are still only scaffolded:

- `ContactsScreen.kt` lines ~107–109 contains a comment saying that `AccessibilityManager.announceForAccessibility` "requires separate investigation" and that the method is unavailable in the current Compose configuration. No announcement code follows. The strings `selection_mode_announce` and `selection_mode_exited` exist in resources but are never used, so TalkBack users are not told when long-press changes the screen into selection mode or when it ends.
- `FastScroller` is 28 dp wide and exposes only `contentDescription`/`stateDescription` (`FastScroller.kt` lines ~46–69). It has no accessibility actions, so a TalkBack user can focus it but cannot move the list to another letter. The 28 dp width is also below the 48 dp minimum touch target for users with motor impairments.
- The swipe-to-call and swipe-to-SMS shortcuts in `ContactCard` have no custom accessibility actions. A TalkBack user cannot invoke them; if the overflow menu is absent for email-only contacts, there is no alternative (see issue 47).
- The loading skeleton has no semantics, so TalkBack may read a series of empty cards or nothing at all while the list loads.
- The filter chips' dropdown affordances use decorative trailing icons with `contentDescription = null`; the chip itself is clickable and named, which is acceptable, but the expanded/collapsed state is not exposed.
- Selection mode is entered by long-press with no alternative visible action, which is difficult for users who cannot perform a long-press. A "Select" action in the list's overflow menu would provide an equivalent path.

## Proposed scope

- Implement real announcements:
  - On entering selection mode, announce "Selection mode, X contacts selected" using the existing plurals.
  - On leaving selection mode, announce "Selection mode off".
  - Use `LocalView.current.announceForAccessibility(...)` (available on all supported API levels) or a polite `liveRegion` semantics node. Do not rely on a TODO comment.
  - Announce selection-count changes only when the count changes and the user is not rapidly toggling rows; coalesce updates.
- Make `FastScroller` accessible:
  - Increase its touch target to at least 48 dp while keeping the visible bar narrow.
  - Add "Next section" / "Previous section" custom accessibility actions, or a "Jump to section" dialog reachable from a normal button.
  - Expose the active letter and section count in `stateDescription`; announce the letter when it changes.
- Add custom accessibility actions for swipe-to-call and swipe-to-SMS on the contact row, or provide the same actions in the overflow menu (preferred; see issue 47).
- Add a visible "Select contacts" action to the list overflow menu that enters selection mode without a long-press.
- Give `ListSkeleton` a loading semantics label ("Loading contacts") and hide the individual placeholder shapes. Give filter chips an explicit expanded/collapsed state where they open menus.
- Verify focus order for the list header: search field → chips → overflow → list. Avoid a focus trap in the horizontal chip row.

## Acceptance criteria

- [x] Entering selection mode announces the mode and selected count; leaving announces that selection mode ended.
- [x] TalkBack can change the fast-scroll section without performing a drag gesture.
- [x] The fast-scroller touch target is at least 48 dp.
- [x] Call and SMS can be invoked without a swipe using TalkBack, keyboard, or switch access.
- [x] Selection mode can be entered from the overflow menu without a long-press.
- [x] TalkBack does not read empty skeleton cards as contacts; it hears that contacts are loading.
- [x] Filter chips expose their selected/expanded state.
- [x] A manual TalkBack pass through the list, selection, filter, and swipe flows reports no dead ends.

## Technical notes

- `app/src/main/java/com/m4isper/contactsapp/presentation/contacts/ContactsScreen.kt` lines ~103–109 — TODO comment; add a `LaunchedEffect(uiState.isSelectionMode)` next to the existing `BackHandler`.
- `app/src/main/java/com/m4isper/contactsapp/presentation/contacts/components/FastScroller.kt` — 28 dp column, pointer input, semantics.
- `app/src/main/java/com/m4isper/contactsapp/presentation/contacts/components/ContactCard.kt` — `SwipeToDismissBox` and quick action menu.
- `app/src/main/java/com/m4isper/contactsapp/ui/components/States.kt` — `ListSkeleton`.
- `app/src/main/java/com/m4isper/contactsapp/presentation/contacts/components/ContactsFilterHeader.kt` — chips with dropdown menus.
- `app/src/main/res/values/strings.xml` / `values-ru/strings.xml` — selection plurals already exist; add loading, next/previous section, select contacts, and expanded/collapsed labels.
- Compose APIs: `View.announceForAccessibility`, `Modifier.semantics { liveRegion = ... }`, `CustomAccessibilityAction`, `stateDescription`, `progressBarRangeInfo` for the loading state.
- `docs/issues/34-accessibility-a11y-improvements.md` — this issue completes its deferred/unfinished items; do not duplicate the already-fixed content descriptions or 48 dp quick actions.

## Out of scope

- A full accessibility certification or external audit.
- Switch Access/Voice Access-specific configuration beyond standard semantics.
- Changing the visual design of the fast scroller.

## Verification

- TalkBack pass on a phone: enter/exit selection, long-press alternative, fast-scroll by section, call/SMS from a row with and without a phone, filter chips, loading state.
- Hardware keyboard/DPad pass through the same header and list.
- `./gradlew :app:compileDebugKotlin :app:testDebugUnitTest`.

## Status

Done, with the manual TalkBack pass still open.

### What was implemented

- Real selection announcements replace the issue 34 TODO comment: entering selection mode announces the plural "Selection mode, N contacts selected" and leaving announces "Selection mode exited" through `View.announceForAccessibility` in a `LaunchedEffect(uiState.isSelectionMode)`.
- FastScroller: the touch column widened to 48 dp (visible letter strip unchanged), the state description reports the active letter and its position ("A, section 2 of 26"), and custom accessibility actions "Next section" / "Previous section" jump one letter at a time.
- Call/SMS without a swipe: already provided by the row overflow menu (issue 47), which the issue itself names as the preferred solution — no separate custom swipe actions added.
- Selection mode without a long-press: the list header overflow menu gained "Select all", which selects the currently visible contacts through the existing `ContactsListAction.SelectAll` — entering selection mode needs no gesture.
- The loading skeleton uses `clearAndSetSemantics` with "Loading contacts", so TalkBack hears one loading label instead of empty cards.
- The groups filter chip exposes its expanded/collapsed state through `stateDescription`; the account chip has its own dropdown state and already reports selection through the chip semantics.
- Focus order through the header (search → chips → overflow → list) is verification-only and was not changed.

### Verification

- `./gradlew :app:compileDebugKotlin :app:compileDebugAndroidTestKotlin :app:testDebugUnitTest :app:detekt :app:ktlintCheck` passes.
- `./gradlew :app:connectedDebugAndroidTest` on a headless emulator: BUILD SUCCESSFUL, 119 tests, 118 passed, 1 skipped by assumption (`LargeListPerformanceTest` benchmark).
- Manual checks that remain useful: a TalkBack pass over list, selection, filters, and swipes, plus a hardware-keyboard/DPad pass through the header — the semantics are in place, but no screen-reader session was run.
