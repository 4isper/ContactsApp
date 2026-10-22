# fix: make duplicate normalization locale-independent

**Priority:** P1  
**Area:** duplicates / correctness

## Problem

`ContactDuplicates.normalized()` calls `lowercase()` without an explicit locale. The result depends on the device locale: in Turkish, `I` lowercases to `ı` instead of `i`, so duplicate detection and email/name comparison can produce different results on different devices for the same data.

The same normalization may also be used by search or grouping, so a locale-sensitive implementation can cause inconsistent matching across screens.

## Proposed scope

- Use `lowercase(Locale.ROOT)` for all identifier-like normalization (emails, names used for matching, search keys).
- Apply Unicode normalization (for example NFKC) before case folding where it affects matching.
- Keep one shared normalization function for duplicates, search, and grouping.
- Add tests that run under a Turkish locale and cover Unicode/case edge cases.

## Acceptance criteria

- [x] Duplicate normalization does not depend on the default device locale.
- [x] Tests cover Turkish `I`/`i`, uppercase/lowercase variants, and Unicode normalization.
- [x] Search and duplicate detection use the same normalization rules.
- [x] No duplicate/grouping behavior regressions on the existing test suite.

## Status

Done. The normalization lives in one place — `domain/model/ContactText.kt`:

- `normalizeContactText` — NFKC, trimming, `lowercase(Locale.ROOT)`;
- `normalizeContactTextForMatch` — the same plus collapsing of repeated spaces;
- `normalizePhoneDigits` — digits of a phone number.

It is used by `ContactDuplicates`, the search, the grouping, the duplicate detection and the
import duplicates, so all of them match the same data the same way. The widget and the recent
contacts list use it too, instead of their own `lowercase()`.

Tests run under `tr-TR` for the search (`ContactSearchTest`) and for the duplicate detection
(`DuplicateDetectionTest`): `I`/`i` and `INFO`/`info` must match regardless of the device locale.

## Technical notes

- `domain/src/main/java/com/m4isper/contactsapp/domain/model/ContactText.kt`
- `domain/src/main/java/com/m4isper/contactsapp/domain/model/ContactDuplicates.kt`
- `domain/src/main/java/com/m4isper/contactsapp/domain/contacts/ContactSearch.kt`
- `domain/src/main/java/com/m4isper/contactsapp/domain/duplicates/DuplicateGroup.kt`

## Out of scope

- Changing the fuzzy-matching algorithm or duplicate confidence thresholds.
- Server-side or cross-device deduplication.
