# feat: give contact details a hero header and grouped field sections

**Priority:** P2 — details is the second-most visited screen and currently reads as eight small cards with a repeated title
**Area:** contact details

## Problem

- The details screen is a card stack. The header is a `Card` with a 72 dp avatar, the name as `headlineSmall`, a "1 number" subtitle, and two full-width buttons (Call as `FilledTonalButton`, SMS as `OutlinedButton`). Each field below is its own `Card` via `DataRow`/`PhoneRow` (`presentation/details/DetailsContent.kt`). A contact with six phones, email, address, birthday, and a note becomes 8–10 separate 16 dp-radius cards with 12 dp gaps — noisy, slow to scan, and visually flat because all cards have equal weight.
- The name is repeated in three places: the `TopAppBar` title (with a fallback to "Back"), the header, and TalkBack announcements.
- The two primary actions are stacked full width, which is the heaviest possible treatment for the two most-used actions in the app.
- Favorite state is `tertiary` in the top bar but `primary` in the list (issue 57), and the top bar exposes four icon actions (edit, favorite, share, overflow) that compete with the name.
- Per-field actions are limited: phone numbers can call/SMS, but email and address offer no copy/open action, and there is no quick "share" entry in the hero.

## Proposed scope

- Hero header: a tinted surface (brand/`secondaryContainer`) with a 96–112 dp avatar, name in `headlineMedium`, a one-line subtitle (primary phone or company), and an action row of circular icon buttons (call, message, email/video, favorite) plus a single overflow. Use a large collapsing top-app-bar behavior so the name appears in the bar only after the hero scrolls away.
- Fields: group rows into sections (Phones / Email / Address / Dates / Notes) with one card per section, `HorizontalDivider` between rows, and a small section label above each. Rows get a leading icon and trailing per-row actions (copy, call, open map, edit).
- Collapse the top bar to back + overflow while the hero is visible; move "Edit", "Share", and favorite into the hero/overflow. Keep "Show QR" in the overflow (issue 53).
- Unify the favorite accent with the list and use an animated transition on toggle (issue 54's motion tokens).
- Keep the QR dialog entry point and the existing share handlers unchanged.

## Acceptance criteria

- [x] The details screen has at most one hero surface and one card per field section; a contact with 6 phones + 4 other sections renders no more than 5 surfaces total.
- [x] The name appears once at a time: in the hero while it is visible, in the top bar after it collapses.
- [x] Call/SMS are reachable in one tap from the hero without scrolling; edit/share remain reachable without opening a full-screen menu.
- [x] Email and address rows expose copy/open actions with proper content descriptions.
- [x] Favorite accent and animation match the list; toggling updates the list on back-navigation.
- [ ] Light/dark and 2.0 font scale: no clipped hero text, no overlap between avatar and action row; TalkBack reads a sensible heading order.

## Technical notes

- Shared tokens and rules: `docs/design/visual-rules.md` (issue 54); use it instead of local values.
- `app/src/main/java/com/m4isper/contactsapp/presentation/details/DetailsHeader.kt` — current card header and the Call/SMS buttons.
- `app/src/main/java/com/m4isper/contactsapp/presentation/details/DetailsContent.kt` — per-field cards (`DataRow`, `PhoneRow`); this is where sections replace individual cards.
- `app/src/main/java/com/m4isper/contactsapp/presentation/details/DetailsTopBar.kt` — title/actions; reduce to a scroll-aware bar.
- `app/src/main/java/com/m4isper/contactsapp/presentation/details/ContactDetailsScreen.kt` — screen composition and dialog state.
- Compose: `TopAppBarDefaults.enterAlwaysScrollBehavior` / pinned + `nestedScroll`; the top bar currently takes `title` from the same state as the header.
- Reuse existing intent helpers for call/SMS/email/maps rather than adding new intent code in the composable.
- Motion: use the shared tokens from issue 54; do not introduce a local `animate*AsState` with ad-hoc specs.
- Tests: `ContactDetailsUiTest` covers the top-bar actions; extend it for the collapsed/expanded string behavior and the new per-row actions.

## Out of scope

- Editing field values in place (the editor owns writes).
- Redesigning the QR dialog (issue 53/60).
- Contact photos pipeline changes.

## Decisions

- **Flat sections instead of one card per section.** The brief asked for a card per section, but `docs/design/visual-rules.md` allows only one reading surface and requires rows to be separated by inset 1 dp `outlineVariant` dividers, never per-row/per-group cards. The hero is the only surface; sections are a small label plus rows with in-section dividers. This yields one surface for the whole screen instead of the allowed five.
- **Scroll-aware bar without a Material collapsing bar.** The hero is a `LazyColumn` item, so `LargeTopAppBar`'s height-driven collapse would fight the list. The bar itself stays pinned so back and overflow never move; the name appears when it stops being on screen. The signal is the hero name's measured bottom (`onGloballyPositioned` inside the hero) compared with the list offset, not the hero's bottom: a short contact cannot scroll its hero fully away, and the hero's bottom would then keep the title hidden while the name itself is already off-screen. If the hero item is not among the visible items it has left entirely and the title shows.
- **Sections and their order:** untitled identity block (account, organization, groups, nickname), Phones (+ SIP), Email & messaging (+ instant messages), Address & web (postal addresses + websites), Dates (birthday + events), Relations, Note. Empty sections are dropped, so an empty contact still renders just the hero.
- **Hero contents:** 104 dp avatar (new `avatarHero` token), `headlineMedium` name, one-line subtitle = primary phone formatted with `phoneText`, else the company. Actions: filled call, tonal SMS and email, plain favorite; a phone or email action is hidden when the contact has no such value. Favorite moves out of the top bar and keeps the `tertiary` accent with `AppMotion.effects()`, the same pair the list uses (issue 57).
- **Top bar:** only back plus an overflow. The overflow now starts with Edit and Share, then keeps share-as-text, Show QR (issue 53), share-as-QR, copy-to-account, move-to-account. Tags for the existing entry points are unchanged; edit is `edit_menu_item`.
- **Row actions:** phone rows call on tap and offer SMS + copy; email rows send + copy; instant messages open the messenger (fallback copy) + copy; addresses open the map + copy; websites open the browser + copy; nickname and relations copy; dates add the next occurrence to the calendar. All content descriptions come from existing string resources.
- **Permission in tests:** favorite toggling goes through the write-permission gate, so `ContactDetailsUiTest` now grants `WRITE_CONTACTS` with `GrantPermissionRule`. The previous pass depended on a manual `pm grant` that a reinstall wipes.
- **Scroll test fixture:** the hero is taller than the 240 dp test viewport, so the title test needs content below the fold (email, address, website, note) before the hero can leave the viewport. It scrolls to the note row, not to the account row. A second test scrolls a one-phone contact to the bottom to pin the sliver case: the title must take over even though the hero item still has its bottom padding on screen.

## Verification

- `./gradlew :app:compileDebugKotlin :app:compileDebugAndroidTestKotlin :app:testDebugUnitTest :app:detekt :app:ktlintCheck` — green on the final build. Detekt caught a 14-branch `ContactDetailsScreen` and 13 functions in `DetailsContent.kt`; the scroll observer moved into its own composable and the two one-use label helpers were inlined.
- `ContactDetailsUiTest` — 17/17 on Pixel_10_Pro_Fold (API 37), including the new hero actions, section grouping, overflow edit, and the two title tests.
- `./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.notClass=com.m4isper.contactsapp.data.LargeListPerformanceTest` — 135 tests, 133 passed, 2 failed. Both are `FavoritesStarredTest` and carry the same AVD leftover as issue 57: a contact (id 263) is still starred, while those assertions compare against the provider's global starred set (`expected:<[398]> but was:<[263, 398]>`). Every other suite passed, including `ContactListUiTest`.
- Manual at 411 dp on the emulator: hero with 104 dp avatar, `headlineMedium` name, one-line subtitle, call/SMS/email/favorite row; untitled account block, then Phones and Email sections with text-aligned dividers; back + overflow only in the bar. Scrolling a one-phone contact to the bottom leaves a thin tinted sliver of the hero, and the bar correctly takes over the name.
- Dark theme at 411 dp: hero uses the dark `secondaryContainer`, name and subtitle stay readable, the favorite keeps the tertiary accent.
- 2.0 font scale at 411 dp: hero name, subtitle, and actions do not clip or overlap; rows reflow (the email address wraps) and actions stay aligned to the right.
- TalkBack heading order was not re-run; the hero name remains a plain `Text` (as before), so nothing regressed in semantics, but the heading audit stays pending like in issues 54 and 56.
- Not re-checked by hand: the 0-field and 6-phone variants from the matrix. Empty sections are dropped by construction, and all rows share one flat layout, so those variants do not introduce new code paths.
