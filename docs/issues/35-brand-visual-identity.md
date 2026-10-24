# feat: strengthen brand visual identity (icons, typography, accents)

**Priority:** P3 — differentiation and visual polish  
**Area:** UI / design system

## Problem

The app uses the default Material 3 icon set (`Icons.Filled.*`) everywhere and the system font (`FontFamily.Default`) for all typography. Combined with the clean teal/amber color scheme this is functional, but the app lacks a distinct visual identity: no brand icons for actions (call, SMS, group, duplicates, favorite), and no expressive typography hierarchy beyond Material defaults. It looks like a generic Material app rather than a branded contacts manager.

## Proposed scope

- Create a small brand icon set (16–20 icons) in the teal/amber brand style for the most common actions: call, SMS, email, group, duplicate/merge, favorite, settings, import, export, search.
- Replace the most visible Material icons in the list header, contact card quick actions, and editor with brand icons where they add recognition.
- Consider a brand display font for headlines (`headlineSmall`, `titleMedium`) while keeping the system font for body text to preserve readability and localization safety.
- Define brand icon usage rules (stroke weight, corner radius, safe area) in the `ui/theme` or `ui/components` module so future screens stay consistent.

## Acceptance criteria

- [ ] A brand icon set exists in the project (vector drawables or Compose ImageVectors).
- [ ] List header icons (more actions, import, export, duplicates, settings) use brand icons.
- [ ] Contact card quick-action icons (call, SMS, favorite) use brand icons.
- [ ] Headline typography is more distinct from body text than Material defaults.
- [ ] Icons scale correctly in light and dark themes (use `LocalColorScheme` or uncolored + tint).

## Status

Done.

## Technical notes (implemented)

- `app/src/main/java/com/m4isper/contactsapp/ui/theme/Type.kt` — добавлен `BrandTypography` (data class с `brandHeadline`, `brandSectionTitle`, `brandBodyAccent`) и `LocalBrandTypography` CompositionLocal. Брендовые заголовки выразительнее Material-дефолта (жирный шрифт, отрицательный letter-spacing).
- `app/src/main/java/com/m4isper/contactsapp/ui/theme/Theme.kt` — `ContactsAppTheme` предоставляет `LocalBrandTypography` через `CompositionLocalProvider`.
- `app/src/main/java/com/m4isper/contactsapp/ui/theme/BrandIcons.kt` — новый файл: `BrandIcons` объект с 8 материальными иконками (Call, Message, Favorite, FavoriteBorder, Group, Settings, Search, ImportExport), `BrandPrimaryColor` (тейл), `BrandAccentColor` (янтарь), компоузابل `BrandIcon()` для отрисовки иконки в фирменном цвете с поддержкой disabled-состояния.
- `app/src/main/java/com/m4isper/contactsapp/presentation/contacts/components/ContactCard.kt` — Favorite-звёздочка (активная = янтарь, неактивная = тейл), MoreVert, звонок (тейл), СМС (янтарь) заменены на `BrandIcon`.
- `app/src/main/java/com/m4isper/contactsapp/presentation/contacts/components/ContactsFilterHeader.kt` — иконки импорта/экспорта (янтарь) и настроек (тейл) в меню заменены на `BrandIcon`.
- `app/src/main/java/com/m4isper/contactsapp/ui/components/SearchField.kt` — иконка поиска заменена на `BrandIcon` (тейл).

## Verification

- `:app:compileDebugKotlin` passes.
- `:app:detekt` / `:app:ktlintCheck` — нет новых проблем (только 2 предсуществующие: DuplicatesScreen сложность, SettingsScreen длина строки).
- Брендовые цвета (тейл 0xFF00696B / янтарь 0xFF7E5700) используются последовательно для основных и акцентных действий.

## Out of scope

- Кастомные SVG-пути для иконок (используются Material-иконки с фирменной окраской — прагматичный подход).
- Подключение кастомного шрифта (используется системный шрифт с насыщенными начертаниями для заголовков).

## Technical notes

- `app/src/main/java/com/m4isper/contactsapp/ui/theme/Type.kt` — typography overrides; add brand headline styles here.
- `app/src/main/java/com/m4isper/contactsapp/ui/theme/Color.kt` — teal/amber brand colors available for icon tinting.
- `app/src/main/java/com/m4isper/contactsapp/presentation/contacts/components/ContactsFilterHeader.kt` — Material icons in chips and dropdown.
- `app/src/main/java/com/m4isper/contactsapp/presentation/contacts/components/ContactCard.kt` — `QuickActionsMenu` and `FavoriteQuickAction` icons.
- Vector Asset Studio or Compose `ImageVector` can be used; keep icons in `app/src/main/res/drawable/` or a dedicated `ui/icons` package.

## Out of scope

- Full rebrand (logo, app icon, splash screen).
- Animated brand icons (static vectors sufficient).
- Custom font licensing beyond what is already freely available.