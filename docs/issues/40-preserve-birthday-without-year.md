# fix: preserve "no year" birthdays and event dates

**Priority:** P1 — editing and saving can silently change a birthday to the year 2000
**Area:** contact editor / birthdays and events

## Problem

A birthday can legitimately have no year (the provider stores `--MM-dd`), and the editor shows a "No year" switch for it. That mode is not preserved:

- `BirthdayEditorRow` (lines ~82–108) turns the switch on by copying the date with `year = null`, but turning it off copies `year = LocalDate.now().year`. Merely toggling "No year" off therefore invents the current year, without the user choosing a date.
- `BirthdayPickerDialog` initializes a year-less birthday with `DEFAULT_PICKER_YEAR = 2000` (line 73) so the Material date picker can display it. On confirmation it always reads `date.year` and builds `Birthday(day, month, year = date.year)` (lines ~42–56). A no-year birthday opened and saved without changes becomes `2000-MM-dd`.
- The same code path is used for contact events, so anniversaries and other dated events have the same defect.
- There is no way to pick a day/month without a year inside the dialog; the date picker's calendar grid requires a year.

This is silent data corruption in a field users expect to be stable. It also changes reminder calculations and the displayed date format.

## Proposed scope

- Add explicit year-less mode to the shared date dialog. When the incoming value has `year == null`, open in that mode, display the day/month, and keep `year = null` on confirm unless the user explicitly enables a year.
- Provide a "No year" switch/checkbox inside the dialog (not only in the editor row) so the saved mode is visible at the moment of confirmation. Use a synthetic leap-safe year (for example 2000) only for the picker UI; never write it to the model in no-year mode.
- Turning the editor-row switch off should not fabricate the current year. Either open the year picker for an explicit choice, or require a year before the switch can be turned off; canceling the picker must restore the previous no-year value.
- When a year is chosen, store it normally; when "No year" is enabled, clear it. Persist through the existing `Birthday` model (`year: Int?`).
- Apply the same behavior to `EventEditorRow` and the event date picker path.
- Keep the displayed text via `birthdayText()` so the UI shows a localized day/month without a year.

## Acceptance criteria

- [ ] Open a contact with a year-less birthday, press the date button, then Save without changing anything — the birthday still has no year.
- [ ] Saving a year-less birthday writes `--MM-dd` to the provider, not a year.
- [ ] Turning "No year" off does not immediately set the current year; the user explicitly chooses or confirms a year.
- [ ] Canceling the date picker leaves the original value unchanged.
- [ ] Choosing a date with a year stores and displays that year.
- [ ] Events with no year behave the same way as birthdays.
- [ ] UI tests cover: no-year round trip, adding a year, removing a year, cancel.

## Technical notes

- `app/src/main/java/com/m4isper/contactsapp/presentation/editor/rows/BirthdayEditorRow.kt` lines ~82–108 — switch handler.
- `app/src/main/java/com/m4isper/contactsapp/presentation/editor/rows/EventEditorRow.kt` lines ~101–129 — duplicated switch logic.
- `app/src/main/java/com/m4isper/contactsapp/presentation/editor/dialogs/BirthdayPickerDialog.kt` — `DEFAULT_PICKER_YEAR`, initial millis, confirm conversion.
- `app/src/main/java/com/m4isper/contactsapp/presentation/editor/EditorForm.kt` lines ~220–242 — both dialog call sites.
- `domain/src/main/java/com/m4isper/contactsapp/domain/model/contact/ContactDetails.kt` lines ~171–188 — `Birthday.year` is nullable; `ContactEvent` reuses it.
- `app/src/main/java/com/m4isper/contactsapp/presentation/common/format/BirthdayFormat.kt` — display formatting already supports a missing year.
- Search the provider mapping for `--MM-dd` handling to ensure the no-year representation is not normalized into a year on save.

## Out of scope

- Changing the date picker library.
- Supporting partial dates other than missing year (for example unknown day).
- Changing reminder scheduling rules.

## Verification

- Manual round trip for a birthday with a year, without a year, and with an event date.
- Inspect the raw contact row after saving to confirm `--MM-dd` is preserved.
- Unit tests around the date dialog conversion and the editor row handlers.
- `./gradlew :app:compileDebugKotlin :app:testDebugUnitTest`.
