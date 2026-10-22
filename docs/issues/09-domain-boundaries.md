# refactor: tighten domain ports and remove infrastructure leaks

**Priority:** P1  
**Area:** domain / architecture

## Problem

The domain module is Android-free at the import level, but its boundaries are still too wide and leak implementation details:

- `ContactsRepository` and `SettingsRepository` are broad "god" interfaces; the `TooManyFunctions` suppression is already needed for them.
- `ContactsRepository` contains default implementations of business logic (`getContactsPage`, `getContactAccount`, `copyContact`, `moveContact`). A port should describe a contract, not provide a hidden fallback implementation.
- `ContactLink` exposes Android raw contact ids, and `ContactDetails.photoUri` is a `content://` URI string. Both tie domain models to the Android provider representation.
- The domain imports the Metro DI annotation `dev.zacsweers.metro.Inject`, so it is not framework-free.
- PIN hashing is implemented in `AppLock` with `MessageDigest`; cryptography is an infrastructure concern and belongs behind a port.

These choices make the domain harder to test in isolation, hide behavior from implementers, and force consumers to depend on methods they do not use.

## Proposed scope

- Split `ContactsRepository` into narrow ports: query/observe, mutations, accounts, raw-contact links/merge.
- Split `SettingsRepository` by feature (or at least into read/write contracts) instead of one settings god interface.
- Remove default method bodies from repository interfaces; move that logic into use cases or domain services.
- Introduce opaque value objects for provider-specific identifiers (`RawContactRef`, `PhotoRef`) and map them in the data layer.
- Move Metro wiring to `:app`; keep domain constructors free of DI annotations.
- Introduce a `PinHasher` port and keep the implementation in `:data`.
- Add architecture tests that fail if `:domain` imports Android, `:data`, `:app`, or a DI framework.

## Acceptance criteria

- [x] No business logic remains in repository interface default methods.
- [x] Each consumer depends only on the narrow port it actually uses.
- [x] `:domain` has no Metro imports and no Android provider types in public models.
- [x] Provider ids and photo references are represented by opaque domain types and mapped in `:data`.
- [x] Existing behavior is covered by tests, and architecture tests reject new boundary violations.

## Status

Done.

- The contacts port is split into the reader, the accounts, the writer and the merger; the
  settings port into the reader, the appearance, the list, the accounts, the language, the
  privacy, the reminders and the maintenance. Every use case depends only on its own port.
- The ports have no default bodies any more. The copy and the move of a contact were the
  business rules hidden in the port - they are rules of the use cases now.
- The data layer contributes the narrow ports: the implementation of the contacts, four small
  implementations of the settings.
- The domain has no Metro import any more: the use cases are plain classes, and the
  composition root of the app lists how they are built. The architecture check rejects a DI
  import in the domain, the same way it rejects Android or the data layer.
- The cryptography left the domain: the port `PinHasher` describes the contract, the PBKDF2
  implementation lives in the data module.
- The provider identifiers are hidden behind value objects: `RawContactRef` for the rows of the
  provider and `PhotoRef` for the photo. The data layer maps them, the domain never
  interprets them.

## Technical notes

- `domain/src/main/java/com/m4isper/contactsapp/domain/repository/ContactsRepository.kt`
- `domain/src/main/java/com/m4isper/contactsapp/domain/repository/SettingsRepository.kt`
- `domain/src/main/java/com/m4isper/contactsapp/domain/model/ContactLink.kt`
- `domain/src/main/java/com/m4isper/contactsapp/domain/model/ContactDetails.kt`
- `domain/src/main/java/com/m4isper/contactsapp/domain/model/AppLock.kt`
- `app/src/main/java/com/m4isper/contactsapp/di/AppGraph.kt`

## Out of scope

- Rewriting contact storage or replacing `ContentResolver`.
- Changing the vCard format or import/export behavior.
