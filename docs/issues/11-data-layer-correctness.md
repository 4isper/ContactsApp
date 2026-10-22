# fix: make data-layer IO safe and group merge atomic

**Priority:** P1  
**Area:** data / performance / consistency

## Problem

There are three data-layer correctness issues:

- `FavoritesDataSourceImpl` performs blocking `ContentResolver` work in `callbackFlow`/suspend methods without `withContext(Dispatchers.IO)`. This can block the collector thread (including main) and cause jank or ANR.
- `GroupsDataSourceImpl.mergeGroups` performs add and delete without a transaction or compensation. If the delete fails, the provider is left in a partially merged state.
- `MergePlans.mergeContactForms` is not used in production; it is kept alive only by tests. Tests for it do not prove that the real merge path works.

## Proposed scope

- Give the favorites data source a dedicated IO dispatcher and use `withContext(Dispatchers.IO)` for provider calls; apply `flowOn` for the observable path.
- Make group merge transactional where possible (`ContentResolver.applyBatch`/provider batch operations) or add explicit compensating cleanup.
- Make merge operations idempotent and test failure paths, not only the happy path.
- Either integrate `mergeContactForms` into the production merge flow or delete it and rewrite tests against the real implementation.
- Add regression tests for favorites IO and partial merge failures.

## Acceptance criteria

- [x] No blocking provider call runs on the main thread or the flow collector thread.
- [x] A group merge either completes fully or leaves the provider in a consistent state.
- [x] Failure-path tests cover a failed add/delete and verify cleanup or rollback.
- [x] No production-dead merge plan remains; tests exercise the real merge path.
- [ ] Favorites and group operations remain covered by unit/instrumentation tests.

## Status

Done, except the last point: the favorites provider itself is still covered only indirectly
(by UI tests), because a unit test of `ContentResolver` needs a fake provider or Robolectric —
that work is tracked in issue 17.

What changed:

- `FavoritesDataSourceImpl` runs every provider call in `Dispatchers.IO`; the observable path
  reads and migrates under `withContext(Dispatchers.IO)`.
- The group provider was split: `GroupsProvider` (interface) + `ContactsContractGroupsProvider`
  (queries and batches) + `GroupsDataSourceImpl` (thin adapter over IO dispatcher).
- The merge is planned as data (`GroupMergePlan`, `planGroupMerge`) and applied by one call:
  the membership inserts and the deletion of the source group go into a single `applyBatch`,
  so the provider either applies everything or rolls everything back.
- `GroupsDataSourceImplTest` covers the merge path: members are moved, the members that are
  already in the target group are skipped, a group is not merged into itself, an empty group
  only deletes itself, and a failed merge leaves the provider state unchanged.
- `MergePlans.mergeContactForms` was deleted: the production merge links raw contacts
  (aggregation exceptions), so the aggregated contact already shows the data of both sides and
  writing merged forms into the target would duplicate it. The real merge path is covered by
  `ContactsRepositoryImplTest`.

## Technical notes

- `data/src/main/java/com/m4isper/contactsapp/data/datasource/FavoritesDataSourceImpl.kt`
- `data/src/main/java/com/m4isper/contactsapp/data/datasource/GroupsProvider.kt`
- `data/src/main/java/com/m4isper/contactsapp/data/datasource/ContactsContractGroupsProvider.kt`
- `data/src/main/java/com/m4isper/contactsapp/data/datasource/GroupsDataSourceImpl.kt`
- `data/src/main/java/com/m4isper/contactsapp/data/datasource/write/GroupMergePlan.kt`

## Out of scope

- Replacing `ContentResolver` or introducing a database cache.
- Changing group merge semantics or UI copy.
