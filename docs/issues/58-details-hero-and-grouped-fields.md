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

- [ ] The details screen has at most one hero surface and one card per field section; a contact with 6 phones + 4 other sections renders no more than 5 surfaces total.
- [ ] The name appears once at a time: in the hero while it is visible, in the top bar after it collapses.
- [ ] Call/SMS are reachable in one tap from the hero without scrolling; edit/share remain reachable without opening a full-screen menu.
- [ ] Email and address rows expose copy/open actions with proper content descriptions.
- [ ] Favorite accent and animation match the list; toggling updates the list on back-navigation.
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

## Verification

- Manual: contact with 0 fields, 1 phone, 6 phones + email + address; light/dark; 2.0 font scale; foldable in tabletop posture.
- `./gradlew :app:compileDebugKotlin :app:compileDebugAndroidTestKotlin :app:testDebugUnitTest :app:detekt :app:ktlintCheck`.
- `./gradlew :app:connectedDebugAndroidTest` for `ContactDetailsUiTest`.
