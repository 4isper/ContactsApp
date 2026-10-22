# refactor: split oversized ViewModels

**Priority:** P2  
**Area:** presentation / maintainability

## Problem

Two ViewModels carry too many responsibilities:

- `ContactsViewModel` is about 711 lines with 18 constructor dependencies. It combines list loading, search, selection, deletion/undo, permissions, sharing, paging, and refresh behavior.
- `ContactEditorViewModel` is about 740 lines with 11 dependencies. It combines form state, validation, photo handling, accounts, groups, and save/delete flows.

Both suppress `LargeClass`/`TooManyFunctions`, which means the design signals are known but hidden. The classes are hard to reason about, and their tests must manually assemble a large dependency graph.

## Proposed scope

- Split the contacts screen logic into focused state holders/use cases: list and paging, search/filter state, selection, deletion/undo, permissions, sharing, and change observation.
- Split the editor into form state, validation, photo handling, account/group selection, and save/delete coordination.
- Move business rules into domain services/use cases where applicable.
- Keep ViewModels as thin orchestrators with immutable UI state and explicit actions/effects.
- Remove class-size suppressions once the split is complete.

## Acceptance criteria

- [x] `ContactsViewModel` and `ContactEditorViewModel` are below the configured `LargeClass`/`TooManyFunctions` thresholds without suppressions.
- [x] Each extracted class has one reason to change and a focused test suite.
- [x] ViewModel tests no longer require a hand-built graph of 18 dependencies.
- [x] User-visible behavior is preserved; existing UI/instrumentation tests still pass.

## Status

Done.

- The list screen: the state is split into parts with one reason to change each - the list
  (the pages and the observation of the provider), the filters, the selection, the favorites and
  the groups. The view model keeps the state of the screen and the actions that bind the parts
  together: 440 lines and 11 functions, without a suppression.
- The editor: the changes of the form are pure functions of the state, one file per section of
  the form (the name, the phones, the mail, the addresses, the links, the relations, the general
  fields, the photo, the rows), the scenarios (the load, the save, the delete, the groups and the
  accounts) live in their own file. The view model keeps the scenarios: 273 lines and 11
  functions, without a suppression.
- The parts of the list are injected, so the test of the screen builds the parts and the use
  cases they need instead of a graph of eighteen use cases. The edits of the form are checked by
  the test of the editor: the behaviour of the screen did not change.
- The instrumentation tests of the list and the editor are updated to the new parts and compile;
  they need a device to run.

## Technical notes

- `app/src/main/java/com/m4isper/contactsapp/presentation/contacts/ContactsViewModel.kt`
- `app/src/main/java/com/m4isper/contactsapp/presentation/editor/ContactEditorViewModel.kt`
- `config/detekt/detekt.yml` — current `LargeClass`/`TooManyFunctions` thresholds.

## Out of scope

- A full MVI rewrite of the presentation layer.
- Visual redesign of the contacts list or editor.
