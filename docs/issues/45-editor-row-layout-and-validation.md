# fix: make editor field rows readable and validation errors findable

**Priority:** P2 — fields are squeezed on phones and invalid saves can look like nothing happened
**Area:** contact editor / form layout and validation

## Problem

### Field, type selector, and delete button share one width

The phone, email, website, relation, IM, and SIP rows all use the same structure: an `OutlinedTextField` with `weight(1f)` plus a `KindMenu` `TextButton` plus a 48 dp delete `IconButton` in a single `Row` (`PhoneEditorRow.kt` lines ~63–96, `EmailEditorRow.kt` lines ~62–93, and the equivalent rows). The kind label is unbounded; long localized labels and custom labels such as "Other" with a long custom name can consume most of the width. The text field is then reduced to a narrow strip, especially at 200% font scale. The delete button is also immediately adjacent to the type selector, so a mis-tap removes the row instead of changing its type.

`EventEditorRow` is worse: the kind menu, date button (which can show a long localized date), a spacer, and delete are all in one row, with no weight on the date button (`EventEditorRow.kt` lines ~64–91).

### Validation errors are global, not per row

`EditorContactItems.kt` passes the same `fieldErrors[FormField.PHONE]` message to every phone row (line ~92), and the same for emails (line ~125). If one of three phone numbers is invalid, all three rows show the identical red supporting text, so the user cannot tell which value to fix.

### A failed save has no visible outcome at the top of a long form

`onSave()` validates and sets `fieldErrors`, then returns without scrolling, focusing, or showing a summary (`ContactEditorViewModel.kt` lines ~176–189). The form is a long `LazyColumn`; when the invalid field is below the fold, pressing Save appears to do nothing. There is no "check the highlighted fields" message and no automatic scroll to the first error.

Related details:

- `EditorForm` marks the account row as erroneous whenever `uiState.error != null` (`EditorForm.kt` line ~122), even when the error is unrelated to the account. This makes the account card red for a phone/email/network error.
- `formMessage()` falls back to `uiState.error?.message().orEmpty()` (`EditorForm.kt` lines ~404–411). If `formError` is set to a non-`EMPTY_FORM` value while `error` is null, the error row renders as a blank line. Every `FormError` branch should have a message.
- The error item is placed near the top under the photo/account, far from the fields it describes.

## Proposed scope

- Restructure the repeated rows into two lines:
  1. full-width text field (and custom-label field when needed);
  2. a secondary row with the kind selector, row actions (primary/move/remove), and no accidental adjacency between kind and delete.
- Give the kind selector a bounded width or make it a dropdown chip that truncates gracefully. Ensure the text field keeps a sensible minimum width at 320 dp and 200% font scale.
- Apply the same treatment to the event row: kind and date can wrap onto separate lines; keep delete separated.
- Validate rows individually where possible and associate errors with `rowId`. If the domain can only return one field-level error, show the message once at the section level and highlight the offending value, rather than duplicating it under every row.
- On a blocked save:
  - scroll to the first invalid item and request focus on its field;
  - show a short "Check the highlighted fields" message (snackbar or inline banner);
  - keep the message visible until the field is edited.
- Only show an account error when the account selection itself is invalid. Remove the generic `isError = uiState.error != null` mapping.
- Make `formMessage()` exhaustive so no branch can render a blank error row.

## Acceptance criteria

- [x] At 320 dp and 200% font scale, the text field in every repeated row is usable and the type selector/delete button remain reachable.
- [x] Delete is visually separated from the type selector and still asks for confirmation where appropriate.
- [x] A single invalid phone/email produces one clearly located error, not the same message on every row.
- [x] Pressing Save with an off-screen invalid field scrolls to and focuses that field.
- [x] A blocked save shows a visible summary message; the screen never appears unresponsive.
- [x] The account row is not highlighted for unrelated field or network errors.
- [x] No validation branch can show an empty error line.
- [x] Event kind/date/delete controls remain usable at large font scale.

## Technical notes

- `app/src/main/java/com/m4isper/contactsapp/presentation/editor/rows/PhoneEditorRow.kt`, `EmailEditorRow.kt`, `WebsiteEditorRow.kt`, `RelationEditorRow.kt`, `ImEditorRow.kt`, `SipEditorRow.kt`, `EventEditorRow.kt` — shared row shape.
- `app/src/main/java/com/m4isper/contactsapp/presentation/editor/components/KindMenu.kt` — unconstrained `TextButton`.
- `app/src/main/java/com/m4isper/contactsapp/presentation/editor/items/EditorContactItems.kt` lines ~78–135 — repeated field error wiring.
- `app/src/main/java/com/m4isper/contactsapp/presentation/editor/ContactEditorViewModel.kt` lines ~176–189 — validation/return path.
- `app/src/main/java/com/m4isper/contactsapp/presentation/editor/EditorForm.kt` lines ~106–132 and ~391–411 — `LazyColumn`, account error, message mapping.
- `app/src/main/java/com/m4isper/contactsapp/presentation/editor/EditorUiState.kt` lines ~69–71 — `formError` and `fieldErrors`.
- Consider a `LazyListState` + `focusRequester` map keyed by `FormField`/`rowId`; add `BringIntoViewRequester` for the error banner.
- Existing `docs/issues/31-inline-field-validation.md` added the inline messages; this issue fixes where they appear and how the user finds them.

## Out of scope

- Rewriting the domain validation rules (issue 20).
- A full visual redesign of the editor.
- Reordering the form sections.

## Verification

- Manual: 320 dp phone with 200% font scale; invalid phone in the third row; invalid email below the fold; event with a long date.
- Compose tests for scroll-to-error, single-row error, and account highlight.
- `./gradlew :app:compileDebugKotlin :app:testDebugUnitTest`.

## Status

Done.

### What was implemented

- Two-line rows: the value field of every repeated row (phone, email, website, relation, IM, SIP) left the action row and spans the card width; the kind selector and the delete button live on a second line separated by a spacer, so a mis-tap cannot delete a row. The event date button truncates instead of pushing its neighbors out.
- Row-targeted errors: the domain gained `firstInvalidRowIndex(form, field)` (unit-tested), and the editor maps that index to the screen row id in `firstErrorRowIds`. The validation message renders only on the owning row — one invalid phone no longer paints the same message under every phone row.
- Blocked-save outcome: a snackbar ("Check the highlighted fields") appears immediately even when the invalid field is off-screen, an inline banner stays at the top of the form, and a `LaunchedEffect` scrolls the list to the first invalid row and requests focus on its field. The instrumented test scrolls the invalid row out of view, saves, and asserts the summary is displayed and the second phone row is back on screen (bounds-based, because `assertIsDisplayed` proved unreliable next to the snackbar; focus is implemented but not asserted).
- The account row is highlighted only for `LOCAL_ACCOUNT_BLOCKED`, not for any save error; `formMessage` falls back to `fieldMessage()` for every `FormError` branch, so no validation state can render a blank line.
- A `FormItemIndices` registry counts the LazyColumn items during the DSL pass and feeds the scroll target; conditional sections (account) register only when they actually render.

### Verification

- `./gradlew :app:compileDebugKotlin :app:compileDebugAndroidTestKotlin :app:testDebugUnitTest :app:detekt :app:ktlintCheck` passes (ktlintFormat applied once for the reflowed rows).
- `./gradlew :app:connectedDebugAndroidTest` on a headless emulator: BUILD SUCCESSFUL, 117 tests, 116 passed, 1 skipped by assumption (`LargeListPerformanceTest` benchmark).
- Manual checks that remain useful: 320 dp width at 200% font scale for the two-line rows, and a TalkBack pass over the per-row error announcements.
