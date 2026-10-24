# fix: refresh group details after undo delete

**Priority:** P2
**Area:** groups

## Problem

When a group is deleted, `GroupDetailViewModel` stores the deleted group and emits `GroupDeleted`. Undo creates a new group with a new ID, but `groupIdFlow` still points at the old ID and the screen is not refreshed. `deletedGroupFlow` is also not cleared, so repeated undo attempts can be unsafe.

## Proposed scope

- Update `groupIdFlow` to the newly created group ID after undo.
- Clear `deletedGroupFlow` after a successful restore.
- Refresh group details and membership after restore.
- Add tests for delete and undo flows.

## Acceptance criteria

- [ ] Undo delete shows the restored group.
- [ ] Membership is restored to the new group.
- [ ] Repeated undo does not reuse stale deleted-group state.
- [ ] Group details no longer show "group not found" after undo.

## Technical notes

- `app/src/main/java/com/m4isper/contactsapp/presentation/groups/GroupDetailViewModel.kt`
- `app/src/main/java/com/m4isper/contactsapp/presentation/groups/GroupDetailScreen.kt`

## Out of scope

- Changing group deletion semantics.
