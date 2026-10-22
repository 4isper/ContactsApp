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

- [ ] `ContactsViewModel` and `ContactEditorViewModel` are below the configured `LargeClass`/`TooManyFunctions` thresholds without suppressions.
- [ ] Each extracted class has one reason to change and a focused test suite.
- [ ] ViewModel tests no longer require a hand-built graph of 18 dependencies.
- [ ] User-visible behavior is preserved; existing UI/instrumentation tests still pass.

## Technical notes

- `app/src/main/java/com/m4isper/contactsapp/presentation/contacts/ContactsViewModel.kt`
- `app/src/main/java/com/m4isper/contactsapp/presentation/editor/ContactEditorViewModel.kt`
- `config/detekt/detekt.yml` — current `LargeClass`/`TooManyFunctions` thresholds.

## Out of scope

- A full MVI rewrite of the presentation layer.
- Visual redesign of the contacts list or editor.
