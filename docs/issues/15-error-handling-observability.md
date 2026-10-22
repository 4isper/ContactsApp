# refactor: unify error handling and release logging

**Priority:** P2  
**Area:** presentation / observability

## Problem

Error handling is duplicated and partly invisible:

- ViewModels catch broad exceptions (`TooGenericExceptionCaught`); this can swallow `CancellationException` and break structured concurrency.
- Errors are logged only in DEBUG builds via `logw`; in release, failures disappear silently.
- Every ViewModel has its own try/catch boilerplate and its own user-facing message mapping.
- There is no shared `Result`/`Outcome`/`AppError` type at the use-case boundary, so data-layer failures are not consistently classified.

The result is inconsistent UX and poor production diagnosability.

## Proposed scope

- Introduce a shared `Result`/`Outcome`/`AppError` model at use-case boundaries.
- Map data/provider exceptions into domain errors; keep technical details out of UI messages.
- Rethrow `CancellationException`; catch only expected exception types.
- Add a centralized logger available in release builds, with PII redaction.
- Replace per-ViewModel message mapping with one error-to-message mapper.
- Add tests for error classification and cancellation propagation.

## Acceptance criteria

- [ ] ViewModels do not swallow `CancellationException`.
- [ ] Broad catch blocks are replaced with specific handling or a shared safe-call helper.
- [ ] Errors are logged in release builds without leaking contact PII.
- [ ] User-facing messages are consistent across screens.
- [ ] Tests cover mapped errors and cancellation.

## Technical notes

- `app/src/main/java/com/m4isper/contactsapp/presentation/**/` — ViewModel catch blocks.
- Domain use-case return types and repository error mapping.
- Existing logging helpers used by the presentation layer.

## Out of scope

- Selecting a crash-reporting vendor.
- Product analytics.
