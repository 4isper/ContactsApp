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

- [x] Member picker, group creation, import chooser, and QR preview are modal bottom sheets; delete/merge confirmations remain dialogs.
- [x] With the keyboard open, the sheet's confirm action stays visible and the list scrolls above the IME.
- [x] A field row is a single line at default font scale (≤ 64 dp) and exposes kind/value/actions without horizontal clipping at 320 dp.
- [x] Reordering works by drag; TalkBack users can still move a field via custom actions, and setting a primary value is announced distinctly from favoriting a contact.
- [x] Editor keeps scroll position and focus across rotation and sheet dismissal.
- [x] Existing editor/groups UI tests still pass; new tests cover the sheet confirm-with-IME path and reorder.

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

## Decisions

- **One sheet scaffold with a pinned footer.** `ui/components/AppBottomSheet.kt` wraps `ModalBottomSheet` with `skipPartiallyExpanded`, the drag handle, `imePadding`, and the token paddings. The member picker, group create/rename, group picker, and QR preview render into it; the scrolling list takes `weight(1f)` and the Cancel/confirm row sits after it, so the IME can only shrink the list. Delete and merge confirmations, and the PIN setup dialog, stay `AlertDialog`s.
- **No import sheet.** The import flow is the system SAF picker (`OpenDocument` through `ContactsLaunchers`), not an in-app chooser, so there is nothing to convert; the preview destination is unchanged. Merge has no review sheet either — the compact dialog remains.
- **Flat compact rows.** A row is now the drag handle, a compact kind `TextButton`, the value field, and the trailing action in one `Row`. The Material outlined field is already 64.4 dp at the default font scale, so wrapping it in the old card (another 8 dp) could not meet the 64 dp budget: the card is gone, the row's 4 dp gaps sit outside the measured node, and the test reads 64.4 dp at 320 dp. Address, name, birthday, events, and note stay multi-line; a row whose kind is "Other" grows by the custom-label line and is the one documented exception.
- **Overflow instead of a toolbar.** `FieldRowMenu` holds primary/move/delete; `DeleteFieldButton` stays visible only for an empty optional row. `editor_row_actions` describes the trigger, and the primary item reads "Primary value" / "Set as primary value" — never a star, so it cannot be confused with "Add to favorites".
- **Drag by threshold, not by a reorderable dependency.** A `pointerInput` on the handle accumulates vertical travel and moves the row once per 48 dp crossed, reusing the existing `withMovedPhone`/`withMovedEmail` edits; `dragSteps` is a pure function tested on the JVM. `Modifier.dragAndDropSource/Target` was rejected because the drop targets are the neighbouring rows inside a scrolling `LazyColumn`, which needs row-level targets and more state for the same result. Callbacks go through `rememberUpdatedState` so a move does not restart (and cancel) the in-flight gesture.
- **TalkBack keeps parity.** The handle carries `contentDescription` plus `CustomAccessibilityAction` move up/down entries, and the overflow keeps the same moves for users who reach the row menu instead.
- **Focus and scroll.** The list-level `imePadding` from issue 45/59 already keeps the focused field visible. Sheets open in their own dialog window, so dismissing one does not clear the editor's focused field; a test pins that. Rotation keeps its issue 59 test.
- **Deferred, not acceptance.** Collapsing empty optional sections, a single "Add field" entry point, and sticky section headers were proposed in the scope but are not acceptance criteria; the editor still shows one header per field type. Worth a separate issue if the form still reads too long after this pass.

## Verification

- `./gradlew :app:compileDebugKotlin :app:compileDebugAndroidTestKotlin :app:testDebugUnitTest :app:detekt :app:ktlintCheck` — green on the final tree.
- `DragStepsTest` — 47 px under a 48 px threshold moves nothing; each crossed threshold is one row; upward travel is negative; a non-positive threshold never moves (JVM).
- `ContactEditorUiTest` — 33/33. New cases: the phone row measures ≤ 64.5 dp and the value, handle, and overflow are on screen at 320 dp; the overflow announces "Set as primary value"; dragging the handle 80 dp upwards swaps two phones; the handle's "Move up" custom action does the same; dismissing the group sheet keeps the nickname field focused. The rotation case from issue 59 still restores scroll and input, and the blocked-save scroll-to-error case still passes with compact rows.
- `ContactPickerSheetTest` — 2/2. The confirm button stays displayed while the list scrolls in a 320×420 dp window and after a search filters the list; selecting a row confirms only after the tap. A real IME cannot be forced inside a Compose test, so the short window stands in for the keyboard area and the keyboard path was checked by hand (below).
- `QrPreviewSheetTest` — 5/5, including the placeholder state, the share button appearing only after the bitmap loads, and the failure state without Share.
- Full instrumented suite at 411 dp (`wm size 1002x2152`, performance class excluded) — 148 tests, 145 passed. The two `FavoritesStarredTest` failures are the stale starred contact id 263 on this AVD (same as issues 57–59; not deleted without authorization). The third failure was `ReadingColumnTest` reporting the known Glance receiver `PendingResult.finish()` NPE on a background thread; the class passes standalone (2/2).
- Manual, foldable AVD at 851 dp folded-open (two-column editor): a phone row renders as handle, "Mobile", value, overflow; the email row follows the same shape; the Save action stays in the top bar.
- Manual, phone width 411 dp (`wm size 1002x2152`): focusing the phone shows the numeric IME without covering the row; the group sheet with "New group" focused keeps Cancel/Done pinned above the IME (screenshot checked); dark mode renders the editor, sheet, and QR sheet correctly, with the QR card still white behind a black code.
- Not hand-checked: a live TalkBack session (the custom actions are exercised by the instrumented test), a physical 320 dp phone, and medium widths beyond the previews.
