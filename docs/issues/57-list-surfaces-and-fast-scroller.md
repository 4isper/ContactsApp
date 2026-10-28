# fix: rework contact-list surfaces and replace the iOS-style fast scroller

**Priority:** P2 — the list reads as a stack of floating cards, and the width-dependent quick-action rule from issue 52 is currently dead
**Area:** contacts list / row visuals / fast scroll

## Problem

- Every contact is its own `Card` on `surfaceContainerLow` with 12 dp gaps (`presentation/contacts/components/ContactCard.kt`, `GroupedContacts.kt`). A screen shows 10–15 disconnected rounded rectangles; in the light theme `surface` and `surfaceContainerLow` differ by only a few percent (`ui/theme/Color.kt`), so the cards read as faint outlines and the list has no single reading surface. Sticky letter headers compete with the cards for weight.
- `presentation/contacts/components/FastScroller.kt` draws a 10 sp thumb label (line 89) — an iOS-style scrubber — instead of the Android pattern (a 48 dp touch target with a letter bubble following the thumb). The separate giant letter overlay uses `displayLarge` inside a primary circle (`GroupedContacts.kt:120`), which is oversized, off-brand, and overlaps the list.
- Regression: `ContactList.kt` computes `showFavoriteQuickAction` (lines 106–110) from `favoriteQuickActionVisible(...)` (width + font scale) but never passes it to `ContactListBody(...)` (line 245). The parameter defaults to `true` (`ContactListBody.kt:44`), so the width-collapse behavior introduced by issue 52 never runs and both the favorite star and the overflow stay visible on narrow screens, crowding the name.
- Favorite state is rendered with `primary` in the list (`ContactCard.kt`) but `tertiary` in the details top bar (`DetailsTopBar.kt`), so the same state looks like two different things.

## Proposed scope

- Choose one list surface model and apply it everywhere: a single `surface` reading surface with sticky headers and `HorizontalDivider` or 8 dp spacing between rows, using tonal fill/outline only for pressed/selected/dragged/favorite rows. Remove the per-row card elevation. The same treatment must hold for GroupedContacts and any favorites section.
- Sticky letter headers: `labelLarge`, 32–36 dp height, on `surface` (no card), with a top divider only when scrolled.
- Fast scroller: replace with the standard thumb + letter bubble; keep a 48 dp touch target, drag-to-jump, and TalkBack custom actions for "jump to letter" (extend issue 48's work rather than replacing it). Delete the standalone `displayLarge` overlay or show it only during an active drag.
- Wire `showFavoriteQuickAction` through `ContactListBody` → `GroupedContacts` → `ContactCard` and add a regression test at 320 dp and font scale 2.0 (the existing `favoriteQuickActionVisible` unit tests do not cover the wiring).
- Unify favorite rendering: pick `tertiary` (amber) for the star in both list and details, and use `primary` only for selection.
- Keep row height predictable: at default font scale a row should stay ≤ 72 dp; phone numbers remain single-line with ellipsis.

## Acceptance criteria

- [x] The contact list has one reading surface with no per-row card elevation; selected/favorite/pressed states are still clearly distinguishable in light and dark.
- [x] Sticky headers remain legible and do not overlap the fast scroller or the FAB.
- [x] The fast scroller has a ≥ 48 dp touch target, follows the thumb, and is operable with TalkBack.
- [x] Passing the computed `showFavoriteQuickAction` actually hides the star on narrow widths; a UI test at 320 dp + 2.0 font scale proves the star is absent and the name is not truncated earlier than necessary at 411 dp.
- [x] The favorite star uses one accent in the list and in details.
- [x] No visual regression for selection mode, swipe actions, or grouped sections.

## Decisions

- Rows are full-bleed on `surface`; the divider is drawn *inside* the row (1 dp `outlineVariant`, start-inset to the text) so the LazyColumn item count does not change and `letterIndices` stays valid. `ContactCard` owns the divider through `showDivider`, so Recents gets the same treatment as the main list.
- Selected rows use the `secondaryContainer` tonal fill and the avatar checkbox; `primary` is reserved for selection, `tertiary` for favorite (same as `DetailsTopBar`).
- Sticky header: `labelLarge`, `m`/`s` padding (36 dp at font scale 1.0) on `surface`, aligned with the avatar's leading edge. A divider above the header was dropped: it would read as a second seam under the row divider.
- Scroller: a 32 × 4 dp thumb on a 48 dp rail. `thumbTop(progress, trackHeight, thumbHeight)` is the only geometry function and is unit-tested. The letter bubble is centered on the thumb and exists only while a finger is down. TalkBack gets "Next/Previous section" actions keyed to the section at the top (`currentLetter`), so repeated actions walk forward instead of always jumping to the first letter; a stale bubble cannot stay on screen after release.
- `ROW_FIXED_WIDTH_DP` is corrected from 204 to 228: 180 dp of row chrome after flattening plus the 48 dp scroller rail, which the old constant never counted. At 360 dp / 1.0 the star now collapses (132 dp < 136 dp) and at 411 dp it stays; the rule finally matches the real column budget.
- `rememberShowFavoriteQuickAction()` is the single place that reads window size and font scale; both `ContactList` and `RecentsScreen` use it, and the masking defaults on the pass-through parameters are gone.
- `ListSkeleton` was flattened to the same row rhythm so loading does not render a stack of cards before the flat list appears.

## Technical notes

- Shared tokens and rules: `docs/design/visual-rules.md` (issue 54); use it instead of local values.
- `app/src/main/java/com/m4isper/contactsapp/presentation/contacts/components/ContactList.kt:106` — computed value; line 245 — the call that drops it.
- `app/src/main/java/com/m4isper/contactsapp/presentation/contacts/components/ContactListBody.kt:44` — `showFavoriteQuickAction: Boolean = true` default that masks the regression.
- `app/src/main/java/com/m4isper/contactsapp/presentation/contacts/components/GroupedContacts.kt:40`/`149` — pass-through parameters.
- `app/src/main/java/com/m4isper/contactsapp/presentation/contacts/components/ContactCard.kt:75`/`286` — star visibility; `favoriteQuickActionVisible` at line 529 is already unit-tested.
- `app/src/main/java/com/m4isper/contactsapp/presentation/contacts/components/FastScroller.kt:89` — 10 sp label; `GroupedContacts.kt:120` — `displayLarge` overlay.
- `app/src/main/java/com/m4isper/contactsapp/presentation/details/DetailsTopBar.kt` — favorite tint (`tertiary`); align with the list.
- Existing screenshot/regression tests live under `app/src/androidTest`; follow the test-tag conventions (`contact_row_*`, `fast_scroller`).

## Out of scope

- Changing the list content model (grouping, sorting) or paging.
- The search/filter chrome (issue 56).
- New list features such as pinned sections beyond the existing favorites.

## Verification

- `./gradlew :app:compileDebugKotlin :app:compileDebugAndroidTestKotlin :app:testDebugUnitTest :app:detekt :app:ktlintCheck` — green on the final build (the first instrumented run had used an APK from before the divider refactor, so everything was rebuilt and re-run).
- `./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.notClass=com.m4isper.contactsapp.data.LargeListPerformanceTest` on Pixel_10_Pro_Fold (API 37) — 131 tests, 129 passed, 2 failed. Both failures are `FavoritesStarredTest` (`favorite_is_written_to_the_provider`, `removing_favorite_clears_the_provider_row`): the AVD already carries a starred contact left by an earlier manual pass (id 263), while those assertions compare against the provider's global starred set. Every suite touched by this issue is green: `ContactListUiTest` 21/21, `FastScrollerUiTest` 2/2, `RecentsUiTest` 2/2, `ContactsEmptyStateUiTest` 7/7, `SelectionTopBarTest` 3/3, `ContactsFilterHeaderSyncTest` 4/4.
- New/updated tests: `FavoriteQuickActionVisibilityTest` (six JVM cases, including the 228 dp chrome boundary), `FastScrollerGeometryTest` (`thumbTop` and `letterIndexAt`), `ContactListUiTest.narrowWindowAtLargeFontScaleDropsTheFavoriteStar`, `ContactListUiTest.phoneWidthAtNormalFontScaleKeepsTheFavoriteStar`, and `FastScrollerUiTest` (bubble lifecycle plus the TalkBack section walk).
- Manual matrix on the emulator: 411 dp light/dark (flat rows, dividers only inside a section, filled tertiary star, muted thumb on the rail), 320 dp / 2.0 (star absent, overflow stays, two-line name), 600 dp (rail plus flat list), drag on the rail (bubble follows the thumb, disappears on release). Re-checked 411 dp light/dark and selection mode after the divider refactor: the row fill, text-inset dividers, and the `secondaryContainer` selection state all render as designed.
- Not re-run: 1000+ contacts performance pass (`LargeListPerformanceTest` needs `contacts.perf=true`); the paging model is untouched by this issue.
