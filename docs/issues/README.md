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
