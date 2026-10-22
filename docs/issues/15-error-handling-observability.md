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

- [x] ViewModels do not swallow `CancellationException`.
- [x] Broad catch blocks are replaced with specific handling or a shared safe-call helper.
- [x] Errors are logged in release builds without leaking contact PII.
- [x] User-facing messages are consistent across screens.
- [x] Tests cover mapped errors and cancellation.

## Status

Done.

- The domain has a common kind of error - `AppError`: permission, provider, save, delete, import,
  unknown. The kind classifies the exception, the text of the provider stays out of the interface.
- The presentation has one helper for the risky calls - `safeCall`: it rethrows the cancellation of
  the coroutine, logs the failure and returns it to the caller. 33 broad catch blocks of the view
  models became calls of the helper; the only remaining broad catch is the one inside the helper.
- The logger writes in the release build as well (before it only wrote in debug), and the text of
  the error goes into the log without the digits: there may be phones and contact ids in it.
- The user facing texts come from one dictionary - `AppError.messageRes()` - and the states of the
  screens keep the kind of the error instead of the text of the exception.
- Tests: the classification of the errors, the propagation of the cancellation and the redaction of
  the log.

## Technical notes

- `app/src/main/java/com/m4isper/contactsapp/presentation/**/` — ViewModel catch blocks.
- Domain use-case return types and repository error mapping.
- Existing logging helpers used by the presentation layer.

## Out of scope

- Selecting a crash-reporting vendor.
- Product analytics.
