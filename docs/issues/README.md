# Local roadmap: feature gaps

This is a local roadmap based on a code review of the app. Each file is a self-contained plan with a problem statement, proposed scope, acceptance criteria, and technical notes.

All eight items are implemented: the acceptance criteria are checked off and every file has a `Status` section describing what was built and where it differs from the plan.

| # | File | Title | Priority | Status | Commits |
|---|---|---|---|---|---|
| 01 | [01-live-updates.md](01-live-updates.md) | Observe contact provider changes for live updates | P0 | Done | `d31b6d4` |
| 02 | [02-search-filters-sorting.md](02-search-filters-sorting.md) | Extend contact search, account filters, and sorting | P1 | Done | `4900d8c` |
| 03 | [03-list-quick-actions.md](03-list-quick-actions.md) | Add list quick actions, pinned favorites, and recents | P1 | Done | `383f2f6` |
| 04 | [04-structured-fields.md](04-structured-fields.md) | Support structured contact fields and primary values | P1 | Done | `c10aac0` |
| 05 | [05-vcard-import-export.md](05-vcard-import-export.md) | Improve vCard import/export fidelity | P1 | Done | `e2bcade`, `b551849` |
| 06 | [06-accounts-system-integration.md](06-accounts-system-integration.md) | Add account management, system contact integration, and safe merge | P2 | Done | `6e05635` |
| 07 | [07-groups-editor-polish.md](07-groups-editor-polish.md) | Complete group management and contact editor polish | P2 | Done | `c299c85` |
| 08 | [08-settings-privacy-performance.md](08-settings-privacy-performance.md) | Expand settings, privacy, large-list performance, and reminders | P2/P3 | Done | `8c05570`, `1da11b2`, `12c506b`, `318d3d5`, `d49d5bd` |

## Dependencies

- Item 04 (structured address) should land before item 05 (vCard `ADR` fidelity).
- Item 06 (default account) overlaps with item 08 (settings UI).
- Item 01 is the smallest independent item and has no dependencies.

## How to use

Pick an item, create a branch, and use its acceptance criteria as the definition of done. The files are intentionally independent so they can be scheduled or split further as the roadmap evolves.

## Not covered by the roadmap

- The "My profile" card (`ContactsContract.Profile`) is not implemented: the app has no own-profile screen and does not write changes back to the profile.
- The language picker offers the languages the app is translated into (system, English, Russian); other translations would need their own `values-xx` resources.
- The reminders fire once a day in the morning; the time of day is fixed, only the lead time (1, 3, 7 or 14 days) is configurable.

## Architecture audit backlog (2026-10-22)

These issues are open and come from the Clean Architecture / Clean Code audit. They are separate from the completed feature roadmap above. Each file is self-contained with a problem statement, proposed scope, acceptance criteria, and technical notes.

| # | File | Title | Priority | Status |
|---|---|---|---|---|
| 09 | [09-domain-boundaries.md](09-domain-boundaries.md) | Tighten domain ports and remove infrastructure leaks | P1 | Done |
| 10 | [10-business-rules-in-domain.md](10-business-rules-in-domain.md) | Move contact business rules from presentation to domain | P1 | Done |
| 11 | [11-data-layer-correctness.md](11-data-layer-correctness.md) | Make data-layer IO safe and group merge atomic | P1 | Done |
| 12 | [12-applock-pin-security.md](12-applock-pin-security.md) | Harden app-lock PIN storage | P1 | Done |
| 13 | [13-duplicate-normalization-locale.md](13-duplicate-normalization-locale.md) | Make duplicate normalization locale-independent | P1 | Done |
| 14 | [14-viewmodel-decomposition.md](14-viewmodel-decomposition.md) | Split oversized ViewModels | P2 | Done |
| 15 | [15-error-handling-observability.md](15-error-handling-observability.md) | Unify error handling and release logging | P2 | Done |
| 16 | [16-static-analysis-quality-gate.md](16-static-analysis-quality-gate.md) | Make detekt and architecture checks effective | P1 | Done |
| 17 | [17-test-architecture.md](17-test-architecture.md) | Add shared fixtures and cover ContentResolver data sources | P2 | Done |
| 18 | [18-composition-root-settings.md](18-composition-root-settings.md) | Centralize the composition root and settings side effects | P2 | Done |
| 19 | [19-ui-actions-side-effects.md](19-ui-actions-side-effects.md) | Replace callback-heavy UI APIs and move side effects out of composition | P2 | Done |
| 20 | [20-harden-validation.md](20-harden-validation.md) | Harden domain validation for email, URL, and phone | P2 | Done |

### Audit dependencies

- 09 (ports) should land before 10 and 14; it defines the contracts the extracted logic will use.
- 13 (normalization) should be done together with 10 (duplicate/search logic) so there is one implementation.
- 14 (ViewModel split) depends on 10 and 15; otherwise the extracted state holders inherit the same mixed responsibilities.
- 17 (test architecture) depends on 11 and 16 to make provider tests and architecture tests meaningful.
- 18 and 19 are independent and can be scheduled separately.

## Bug audit backlog (2026-10-24)

These issues come from a runtime/correctness bug audit. They are separate from the completed feature roadmap and architecture audit above. Each file is self-contained with a problem statement, proposed scope, acceptance criteria, and technical notes.

| # | File | Title | Priority | Status |
|---|---|---|---|---|
| 21 | [21-reminders-and-notifications.md](21-reminders-and-notifications.md) | Make birthday and event reminders actually work | P1 | Open |
| 22 | [22-app-lock-lifecycle.md](22-app-lock-lifecycle.md) | Do not lock app on configuration changes and external activities | P1 | Open |
| 23 | [23-move-contact-account.md](23-move-contact-account.md) | Keep contact details consistent after moving to another account | P1 | Open |
| 24 | [24-write-contacts-permissions.md](24-write-contacts-permissions.md) | Request WRITE_CONTACTS for favorite, delete, group, and merge actions | P1 | Open |
| 25 | [25-vcard-quoted-printable-and-intent.md](25-vcard-quoted-printable-and-intent.md) | Support standard quoted-printable vCard and stream-based share intents | P1 | Open |
| 26 | [26-duplicate-merge-correctness.md](26-duplicate-merge-correctness.md) | Treat null merge result as failure and merge all raw contacts | P2 | Open |
| 27 | [27-group-undo-delete.md](27-group-undo-delete.md) | Refresh group details after undo delete | P2 | Open |
| 28 | [28-list-paging-stability.md](28-list-paging-stability.md) | Make contact paging order deterministic | P2 | Open |
| 29 | [29-clear-and-app-lock-cold-start.md](29-clear-and-app-lock-cold-start.md) | Reset filters and avoid exposing contacts before app-lock settings load | P2 | Open |

### Bug audit dependencies

- 21 should land before any reminder-related UI work; it fixes the data source first.
- 24 should land before or together with 23, 26, and 27 because those flows write contacts.
- 25 is independent and can be scheduled separately.
- 28 is independent and low-risk.

## UX/UI audit backlog (2026-10-24)

These issues come from a UX/UI audit of the app. They are separate from the feature roadmap, architecture audit, and bug audit above. Each file is self-contained with a problem statement, proposed scope, acceptance criteria, and technical notes.

| # | File | Title | Priority | Status |
|---|---|---|---|---|
| 30 | [30-empty-state-cta.md](30-empty-state-cta.md) | Add clear calls-to-action to the empty contacts state | P1 | Open |
| 31 | [31-inline-field-validation.md](31-inline-field-validation.md) | Show validation errors inline next to the invalid editor field | P1 | Open |
| 32 | [32-haptic-feedback.md](32-haptic-feedback.md) | Add haptic feedback to key contact actions | P3 | Open |
| 33 | [33-loading-skeleton-for-contact-list.md](33-loading-skeleton-for-contact-list.md) | Add skeleton placeholders while the contact list loads | P2 | Open |
| 34 | [34-accessibility-a11y-improvements.md](34-accessibility-a11y-improvements.md) | Improve accessibility (TalkBack, contrast, touch targets, keyboard focus) | P1 | Open |
| 35 | [35-brand-visual-identity.md](35-brand-visual-identity.md) | Strengthen brand visual identity (icons, typography, accents) | P3 | Open |
| 36 | [36-account-sync-status-indicator.md](36-account-sync-status-indicator.md) | Show account sync status in the contacts list header | P2 | Done |

### UX/UI audit dependencies

- 31 depends on 20 (hardened validation) for the underlying field-level error data.
- 36 depends on 01 (live updates / provider observer) for real-time sync status.
- 30, 32, 33, 34, 35 are independent and can be scheduled in any order.

## UX/UI audit backlog, second pass (2026-10-25)

A second UX/UI pass looked at the implemented screens rather than the feature specification. These issues are follow-ups and regressions found in the current code: dead UI paths, small-screen overflow, broken state round-trips, permission flows, and accessibility work that was scaffolded but not completed. Each file is self-contained with a problem statement, proposed scope, acceptance criteria, and technical notes.

| # | File | Title | Priority | Status |
|---|---|---|---|---|
| 37 | [37-wire-and-localize-account-sync-status.md](37-wire-and-localize-account-sync-status.md) | Wire up and localize the account sync status indicator | P2 | Done |
| 38 | [38-selection-app-bar-overflow.md](38-selection-app-bar-overflow.md) | Stop the selection app bar from overflowing on phones | P1 | Done |
| 39 | [39-filter-aware-empty-states.md](39-filter-aware-empty-states.md) | Make contact-list empty states aware of group/account filters | P1 | Open |
| 40 | [40-preserve-birthday-without-year.md](40-preserve-birthday-without-year.md) | Preserve "no year" birthdays and event dates | P1 | Open |
| 41 | [41-app-lock-ime-and-accessibility.md](41-app-lock-ime-and-accessibility.md) | Make the app-lock screens usable with the keyboard and TalkBack | P1 | Open |
| 42 | [42-contextual-write-permissions.md](42-contextual-write-permissions.md) | Request contact-write permission when the action happens, not on screen entry | P1 | Open |
| 43 | [43-duplicate-merge-ux.md](43-duplicate-merge-ux.md) | Make duplicate merging clearer and safe to review | P1 | Open |
| 44 | [44-group-member-picker.md](44-group-member-picker.md) | Exclude existing members from the group contact picker | P2 | Open |
| 45 | [45-editor-row-layout-and-validation.md](45-editor-row-layout-and-validation.md) | Make editor field rows readable and validation errors findable | P2 | Open |
| 46 | [46-photo-editor-crop-ux.md](46-photo-editor-crop-ux.md) | Make photo selection and cropping usable on small screens and with TalkBack | P2 | Open |
| 47 | [47-contact-row-actions.md](47-contact-row-actions.md) | Use a real overflow icon and make row actions available without swiping | P2 | Open |
| 48 | [48-talkback-follow-through.md](48-talkback-follow-through.md) | Finish TalkBack support for selection mode, swipes, and fast scrolling | P2 | Open |
| 49 | [49-reminders-notification-permission.md](49-reminders-notification-permission.md) | Keep the reminders switch in sync with notification permission | P2 | Open |
| 50 | [50-skeleton-shimmer-animation.md](50-skeleton-shimmer-animation.md) | Make the loading skeleton actually animate | P3 | Open |
| 51 | [51-system-bars-and-launch-theme.md](51-system-bars-and-launch-theme.md) | Make system bars and the launch window follow the app theme | P2 | Open |
| 52 | [52-contact-row-density-and-hierarchy.md](52-contact-row-density-and-hierarchy.md) | Rebalance contact-row hierarchy and state feedback | P2 | Open |

### Second-pass dependencies

- 37 and 38 are done: 37 replaced the stub with public `ContentResolver` sync state, wired the indicator, and localized the strings; 38 moved the selection actions into an overflow with a compact count title. 39 is the next item.
- 38 is done: the selection bar now has a stable title and a single overflow entry point. 48 follows and can target the final app bar. 48 also overlaps 47 for accessible swipe actions, so those two can share one branch.
- 39 follows 30. It adds filter-specific recovery actions and replaces the secondary filled button with a true secondary style.
- 40 is independent, but the shared date dialog is also used by events; test both paths in one change.
- 41 is independent of 22 (app-lock lifecycle) but both affect the lock experience; avoid changing the same root layout twice.
- 42 supersedes the entry-time permission prompts introduced for 24. It should land before 43 and 44, which add more write actions to the same screens.
- 43 should be coordinated with 26 (merge data correctness): choose and validate the target before changing provider write behavior.
- 44 is independent of 42, but both edit Group Detail; schedule them together if possible.
- 45 builds on 31 and 20; it changes where inline errors appear and how the user finds them.
- 46 is independent. 47 and 35 share the row icon work; update `BrandIcons` once.
- 49 depends on 21 for reliable reminder scheduling, then fixes the settings/permission state on top.
- 50 is a regression fix for 33 and can ship independently.
- 51 is independent, but touches the same theme entry point as 35; keep the brand surface colors in one place.
- 52 complements 47 and 48 rather than replacing them: 47 owns the overflow icon and action availability, 48 owns custom accessibility actions, and 52 owns the remaining name-width, favorite-state, and haptic issues. 52 also refines 47's "keep the favorite visible" rule: favorite visibility becomes width-dependent. Land 47 first, then re-check 52's width budget against the final trailing-button set.

### How to use the second pass

Several issues are UI-visible regressions in features that are already marked Done. Treat their acceptance criteria as the new definition of done, and include before/after screenshots for the layout issues (38, 39, 43, 45, 46, 47, 52) plus a TalkBack recording for 41 and 48.
