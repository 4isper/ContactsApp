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

- [x] Valid international email/phone/URL values are accepted and normalized consistently.
- [x] Invalid values are rejected with a clear, stable error.
- [x] Validation exists in one place; UI does not reimplement the rules.
- [x] Edge-case tests cover international phones, IDN/URLs, and malformed input.

## Status

Done.

- The domain has the values of the form: `Email`, `Phone` and `Url` normalize and check
  themselves, and the value object is created only for a valid input.
- The email keeps its local part, the domain is lower cased and must consist of the labels with
  a letter top level domain.
- The phone keeps only the digits and the leading «+», the length is as in E.164, and the
  formatting (spaces, dashes, brackets) disappears.
- The site gets the `https` scheme, the host is lower cased and converted to ASCII, so an
  international domain works; only `http` and `https` are allowed.
- The rules live in one place: the validation of the form uses the value objects, and the
  normalized form stores the normalized values. The interface only shows the message of the
  error, it does not check anything itself.
- The tests are table driven: the valid and the invalid values of every kind are covered,
  including the international phone, the IDN domain and the malformed input.

## Technical notes

- `domain/src/main/java/com/m4isper/contactsapp/domain/model/ContactForm.kt`
- `app/src/main/java/com/m4isper/contactsapp/presentation/editor/ContactEditorViewModel.kt`
- vCard import/export validation paths.
- `README.md` — current validation claims, if they need updating.

## Out of scope

- Full RFC 5322/URL compliance.
- Server-side validation.
