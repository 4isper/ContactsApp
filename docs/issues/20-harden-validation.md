# fix: harden domain validation for email, URL, and phone

**Priority:** P2  
**Area:** domain / contact editor

## Problem

Validation is in the right layer (domain/`ContactForm`), but it is shallow:

- email, URL, and phone checks are simple regex/format checks with many false positives and false negatives;
- international phone formats and URL edge cases are not normalized consistently;
- the editor can accept values that later fail in the provider or in vCard export;
- validation logic risks being duplicated between domain and UI.

## Proposed scope

- Introduce value objects for `Email`, `PhoneNumber`, and `Url` with normalization and validation.
- Normalize phone numbers to E.164 (for example via libphonenumber in data/infrastructure behind a domain port).
- Use a proper URL parser and cover IDN, scheme, and host edge cases.
- Keep validation messages user-facing but derive them from domain errors.
- Add table-driven tests for valid and invalid values, including international input.

## Acceptance criteria

- [ ] Valid international email/phone/URL values are accepted and normalized consistently.
- [ ] Invalid values are rejected with a clear, stable error.
- [ ] Validation exists in one place; UI does not reimplement the rules.
- [ ] Edge-case tests cover international phones, IDN/URLs, and malformed input.

## Technical notes

- `domain/src/main/java/com/m4isper/contactsapp/domain/model/ContactForm.kt`
- `app/src/main/java/com/m4isper/contactsapp/presentation/editor/ContactEditorViewModel.kt`
- vCard import/export validation paths.
- `README.md` — current validation claims, if they need updating.

## Out of scope

- Full RFC 5322/URL compliance.
- Server-side validation.
