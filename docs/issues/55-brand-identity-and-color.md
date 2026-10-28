# feat: make the brand visible again — brand-first color, generative avatars, real icon and splash

**Priority:** P2 — on Android 12+ the app renders in wallpaper colors and has no recognizable brand mark, so it looks like a generic contacts sample
**Area:** theme / brand / launcher / avatars / widget

## Problem

- `ui/theme/Theme.kt` defaults `dynamicColor = true`. On Android 12+ every user sees a wallpaper-derived palette; the deep-teal / warm-amber scheme in `ui/theme/Color.kt` is only the fallback for API 26–30. The committed screenshots confirm it: the fold screenshot is blue-accented with no teal anywhere. Issue 35's "brand visual identity" work landed `BrandIcons`/`BrandIcon`, but those are tinted Material icons (`ui/theme/BrandIcons.kt`), not a brand mark.
- `ui/components/ContactAvatar.kt` renders every contact as the same `primaryContainer` circle with initials. Under dynamic color that means every avatar shares the wallpaper's primary; there is no per-contact identity, no shape variety, no photo fallback hierarchy beyond initials.
- Launcher icon, notification icon, and the Android 12 splash are still the default resources. Issue 51 deliberately left splash artwork out of scope, so the first branded moment is still a platform-looking window. There is no monochrome/themed launcher icon for Android 13+.
- The Glance widget (if kept) uses default surfaces/typography, so the home-screen presence does not match the app.

## Proposed scope

- Make the brand scheme the default and turn "use wallpaper colors" into an explicit, discoverable setting (Settings → Appearance), with a palette preview. The brand teal must remain the accent for favorites/selection and the amber for the favorite star, at AA contrast in both themes.
- Generative avatars: derive a stable hue from the contact id/name inside a brand-adjacent palette (teal/cyan/amber/sand), with 3–4 silhouette/shape variants and initials in an AA-contrast foreground. Selection state must remain distinct from the generative color (outline + check, not color alone).
- Ship an actual brand mark: a simple geometric monogram, used for the adaptive launcher icon, the monochrome/themed icon, the notification small icon, the Android 12 splash icon, and the widget header.
- Set the Android 12+ splash background to the brand surface (ties into the existing `values-night` launch theme from issue 51) and keep the pre-12 window background matching.
- Bring the widget typography/colors onto the same tokens; no separate palette.
- Keep the brand accent usage under roughly 10% of any screen: brand is an accent, not a wash.

## Acceptance criteria

- [ ] With dynamic color off (new default), every screen shows the brand scheme on all API levels; the setting to opt into wallpaper colors exists, is persisted, and previews the result.
- [ ] Two contacts with different names get visibly different avatar colors; all avatar/initials pairs pass AA contrast in both themes.
- [ ] Selection/pressed states remain distinguishable when the avatar color changes.
- [ ] Launcher icon has adaptive + monochrome variants; splash and notification icons use the same mark; no default Android resource remains in the launcher/splash path.
- [ ] Widget uses the shared color/type tokens and matches light/dark.
- [ ] TalkBack does not announce decorative brand images; avatars keep the contact name semantics.

## Technical notes

- `app/src/main/java/com/m4isper/contactsapp/ui/theme/Theme.kt` — `dynamicColor` default and the scheme selection.
- `app/src/main/java/com/m4isper/contactsapp/ui/theme/Color.kt` — the teal/amber palette; add any new generative-avatar hues here.
- `app/src/main/java/com/m4isper/contactsapp/ui/components/ContactAvatar.kt` — single-color circle today; `DetailsAvatar` shares it.
- `app/src/main/java/com/m4isper/contactsapp/ui/theme/BrandIcons.kt` — icon tint rules; a future brand mark belongs next to it.
- Resources: `app/src/main/res/mipmap-anydpi-v26/`, `app/src/main/res/drawable/`, `app/src/main/res/values-v31/themes.xml` (`windowSplashScreenBackground`, `windowSplashScreenAnimatedIcon`), plus the widget provider/`glance` files.
- Contrast checks: `androidx.core.graphics.ColorUtils.calculateContrast`; add unit tests for the avatar-hue → foreground mapping.
- Issue 51 owns system-bar icon appearance; reuse its day/night launch theme instead of adding a second splash configuration.

## Out of scope

- Commissioning illustration or photography.
- Changing the in-app Material color roles themselves (the schemes stay M3).
- Per-contact user-chosen avatar colors.

## Verification

- Manual matrix: API 26 / 33 / 36 device or emulator, light and dark, dynamic color on and off; cold start to confirm the splash matches the UI.
- Launcher: adaptive, monochrome, and themed icon previews in Android Studio.
- `./gradlew :app:compileDebugKotlin :app:testDebugUnitTest :app:detekt :app:ktlintCheck`.
