# feat: complete group management and contact editor polish

**Priority:** P2  
**Area:** groups / editor UX

## Problem

Groups can be created, renamed, deleted, and assigned from the contact editor or the selection top bar, but there is no group detail screen, no way to remove a contact from a group on the groups screen, and no group merge. Editor polish gaps: the photo can only be picked from the gallery (no camera or crop), validation is weak (only empty form and blank phone), there is no duplicate warning on create, IM addresses can only be copied, and deleting a group has no undo.

## Proposed scope

- Group detail screen: member list, remove member, add members, rename/delete.
- Merge groups (reassign memberships, delete source) with confirmation.
- Undo for group deletion, or at least a clear warning with the member count.
- Photo: camera capture and crop/zoom before saving; keep the gallery picker.
- Validation: email, URL, birthday/event dates, and phone; show inline errors.
- Duplicate warning while creating/saving a contact with a link to the duplicates screen.
- IM row action to open the relevant messenger (or a chooser) instead of copy-only.
- Add UI tests for group membership and editor validation.

## Acceptance criteria

- [ ] A group screen shows members and allows removing them.
- [ ] Two groups can be merged without losing memberships.
- [ ] Group deletion can be undone or is clearly confirmed with the member count.
- [ ] A photo can be taken with the camera and cropped.
- [ ] Invalid email/URL/date blocks saving with a clear message.
- [ ] Creating a likely duplicate shows a warning and a path to merge.
- [ ] The IM action opens a messenger/chooser when possible.

## Technical notes

- `app/src/main/java/com/m4isper/contactsapp/presentation/groups/GroupsScreen.kt`, `GroupsViewModel.kt`.
- `app/src/main/java/com/m4isper/contactsapp/presentation/common/groups/GroupPickerDialog.kt`.
- `app/src/main/java/com/m4isper/contactsapp/presentation/editor/EditorForm.kt`, `PhotoEditor.kt`, `ContactEditorViewModel.kt`.
- `domain/src/main/java/com/m4isper/contactsapp/domain/model/ContactForm.kt` — validation.
- `app/src/main/java/com/m4isper/contactsapp/presentation/details/DetailsContent.kt` — IM row.

## Out of scope

- Group colors/icons.
- Automatic group sync rules.
