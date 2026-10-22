# feat: support structured contact fields and primary values

**Priority:** P1  
**Area:** domain model / data layer / editor

## Problem

The contact model is intentionally simple, but it loses data supported by `ContactsContract`:

- Postal addresses are a single string. `ContactsWriterValues.addressValues()` writes it to `STREET` and nulls `CITY`, `REGION`, `POSTCODE`, `COUNTRY`.
- Organization has only company and title; there is no department, job description, or office location.
- There is no SIP address.
- Phone/email/website kinds are limited; websites always save as homepage.
- Nickname, note, and organization are single-valued; additional provider rows are not shown or edited.
- There is no way to mark a primary phone/email or reorder rows; `primaryPhone` is just the first item.

## Proposed scope

- Introduce structured address fields (street, PO box, neighborhood, city, region, postcode, country) in domain models, editor, read/write mapping, and vCard `ADR`.
- Add organization department, job description, and office location; map them to `Organization.DEPARTMENT`, `JOB_DESCRIPTION`, and `OFFICE_LOCATION`.
- Add SIP address (`SipAddress`).
- Extend kind enums for common provider types (fax, pager, callback, company main; email/website types) while keeping "Other + custom label".
- Support multiple nicknames/notes/organizations, or explicitly preserve untouched rows and document the limitation.
- Add primary selection and row reordering for phones/emails; reflect it in the list subtitle and details.
- Preserve backward compatibility: contacts created by the old version must still open and save.
- Add unit tests for mapping and editor save plans.

## Acceptance criteria

- [x] Editing a structured address preserves city/region/postcode/country.
- [x] New organization fields round-trip through the provider.
- [x] SIP and extended types can be added, edited, and displayed.
- [x] Multiple values are either fully supported or untouched rows are preserved on save.
- [x] The primary phone/email is respected by the list subtitle and details.
- [x] vCard `ADR` stays structured on export/import.

## Status

Done. Addresses are edited as street/city/region/postcode/country with a fallback to the legacy one-line value; the organization carries company, title and department; SIP addresses and extended kinds are supported; the primary phone and email are used in the list subtitle and in the details. Saving reconciles the rows of all raw contacts of a contact and leaves untouched rows alone, and the vCard `ADR` stays structured on export and import. Covered by unit tests in the data layer and by an instrumentation test for the round trip.

## Technical notes

- `domain/src/main/java/com/m4isper/contactsapp/domain/model/ContactDetails.kt`, `ContactForm.kt`.
- `data/src/main/java/com/m4isper/contactsapp/data/datasource/write/ContactsWriterValues.kt`, `ContactsWritePlans.kt`; `data/src/main/java/com/m4isper/contactsapp/data/datasource/read/ContactsRowsReader.kt`.
- `data/src/main/java/com/m4isper/contactsapp/data/datasource/read/ContactsExtraMappers.kt`, `ContactsDataMappers.kt`.
- `app/src/main/java/com/m4isper/contactsapp/presentation/editor/AddressEditorRow.kt`, `EditorExtraItems.kt`.
- Schema changes affect the vCard codec in `data/src/main/java/com/m4isper/contactsapp/data/vcard/`.

## Out of scope

- Custom MIME fields.
- Contact presence/status.
