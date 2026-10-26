# fix: rebalance contact-row hierarchy and state feedback

**Priority:** P2 — secondary actions crowd out the contact name on phones, and the favorite state reads as disabled
**Area:** contacts list / contact row visuals and haptics

## Problem

The contact row is visually action-heavy relative to the information it carries. In normal mode it shows an avatar, a name, one subtitle line, a favorite button, and an overflow button; it also supports tap, long-press selection, and two swipe directions. The information is two lines, but the controls are four interaction paths. That is why the row feels like a control panel rather than a list item.

Issue 47 covers the overflow icon and the availability of the action set for every contact, and issue 48 covers accessible alternatives to the swipes. This issue addresses what remains after those changes: the name column still loses the width fight, the favorite state is rendered backwards, and two independent layers still fire the same haptic pulse.

### The name column is the lowest-priority element

The fixed parts of the row consume most of a phone's width before the name is measured:

- fast scroller: 28 dp when the list has more than one letter;
- card margins (`spacing.m` × 2) and card padding (`spacing.m` × 2): 48 dp;
- avatar and gap (`avatarList` + `spacing.m`): 60 dp;
- favorite and overflow buttons: 2 × 48 dp = 96 dp.

On a 360 dp screen the name column is left with about **128 dp**; on a 320 dp screen, about **88 dp**. Without the fast scroller it is 156 dp and 116 dp. The name uses `titleMedium` with `maxLines = 1`, so at 200% font scale the 88 dp column fits roughly **5–7 characters**. A full name is reduced to an unrecognisable fragment while two secondary buttons keep their full width. The row has no strategy for choosing between the name and the actions; the actions always win.

### The favorite state is inverted

`FavoriteQuickAction` passes `enabled = isFavorite` to `BrandIcon`. `BrandIcon` interprets `enabled = false` as a disabled tint (`onSurfaceVariant` at 38% alpha), but the enclosing `IconButton` stays clickable. The result:

- a non-favorite contact shows a faint, disabled-looking star that is actually actionable;
- a favorite contact shows a normal `onSurfaceVariant` star with no accent emphasis.

This contradicts the rules documented in `BrandIcons.kt`, where active favorite is a primary action and inactive favorite is a secondary/decoration. Sighted users get the wrong affordance, while TalkBack users are told the button is available.

### The favorite tap triggers two haptic pulses

`ContactList` performs a light haptic in its `onToggleFavorite` lambda, and `FavoriteQuickAction` performs another light haptic before invoking that lambda. One tap therefore calls `performHapticFeedback` twice. This is perceptible on devices where predefined effects are queued and makes the action feel glitchy rather than crisp.

### The row chrome is not aligned or stable

- Section headers use `horizontal = spacing.m` (12 dp), while card text starts at 12 dp margin + 12 dp padding = 24 dp. The letter and the contact name do not share a start edge.
- The overflow button is conditional on `hasCallOrSms`, so the favorite star shifts 48 dp to the right on email-only rows. Even after issue 47 makes actions available for email-only contacts, the trailing action group needs a stable alignment rule.
- `QuickActionsMenu` gives every `leadingIcon` the same `contentDescription` as the menu item text. Because the menu item merges its descendants, TalkBack may announce "Call, Call" instead of one label.

## Proposed scope

- Introduce an explicit width budget for the name column instead of letting `weight(1f)` absorb every remaining pixel:
  - define a minimum readable width (for example 136 dp at 320 dp) and collapse secondary trailing actions when the budget cannot be met;
  - at narrow widths keep one permanent action (`MoreVert`); move the favorite toggle into the overflow or details, and if the favorite state must stay visible, render a small indicator inline with the name or avatar rather than as another trailing control;
  - at larger widths allow the favorite toggle and the overflow to coexist, but only while the name column keeps its minimum width;
  - keep every touch target at least 48 dp and do not let actions overlap.
- Refine issue 47's "keep the favorite visible" rule: favorite visibility becomes width-dependent. The favorite remains a quick action when the name budget allows it, and moves into the overflow when it does not.
- Let the name wrap to two lines at large font scale instead of forcing a single ellipsized line. Preserve a one-line name at normal scale so the list stays scannable.
- Re-evaluate the per-row card chrome at phone widths. Flat, edge-to-edge rows can return up to 24 dp of horizontal space and remove the 4 dp gaps between otherwise uniform items. If cards remain, keep them but align section headers and card text to one start edge.
- Fix the favorite visual state:
  - do not pass `enabled = isFavorite` to the icon; the button is enabled in both states;
  - active favorite uses an accent color (primary or the brand accent) and the filled icon;
  - inactive favorite uses a neutral enabled style (for example `onSurfaceVariant`), not the disabled tint;
  - keep the content description in sync with the state and consider an explicit `stateDescription` if the icon alone is ambiguous.
- Make one layer own the favorite haptic. Prefer the list-level callback so the card remains a pure visual component, or remove the list-level pulse and keep the card's; do not keep both.
- Give section headers and row content the same start edge (24 dp with the current card, or 16 dp if the rows become flat).
- Make the trailing action group align consistently on rows with and without an overflow, including during the transition after issue 47.
- Set `contentDescription = null` on decorative leading icons inside menu items that already have a text label, then verify with TalkBack that each action is announced once.
- Add a small adaptive rule rather than hard-coding a device width: derive the collapse decision from the measured available width and `fontScale` (for example `BoxWithConstraints` or a width-aware row composable).

## Acceptance criteria

- [ ] At 320 dp with the fast scroller visible, the row never leaves the name column below 136 dp; if it would, secondary trailing actions collapse into the overflow.
- [ ] At 320 dp with 200% font scale, the name is allowed to wrap to two lines and is not reduced to a one-line fragment of a few characters.
- [ ] At narrow widths, at most one permanent trailing action is visible; all actions remain reachable through the overflow or details.
- [ ] When the favorite control is visible, it never looks disabled while it is clickable; the active state is visually accented and the inactive state is neutral.
- [ ] One tap on any favorite control produces exactly one haptic pulse, never two.
- [ ] Section headers and row text share the same start edge.
- [ ] Trailing actions keep a stable alignment on phone-only and email-only rows.
- [ ] TalkBack reads each overflow menu item once; decorative leading icons do not add a duplicate label.
- [ ] The fixes do not regress the acceptance criteria of issues 47 and 48.
- [ ] Verified at 320 dp, 360 dp, 411 dp, and a tablet width, at normal and 200% font scale.

## Technical notes

- `app/src/main/java/com/m4isper/contactsapp/presentation/contacts/components/ContactCard.kt` lines ~211–280 — row layout, avatar, text column, favorite and overflow buttons.
- `app/src/main/java/com/m4isper/contactsapp/presentation/contacts/components/ContactCard.kt` lines ~303–324 — `FavoriteQuickAction`; the `enabled = isFavorite` tint bug lives here.
- `app/src/main/java/com/m4isper/contactsapp/presentation/contacts/components/ContactCard.kt` lines ~328–377 — `QuickActionsMenu` leading-icon descriptions.
- `app/src/main/java/com/m4isper/contactsapp/presentation/contacts/components/ContactList.kt` lines ~93–96 — list-level favorite haptic; remove one of the two calls.
- `app/src/main/java/com/m4isper/contactsapp/presentation/contacts/components/GroupedContacts.kt` lines ~73–109 — `Row` with the 28 dp fast scroller; it reduces the width available to the list.
- `app/src/main/java/com/m4isper/contactsapp/presentation/contacts/components/GroupedContacts.kt` lines ~174–200 — section header padding; align with card content.
- `app/src/main/java/com/m4isper/contactsapp/ui/components/ContactAvatar.kt` — `ListAvatar`; 48 dp avatar and 40 dp compact avatar.
- `app/src/main/java/com/m4isper/contactsapp/ui/theme/Spacing.kt` — `m = 12.dp`, `avatarList = 48.dp`, `fabClearance = 80.dp`.
- `app/src/main/java/com/m4isper/contactsapp/ui/theme/BrandIcons.kt` — documented color rules for active/inactive favorite; keep the implementation consistent with them.
- `BoxWithConstraints` at the row level is the simplest way to measure the real budget; if it is too costly in a long `LazyColumn`, compute a compact flag once for the list from the window width and font scale and pass it down.
- `titleMedium` defaults to 16 sp; at 200% the line is roughly 32 sp. A 136 dp column fits about eight average characters, which is still tight, hence the two-line fallback.
- `docs/issues/47-contact-row-actions.md` — overflow icon, action availability, and swipe behavior; land it first because it changes the trailing-button set.
- `docs/issues/48-talkback-follow-through.md` — custom accessibility actions and selection announcements; this issue only fixes duplicate labels on the row menu.
- `docs/issues/35-brand-visual-identity.md` — brand icon rules; note that it replaced the row's `MoreVert` with a `BrandIcon`, and the implementation chose `BrandIcons.Settings`. Do not repeat the inaccurate claim that issue 35 itself specified a gear.
- `docs/issues/32-haptic-feedback.md` — original haptic work; this is a follow-up regression, not a new feature.

## Out of scope

- Replacing the overflow with a different action model, changing which contact actions exist, or making email-only actions available; that is issue 47.
- Custom accessibility actions for swipes, selection announcements, and the fast-scroller touch target; that is issue 48.
- Full list redesign, a new grid/two-column layout, or changing the navigation model.
- Changing favorite write behavior, permission handling, or provider synchronization; that is issue 42.
- Reworking the brand icon set beyond choosing an accent/neutral treatment for the existing favorite icon.

## Verification

- Manual matrix: 320 dp, 360 dp, 411 dp, and tablet width; fast scroller visible and hidden; normal and 200% font scale; phone-only, email-only, and favorite/non-favorite contacts.
- Confirm the name column budget and the action collapse rule at the narrowest width; confirm the row still exposes every action from issue 47.
- Tap any favorite control repeatedly and confirm one haptic pulse per tap; check that the icon is not rendered with a disabled tint while the button is enabled.
- TalkBack pass over a row and its overflow menu; confirm each label is read once.
- Compare section-header and card-text start edges with a layout inspector or screenshot.
- `./gradlew :app:compileDebugKotlin :app:testDebugUnitTest`.
