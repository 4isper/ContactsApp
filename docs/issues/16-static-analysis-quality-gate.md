# chore: make detekt and architecture checks effective

**Priority:** P1  
**Area:** build / quality gates

## Problem

Static analysis currently gives a false sense of quality:

- `./gradlew detekt ktlintCheck` succeeds, but detekt reports **0 findings** across roughly 33k lines of code.
- `config/detekt/detekt.yml` exists but is not wired into Gradle, so custom thresholds (`LargeClass` 450, `LongParameterList` 6, `TooManyFunctions` 11, `MagicNumber`) are not applied.
- The project has 65 `@Suppress` annotations at audit time, including `LargeClass`, `TooManyFunctions`, and `TooGenericExceptionCaught`; these are exactly the signals that should drive refactoring.
- There are no architecture tests that enforce the module dependency rule.

As a result, the build can stay green while boundary violations and oversized classes accumulate.

## Proposed scope

- Wire `config/detekt/detekt.yml` into the detekt configuration for every module; enable `buildUponDefaultConfig`, parallel analysis, and type resolution where supported.
- Verify that detekt actually analyzes main and test sources; add a deliberate violation test to confirm the task fails.
- Configure CI to run detekt, ktlint, lint, unit tests, and architecture tests.
- Add Konsist or ArchUnit tests: `:domain` must not depend on Android/`:data`/`:app`/DI; `:data` must not depend on `:app`; presentation must not depend on data outside DI.
- Review existing suppressions and require a justification for each remaining one.
- Use a baseline only temporarily for legacy findings, not as a permanent mute.

## Acceptance criteria

- [x] Detekt uses the repository config and fails on a known violation.
- [x] Custom thresholds are active and visible in CI output.
- [x] Architecture tests enforce the module dependency rule.
- [x] CI runs static analysis and tests on every change.
- [ ] Remaining suppressions are documented and limited to justified cases.

## Status

Mostly done. What is in place:

- `config/detekt/detekt.yml` is wired into every module through the root `subprojects` block,
  so the custom thresholds (`LargeClass` 450, `LongParameterList` 6, `TooManyFunctions` 11,
  `MagicNumber`) really apply. The wiring was verified with a deliberate violation: the task
  failed on it, and the current code has 0 findings.
- The root task `checkArchitecture` checks the module boundaries: `:domain` must not import
  `android.*`, `androidx.*`, `:data`, `:app` or the DI package. The task is hooked into the
  `check` task of every module.
- `.github/workflows/ci.yml` runs detekt, ktlint, `checkArchitecture`, the unit tests of all
  three modules and `:app:lintDebug` on every push and pull request.

What is left:

- `:domain` still uses the Metro annotations (`@Inject` in 47 use cases). The check forbids
  `android*`, `:data`, `:app` and `di` imports, but the DI annotations are still allowed; the
  rule is switched on after issue 09 removes the annotations from the domain.
- The audit of the 65 `@Suppress` annotations is not finished: the new suppressions
  (`TooManyFunctions` in the group provider) are justified in a comment, the old ones are not
  reviewed yet.

## Technical notes

## Technical notes

- `config/detekt/detekt.yml`
- Root and module `build.gradle.kts` files.
- CI configuration for the repository.
- Existing `@Suppress` usages across `:app`, `:data`, and `:domain`.

## Out of scope

- Fixing every existing detekt finding in one change; a short-lived baseline is acceptable.
- Adding new code-style rules unrelated to architecture or correctness.
