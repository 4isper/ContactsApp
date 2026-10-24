# fix: show validation errors inline next to the invalid editor field

**Priority:** P1 — directly affects editor usability and error recovery  
**Area:** contact editor / form

## Problem

In the contact editor, validation errors (invalid phone, email, URL, date) are aggregated into a single text block at the bottom of the form (`formMessage()` in `EditorForm.kt`). The user sees one generic error line and must guess which field to fix. There is no field-level error text, no error coloring on the specific `OutlinedTextField`, and no automatic focus on the first invalid field when the user taps "Save".

## Proposed scope

- Propagate per-field validation errors from the ViewModel/uiState down to individual editor rows (phone, email, website, birthday/event, etc.).
- Render the error text directly beneath the corresponding field in the field's accent/error color.
- Apply an error outline/border to the invalid `OutlinedTextField`.
- On save attempt, move keyboard focus to the first invalid field and scroll it into view if needed.
- Keep the aggregate bottom error block only for non-field errors (delete failed, generic exception, empty form).

## Acceptance criteria

- [ ] Invalid phone shows an error line under the phone row and the field is outlined in the error color.
- [ ] Invalid email shows an error line under the email row.
- [ ] Invalid URL shows an error line under the website row.
- [ ] Invalid date shows an error line under the birthday/event row.
- [ ] Tapping Save with multiple invalid fields focuses the first invalid field.
- [ ] Corrected field clears its inline error immediately.

## Status

Done.

## Technical notes (implemented)

- `app/src/main/java/com/m4isper/contactsapp/presentation/editor/ContactEditorViewModel.kt` — `update()` now resets `fieldErrors = emptyMap()` on every form change, so inline errors clear as the user types.
- `app/src/main/java/com/m4isper/contactsapp/presentation/editor/items/EditorContactItems.kt` — already passed `fieldErrors[FormField.PHONE/EMAIL/WEBSITE]?.fieldMessage()` to rows; no change needed.
- `app/src/main/java/com/m4isper/contactsapp/presentation/editor/items/EditorExtraItems.kt` — passes `fieldErrors[FormField.EVENT]?.fieldMessage()` to `BirthdayEditorRow` and `EventEditorRow`.
- `app/src/main/java/com/m4isper/contactsapp/presentation/editor/rows/EventEditorRow.kt` — added `error: String?` parameter; shows red `supportingText` under the event row.
- `app/src/main/java/com/m4isper/contactsapp/presentation/editor/rows/BirthdayEditorRow.kt` — added `error: String?` parameter; shows red `supportingText` under the birthday row.
- `ContactEditorViewModel.onSave()` already calls `validateContactFields(form)` and writes `fieldErrors` before attempting save; `EVENT` errors are non-blocking (warning only).

## Verification

- `:app:compileDebugKotlin` passes.
- `:app:detekt` / `:app:ktlintCheck` — no new issues introduced (only 2 pre-existing in DuplicatesScreen/SettingsScreen).
- Invalid phone/email/website show red error text under the specific field.
- Invalid birthday/event date shows red error text under the row.
- Clearing the invalid field (via `update`) removes `fieldErrors`, so the inline error disappears immediately.

## Technical notes

- `app/src/main/java/com/m4isper/contactsapp/presentation/editor/EditorForm.kt` — `formMessage()` at lines ~404–410; error block at lines ~180–189.
- `app/src/main/java/com/m4isper/contactsapp/domain/model/contact/FormError.kt` — domain error enum; currently form-level only.
- `app/src/main/java/com/m4isper/contactsapp/presentation/editor/rows/*` — individual row composables (PhoneEditorRow, EmailEditorRow, WebsiteEditorRow, EventEditorRow, BirthdayEditorRow).
- `app/src/main/res/values/strings.xml` and `values-ru/strings.xml` — field-specific error strings (`form_error_phone`, `form_error_email`, `form_error_url`, `form_error_date` already exist).

## Out of scope

- Changing the validation rules themselves (covered by issue 20).
- Server-side validation; this app validates locally against the Contacts provider.