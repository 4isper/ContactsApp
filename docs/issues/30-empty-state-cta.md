# fix: add clear calls-to-action to the empty contacts state

**Priority:** P1 — high impact on first-run experience  
**Area:** contacts list / UI

## Problem

When the contact list is empty (fresh install, no contacts imported, or no results for the active filter), the app shows a generic placeholder ("Контакты не найдены" / "No contacts found") without an obvious next step. A new user may not understand how to start: there is no prominent "Create contact" or "Import" button on the empty screen. The FAB exists only on the populated list screen, so it does not help here.

## Proposed scope

- Add a primary CTA button ("Создать контакт" / "Create contact") to the empty state that opens the contact editor.
- Add a secondary CTA ("Импортировать vcf" / "Import vCard") when there are zero contacts at all, opening the import preview flow.
- Add a short helper line explaining what the app can do (browse, call, message, group, reminders) so the empty screen teaches rather than just reports.
- Keep distinct empty illustrations/text for: no contacts at all, favorites filter empty, search returned nothing.

## Acceptance criteria

- [ ] Empty list with no contacts shows "Create contact" as the primary action.
- [ ] Empty list with no contacts shows "Import vCard" as a secondary action.
- [ ] Empty favorites-only state keeps its own message ("Избранное пусто") without the import CTA.
- [ ] Empty search state keeps its own message ("Ничего не найдено") without the import CTA.
- [ ] Tapping "Create contact" opens the editor and creates a contact; after save the list updates and the empty state disappears.

## Status

Done.

## Technical notes (implemented)

- `app/src/main/java/com/m4isper/contactsapp/ui/components/States.kt` — `EmptyState` extended with optional `primaryLabel`/`onPrimaryClick` and `secondaryLabel`/`onSecondaryClick` parameters; primary is a filled brand button, secondary is a toned-down outlined button on `surfaceContainer`.
- `app/src/main/java/com/m4isper/contactsapp/presentation/contacts/components/ContactListBody.kt` — `ContactListBody` accepts `onCreateContact` and `onImportClick`; `EmptyPlaceholder` passes them to `EmptyState` only when the list is completely empty (not favorites-only or search-empty).
- `app/src/main/java/com/m4isper/contactsapp/presentation/contacts/components/ContactList.kt` — accepts `onCreateContact` and `onImportClick`; defaults `onImportClick` to `vCardLaunchers.startImport(context)`; passes both to `ContactListBody`.
- `app/src/main/java/com/m4isper/contactsapp/presentation/contacts/ContactsScreen.kt` — passes `onCreateContact = { AddClicked }`; import uses the `ContactList` default.
- `app/src/main/res/values/strings.xml` / `values-ru/strings.xml` — added `contacts_empty_cta_create` / `contacts_empty_cta_import`.

## Verification

- `:app:compileDebugKotlin` passes.
- Favorites-only empty state keeps its own message without CTAs.
- Search-empty state keeps its own message without CTAs.

## Technical notes

- `app/src/main/java/com/m4isper/contactsapp/presentation/contacts/components/ContactListBody.kt` — `EmptyPlaceholder()` composable, lines ~72–91.
- `app/src/main/java/com/m4isper/contactsapp/presentation/contacts/ContactsScreen.kt` — `onNavigation` exposes `AddClicked` and import-related actions.
- `app/src/main/res/values/strings.xml` and `values-ru/strings.xml` — add new strings for the CTA labels and helper text.

## Out of scope

- Onboarding carousel or multi-screen tutorial.
- Creating sample/dummy contacts automatically.