# fix: reclaim home header vertical space by merging the app bar and the search row

**Priority:** P2 — before the first contact row the home screen stacks ~175–200 dp of fixed chrome: the app-name app bar, the search row, and the filter chips. That is roughly 3 list rows or ~25% of a compact phone viewport
**Area:** contacts list / home chrome

## Problem

Issue 56 introduced a real home top app bar alongside the search field. Individually both are standard M3 elements, but on the home screen they are stacked permanently and duplicate each other's role:

Measured idle-state stack (no search, no selection), `ContactsScreen` → `ContactList` → `ContactListTopBar` + `FilterHeader`:

1. `HomeTopBar` (`presentation/contacts/components/HomeTopBar.kt`) — M3 `TopAppBar`, small height: 64 dp + the status-bar inset (`MainActivity` is edge-to-edge, ~24–40 dp depending on device). It holds only the static brand row (contacts icon + app name) as the title, plus Settings and one overflow (`MoreVert`) as actions.
2. `ContactsSearchField` (`presentation/contacts/components/ContactsSearchField.kt`) — `SearchBarDefaults.InputField`, collapsed spec height 56 dp + vertical padding `spacing.s` (8+8 dp) → ~72 dp.
3. The filter-chips `FlowRow` in `ContactsFilterHeader.kt` → ~40 dp.

The cheapest win is item 1: the title row carries no information the user does not already have (the app name is static and the screen is self-evident), and in search mode the top bar already disappears in favor of the field — so a dedicated 64 dp row exists only for the idle state. Google's own Contacts app has no separate title row: the search surface is the top bar and the action icons live in the same row.

The bar also has no `scrollBehavior`, so while scrolling the app name eats exactly one row of the list forever. The chips row is real estate too, but it is deliberately out of scope here (see below).

## Proposed scope

- In the idle state render a single fixed bar row instead of two: the collapsed `SearchBarDefaults.InputField` becomes the top row, with Settings and the `MoreVert` overflow moved to trailing icons inside that same row. The overflow keeps its current contents (Recents, Duplicates, Groups, My profile, Import/Export, Select all) and the same `DropdownMenu` composition — only its anchor moves.
- Drop the dedicated app-name/title row from the home idle state. The brand stays where it still has a role (launch theme, Settings); this partially supersedes issue 56's "home has a top app bar with the app name/brand" — that criterion is reworked, not reverted.
- Search semantics untouched: focus expands into search mode, system Back / leading arrow closes it, the query and filters survive; `ContactsUiState.isSearchMode` wiring stays as is.
- Selection mode is untouched: `SelectionTopBar` keeps its own `TopAppBar`.
- Keep the `search_field` and `home_top_bar` test tags working with the new composition (re-tag the merged row if needed) so existing instrumented tests keep their meaning.

## Acceptance criteria

- [ ] In the idle state (no search, no selection) the home screen has exactly one fixed bar row above the filter-chip row; there is no separate app-name/top-app-bar row.
- [ ] Settings and every overflow destination (Recents, Duplicates, Groups, My profile, Import, Export, Select all) remain reachable from the idle home state — no entry point is lost.
- [ ] Search behavior is unchanged: focus expands, Back closes search before leaving the screen, clearing/query/filter state survives mode switches; existing search-mode tests pass (updated for re-tagging, not rewritten semantics).
- [ ] Selection mode keeps the current `SelectionTopBar` layout.
- [ ] The merged row handles edge-to-edge insets itself (no content under the status bar after `TopAppBar` is gone).
- [ ] The first visible contact row sits at least ~64 dp higher than before, verified with before/after screenshots.
- [ ] At 320 dp width and 2.0 font scale the merged row shows the field and both trailing actions without clipping or overlap.
- [ ] TalkBack: Settings and the overflow keep descriptive labels; the merged row reads sensibly (no unlabeled decoration icons).
- [ ] `./gradlew :app:compileDebugKotlin :app:compileDebugAndroidTestKotlin :app:testDebugUnitTest :app:detekt :app:ktlintCheck` passes; affected instrumented tests are green on the emulator.

## Technical notes

- The switch lives in `ContactListTopBar` (`ContactList.kt`, ~lines 310–360) and the `Scaffold` `topBar` slot (~lines 185–210). The merged row replaces the `HomeTopBar` branch in the `when`; `SelectionTopBar` branch stays as is.
- Two viable compositions: (a) keep `SearchBarDefaults.InputField` as the row and pass trailing `IconButton`s into it; (b) keep a `TopAppBar`-height custom `Row` with the input field and actions as children. Pick whichever preserves ~56–64 dp row height and 48 dp touch targets for the two actions. Option (a) reuses the existing focus/expansion plumbing unchanged.
- Insets: `TopAppBar` applies `TopAppBarDefaults.windowInsets` itself; a plain `Row`/`InputField` will not. The merged row needs `statusBarsPadding()` or `windowInsetsPadding(WindowInsets.statusBars)` explicitly, since `MainActivity` runs `enableEdgeToEdge()`.
- `SearchField` (`ui/components/SearchField.kt`, the outlined field) is not part of this change — it is used on other screens; only `ContactsSearchField.kt` (the M3 `InputField` wrapper) is involved.
- Strings: all labels exist (`R.string.settings_menu`, `R.string.action_more`, and the overflow items); no new resources expected in `values` or `values-ru`.
- Tests: `ContactListUiTest.searchModeClosesBeforeLeavingTheScreen` (~lines 287–301) asserts `home_top_bar` does not exist in search mode and is displayed afterwards. Keeping the `home_top_bar` tag on the merged idle row makes this test pass without semantics changes; decide between that and retagging. Group-detail and picker tests drive `search_field` by tag — keep that tag stable.

## Out of scope

- The filter-chips `FlowRow` stays exactly as it is; it is measured above but not modified in this issue.
- No collapsing-on-scroll behavior for the remaining single row (`enterAlwaysScrollBehavior` etc.) — that is a separate follow-up in issue 63, and its viable implementation approach (M3 app bar vs. custom offset) depends on the composition choice recorded here.
- No changes to search semantics, contact rows, or the adaptive rail/list-detail shell.

## Verification

- Manual: home → focus search → Back; search → filter → open detail → back; selection mode in/out; 320/600/840 dp widths and 2.0 font scale; TalkBack over Settings and the overflow.
- Screenshots before/after at compact, medium, and expanded widths for the height comparison.
- `./gradlew :app:compileDebugKotlin :app:compileDebugAndroidTestKotlin :app:testDebugUnitTest :app:detekt :app:ktlintCheck`.
- Instrumented: `ContactListUiTest` (search-mode back behavior and any re-tagged assertions) plus the standard connected suite.

## Status

Open
