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

- [ ] No business logic remains in repository interface default methods.
- [ ] Each consumer depends only on the narrow port it actually uses.
- [ ] `:domain` has no Metro imports and no Android provider types in public models.
- [ ] Provider ids and photo references are represented by opaque domain types and mapped in `:data`.
- [ ] Existing behavior is covered by tests, and architecture tests reject new boundary violations.

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
