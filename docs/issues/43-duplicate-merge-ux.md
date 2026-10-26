# fix: make duplicate merging clearer and safe to review

**Priority:** P1 — import can merge into the wrong contact, and long previews become unreachable
**Area:** import preview / duplicates / merge dialogs

## Problem

### Import always merges into the first duplicate

In `ImportPreviewScreen`, an imported contact that matches several existing contacts shows only `entry.duplicates.first().name` (lines ~239–247), and the Merge chip always selects `entry.duplicates.first().contactId` (lines ~274–283). The user cannot see the other candidates or choose which contact should receive the imported fields. Choosing "Merge" can therefore modify an unintended contact, with no undo in the import flow.

The three decision chips are also placed in a plain `Row` (lines ~264–290). Longer translations ("Create", "Merge", "Skip" plus icons/padding) or a 200% font scale overflow the card instead of wrapping.

### Merge preview cannot be scrolled

`DuplicatesScreen`'s `MergePreviewDialog` renders every addition in a plain `Column` inside `AlertDialog` (`DuplicatesScreen.kt` lines ~202–250). A duplicate group with many phones, emails, addresses, notes, and events produces more rows than the dialog can display. The text and the confirm button are clipped, especially in landscape or at large font scale.

The same problem exists in `EditorForm`'s `SimilarContactsDialog` (`EditorForm.kt` lines ~339–379), which lists every similar contact in an unbounded `Column`.

### Birthday is shown as a data-class dump

`MergePreview.kt` builds the birthday value with `source.birthday.toString()` (line ~99), so the preview shows `Birthday(day=14, month=3, year=null)` instead of a localized date. The details screen already uses `birthdayText()` for this purpose.

## Proposed scope

- When an imported contact has more than one duplicate, show all candidates (or a count with an expandable list) and let the user choose the merge target. Display enough context to distinguish same-name contacts: phone/email, account, and photo if available.
- Keep the chosen target per entry in UI state; do not silently fall back to the first duplicate when the list changes. If the selected target disappears, clear the merge decision and ask the user to choose again.
- Reflect the selected target in the Merge chip label or a summary line ("Merge into Alex (work)").
- Replace the decision chip `Row` with `FlowRow` (or a column at large font scale) so actions wrap.
- Make `MergePreviewDialog` and `SimilarContactsDialog` scrollable with a bounded height. Use `LazyColumn` for the similar-contacts list and `verticalScroll`/`LazyColumn` for merge additions, keeping the dialog buttons reachable.
- Format the birthday using `birthdayText()` and keep all other field values localized/readable.
- Add a confirmation summary or undo path for import merges if the architecture allows it; at minimum make the target unambiguous before the write.

## Acceptance criteria

- [ ] With two or more duplicates, the import card lists all candidates and requires an explicit target choice before Merge.
- [ ] The merge decision stores and uses the selected contact id, not `duplicates.first()`.
- [ ] The selected target is visible in the card and survives configuration changes where the screen state is retained.
- [ ] The merge preview is scrollable and its confirm/cancel buttons remain visible with 20+ additions and at 200% font scale.
- [ ] The similar-contacts dialog is scrollable and does not push its buttons off-screen.
- [ ] Birthdays in the merge preview are localized dates, never `Birthday(day=..., month=..., year=...)`.
- [ ] Decision chips wrap instead of clipping on a 320 dp screen.
- [ ] Canceling the merge preview leaves the duplicate group unchanged.

## Technical notes

- `app/src/main/java/com/m4isper/contactsapp/presentation/import/ImportPreviewScreen.kt` lines ~210–290 — duplicate text, target selection, chips.
- `app/src/main/java/com/m4isper/contactsapp/presentation/import/ImportPreviewViewModel.kt` and `ImportPreviewUiState` — where per-entry decisions live.
- `domain/src/main/java/com/m4isper/contactsapp/domain/model/contact/ImportDecision.kt` (or the model file) — `ImportDecision.Merge(targetId)` already supports a target.
- `app/src/main/java/com/m4isper/contactsapp/presentation/duplicates/DuplicatesScreen.kt` lines ~202–250 — `MergePreviewDialog`.
- `app/src/main/java/com/m4isper/contactsapp/presentation/duplicates/MergePreview.kt` line ~99 — raw `toString()`.
- `app/src/main/java/com/m4isper/contactsapp/presentation/common/format/BirthdayFormat.kt` — `birthdayText()`.
- `app/src/main/java/com/m4isper/contactsapp/presentation/editor/EditorForm.kt` lines ~339–379 — `SimilarContactsDialog`.
- Add strings for the target chooser and selected-target summary in both locales.
- `docs/issues/26-duplicate-merge-correctness.md` — separate data-correctness work; this issue is about preventing an ambiguous user choice and unreachable UI.

## Out of scope

- Redesigning the duplicate matching algorithm.
- Selecting which individual fields to merge; this issue only chooses the destination contact.
- Undo for import merges beyond a possible confirmation step.

## Verification

- Import a vCard that matches two existing contacts; verify the chooser and the actual provider writes.
- Open a duplicate group with many fields at 200% font scale and in landscape.
- UI tests for target selection, scrolling, and canceled merges.
- `./gradlew :app:compileDebugKotlin :app:testDebugUnitTest`.
