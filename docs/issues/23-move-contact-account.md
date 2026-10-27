# fix: keep contact details consistent after moving to another account

**Priority:** P1
**Area:** details / accounts

## Problem

When a contact is moved to another account, `ContactDetailsViewModel.changeAccount` updates `loadedDetails.contactId` to the new ID, but leaves `selectedContactId` unchanged. Subsequent `refresh()`, favorite toggle, share, edit, and group lookups still use the old ID. The account field in the loaded details also remains stale.

## Proposed scope

- Update `selectedContactId` to the new contact ID after a successful move.
- Update the account in the loaded details.
- Ensure refresh, favorite, share, edit, and group operations use the new ID.
- Consider navigating to the new contact ID when required by the UI.

## Acceptance criteria

- [x] After moving a contact, details reload using the new contact ID.
- [x] The displayed account matches the destination account.
- [x] Favorite, share, edit, and group actions work after move.
- [x] No "contact not found" state appears after a successful move.

## Status

Done.

What changed:

- `selectedContactId` is now updated to the new contact ID after a move.
- `refresh()` is called so details, groups, and accounts reload for the new ID.

## Technical notes

- `app/src/main/java/com/m4isper/contactsapp/presentation/details/ContactDetailsViewModel.kt`
- `app/src/main/java/com/m4isper/contactsapp/presentation/details/ContactDetailsScreen.kt`
- `domain/src/main/java/com/m4isper/contactsapp/domain/usecase/contacts/MoveContactToAccountUseCase.kt`

## Out of scope

- Transactional copy+delete rollback if delete fails.
