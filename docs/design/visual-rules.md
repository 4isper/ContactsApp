# Visual rules

The app follows Material 3 with a small, owned token layer. Use these rules for new and touched screens instead of inventing local values; the tokens live in `ui/theme/` and the reasons behind them are in [issue 54](../issues/54-expressive-design-system.md).

## Color and surfaces

- Brand scheme by default (deep teal primary, warm amber tertiary); wallpaper colors are an explicit user choice, never an implicit override.
- Brand accent is an accent: favorites, selection, and one primary action per screen. Do not wash whole surfaces in `primary`.
- Prefer tonal layering over shadows: `surface` for the reading surface, `surfaceContainerLow/High` for grouped or raised regions. A card should earn its elevation by grouping content, not by decorating a row.
- List rows share one `surface`; separate rows inside a section with 1 dp `outlineVariant` dividers inset to the text. Selection is a `secondaryContainer` fill, favorite is `tertiary` — never a per-row card.
- Use `outlineVariant` for separators; `outline` only for focused/active boundaries.
- Destructive actions use the `error` role and are confirmed.

## Typography

| Role | Usage |
|---|---|
| `displayLarge` / `displayMedium` | One-off numeric/decorative moments only |
| `headlineLarge` / `headlineMedium` | Screen hero, details name |
| `headlineSmall` | Section hero inside a screen |
| `titleLarge` / `titleMedium` | App bar titles, list names, dialog titles |
| `titleSmall` | Group headers, dense row emphasis |
| `bodyLarge` / `bodyMedium` | Primary and secondary content |
| `bodySmall` | Metadata: counts, accounts, timestamps |
| `labelLarge` / `labelMedium` / `labelSmall` | Buttons, chips, sticky headers |

- At most three type roles per screen.
- The system font is intentional until a licensed brand face is chosen; do not add a font dependency for a stylistic effect.
- All text must survive `fontScale` 2.0 without clipping (no fixed-height text rows).

## Shape

- `PillShape` (circle) for primary actions and filter chips.
- `MaterialTheme.shapes.medium` (16 dp) for cards, fields, sheets, and images. This is the default working radius.
- `large`/`extraLarge` (22/30 dp) only for hero or feature surfaces.
- Avatars and icon buttons are circles. Do not introduce a third radius on one screen.

## Spacing

Use `LocalSpacing` (`xs` 4, `s` 8, `m` 12, `l` 16, `xl` 24, `xxl` 32). Screen horizontal padding is `m`. Avoid raw dp in new composables.

## Motion

Use `AppMotion` (see `ui/theme/AppMotion.kt`):

| Token | Use for |
|---|---|
| `expressiveSpatial()` | Interactive feedback that changes bounds: expand/collapse, sheet entry, prominent state changes |
| `standardSpatial()` | Calm layout changes: reordering, list rearrangement |
| `effects()` | Color, alpha, and tint changes (selection, favorite, validation) — never spatial bounce |
| `navigation()` | Screen-to-screen travel; keep predictive back responsive |

- Compose scales these with the system animation setting; never check the animator scale manually for new animations.
- Motion must not be the only carrier of state: selection, favorite, and errors also need color/shape/semantics.
- Prefer `AnimatedVisibility`/`animate*AsState` over custom frame loops; keep durations under ~400 ms for navigation.

## Components

- Indeterminate progress uses `AppLoadingIndicator` (themed color, 4 dp stroke, track) instead of a bare `CircularProgressIndicator`.
- Selection and favorite toggles animate their color/tint with `effects()`.
- Dialogs are for confirmation and short input; selection and content-heavy flows should be bottom sheets (see [issue 60](../issues/60-dialogs-sheets-and-editor-density.md)).

## Accessibility

- Touch targets ≥ 48 dp; icons get content descriptions unless decorative.
- Text contrast ≥ 4.5:1 for body, ≥ 3:1 for large text in both themes.
- Verify every screen at font scale 1.0, 1.5, and 2.0, and in light/dark.

## Quick checklist

- [ ] Tokens from `ui/theme` only — no new colors, radii, durations, or spacing literals.
- [ ] One primary action treatment (pill) and no competing accents.
- [ ] Surfaces are tonal, not a stack of identical cards.
- [ ] Motion uses `AppMotion` and does not hide state from TalkBack.
- [ ] Layout survives 320 dp width and 2.0 font scale.
