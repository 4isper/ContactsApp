# feat: expand settings, privacy, large-list performance, and reminders

**Priority:** P2/P3  
**Area:** settings / privacy / performance / notifications

## Problem

Settings only cover theme, sort order, and list density. There is no app language selector, dynamic color toggle, favorites-first option, photo visibility, reset, About/licenses/privacy. There is no app lock or recent-task preview protection. The list loads all contacts into memory and filters in memory; there is no paging. Birthdays/events are stored but never surface as reminders or calendar entries. There is no widget, shortcuts, or QR sharing.

## Proposed scope

- Settings: app language, dynamic color toggle, favorites first, show/hide photos, default account (linked to the accounts item), reset settings, About/version/licenses/privacy.
- Privacy: optional PIN/biometric app lock; hide content in recent tasks (`FLAG_SECURE` or a blurred placeholder).
- Performance: paging or incremental loading for large contact lists; keep search/filter responsive; measure with a generated large dataset.
- Reminders: local notification for upcoming birthdays/events with configurable lead time; optional "add to calendar" action.
- Extras: app shortcuts (New contact, Favorites) and a contacts widget; QR/text sharing for a contact.
- Tests: settings persistence, app-lock gate, paging behavior, reminder scheduling.

## Acceptance criteria

- [ ] Settings changes persist and are reflected immediately.
- [ ] App lock can be enabled/disabled and protects the app after backgrounding.
- [ ] A 5k-contact list loads and scrolls without jank; search remains responsive.
- [ ] A birthday reminder fires at the configured time and opens the contact.
- [ ] Shortcuts, widget, and QR sharing work on supported Android versions.
- [ ] About/privacy/licensing is reachable from settings.

## Technical notes

- `app/src/main/java/com/m4isper/contactsapp/presentation/settings/SettingsScreen.kt`, `SettingsViewModel.kt`.
- `domain/src/main/java/com/m4isper/contactsapp/domain/model/AppSettings.kt`; `data/src/main/java/com/m4isper/contactsapp/data/datasource/SettingsDataSourceImpl.kt`.
- `app/src/main/java/com/m4isper/contactsapp/presentation/contacts/ContactsViewModel.kt` — full in-memory list.
- `app/src/main/AndroidManifest.xml` — permissions/receivers.
- `data/src/main/java/com/m4isper/contactsapp/data/datasource/read/ContactsDataProjection.kt` — birthday/event reads.

## Out of scope

- Cloud sync.
- Advanced analytics.
