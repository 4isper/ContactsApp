# ContactsApp

A native Android contacts manager built with modern Jetpack libraries and Clean Architecture: browse, search and favorite phone contacts, view details, create, edit, delete and share them (vCard), and place calls — in English and Russian, on phones, tablets and foldables, in light and dark themes.

| Fold, dark theme | Phone, light theme, details |
|---|---|
| ![Fold dark](docs/screenshots/fold-dark.png) | ![Phone light details](docs/screenshots/phone-light-details.png) |

## Features

- Contact list: search, favorites filter, alphabetical grouping with fast scroller, pull-to-refresh
- Contact details: phone numbers with type labels, photos, call (with confirmation), SMS, copy, vCard sharing, favorites toggle
- Contact editor: create, update and delete contacts, dynamic phone rows, validation, unsaved-changes guard
- Favorites persisted with DataStore; Material You dynamic color; adaptive two-pane layout on wide screens

## Architecture

Clean Architecture with a strict dependency rule — `presentation → domain ← data`:

```
app/src/main/java/com/m4isper/contactsapp/
├── presentation/   # screens, ViewModels, UiState (contacts/, details/, editor/, common/, navigation/)
├── domain/         # pure models, repository interfaces, use cases
├── data/           # ContentResolver sources, repository implementations
├── di/             # Metro dependency graph
└── ui/             # design system: theme tokens + components
```

- **DI — Metro** (compile-time, no KSP/KAPT) instead of Hilt: smaller generated code, faster builds.
- **Navigation — Navigation 3** (`NavDisplay` + `NavBackStack`) with Material adaptive `ListDetailSceneStrategy` for tablets/foldables and custom slide/fade transition specs.
- **State** — single `UiState` + sealed `Loadable<T>` (Loading/Empty/Content/Error) per screen.
- **Static analysis** — Detekt + ktlint are part of the build (`detekt`, `ktlintCheck`).

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
./gradlew :app:testDebugUnitTest      # 50 unit tests
./gradlew :app:connectedDebugAndroidTest  # 16 Compose UI tests (needs a running emulator)
./gradlew :app:detekt :app:ktlintCheck    # static analysis
```
