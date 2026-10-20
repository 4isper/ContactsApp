# Local roadmap: feature gaps

This is a local roadmap based on a code review of the app. Each file is a self-contained plan with a problem statement, proposed scope, acceptance criteria, and technical notes.

| # | File | Title | Priority |
|---|---|---|---|
| 01 | [01-live-updates.md](01-live-updates.md) | Observe contact provider changes for live updates | P0 |
| 02 | [02-search-filters-sorting.md](02-search-filters-sorting.md) | Extend contact search, account filters, and sorting | P1 |
| 03 | [03-list-quick-actions.md](03-list-quick-actions.md) | Add list quick actions, pinned favorites, and recents | P1 |
| 04 | [04-structured-fields.md](04-structured-fields.md) | Support structured contact fields and primary values | P1 |
| 05 | [05-vcard-import-export.md](05-vcard-import-export.md) | Improve vCard import/export fidelity | P1 |
| 06 | [06-accounts-system-integration.md](06-accounts-system-integration.md) | Add account management, system contact integration, and safe merge | P2 |
| 07 | [07-groups-editor-polish.md](07-groups-editor-polish.md) | Complete group management and contact editor polish | P2 |
| 08 | [08-settings-privacy-performance.md](08-settings-privacy-performance.md) | Expand settings, privacy, large-list performance, and reminders | P2/P3 |

## Dependencies

- Item 04 (structured address) should land before item 05 (vCard `ADR` fidelity).
- Item 06 (default account) overlaps with item 08 (settings UI).
- Item 01 is the smallest independent item and has no dependencies.

## How to use

Pick an item, create a branch, and use its acceptance criteria as the definition of done. The files are intentionally independent so they can be scheduled or split further as the roadmap evolves.
