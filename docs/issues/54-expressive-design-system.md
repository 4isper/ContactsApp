# feat: adopt Material 3 Expressive as the app's visual language

**Priority:** P2 — the app is functional but reads as "default Material 3 from 2022"; the 2026 look needs an explicit system, not more screens
**Area:** theme / design system / motion

## Problem

The UI is built from Material 3 defaults with a few token overrides, and no screen uses the expressive layer that Material 3 has offered since 2025. Concretely:

- `ui/theme/Type.kt` overrides only seven type styles and leaves the rest at Material defaults; `BrandTypography` and `LocalBrandTypography` are defined and provided in `ui/theme/Theme.kt` but **never consumed** anywhere (grep finds no readers), so the file suggests a brand type system that does not exist on screen.
- `ui/theme/Shape.kt` defines fixed radii (6/10/16/22/30 dp); every card uses the same 16 dp corner, so shape carries no hierarchy. There is no use of `MaterialShapes`, pill shapes for inputs/actions, or shape morphing.
- No `MotionScheme` and no shared motion tokens: transitions are default fades/slides, selection and favorite toggles are unspringed, and nothing reacts to `LocalMotionDurationScale` beyond the skeleton (issue 50). The Compose BOM resolves Material3 1.4.0, where `MotionScheme`/`MaterialExpressiveTheme` are `internal` and `MaterialShapes`/the M3 loading indicator are not available, so the app has to own its tokens instead of configuring the Material scheme.
- The visual grammar is identical everywhere: rounded rectangles stacked with 12 dp gaps. Cards, dialogs, list rows, and sections all share the same weight, so the eye has no large/small rhythm — this is the "card soup" look that dates the app more than any single color.
- Loading feedback is a `CircularProgressIndicator` (QR preview, sharing) rather than the M3 loading indicator; primary buttons are not pill-shaped; the FAB is a plain circle with a Material `Add` icon.

## Proposed scope

- Complete the type scale for the styles that appear on screen (display/headline/title/body/label), tuned for the system font and large `fontScale`; either delete `BrandTypography`/`LocalBrandTypography` or use it for the two or three real brand moments (home header, empty-state title). Do not keep both a dead token and a parallel "brand" concept.
- Introduce a small motion vocabulary (one "expressive" spring for interactive feedback, one standard easing for navigation) and apply it to selection, favorite toggles, chip selection, FAB↔editor transitions, and sheet/dialog entry. Respect the system animation scale. Because `MotionScheme` is internal in Material3 1.4.0, the vocabulary lives in an app-owned `AppMotion` object with the same spec values; revisit the public scheme when the dependency exposes it.
- Use shape as hierarchy: pill (`CircleShape`) for primary actions and filter chips, consistent 12–16 dp for surfaces, and a larger radius only for hero/feature surfaces. Decorative shape variety waits for a Material version that ships `MaterialShapes`.
- Replace the plain spinner with one themed loading indicator (`AppLoadingIndicator`) wherever the operation is indeterminate and short. The M3 expressive loading indicator is not in Material3 1.4.0.
- Add a one-page visual rules document under `docs/` (surfaces, elevation, radius, spacing, motion, type usage) so new screens stop inventing their own stack-of-cards layout.
- Do not restyle individual screens here beyond what is needed to prove the tokens; the per-screen work is split across issues 56–61.

## Acceptance criteria

- [x] `LocalBrandTypography` is either used in at least one real surface or removed; no dead theme token remains.
- [x] A motion token object exists and is used by at least selection, favorite toggle, and one screen transition; behavior is unchanged when animations are disabled system-wide.
- [x] Primary actions and filter chips are pill-shaped; at most two surface radii are used on a single screen.
- [x] No screen still shows a bare `CircularProgressIndicator` for an app-initiated short task.
- [x] `docs/design/visual-rules.md` exists and issues 56–61 reference it instead of re-deciding tokens.
- [ ] Light/dark screenshots at default and 2.0 font scale show no clipping introduced by the new type scale.

## Technical notes

- `app/src/main/java/com/m4isper/contactsapp/ui/theme/Type.kt` — the dead `BrandTypography` was removed; the full M3 role set is now defined.
- `app/src/main/java/com/m4isper/contactsapp/ui/theme/AppMotion.kt` — app-owned motion tokens (expressive/standard springs, effects, navigation tween).
- `app/src/main/java/com/m4isper/contactsapp/ui/theme/Theme.kt` — `MaterialTheme` assembly; only `LocalSpacing` is provided now.
- `app/src/main/java/com/m4isper/contactsapp/ui/theme/Shape.kt` — `AppShapes` plus the `PillShape` token.
- `app/src/main/java/com/m4isper/contactsapp/ui/components/AppLoadingIndicator.kt` — the single themed indeterminate indicator.
- Material 3 expressive APIs (`MotionScheme`, `MaterialShapes`, loading indicator) are not public in the resolved Material3 1.4.0; do not reflect into internals, and revisit when the BOM upgrades.
- Applied call sites: `ui/components/ContactAvatar.kt`, `ui/components/States.kt`, `presentation/details/DetailsTopBar.kt`, `presentation/details/DetailsHeader.kt`, `presentation/navigation/ContactsTransitions.kt`, `presentation/contacts/components/ContactsFilterHeader.kt`, `presentation/settings/AppLockScreen.kt`, `presentation/common/permissions/PermissionRequest.kt`, `presentation/duplicates/DuplicatesScreen.kt`, `presentation/details/QrPreviewDialog.kt`, `presentation/editor/dialogs/CropPhotoDialog.kt`.
- Accessibility: Compose scales spring/tween specs with the system animation setting; no manual animator-scale checks were added.

## Out of scope

- Per-screen redesigns (56–61 own those).
- A custom display font: this issue keeps the system font unless a licensed brand face is provided.
- Changing the color palette itself (issue 55).

## Verification

- Manual: walk every screen at 1.0/1.5/2.0 font scale in light and dark; toggle system animations off and confirm nothing depends on them.
- `./gradlew :app:compileDebugKotlin :app:testDebugUnitTest :app:detekt :app:ktlintCheck`.

## Status

Done, with one manual check pending.

### What was implemented

- The full M3 type scale is defined in `ui/theme/Type.kt` (all 15 roles) and the dead `BrandTypography`/`LocalBrandTypography` token was removed from `Type.kt` and `Theme.kt`.
- `ui/theme/AppMotion.kt` owns the motion vocabulary: expressive (0.6/800) and standard (0.8/380) spatial springs, non-spatial effects (1.0/1600), and the 300 ms navigation tween with `FastOutSlowInEasing`. It is used by `ContactAvatar` (selection color), `DetailsTopBar` (favorite tint), and `ContactsTransitions` (screen travel). Compose scales the specs with the system animation setting.
- `PillShape` was added to `ui/theme/Shape.kt` and applied to the filter chips and the primary CTAs: `EmptyState`/`ErrorState`, details Call/SMS, permission request, app-lock unlock, and the duplicates merge action.
- `ui/components/AppLoadingIndicator.kt` is the single themed indeterminate indicator (primary color, 4 dp round stroke, tonal track); all four bare `CircularProgressIndicator` usages were replaced (loading state, app lock, crop photo, QR preview). The QR preview keeps a black-on-white indicator because the code area must stay camera-readable.
- `docs/design/visual-rules.md` records color/surface, type, shape, spacing, motion, component, and accessibility rules; issues 56–61 link it instead of re-deciding tokens.

### Deviation from the plan

- Material3 1.4.0 (the version resolved by the BOM) marks `MotionScheme` and `MaterialExpressiveTheme` as `internal` in Kotlin metadata and does not ship `MaterialShapes` or the expressive loading indicator. Reflecting into internals was not acceptable, so the app owns equivalent tokens (`AppMotion`, `PillShape`, `AppLoadingIndicator`). Revisit the public M3 scheme when the BOM upgrades.
- No new automated test was added: token-only changes have no honest behavioral assertion in this project (no screenshot-test tooling is configured), and the existing UI suites already cover the affected components. Per the ub-mode rule, a bad test was not written.

### Verification

- `./gradlew :app:compileDebugKotlin :app:testDebugUnitTest :app:detekt :app:ktlintCheck` passes.
- Still manual: light/dark screenshots and the font-scale 1.0/1.5/2.0 walk (no emulator or device attached in this session), plus TalkBack over the changed CTAs.
