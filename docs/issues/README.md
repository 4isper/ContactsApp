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
- ~~The language picker offers the languages the app is translated into (system, English, Russian)~~ — resolved by [66-more-app-languages.md](66-more-app-languages.md): the app ships six languages and takes part in the Android 13+ per-app language page.
- ~~The reminders fire once a day in the morning; the time of day is fixed~~ — resolved by [65-reminder-time-of-day.md](65-reminder-time-of-day.md); only the lead time (1, 3, 7 or 14 days) and the time of day are configurable, the digest stays once a day.

## Architecture audit backlog (2026-10-22)

All twelve items are implemented: every file has a `Status` section. They come from the Clean Architecture / Clean Code audit and are separate from the completed feature roadmap above. Each file is self-contained with a problem statement, proposed scope, acceptance criteria, and technical notes.

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

These issues come from a runtime/correctness bug audit and are separate from the completed feature roadmap and architecture audit above. All nine items are implemented: every file has a `Status` section. Each file is self-contained with a problem statement, proposed scope, acceptance criteria, and technical notes.

| # | File | Title | Priority | Status |
|---|---|---|---|---|
| 21 | [21-reminders-and-notifications.md](21-reminders-and-notifications.md) | Make birthday and event reminders actually work | P1 | Done |
| 22 | [22-app-lock-lifecycle.md](22-app-lock-lifecycle.md) | Do not lock app on configuration changes and external activities | P1 | Done |
| 23 | [23-move-contact-account.md](23-move-contact-account.md) | Keep contact details consistent after moving to another account | P1 | Done |
| 24 | [24-write-contacts-permissions.md](24-write-contacts-permissions.md) | Request WRITE_CONTACTS for favorite, delete, group, and merge actions | P1 | Done |
| 25 | [25-vcard-quoted-printable-and-intent.md](25-vcard-quoted-printable-and-intent.md) | Support standard quoted-printable vCard and stream-based share intents | P1 | Done |
| 26 | [26-duplicate-merge-correctness.md](26-duplicate-merge-correctness.md) | Treat null merge result as failure and merge all raw contacts | P2 | Done |
| 27 | [27-group-undo-delete.md](27-group-undo-delete.md) | Refresh group details after undo delete | P2 | Done |
| 28 | [28-list-paging-stability.md](28-list-paging-stability.md) | Make contact paging order deterministic | P2 | Done |
| 29 | [29-clear-and-app-lock-cold-start.md](29-clear-and-app-lock-cold-start.md) | Reset filters and avoid exposing contacts before app-lock settings load | P2 | Done |

### Bug audit dependencies

- 21 should land before any reminder-related UI work; it fixes the data source first.
- 24 should land before or together with 23, 26, and 27 because those flows write contacts.
- 25 is independent and can be scheduled separately.
- 28 is independent and low-risk.

## UX/UI audit backlog (2026-10-24)

These issues come from a UX/UI audit of the app and are separate from the feature roadmap, architecture audit, and bug audit above. All seven items are implemented: every file has a `Status` section. Each file is self-contained with a problem statement, proposed scope, acceptance criteria, and technical notes.

| # | File | Title | Priority | Status |
|---|---|---|---|---|
| 30 | [30-empty-state-cta.md](30-empty-state-cta.md) | Add clear calls-to-action to the empty contacts state | P1 | Done |
| 31 | [31-inline-field-validation.md](31-inline-field-validation.md) | Show validation errors inline next to the invalid editor field | P1 | Done |
| 32 | [32-haptic-feedback.md](32-haptic-feedback.md) | Add haptic feedback to key contact actions | P3 | Done |
| 33 | [33-loading-skeleton-for-contact-list.md](33-loading-skeleton-for-contact-list.md) | Add skeleton placeholders while the contact list loads | P2 | Done |
| 34 | [34-accessibility-a11y-improvements.md](34-accessibility-a11y-improvements.md) | Improve accessibility (TalkBack, contrast, touch targets, keyboard focus) | P1 | Done |
| 35 | [35-brand-visual-identity.md](35-brand-visual-identity.md) | Strengthen brand visual identity (icons, typography, accents) | P3 | Done |
| 36 | [36-account-sync-status-indicator.md](36-account-sync-status-indicator.md) | Show account sync status in the contacts list header | P2 | Done |

### UX/UI audit dependencies

- 31 depends on 20 (hardened validation) for the underlying field-level error data.
- 36 depends on 01 (live updates / provider observer) for real-time sync status.
- 30, 32, 33, 34, 35 are independent and can be scheduled in any order.

## UX/UI audit backlog, second pass (2026-10-25)

A second UX/UI pass looked at the implemented screens rather than the feature specification. These issues are follow-ups and regressions found in the current code: dead UI paths, small-screen overflow, broken state round-trips, permission flows, and accessibility work that was scaffolded but not completed. Each file is self-contained with a problem statement, proposed scope, acceptance criteria, and technical notes. All sixteen issues are implemented: every file has a `Status` section describing what was built, deviations from the plan, and the verification that ran.

| # | File | Title | Priority | Status |
|---|---|---|---|---|
| 37 | [37-wire-and-localize-account-sync-status.md](37-wire-and-localize-account-sync-status.md) | Wire up and localize the account sync status indicator | P2 | Done |
| 38 | [38-selection-app-bar-overflow.md](38-selection-app-bar-overflow.md) | Stop the selection app bar from overflowing on phones | P1 | Done |
| 39 | [39-filter-aware-empty-states.md](39-filter-aware-empty-states.md) | Make contact-list empty states aware of group/account filters | P1 | Done |
| 40 | [40-preserve-birthday-without-year.md](40-preserve-birthday-without-year.md) | Preserve "no year" birthdays and event dates | P1 | Done |
| 41 | [41-app-lock-ime-and-accessibility.md](41-app-lock-ime-and-accessibility.md) | Make the app-lock screens usable with the keyboard and TalkBack | P1 | Done |
| 42 | [42-contextual-write-permissions.md](42-contextual-write-permissions.md) | Request contact-write permission when the action happens, not on screen entry | P1 | Done |
| 43 | [43-duplicate-merge-ux.md](43-duplicate-merge-ux.md) | Make duplicate merging clearer and safe to review | P1 | Done |
| 44 | [44-group-member-picker.md](44-group-member-picker.md) | Exclude existing members from the group contact picker | P2 | Done |
| 45 | [45-editor-row-layout-and-validation.md](45-editor-row-layout-and-validation.md) | Make editor field rows readable and validation errors findable | P2 | Done |
| 46 | [46-photo-editor-crop-ux.md](46-photo-editor-crop-ux.md) | Make photo selection and cropping usable on small screens and with TalkBack | P2 | Done |
| 47 | [47-contact-row-actions.md](47-contact-row-actions.md) | Use a real overflow icon and make row actions available without swiping | P2 | Done |
| 48 | [48-talkback-follow-through.md](48-talkback-follow-through.md) | Finish TalkBack support for selection mode, swipes, and fast scrolling | P2 | Done |
| 49 | [49-reminders-notification-permission.md](49-reminders-notification-permission.md) | Keep the reminders switch in sync with notification permission | P2 | Done |
| 50 | [50-skeleton-shimmer-animation.md](50-skeleton-shimmer-animation.md) | Make the loading skeleton actually animate | P3 | Done |
| 51 | [51-system-bars-and-launch-theme.md](51-system-bars-and-launch-theme.md) | Make system bars and the launch window follow the app theme | P2 | Done |
| 52 | [52-contact-row-density-and-hierarchy.md](52-contact-row-density-and-hierarchy.md) | Rebalance contact-row hierarchy and state feedback | P2 | Done |

### Second-pass dependencies

- 37, 38, and 39 are done: 37 replaced the stub with public `ContentResolver` sync state, wired the indicator, and localized the strings; 38 moved the selection actions into an overflow with a compact count title; 39 added filter-aware empty states with clear-search/clear-filters recovery actions.
- 38 is done: the selection bar now has a stable title and a single overflow entry point. 48 follows and can target the final app bar. 48 also overlaps 47 for accessible swipe actions, so those two can share one branch.
- 39 is done: it also replaced 30's secondary filled button with a true secondary style (`OutlinedButton`), completing the 30 follow-up.
- 40 is independent, but the shared date dialog is also used by events; test both paths in one change.
- 41 is independent of 22 (app-lock lifecycle) but both affect the lock experience; avoid changing the same root layout twice.
- 42 supersedes the entry-time permission prompts introduced for 24. 43 and 44 landed without it (their tests grant `WRITE_CONTACTS` directly), so 42 remains the outstanding permission rework; it also removes the GrantPermissionRule workaround those tests needed.
- 43 should be coordinated with 26 (merge data correctness): choose and validate the target before changing provider write behavior.
- 44 is done: it excluded existing members from the picker, added picker empty states, and hid the add-member FAB when nobody is eligible.
- 45 builds on 31 and 20; it changes where inline errors appear and how the user finds them.
- 46 is independent. 47 and 35 share the row icon work; update `BrandIcons` once.
- 49 depends on 21 for reliable reminder scheduling, then fixes the settings/permission state on top.
- 50 is a regression fix for 33 and can ship independently.
- 51 is independent, but touches the same theme entry point as 35; keep the brand surface colors in one place.
- 52 complements 47 and 48 rather than replacing them: 47 owns the overflow icon and action availability, 48 owns custom accessibility actions, and 52 owns the remaining name-width, favorite-state, and haptic issues. 52 also refines 47's "keep the favorite visible" rule: favorite visibility becomes width-dependent. Land 47 first, then re-check 52's width budget against the final trailing-button set.

### How to use the second pass

Several issues are UI-visible regressions in features that are already marked Done. Treat their acceptance criteria as the new definition of done, and include before/after screenshots for the remaining layout issues (45, 46, 47, 52) plus a TalkBack recording for 48. 38, 39, 41, 43, and 44 landed without recordings; their files note the manual checks that remain useful.

## Share UX backlog (2026-10-28)

A follow-up from the QR share code review: the app can export a contact QR as a PNG to the share sheet, but never displays the code itself, so face-to-face sharing routes through another app before the second phone can scan anything. The item is implemented; each file has a `Status` section.

| # | File | Title | Priority | Status |
|---|---|---|---|---|
| 53 | [53-qr-preview-dialog.md](53-qr-preview-dialog.md) | Show the contact QR code on screen instead of only exporting it as an image | P2 | Done |

## Visual audit backlog, third pass (2026-10-28)

A third UX/UI pass looked at the app as a 2026 product rather than at feature completeness. The findings are visual-systemic rather than correctness bugs: the app is built from default Material 3 tokens, the brand is bypassed by dynamic color, every surface is a rounded card, and the adaptive shell stops at list-detail. Each file follows the same structure (problem, scope, acceptance criteria, technical notes, out of scope, verification). All seven issues, 54 and 56 through 61, are implemented.

| # | File | Title | Priority | Status |
|---|---|---|---|---|
| 54 | [54-expressive-design-system.md](54-expressive-design-system.md) | Adopt Material 3 Expressive as the app's visual language | P2 | Done (screenshot check pending) |
| 56 | [56-home-search-and-navigation.md](56-home-search-and-navigation.md) | Modernize home chrome with a search bar, real app bar, and adaptive navigation | P2 | Done (320 dp verified; TalkBack pending) | `b372ef7`, `5d4fa86`, `2ed1aff` |
| 57 | [57-list-surfaces-and-fast-scroller.md](57-list-surfaces-and-fast-scroller.md) | Rework contact-list surfaces and replace the iOS-style fast scroller | P2 | Done (129/131 on the AVD; two stale-star failures) | `13fc6dc`, `3dc6619` |
| 58 | [58-details-hero-and-grouped-fields.md](58-details-hero-and-grouped-fields.md) | Give contact details a hero header and grouped field sections | P2 | Done (details 17/17; full suite 133/135, two stale-star failures; TalkBack pending) | `ec6a72b`, `0ff3e9e` |
| 59 | [59-adaptive-layouts.md](59-adaptive-layouts.md) | Make every screen adaptive, not just contacts↔details | P2 | Done (142 instrumented tests at 411 dp: 139 passed; two stale-star, one Glance-receiver flake) | `32ec6fc`, `ae5cff4`, `a0a4fbc`, `56f1c7f`, `eb4c0f1` |
| 60 | [60-dialogs-sheets-and-editor-density.md](60-dialogs-sheets-and-editor-density.md) | Replace oversized dialogs with bottom sheets and compact the editor | P3 | Done (editor 33/33, picker 2/2, QR 5/5; full suite 148 tests, 145 passed: two stale-star, one Glance-receiver flake) | `433005c`, `9732413` |
| 61 | [61-settings-and-secondary-screens.md](61-settings-and-secondary-screens.md) | Refresh settings and the secondary screens with a scannable style | P3 | Done (settings 9/9, import preview 4/4, export banner 3/3; full suite 157 tests, 153 passed: two stale-star, one Glance-receiver and one focus flake) | `d4068f4`, `05a33a0`, `acf9779` |

### Third-pass dependencies

- 54 (tokens and motion) should land first: 56–61 all consume its type/shape/motion decisions instead of inventing local values.
- 56 (home chrome and navigation) and 59 (content width and two-pane) overlap in the shell: 56 owns the rail/suite and the home app bar, 59 owns pane strategies and max content width. Land 56 first, then 59.
- 57 is a partial regression fix for 52 (`showFavoriteQuickAction` is computed but never passed) and unifies the favorite accent with 58; it can ship independently and early.
- 58 depends on 54 for motion and on 57 for the favorite accent. 60 depends on 54 (sheet motion) and touches the same editor files as 59's two-column work — coordinate the editor branch.
- 61 depends on 56 (home entry point to settings); it also reuses 57's row tokens for utility screens.

### How to use the third pass

These are visual improvements, not missing features, so they can be scheduled opportunistically. Keep the before/after screenshots for 54–58 and 61, and run the adaptive checks (320/600/840 dp, font scale 2.0) for 56, 58, 59, and 60. If only part of the pass ships, 57 (the wiring regression) gives the largest visual return for the smallest change.

## Home header density follow-up (2026-10-29)

A density review of the home screen found that issue 56's app bar and search field are each standard, but stacked permanently they cost ~175–200 dp of fixed chrome before the first contact row (~3 list rows). The merge is the cheapest large win: the static app-name row is the least valuable element, and in search mode the top bar already disappears. The filter-chip row is deliberately untouched in this item.

| # | File | Title | Priority | Status |
|---|---|---|---|---|
| 62 | [62-home-header-density.md](62-home-header-density.md) | Merge the home app bar and search row to reclaim vertical space | P2 | Done (26/26 connected on the 320 dp AVD; screenshots and TalkBack manual) |
| 63 | [63-home-header-scroll-collapse.md](63-home-header-scroll-collapse.md) | Collapse the home header on scroll (enter-always behavior) | P3 | Done (32/32 connected + new state JVM tests; gates green) |

This item partially supersedes issue 56's "top app bar with the app name/brand" acceptance criterion for the idle home state; search-mode and selection bars from 56 and 38 stay as they are. 63 depends on 62 and is implemented with a small custom nested-scroll behavior (`HeaderCollapseState.kt`) instead of an M3 `TopAppBar` wrapper — the composition choice made in 62 (bare merged row) is documented in the 63 Status.

## Contact exchange follow-up (2026-10-29)

A follow-up from issue 53's "Out of scope" note: the app could show a contact QR on screen but not accept one, so face-to-face exchange stayed one-sided. The item is implemented; the file has a `Status` section.

| # | File | Title | Priority | Status |
|---|---|---|---|---|
| 64 | [64-scan-contact-qr.md](64-scan-contact-qr.md) | Accept a contact by scanning a QR code | P2 | Done (`fc1de8f`, `a10be6f`; manual device pass pending) |

## Feature gap backlog (2026-10-29)

A gap review against what a typical contacts manager already offers as a given. None of these was ever planned in the backlogs above; each file is a self-contained proposal with a problem statement, proposed scope, acceptance criteria, and technical notes. No Status sections yet — they appear when an item is implemented.

| # | File | Title | Priority | Status |
|---|---|---|---|---|
| 65 | [65-reminder-time-of-day.md](65-reminder-time-of-day.md) | Let the user pick the daily reminder time | P3 | Done |
| 66 | [66-more-app-languages.md](66-more-app-languages.md) | Expand localization beyond English and Russian | P2 | Done |
| 67 | [67-biometric-unlock.md](67-biometric-unlock.md) | Unlock the app with biometrics | P2 | Done |

### Feature gap dependencies

- 65 is independent and the smallest of the three: the scheduler, coordinator rollback, and boot receiver only need the time value threaded through.
- 66 is independent per locale, but the override-gap fixes (widget and notification language, `localeConfig`) should land with the first new locale, not after.
- 67 is independent of 65 and 66, but both it and 66 touch `MainActivity`: 66 may rework `attachBaseContext` wrapping, 67 changes the activity base class to `FragmentActivity` — coordinate those two changes.

## PIN entry follow-up (2026-10-30)

A design review of the lock screen after issue 67: as a security screen it holds, but as a PIN input it relies on the system IME, stays silent when the entry is below the minimum, has no haptics, and never explains the forgotten-PIN trade-off. One self-contained issue; not implemented yet.

| # | File | Title | Priority | Status |
|---|---|---|---|---|
| 68 | [68-pin-entry-redesign.md](68-pin-entry-redesign.md) | Make the PIN entry a genre-standard numeric pad with explicit feedback | P3 | Done |

68 builds on 67's biometric branch ("Use PIN" reveals the pad) and deliberately stays out of the hashing/throttling territory of issues 12 and 29. All nine items of the local roadmap are now implemented.

