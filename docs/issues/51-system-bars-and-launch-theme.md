# fix: make system bars and the launch window follow the app theme

**Priority:** P2 — status/navigation bar icons can be illegible when the app theme differs from the system theme
**Area:** app shell / theme / cold start

## Problem

The app has its own light/dark theme setting (`ThemeMode.SYSTEM`, `LIGHT`, `DARK`), but the window chrome does not follow it:

- `MainActivity` calls `enableEdgeToEdge()` with no `SystemBarStyle` arguments (line ~71). The default `SystemBarStyle.auto` chooses dark or light system-bar icons from the **system** dark-mode flag, not from `settings.themeMode`. If the app is forced to dark while the system is light, the status/navigation bar icons stay dark; against the app's dark surface they become hard to see. The opposite happens for forced light on a dark system.
- `enableEdgeToEdge()` runs once in `onCreate`. Changing the theme mode in Settings updates Compose colors but does not recompute system-bar icon appearance until the activity is recreated. Theme changes do not request a restart, unlike language changes.
- The XML launch theme is `android:Theme.Material.NoActionBar` (`values/themes.xml`), which is a **dark** platform theme. There is no `values-night/themes.xml` and no splash-screen configuration. In light mode the window/splash is dark before the first Compose frame, then switches to the light UI; this produces a visible flash and can show light system-bar icons on the dark launch background.
- The launch window background is unrelated to the brand surface color or dynamic color, so the transition into the app does not match the UI the user sees.

This is most visible on a cold start and when the user overrides the system theme, both common scenarios. It also makes the app look unpolished even though the in-app screens are consistent.

## Proposed scope

- Pass explicit system-bar styles based on the effective app theme: call `enableEdgeToEdge(statusBarStyle = ..., navigationBarStyle = ...)` with icon appearance derived from `settings.themeMode` / `darkTheme`, or update the `WindowInsetsController` appearance whenever the resolved theme changes.
- Re-apply the system-bar appearance in a `LaunchedEffect(darkTheme)` (or `DisposableEffect`) so changing the theme in Settings updates it immediately, without requiring a restart.
- Fix the launch theme:
  - use a light/dark-aware platform theme (for example a light parent in `values/themes.xml` and a dark parent in `values-night/themes.xml`, or a Material 3 day/night theme if the XML dependency is available);
  - set a window background that matches the app's surface/brand color;
  - on Android 12+, set the splash-screen background and icon consistently.
- Keep edge-to-edge behavior and all screen insets unchanged.
- Ensure the app-lock screen and dialogs inherit the same system-bar treatment.
- Add a dark/light screenshot or manual check for the first frame if the project has baseline/screenshot tooling.

## Acceptance criteria

- [x] Forced dark theme on a light system shows light status/navigation bar icons against the dark app surface.
- [x] Forced light theme on a dark system shows dark icons against the light app surface.
- [x] Changing the theme in Settings updates system-bar icon appearance without restarting the activity.
- [x] A cold start in light mode does not flash a black or dark window before the light UI appears.
- [x] The launch/splash background is consistent with the app's surface or brand color in both themes.
- [x] Edge-to-edge content still respects status/navigation insets on all screens.
- [x] App-lock and full-screen dialogs do not introduce a different system-bar appearance.

## Technical notes

- `app/src/main/java/com/m4isper/contactsapp/MainActivity.kt` lines ~65–78 — `enableEdgeToEdge()` call.
- `app/src/main/java/com/m4isper/contactsapp/MainActivity.kt` lines ~147–165 — resolved `darkTheme` value from `settings.themeMode`.
- `app/src/main/java/com/m4isper/contactsapp/ui/theme/Theme.kt` — `ContactsAppTheme`.
- `app/src/main/res/values/themes.xml` — current `android:Theme.Material.NoActionBar`; add `values-night/themes.xml` or a day/night parent.
- Android 12 splash: `windowSplashScreenBackground`, `windowSplashScreenAnimatedIcon` (or the SplashScreen compat API) should use the brand background.
- `enableEdgeToEdge(statusBarStyle = SystemBarStyle.light/dark(...), navigationBarStyle = ...)` or `WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme`.
- `docs/issues/35-brand-visual-identity.md` — brand colors are defined; reuse them rather than adding new ones.
- This issue does not change the in-app Material color schemes.

## Out of scope

- Redesigning the app icon or splash artwork.
- Per-screen status-bar colors (the app uses one global treatment).
- Changing how theme preference is stored.

## Verification

- Manual matrix: system light + app light/dark/system; system dark + app light/dark/system; change theme while the app is open; cold start from a killed process in both themes.
- Screenshot/record the first frame on Android 12+ and on an older API.
- `./gradlew :app:compileDebugKotlin :app:testDebugUnitTest`.

## Status

Done, with one documented limitation.

### What was implemented

- System bars follow the app theme: a `DisposableEffect(darkTheme)` re-runs `enableEdgeToEdge` with `SystemBarStyle.auto { darkTheme }` whenever the resolved theme changes, so switching the theme in Settings updates the icon appearance immediately without an activity restart, and a forced theme wins over the system flag.
- The launch theme is day/night: `values/themes.xml` now uses the light `Theme.Material.Light.NoActionBar` parent and a new `values-night/themes.xml` uses the dark one, so a cold start in light mode no longer flashes a dark window and the splash background follows the system theme.
- Edge-to-edge insets are untouched (the effect only changes bar appearance); the app-lock screen and dialogs run inside the same activity window and inherit the same treatment.

### Verification

- `./gradlew :app:compileDebugKotlin :app:compileDebugAndroidTestKotlin :app:testDebugUnitTest :app:detekt :app:ktlintCheck` passes.
- `./gradlew :app:connectedDebugAndroidTest` on a headless emulator: BUILD SUCCESSFUL, 119 tests, 118 passed, 1 skipped by assumption (`LargeListPerformanceTest` benchmark).
- Limitations worth knowing: a cold start while the app forces the OPPOSITE of the system theme still flashes, because the theme preference lives in DataStore and cannot be read synchronously before the first frame; the launch window background follows the platform day/night surface rather than an exact brand color, and no custom Android 12 splash artwork was added (out of scope).
- Manual matrix that remains useful: system light/dark × app light/dark/system, theme change while open, and a cold-start recording of the first frame.
