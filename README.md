# ContactsApp

A native Android contacts manager built with modern Jetpack libraries and Clean Architecture: browse, search and favorite contacts, view details, create, edit, delete and share them (vCard), and place calls — in English and Russian, on phones, tablets and foldables, in light and dark themes.

| Fold, dark theme | Phone, light theme, details |
|---|---|
| ![Fold dark](docs/screenshots/fold-dark.png) | ![Phone light details](docs/screenshots/phone-light-details.png) |

## Features

- Contact list: search by name (including structured name components: given/family/middle/prefix/suffix and their phonetics), phone, email, nickname and relation names; favorites filter; group (label) filter from the header chip; alphabetical grouping with fast scroller; pull-to-refresh; phoneless contacts stay visible; multi-select mode (long press) with select all, batch delete (with confirmation and undo via a snackbar), batch vCard sharing and batch add to a group; import from a .vcf file and export of all contacts to a .vcf file
- Contact details: phones, emails, instant messengers, addresses, websites, organization, note, birthday, nickname, relations (family, friends, etc.), events (anniversaries and custom dates with labels) and group memberships (labels); call (with confirmation), SMS, email, map, open link and copy actions; vCard sharing (with structured `N`, `IMPP`, `NICKNAME`, `RELATED` and `ANNIVERSARY`/`X-ABDATE` properties); favorites toggle
- Contact editor: create, update and delete contacts; structured name — a single full-name field or given/family/middle/prefix/suffix components with phonetic variants, toggled with an animated chevron; multi-raw contacts are updated consistently (deterministic target raw contact, group membership rows of all raw contacts reconciled); photo pick with preview and removal; dynamic typed rows for phones, emails, instant messengers, addresses, websites, relations and events (with custom labels, optional year for dates and a shared date picker); group membership picker with inline creation of a new group; nickname field; validation and unsaved-changes guard
- Groups (labels): a dedicated screen to create, rename and delete groups with member counts, opened from the list filter or the filter chip menu
- Duplicates: a dedicated screen (list overflow menu) that finds likely duplicates by normalized phone, email or full name, shows name-only matches with a warning, lets you pick the primary contact and merges all data, labels and the photo into it while keeping favorites; the merged-away contacts are deleted
- Favorites persisted with DataStore; Material You dynamic color; adaptive two-pane layout on wide screens

## Architecture

Multi-module Clean Architecture with a build-enforced dependency rule — `:domain` (pure JVM) ← `:data` (android-library) ← `:app`:

```
ContactsApp/
├── domain/   # pure models, repository interfaces, use cases, ports (+ fast JVM tests)
├── data/     # ContactsContract sources (datasource/read/, datasource/write/), repositories, vCard codec
└── app/      # presentation/, di/, ui/
    ├── presentation/   # screens, ViewModels, UiState (contacts/, details/, editor/, groups/, duplicates/,
    │                   # navigation/; shared helpers in common/: permissions/, call/, share/, format/)
    ├── di/             # Metro dependency graph (needs :data on classpath for codegen)
    └── ui/             # design system: theme tokens + components
```

- **DI — Metro** (compile-time, no KSP/KAPT) instead of Hilt: smaller generated code, faster builds.
- **Navigation — Navigation 3** (`NavDisplay` + `NavBackStack`) with Material adaptive `ListDetailSceneStrategy` for tablets/foldables and custom slide/fade transition specs.
- **State** — single `UiState` + sealed `Loadable<T>` (Loading/Empty/Content/Error) per screen.
- **vCard** — разбор и сборка .vcf живут в `:data` за портом `VCardCodec` из `:domain`: presentation не знает формата, а UI получает готовый текст (share/export) или формы контактов (import); методы порта suspend — разбор и сборка больших файлов не идут на главном потоке.
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
./gradlew :domain:test :data:testDebugUnitTest :app:testDebugUnitTest  # 251 unit tests
./gradlew :app:connectedDebugAndroidTest  # 33 Compose UI tests (needs a running emulator)
./gradlew detekt ktlintCheck          # static analysis, all modules
```
