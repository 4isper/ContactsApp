# ContactsApp

A native Android contacts manager built with modern Jetpack libraries and Clean Architecture: browse, search and favorite contacts, view details, create, edit, delete and share them (vCard), and place calls — in English and Russian, on phones, tablets and foldables, in light and dark themes.

| Fold, dark theme | Phone, light theme, details |
|---|---|
| ![Fold dark](docs/screenshots/fold-dark.png) | ![Phone light details](docs/screenshots/phone-light-details.png) |

## Features

- Contact list: search by name, phone and email; favorites filter; alphabetical grouping with fast scroller; pull-to-refresh; phoneless contacts stay visible
- Contact details: phones, emails, addresses, websites, organization, note and birthday; call (with confirmation), SMS, email, map, open link and copy actions; vCard sharing (with structured `N` property); favorites toggle
- Contact editor: create, update and delete contacts; structured name — a single full-name field or given/family/middle/prefix/suffix components toggled with an animated chevron; photo pick with preview and removal; dynamic typed rows for phones, emails, addresses and websites with custom labels; birthday picker with optional year; validation and unsaved-changes guard
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
./gradlew :domain:test :data:testDebugUnitTest :app:testDebugUnitTest  # 82 unit tests
./gradlew :app:connectedDebugAndroidTest  # 18 Compose UI tests (needs a running emulator)
./gradlew detekt ktlintCheck          # static analysis, all modules
```
