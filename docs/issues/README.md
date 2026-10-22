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
| 15 | [15-error-handling-observability.md](15-error-handling-observability.md) | Unify error handling and release logging | P2 | Open |
| 16 | [16-static-analysis-quality-gate.md](16-static-analysis-quality-gate.md) | Make detekt and architecture checks effective | P1 | Done |
| 17 | [17-test-architecture.md](17-test-architecture.md) | Add shared fixtures and cover ContentResolver data sources | P2 | Open |
| 18 | [18-composition-root-settings.md](18-composition-root-settings.md) | Centralize the composition root and settings side effects | P2 | Open |
| 19 | [19-ui-actions-side-effects.md](19-ui-actions-side-effects.md) | Replace callback-heavy UI APIs and move side effects out of composition | P2 | Open |
| 20 | [20-harden-validation.md](20-harden-validation.md) | Harden domain validation for email, URL, and phone | P2 | Open |

### Audit dependencies

- 09 (ports) should land before 10 and 14; it defines the contracts the extracted logic will use.
- 13 (normalization) should be done together with 10 (duplicate/search logic) so there is one implementation.
- 14 (ViewModel split) depends on 10 and 15; otherwise the extracted state holders inherit the same mixed responsibilities.
- 17 (test architecture) depends on 11 and 16 to make provider tests and architecture tests meaningful.
- 18 and 19 are independent and can be scheduled separately.
