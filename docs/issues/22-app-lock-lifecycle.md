# fix: do not lock app on configuration changes and external activities

**Priority:** P1
**Area:** app lock / lifecycle

## Problem

`MainActivity` locks the app on every `Lifecycle.Event.ON_STOP`:

```kotlin
LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
    lockViewModel.lock()
}
```

This fires not only when the app is truly backgrounded, but also during rotation and when launching external activities such as camera, file picker, or share sheets. As a result, the app can lock unexpectedly and destroy editor/navigation state.

## Proposed scope

- Distinguish a real background transition from a configuration change.
- Lock only when the app actually leaves the foreground.
- Preserve navigation/editor state across rotation when app lock is enabled.
- Add tests or documented manual checks for rotation and external activity flows.

## Acceptance criteria

- [ ] Rotating the device does not trigger app lock.
- [ ] Launching an external activity does not destroy unsaved editor state unexpectedly.
- [ ] App lock still engages when the app is genuinely backgrounded.
- [ ] Navigation state is preserved across configuration changes.

## Technical notes

- `app/src/main/java/com/m4isper/contactsapp/MainActivity.kt`
- `app/src/main/java/com/m4isper/contactsapp/presentation/settings/AppLockViewModel.kt`
- `app/src/main/java/com/m4isper/contactsapp/presentation/settings/AppLockScreen.kt`

## Out of scope

- Biometric authentication.
- Changing the PIN policy.
