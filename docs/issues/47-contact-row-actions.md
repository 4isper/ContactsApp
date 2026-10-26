# fix: use a real overflow icon and make row actions available without swiping

**Priority:** P2 — the overflow affordance is visually wrong, and phone-only contacts expose no actions
**Area:** contacts list / contact card quick actions

## Problem

Each contact row ends with a favorite button and, only when the contact has a phone number, an icon button that opens Call/SMS actions. That button is drawn with `BrandIcons.Settings` (`ContactCard.kt` lines ~254–277), so every row appears to offer "Settings" rather than "More actions". The content description says "More actions", but sighted users see a gear. Issue 35 replaced the row's `MoreVert` with a brand icon, but the implementation chose `BrandIcons.Settings` rather than a dedicated "more" icon.

The gating creates an accessibility and discoverability gap:

- For contacts with an email but no phone, `hasCallOrSms` is false, so no overflow menu is rendered at all. The only row action is the favorite toggle. Sending an email, copying an address, or sharing the contact requires opening details.
- Call and SMS are also available through hidden swipe gestures (`SwipeToDismissBox` lines ~96–147). Nothing in the row communicates that swiping left calls and swiping right opens SMS. Users who do not discover the gesture lose the fastest path, and keyboard/switch/TalkBack users cannot perform a swipe-dismiss reliably. The swipe is a shortcut, not a complete interaction model.
- The menu and the swipe use different confirmation behavior: a swipe-to-call opens `ConfirmCallDialog`, while swipe-to-SMS opens the SMS app immediately. Call from the overflow also confirms; SMS does not. The asymmetry is not explained.

## Proposed scope

- Replace the gear with a standard `MoreVert` icon (or a brand "more" icon that is visually distinct from Settings). Keep the "More actions" content description.
- Render the overflow button for every contact that has at least one meaningful action, not only phone contacts. At minimum:
  - Call (when a phone exists);
  - Send SMS (when a phone exists);
  - Send email (when an email exists);
  - Copy email/phone;
  - Share contact.
  Reuse the existing intents/handlers so there is one behavior, not a second implementation.
- Keep the favorite toggle visible as a quick action when the name column remains readable. On narrow widths it may collapse into the overflow; see issue 52 for the width budget.
- Treat swipe-to-call/SMS as an optional shortcut: expose the same actions in the overflow, add custom accessibility actions if the swipe remains, and make the confirmation behavior consistent (SMS confirmation or an undo path if it launches an external app immediately).
- Make swipe actions discoverable without a permanent tutorial: for example, a one-time dismissible hint on the first populated list, or a small "Swipe for actions" line in the list header. Do not rely on a tooltip that requires a long-press, because long-press already enters selection mode.
- Ensure the overflow menu labels are localized and the destructive/external actions are distinguishable.

## Acceptance criteria

- [ ] The row's more-actions button uses a MoreVert-style icon, not a gear.
- [ ] A contact with only an email still exposes a visible actions menu with "Send email" and/or "Copy email".
- [ ] Every action available by swiping is also available from the menu.
- [ ] Call and SMS behavior is consistent between the menu and the swipe path (confirmation or immediate launch, documented in one place).
- [ ] The menu is reachable by keyboard and TalkBack; it does not depend on a swipe gesture.
- [ ] The name/subtitle column remains readable at 320 dp with the trailing actions that remain visible; the width budget and action collapse are defined in issue 52.
- [ ] A first-run hint (if added) can be dismissed and does not reappear on every launch.

## Technical notes

- `app/src/main/java/com/m4isper/contactsapp/presentation/contacts/components/ContactCard.kt` lines ~168–182 (`hasCallOrSms`), ~247–278 (favorite + overflow), ~328–377 (`QuickActionsMenu`), ~96–147 (swipe).
- `app/src/main/java/com/m4isper/contactsapp/presentation/contacts/components/ContactList.kt` lines ~233–237 — only `onCallContact`/`onSmsContact` are passed; add email/copy/share callbacks or reuse the details handlers.
- `app/src/main/java/com/m4isper/contactsapp/presentation/common/share/ContactIntents.kt` — `openEmail`, `openSms`, clipboard helpers.
- `app/src/main/java/com/m4isper/contactsapp/ui/theme/BrandIcons.kt` — add/choose a More icon; do not reuse `Settings`.
- `app/src/main/java/com/m4isper/contactsapp/presentation/common/call/rememberCallHandler.kt` / `ConfirmCallDialog` — existing call confirmation.
- `docs/issues/03-list-quick-actions.md` and `docs/issues/32-haptic-feedback.md` — keep the existing haptic behavior for swipe/toggle.
- `docs/issues/52-contact-row-density-and-hierarchy.md` — name-width budget, favorite visibility, and state/haptic polish; coordinate the two changes so the trailing action set is decided once.

## Out of scope

- Removing swipe gestures entirely.
- Adding a full contextual action mode on row tap.
- Changing contact details actions.

## Verification

- Manual: phone-only, email-only, and contact with neither; keyboard navigation; TalkBack custom actions.
- Confirm the same actions produce the same result from swipe and menu.
- `./gradlew :app:compileDebugKotlin :app:testDebugUnitTest`.
