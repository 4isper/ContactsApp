# refactor: move contact business rules from presentation to domain

**Priority:** P1  
**Area:** domain / presentation

## Problem

Several domain rules live in `:app/presentation` instead of `:domain`:

- `ContactSearch.kt` implements search and filtering.
- `ContactGrouping.kt` implements sorting and grouping.
- `DuplicateGroup.kt` implements duplicate detection.
- `UpcomingOccasions.kt` implements reminder date calculations.

These are not UI concerns. Because they live in the presentation module, they are harder to reuse, harder to test without Android/Compose, and can drift when screens change. The duplication risk is especially high for normalization and duplicate matching, which are also affected by the locale bug in `ContactDuplicates`.

## Proposed scope

- Extract pure domain services/use cases for search, sorting/grouping, duplicate detection, and upcoming occasions.
- Inject `Clock`, time zone, and `Locale` where time or case normalization matters; do not read global state inside the algorithms.
- Keep presentation responsible for collecting state, formatting labels, and rendering; it should call domain services, not implement them.
- Move or add JVM unit tests next to the extracted rules.
- Use one normalization implementation for search and duplicate detection.

## Acceptance criteria

- [x] Search, sorting/grouping, duplicate detection, and occasion calculations live in `:domain`.
- [x] Presentation files only adapt state and UI; they contain no matching/sorting algorithms.
- [x] Reminder calculations are deterministic under an injected clock and time zone.
- [x] Duplicate normalization has a single implementation shared by all callers.
- [x] Existing behavior is preserved and covered by domain unit tests.

## Status

Done. The rules moved to `:domain` and presentation only adapts state:

- `domain/contacts/ContactSearch.kt` — search and filtering (public `filterContacts`, `phoneDigitsMatch`).
- `domain/contacts/ContactGrouping.kt` — sorting, grouping, `letterIndices`, `FAVORITES_GROUP`.
- `domain/duplicates/DuplicateGroup.kt` — duplicate detection.
- `domain/duplicates/ImportDuplicates.kt` — duplicates for vCard import.
- `domain/reminders/UpcomingOccasions.kt` — upcoming birthdays and events.
- `domain/calendar/NextOccurrence.kt` — the next occurrence of a date, extracted from `CalendarIntents`.
- `domain/model/ContactText.kt` — the single normalization used by search, grouping and duplicates.

Time is no longer read from the global state inside the algorithms: `upcomingOccasions` and
`nextOccurrence` take the current date as an argument, and the call sites (`BirthdayReminderReceiver`,
`DetailsContent`) pass it explicitly. The rules work with calendar dates, so a time zone is not needed.

The local search of `ContactPickerDialog` was replaced by the domain one; `ContactsWidgetProvider`
and `RecentContacts` now use the shared normalization instead of their own `lowercase()`.

Tests moved to the domain module: `ContactSearchTest`, `ContactSortingTest`, `DuplicateDetectionTest`,
`UpcomingOccasionsTest`. `GroupContactsTest` stays in `:app`: it also checks the UI helper
`letterIndexAt`.

## Technical notes

- `domain/src/main/java/com/m4isper/contactsapp/domain/contacts/`
- `domain/src/main/java/com/m4isper/contactsapp/domain/duplicates/`
- `domain/src/main/java/com/m4isper/contactsapp/domain/reminders/`
- `domain/src/main/java/com/m4isper/contactsapp/domain/calendar/`
- `domain/src/main/java/com/m4isper/contactsapp/domain/model/ContactText.kt`

## Out of scope

- Redesigning the search/duplicates/reminders UI.
- Changing notification delivery; scheduling stays outside the domain.
