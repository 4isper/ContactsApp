# feat: modernize the home chrome with a search bar, real app bar, and adaptive navigation

**Priority:** P2 — search occupies a permanent 72 dp band, eight destinations are hidden in one overflow menu, and tablets get a stretched phone layout
**Area:** contacts list / navigation / adaptive shell

## Problem

The home screen is a `Column` with a permanently visible outlined search field, a horizontally scrolling `FilterChip` row, and an overflow menu that hides most of the app's destinations:

- `presentation/contacts/components/ContactsFilterHeader.kt` renders `SearchField` (`ui/components/SearchField.kt`, an `OutlinedTextField` with a floating "Search" label — a dated pattern) above chips for Favorites/Groups/Accounts plus a "More" menu containing select-all, recents, import, export, profile, duplicates, groups management, and settings. That menu is a junk drawer: it mixes navigation, bulk actions, and file IO.
- There is no top app bar on the home screen. The screen has no title, no brand presence, and no place for "Settings" other than the overflow.
- Groups and accounts appear twice: as filter chips and again inside the overflow. Active filters are not summarized; on small screens the chip row scrolls, hiding the fact that a filter is applied.
- `presentation/navigation/ContactsNavHost.kt` applies `ListDetailSceneStrategy` only to Contacts↔ContactDetails. Every other destination (Groups, Settings, Editor, Duplicates, Recents, Import) opens full-screen even on expanded widths, and there is no navigation rail/suite.
- Search state survives navigation inconsistently: there is no dedicated "search mode" with back-to-close behavior; back exits the screen with the field focused.

## Proposed scope

- Replace the permanent outlined field with an M3 `SearchBar` (or `DockedSearchBar`) that expands on focus, shows the query and results, and collapses to a single-line surface when idle. Back closes search before it leaves the screen; clearing the query restores the browse state.
- Add a home top app bar: app title (brand mark + name), primary actions (Settings, maybe "select all"), and keep the FAB for "add". Move navigation entries (Recents, Duplicates, Groups, Profile) out of the junk drawer into the app bar, a nav drawer/sheet, or the adaptive navigation; keep file IO (import/export) in one clearly labelled place.
- Filters: keep chips only for the *active* filter set, with a leading "Filters" entry that opens a bottom sheet for group/account selection; do not duplicate the same selector in two places.
- Adaptive shell: introduce a `NavigationSuiteScaffold`/nav rail for expanded widths with top-level destinations, keeping list-detail for Contacts↔Details (and extending it to Groups↔Group details if that lands). The rail must not appear in compact width.
- Predictive back: back from a detail pane returns to the list; back with search open closes search; back from home leaves the app.

## Acceptance criteria

- [x] Home has a top app bar with the app name/brand; no destination is reachable only through an unlabeled overflow.
- [x] Search expands on focus and collapses when idle; back closes search first; clearing restores the browse state with filters intact.
- [x] Active filters are always visible (removable chips or a summary) even when the chip row is scrolled.
- [x] At ≥ 600 dp width the top-level destinations are reachable without opening a drawer/menu, and no phone-only full-screen navigation appears for a destination that has a natural two-pane layout.
- [x] The home screen at 320 dp width and 2.0 font scale has no clipped or overlapping app-bar actions.
- [x] Existing tests for search/filter identity and navigation actions still pass; new tests cover search-mode back behavior and rail navigation.

## Technical notes

- Shared tokens and rules: `docs/design/visual-rules.md` (issue 54); use it instead of local values.
- `app/src/main/java/com/m4isper/contactsapp/presentation/contacts/components/ContactsFilterHeader.kt` — current header composition and the overflow menu items.
- `app/src/main/java/com/m4isper/contactsapp/ui/components/SearchField.kt` — the outlined field to replace; it is used elsewhere, so keep or delete deliberately.
- `app/src/main/java/com/m4isper/contactsapp/presentation/contacts/components/ContactList.kt` — `Scaffold` `topBar` slot (lines 180–198) currently only holds the selection bar; the FAB is at lines 199–211.
- `app/src/main/java/com/m4isper/contactsapp/presentation/navigation/ContactsNavHost.kt` — scene strategies; add `SupportingPaneSceneStrategy`/`NavigationSuiteScaffold` here, not per screen.
- `currentWindowAdaptiveInfoV2` / `calculatePaneScaffoldDirective` are already used for list-detail; reuse them for the suite decision instead of `screenWidthDp` checks scattered in composables.
- Strings live in `values/strings.xml` and `values-ru/strings.xml`; the overflow entries already have string resources — reuse them when relocating.
- Keep `select all` available in selection mode (it already exists in `SelectionTopBar`); do not lose the entry point when the overflow is dismantled.

## Out of scope

- Changing search semantics (fields searched, matching rules).
- A full navigation rewrite away from Navigation 3.
- Redesigning the contact rows themselves (issue 57).

## Verification

- Manual: compass — search → filter → open detail → back; rotate; 320 dp and 840 dp widths; TalkBack through the app bar, search expansion, and rail.
- `./gradlew :app:compileDebugKotlin :app:compileDebugAndroidTestKotlin :app:testDebugUnitTest :app:detekt :app:ktlintCheck`.
- Screenshots at compact/medium/expanded widths for the before/after comparison.

## Status

Done; the 320 dp / 2.0 font-scale check is verified, wider-width screenshots and TalkBack remain manual.

### What was implemented

- `presentation/contacts/components/HomeTopBar.kt` is a Material3 `TopAppBar`: the brand (contacts icon + app name, ellipsized) plus Settings as its own action and an overflow grouped by dividers into navigation (Recents, Duplicates, Groups, My profile), file IO (Import, Export), and Select all. Every item has a text label.
- `presentation/contacts/components/ContactsSearchField.kt` wraps `SearchBarDefaults.InputField`: focus expands it, the app bar yields to the field, the leading arrow or system Back closes search, and the trailing clear button empties the query. Search mode lives in `ContactsFilterState`/`ContactsUiState.isSearchMode`, so it survives refresh and navigation; `BackHandler` in `ContactList` closes search before the screen and stands down while a selection is active.
- `ContactsFilterHeader` puts the Favorites, Groups, and Account chips in a `FlowRow` under the search field. They wrap instead of scrolling, so an applied filter is never scrolled out of view.
- `presentation/navigation/TopLevelDestination.kt` and `HomeNavigationRail` in `ContactsNavHost` show the rail at ≥ 600 dp (`WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND`) on top-level entries only. Selecting a section collapses its stack to the root, so Back from a section returns to Contacts.
- `action_close_search` was added in `values/strings.xml` and `values-ru/strings.xml`.

### Deviations from the plan

- Search is a hoisted mode over the stateless `InputField`, not the state-based `SearchBar` + `ExpandedFullScreenSearchBar` dialog. The dialog owns the results list, so the screen would have to render the list twice, and it could not be verified without an emulator; the stateless field still gives the M3 surface, focus-driven expansion, and automatic focus clearing on collapse.
- At compact width, navigation destinations remain in an overflow menu — now a labelled, sectioned app-bar menu rather than a junk drawer inside the filter row. No drawer or sheet was added; the rail covers ≥ 600 dp and the overflow items are all labelled.
- Filters stay as chips (always visible in a `FlowRow`) rather than a "Filters" bottom sheet. The Groups chip filters the list, while the Groups destination manages groups, so the two entry points do different jobs; a bottom sheet can follow if the duplication becomes confusing.
- The rail is hidden on detail, editor, import-preview, and group-detail screens so list-detail and the editor keep the full width.

### Verification

- `./gradlew :app:compileDebugKotlin :app:compileDebugAndroidTestKotlin :app:testDebugUnitTest :app:detekt :app:ktlintCheck :app:lintDebug` passes.
- New JVM tests: `ContactsFilterStateTest` (search mode open/close/clear) and `TopLevelNavigationTest` (destination mapping, rail visibility, stack collapse).
- New instrumented tests: `searchModeClosesBeforeLeavingTheScreen` in `ContactListUiTest` and `HomeNavigationRailTest`; both pass on the emulator.
- The full instrumented suite passes on the `Pixel_10_Pro_Fold` AVD (API 37): `:app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.notClass=com.m4isper.contactsapp.data.LargeListPerformanceTest` → 127 tests in 23 classes, 0 failures. The perf test is excluded because it is gated behind `contacts.perf=true` and would report an assumption violation as a failure. The emulator must run with a window: headless mode leaves the test window unfocused and `Espresso.closeSoftKeyboard()` fails across `ContactEditorUiTest`.
- `:app:lintDebug` was red on `HEAD` before this issue: the Glance widget read strings through `LocalContext` inside composition. Fixed separately in `aadf6f7` by resolving the strings in `provideGlance`.
- Manual 320 dp / 2.0 font-scale check on the `Pixel_10_Pro_Fold` AVD (`wm size 720x1560`, `wm density 360` → 320 dp; `font_scale 2.0`): the top bar keeps the brand, Settings, and overflow with no clipping or overlap; the overflow opens with all six items readable; search mode replaces the bar with the field and its back arrow without clipping; the first Back dismisses the IME and the second closes search, restoring the bar; the filter chips wrap to three rows and stay visible. Captured locally, not committed.
- Still manual: medium/expanded screenshots and TalkBack over the app bar, search expansion, and rail.
