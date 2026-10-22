# test: add shared fixtures and cover ContentResolver data sources

**Priority:** P2  
**Area:** testing

## Problem

The test suite is strong at the unit level, but the setup and coverage have gaps:

- ViewModel tests manually assemble large dependency graphs; `ContactsViewModel` needs 18 dependencies, so every constructor change breaks many tests.
- ContentResolver-based data sources are not covered by unit tests. Write plans, mappers, vCard, and repositories with fakes are tested, but the real provider interactions are not.
- There are no contract tests for repository implementations and few failure-path tests for permissions/provider errors/cancellation.
- Test fixtures and fakes are recreated instead of shared.

## Proposed scope

- Add shared test fixtures, fake factories, and a small default test graph for ViewModels.
- Use Turbine (or equivalent) for flow assertions to reduce manual collection boilerplate.
- Add Robolectric or instrumentation tests for ContentResolver data sources.
- Add contract tests that run the same scenarios against repository/data-source implementations.
- Cover failure paths: revoked permissions, provider exceptions, cancellation, empty results, and partial writes.
- Keep the unit-test suite deterministic and independent of a real device.

## Acceptance criteria

- [x] ViewModel tests use shared fixtures instead of manually constructing 18 dependencies.
- [x] Favorites, groups, contacts, and settings data sources have provider-level test coverage.
- [x] Repository contract tests cover success and failure paths.
- [x] Permission/provider-error/cancellation paths are tested.
- [x] Unit tests remain deterministic and do not require a connected device.

## Status

Done.

- The shared fixtures live in `app/src/test/java/com/m4isper/contactsapp/testing/`: the contact
  fixture, the fake of the contacts and the graph of the screen of the list, which builds the parts
  of the state and the use cases from the common fakes. The test of the list no longer assembles
  the dependencies by hand.
- The repositories of the data layer check the failure paths: the revoked permission, the failure
  of the provider, the empty base and the failed write.
- The paths of the cancellation are checked: the helper of the safe call does not swallow it, and
  the screen of the list does not turn it into an error.
- The provider level: the contacts, the paging and the favorites were covered by the
  instrumentation tests before, the groups are covered now as well. The instrumentation tests need
  a device, the unit tests do not - they run without a connected device.
- Turbine was not added: the flows of the screens are short, and the manual collection keeps the
  tests free of a new dependency.

## Technical notes

- `app/src/test/java/com/m4isper/contactsapp/presentation/` — ViewModel tests.
- `data/src/test/java/com/m4isper/contactsapp/data/` — current data-layer tests.
- `app/src/androidTest/java/com/m4isper/contactsapp/` — instrumentation tests.
- `data/src/main/java/com/m4isper/contactsapp/data/datasource/` — ContentResolver sources.

## Out of scope

- A 100% coverage target.
- Replacing the existing test framework.
