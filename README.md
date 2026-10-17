# ContactsApp

A native Android contacts manager built with modern Jetpack libraries and Clean Architecture: browse, search and favorite contacts, view details, create, edit, delete and share them (vCard), and place calls — in English and Russian, on phones, tablets and foldables, in light and dark themes.

| Fold, dark theme | Phone, light theme, details |
|---|---|
| ![Fold dark](docs/screenshots/fold-dark.png) | ![Phone light details](docs/screenshots/phone-light-details.png) |

## Features

- Contact list: search by name (including structured name components: given/family/middle/prefix/suffix and their phonetics), phone, email, nickname and relation names; favorites filter; alphabetical grouping with fast scroller; pull-to-refresh; phoneless contacts stay visible; multi-select mode (long press) with select all, batch delete (with confirmation and undo via a snackbar) and batch vCard sharing; import from a .vcf file and export of all contacts to a .vcf file
- Contact details: phones, emails, instant messengers, addresses, websites, organization, note, birthday, nickname, relations (family, friends, etc.) and events (anniversaries and custom dates with labels); call (with confirmation), SMS, email, map, open link and copy actions; vCard sharing (with structured `N`, `IMPP`, `NICKNAME`, `RELATED` and `ANNIVERSARY`/`X-ABDATE` properties); favorites toggle
- Contact editor: create, update and delete contacts; structured name — a single full-name field or given/family/middle/prefix/suffix components with phonetic variants, toggled with an animated chevron; multi-raw contacts are updated consistently (deterministic target raw contact); photo pick with preview and removal; dynamic typed rows for phones, emails, instant messengers, addresses, websites, relations and events (with custom labels, optional year for dates and a shared date picker); nickname field; validation and unsaved-changes guard
- Favorites persisted with DataStore; Material You dynamic color; adaptive two-pane layout on wide screens

## Architecture

Multi-module Clean Architecture with a build-enforced dependency rule — `:domain` (pure JVM) ← `:data` (android-library) ← `:app`:

```
ContactsApp/
├── domain/   # pure models, repository interfaces, use cases (+ fast JVM tests)
├── data/     # ContentResolver sources, repository implementations
└── app/      # presentation/, di/, ui/
    ├── presentation/   # screens, ViewModels, UiState (contacts/, details/, editor/, common/, navigation/)
    ├── di/             # Metro dependency graph (needs :data on classpath for codegen)
    └── ui/             # design system: theme tokens + components
```

- **DI — Metro** (compile-time, no KSP/KAPT) instead of Hilt: smaller generated code, faster builds.
- **Navigation — Navigation 3** (`NavDisplay` + `NavBackStack`) with Material adaptive `ListDetailSceneStrategy` for tablets/foldables and custom slide/fade transition specs.
- **State** — single `UiState` + sealed `Loadable<T>` (Loading/Empty/Content/Error) per screen.
- **Static analysis** — Detekt + ktlint applied to every module (`detekt`, `ktlintCheck`).

## Stack

| Library | Version |
|---|---|
| Kotlin / AGP / Gradle | 2.4.21 / 9.4.1 / 9.8.1 |
| Compose BOM / compileSdk / minSdk | 2026.09.00 / 37 / 26 |
| Metro DI | 1.4.5 |
| Navigation3 / Adaptive | 1.2.0 / 1.4.0-alpha03 |
| Coil / DataStore / kotlinx.serialization | 3.6.3 / 1.2.1 / 1.11.0 |
| Detekt / ktlint-gradle | 2.0.0-alpha.6 / 14.2.0 |

Android permissions used: `READ_CONTACTS` (list), `WRITE_CONTACTS` (editor), `CALL_PHONE` (direct calls, with dialer fallback).

## Build & verify

```bash
./gradlew :app:assembleDebug          # debug APK
./gradlew :domain:test :data:testDebugUnitTest :app:testDebugUnitTest  # 144 unit tests
./gradlew :app:connectedDebugAndroidTest  # 28 Compose UI tests (needs a running emulator)
./gradlew detekt ktlintCheck          # static analysis, all modules
```
