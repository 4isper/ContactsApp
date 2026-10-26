# fix: make the loading skeleton actually animate

**Priority:** P3 — visual polish and an honest loading indicator
**Area:** contacts list / loading state

## Problem

Issue 33 added `ListSkeleton` with a "subtle opacity pulse", but the animation never runs. In `States.kt`:

```kotlin
val pulse by animateFloatAsState(
    targetValue = 1f,
    animationSpec = infiniteRepeatable(
        animation = tween(durationMillis = 1000),
        repeatMode = RepeatMode.Reverse,
    ),
)
```

`animateFloatAsState` starts at `targetValue`, so the state is already 1f and there is nothing to animate. The skeleton is a static set of gray bars. `SkeletonLine` also paints a `linearGradient` with fixed colors and no animated offset, so even a working alpha pulse would not look like a shimmer.

This is a regression against issue 33's acceptance criterion "Placeholders have a subtle shimmer/pulse animation". A completely static skeleton can also be mistaken for real empty cards.

## Proposed scope

- Drive the animation with `rememberInfiniteTransition`:
  - either animate a 0.35f–1f alpha with `infiniteRepeatable(..., RepeatMode.Reverse)`; or
  - animate a gradient offset across the line for a true shimmer.
- If using alpha, apply it to the line color, not by copying an already opaque color. Keep the contrast subtle in both light and dark themes.
- If using a gradient, make the gradient direction RTL-aware and keep the animation duration around 900–1200 ms.
- Respect the system animation-duration scale; do not force animations when the user has disabled them.
- Add semantics to the skeleton: a single "Loading contacts" description (or a polite progress state), and hide the individual placeholder shapes from TalkBack. Do not let TalkBack read five fake cards.
- Keep the layout, card count, and no-layout-jump behavior from issue 33 unchanged.
- Add a simple Compose test or screenshot check that the pulse value changes over time (or at least that the infinite transition is present).

## Acceptance criteria

- [ ] The skeleton bars visibly pulse or shimmer in light and dark themes.
- [ ] The animation continues while the list is loading and stops when the content appears.
- [ ] With system animations disabled, the skeleton is static but still readable.
- [ ] TalkBack announces that contacts are loading and does not focus individual skeleton bars.
- [ ] The skeleton still matches the real contact card layout and does not cause a layout jump.
- [ ] No new lint/detekt warnings.

## Technical notes

- `app/src/main/java/com/m4isper/contactsapp/ui/components/States.kt` lines ~52–130 — `ListSkeleton`, `SkeletonLine`, `pulse`.
- `app/src/main/java/com/m4isper/contactsapp/presentation/contacts/components/ContactListBody.kt` lines ~42–44 — renders `ListSkeleton()` for `Loadable.Loading`.
- Compose: `rememberInfiniteTransition`, `animateFloat`, `RepeatMode.Reverse`, `Brush.linearGradient` with an animated start/end offset, `clearAndSetSemantics`.
- Issue 33's verification claimed a pulse; this issue is a bug fix, not a redesign.
- Consider using `Modifier.semantics { progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate }` plus a content description instead of exposing the placeholder shapes.

## Out of scope

- Adding shimmer to other screens (details, editor, groups).
- A redesign of the skeleton card layout.
- Third-party shimmer libraries.

## Verification

- Record a short screen capture or inspect values over time in a Compose test.
- Toggle "Remove animations" in developer options and verify a static fallback.
- TalkBack traversal of the loading list.
- `./gradlew :app:compileDebugKotlin :app:testDebugUnitTest`.
