# feat: replace oversized dialogs with bottom sheets and compact the contact editor

**Priority:** P3 — dialogs work, but selection flows are cramped on phones and the editor is unnecessarily tall
**Area:** dialogs / contact editor / groups / QR preview

## Problem

- Nearly every secondary flow is an `AlertDialog`: the group member picker puts a 320 dp-tall `LazyColumn` inside the dialog text slot (`presentation/groups/ContactPickerDialog.kt`), PIN setup stacks two text fields and an error line in a dialog (`presentation/settings/PinSetupDialog.kt`), and the QR preview shows a 240 dp image in a dialog (`presentation/details/QrPreviewDialog.kt`). On a phone the picker is cramped and the keyboard covers the confirm button; on a tablet a 560 dp dialog wastes the window.
- The contact editor is a long `LazyColumn` of per-field cards. Each phone row carries a kind menu, a delete button, a star ("primary"), and up/down arrows (`presentation/editor/PhoneEditorRow.kt`, `EditorRowToolbar.kt`). The toolbar consumes a full row per field, so a contact with four phones is very tall, and reordering by repeated arrow taps is tedious.
- Photo crop and date pickers are separate dialogs stacked over the form, each resetting scroll/focus on dismissal.
- There is no drag handle or reorder affordance; the primary-value star is ambiguous next to the favorite star used elsewhere in the app.
- The QR preview's loading spinner is hard-coded black on a white card; it does not follow the theme (also touched by issue 53/54).

## Proposed scope

- Convert selection/creation flows to `ModalBottomSheet`: group member picker (with search + sticky confirm bar), group creation, import source chooser, merge review (where it fits), and the QR preview (code + hint + share). Keep destructive confirmations as `AlertDialog`.
- Sheets must handle IME insets (`imePadding`), keep the confirm action pinned above the keyboard, and restore focus correctly on dismiss.
- Editor: compact each field row to a single line (leading kind chip, value, trailing overflow). Move "set primary", "move up/down", and delete into the row overflow; keep delete visible for empty optional rows.
- Add drag-to-reorder with a visible drag handle (or at minimum a reorder mode), replacing arrow taps; keep arrow actions in the overflow for TalkBack users.
- Collapse optional empty sections; show one "Add field" entry point instead of many placeholder cards.
- Sticky section headers and `imePadding` at the list level so focus does not hide the active field (extends issue 45's scroll-to-error work).
- Rename the primary-value affordance so it does not read as the contact favorite (e.g. "Primary number" label in the overflow, not a star icon).

## Acceptance criteria

- [ ] Member picker, group creation, import chooser, and QR preview are modal bottom sheets; delete/merge confirmations remain dialogs.
- [ ] With the keyboard open, the sheet's confirm action stays visible and the list scrolls above the IME.
- [ ] A field row is a single line at default font scale (≤ 64 dp) and exposes kind/value/actions without horizontal clipping at 320 dp.
- [ ] Reordering works by drag; TalkBack users can still move a field via custom actions, and setting a primary value is announced distinctly from favoriting a contact.
- [ ] Editor keeps scroll position and focus across rotation and sheet dismissal.
- [ ] Existing editor/groups UI tests still pass; new tests cover the sheet confirm-with-IME path and reorder.

## Technical notes

- Shared tokens and rules: `docs/design/visual-rules.md` (issue 54); sheet motion uses the `AppMotion` tokens.
- `app/src/main/java/com/m4isper/contactsapp/presentation/groups/ContactPickerDialog.kt` — `AlertDialog` with a `LazyColumn` in `text`; the confirm button uses a plural string, keep it.
- `app/src/main/java/com/m4isper/contactsapp/presentation/settings/PinSetupDialog.kt` — keep this one a dialog (security context, no scrolling content).
- `app/src/main/java/com/m4isper/contactsapp/presentation/details/QrPreviewDialog.kt` — candidate for a sheet; the QR must stay black-on-white regardless of theme.
- `app/src/main/java/com/m4isper/contactsapp/presentation/editor/` — `EditorScreen.kt`, `EditorForm.kt`, `PhoneEditorRow.kt`, `EditorRowToolbar.kt`.
- Compose: `ModalBottomSheet` + `SheetState(skipPartiallyExpanded = true)` for tall content; `WindowInsets.ime` handling; drag-and-drop via `Modifier.dragAndDropSource/Target` or a reorderable LazyColumn helper — pick one and document the choice.
- Issue 45 introduced scroll-to-first-error; keep it working when rows become compact and sections collapse.
- Test tags follow existing conventions (`contact_picker_dialog`, `editor_*`); keep them so current tests can be updated mechanically.

## Out of scope

- New editor capabilities (new field types, validation rules).
- Redesigning the photo crop UI (issue 46).
- Theming the QR dialog beyond making the placeholder follow the surface color.

## Verification

- Manual: small phone with keyboard open, tablet, foldable; light/dark; TalkBack reorder; add/edit contacts with 0/1/6 phones.
- `./gradlew :app:compileDebugKotlin :app:compileDebugAndroidTestKotlin :app:testDebugUnitTest :app:detekt :app:ktlintCheck`.
- `./gradlew :app:connectedDebugAndroidTest` for editor/group suites.
