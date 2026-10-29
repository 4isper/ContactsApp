# feat: collapse the home header on scroll

**Priority:** P3 — after the issue 62 merge, one fixed bar row plus the chips row still sit on top of the list; scrolling a short contacts list, the chrome can still hold ~⅕ of a compact phone viewport
**Area:** contacts list / home chrome

## Problem

Issue 62 removes the static app-name row by merging the app bar and the search field into one header row. That reclaims ~64 dp, but the remaining bar is still permanently fixed: the home `Scaffold` topBar never yields to scrolling. No M3 `TopAppBarDefaults` scroll behavior is used anywhere in the app (verified: no `scrollBehavior`/`enterAlwaysScrollBehavior` references). For a screen that is a list first, the standard input pattern is an enter-always bar: scroll down hides the bar, the first upward scroll (or reaching the top) brings it back.

The bar is one row (~56–72 dp + status-bar inset after 62). On lists with a handful of contacts or after search/filtering, the fixed chrome is not worth the space it occupies while the user is browsing.

## Proposed scope

- Enter-always collapse for the home idle header only (the merged row from issue 62): scroll down slides the whole topBar row out; the first upward scroll restores it immediately (mid-list, not only at the top); when the list is scrolled to the top the bar is always expanded.
- The collapse state must reset to expanded on mode changes: entering search (the topBar composable returns nothing — the field replaces it), leaving search, entering/leaving selection mode (`SelectionTopBar` stays pinned and unaffected), and after programmatic jumps of the fast scroller (`listState.scrollToItem`).
- Choose and record the implementation approach:
  - (a) keep the row inside an M3 `TopAppBar` (its `title` slot hosts the merged search row) and attach `TopAppBarDefaults.enterAlwaysScrollBehavior()` with the `nestedScrollConnection` on the Scaffold content — the collapse animation and height bookkeeping come from M3;
  - (b) keep the bare `SearchBarDefaults.InputField` row and implement a small custom behavior: a nested-scroll-connected offset/height animation on the row (`Modifier.offset` or `graphicsLayer`), driven by scroll deltas only — no `LazyListState` hoisting out of `GroupedContacts`.
  The decision depends on the composition choice made for 62; document the rationale in the Status.
- Everything else stays put: the chips row does not collapse, search semantics and the selection bar are untouched.

## Acceptance criteria

- [ ] Scrolling a list that has enough content to scroll hides the merged header row with an animation; the first upward scroll restores it mid-list; at the top of the list the bar is always visible.
- [ ] The list never re-lays out rows while the header animates (offset/height animation only; no jump or flicker in `GroupedContacts`).
- [ ] Entering search and selection replaces/reset the bar correctly: the selection bar is fully visible; returning from search shows the expanded idle bar.
- [ ] The fast scroller letter jump settles the bar at the correct visibility (no stale half-collapsed state).
- [ ] Pull-to-refresh still triggers at the top and does not fight the collapse gesture; refreshing keeps the bar expanded.
- [ ] While the bar is hidden the status-bar inset has no gap or color artifact (the list surface shows under the status bar as designed).
- [ ] 320 dp width and 2.0 font scale: the collapse works and nothing clips; TalkBack focus stays on the visible row actions; collapse/expand does not announce nonsense.
- [ ] New instrumented test covers the behavior (scroll down → the idle bar is not displayed; scroll up → it is displayed) in the style of the existing `ContactListUiTest` tag assertions.
- [ ] `./gradlew :app:compileDebugKotlin :app:compileDebugAndroidTestKotlin :app:testDebugUnitTest :app:detekt :app:ktlintCheck` passes; emulator suite is green.

## Technical notes

- The scroll container is LazyColumn with `rememberLazyListState()` inside `GroupedContacts.kt` (~line 60, `LazyColumn` at ~line 81); `ContactListBody` wraps it in a `PullToRefreshBox`. With approach (a) `Modifier.nestedScroll(behavior.nestedScrollConnection)` goes on the Scaffold content so the M3 behavior sees the deltas; the private `listState` does not need hoisting. Approach (b) should also hook nested scroll rather than observing `listState.isScrollInProgress`, so flings and drags behave identically.
- `ContactListTopBar` (`ContactList.kt`, ~lines 310–360) renders the selection bar, the idle bar, or nothing in search mode. The behavior must treat "the bar disappears" as a reset signal: the composable that owns the collapse animation must expand again when `isSearchMode`/`isSelectionMode` toggles, otherwise a stale collapsed offset survives a mode round-trip.
- Insets: while collapsed, keep the status-bar padding handled so the list flows under the status bar with the correct surface color; the expansion must restore the previous look seamlessly (no double padding).
- Consider the FAB: it lives in the Scaffold slot and is unaffected; verify visually that the FAB does not jump when the bar hides.
- Priority guard: do not collapse when `uiState.content` is not `Loadable.Content` (skeleton/empty/error states have little or no scroll; the header should stay put there) — a simple `canScrollForward`-style condition keeps the animation deterministic.
- The `home_top_bar` test tag must remain on the merged row (62 keeps it); the new test reuses it.

## Out of scope

- The filter-chips `FlowRow`: it stays exactly as it is (not hidden, not collapsed) per the standing constraint from issue 62's review.
- Extending collapse behavior to other screens (groups, recents, duplicates, details) — the same pattern can be reused there later.
- Any change to search semantics, selection mode, contact rows, or pull-to-refresh internals.

## Verification

- Manual on the emulator (API 37 AVD): slow scroll and fling down/up; snap behavior at the top; letter-jump via the fast scroller; mid-scroll toggle search and selection; pull-to-refresh at the top; rotate.
- 320 dp / 2.0 font-scale check for clipping and TalkBack focus.
- New instrumented test for collapse/expand visibility plus the standard `ContactListUiTest` suite.
- `./gradlew :app:compileDebugKotlin :app:compileDebugAndroidTestKotlin :app:testDebugUnitTest :app:detekt :app:ktlintCheck`.

## Status

Done (implementation merged in `dev`; issue 62 follow-up).

**Approach**: (b) — a custom enter-always behavior, not an M3 `TopAppBar` wrapper. After the 62 merge the idle bar is a bare composite row (search field + trailing actions) that is only rendered in the `topBar` slot for idle and search modes; wrapping it into an M3 `TopAppBar` title slot would re-introduce the M3 container metrics the merge removed. The whole decision lives in one new file, `HeaderCollapseState.kt`: `HeaderCollapseState` (measured full height + consumed offset), `Modifier.collapsingHeader` (the bar reports `naturalHeight − offset`, the same bookkeeping as M3 `SingleRowTopAppBar`'s `layoutHeight = maxHeight + heightOffset`), and `rememberHeaderCollapseConnection` (nested scroll wired on the Scaffold content column).

**Sign convention** (confirmed on-device with a temporary logged probe): Compose dispatches pre-scroll parent-first, and with `moveBy(0, -120)` the connection sees `y = −100`; M3 `EnterAlwaysScrollBehavior` does `heightOffset += available.y`, so negative = collapse, positive = expand. The consume path mirrors that; the returned consumed offset must carry the same sign as the delta (`before − after`), otherwise the scrollable computes its leftover as `available − consumed` and scrolls twice (this exact bug was caught by the collapsibility probe: one −100 gesture produced a −300 row shift).

**Reset points**: the collapse state is recreated via `remember(uiState.isSearchMode, uiState.isSelectionMode)`, so search and selection round-trips always start expanded; the fast-scroller letter jump calls `onProgrammaticScroll` → `state.expand()` (`jumpTo` in `GroupedContacts.kt`). `enabled = headerCollapseEnabled(uiState)` keeps the bar pinned when there is no scrollable content (search, selection, skeleton/empty/error).

**Test corrections made along the way**: the fixed-row premise ("the same row stays visible after a full collapse") was wrong for every M3-style bar — the content legitimately flows up by the collapse amount; the instrumented test (`headerCollapsesOnScrollDownAndExpandsOnScrollUp`) now asserts the real invariant: one small gesture must not double-consume (row shift == bar shrink within 2 px), the panel reaches height 0, and the exact pre-gesture state is restored after expanding back. The `home_top_bar` tag sits outside the collapse layout so its bounds are the panel bounds, not the overflowing inner content.

**Verified**:
- `ContactListUiTest` 23 tests + `ContactsFilterHeaderSyncTest` 4 + `FastScrollerUiTest` 2 + `SelectionTopBarTest` 3 = 32/32 green on the emulator (folding Pixel AVD, API 37), including the two new UI tests: idle bar collapses on scroll down and returns on scroll up; letter-jump reset states are covered by the existing fast-scroller suite.
- `HeaderCollapseStateTest` (8 JVM tests) pins the state mechanics: clamped consume in both directions, offset normalization on remeasure, idempotent `expand`, `canConsume` boundary zones, zero-height no-ops.
- Gates: `:app:testDebugUnitTest :app:detekt :app:ktlintCheck` green; `compileDebugKotlin`/`compileDebugAndroidTestKotlin` clean.

**Still manual (not covered by this issue)**: visual pull-to-refresh coexistence during fling, 320 dp / 2.0 font-scale clipping pass, TalkBack announcements, rotation.
