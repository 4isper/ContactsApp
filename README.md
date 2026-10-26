# ContactsApp

A native Android contacts manager built with modern Jetpack libraries and Clean Architecture: browse, search and favorite contacts, view details, create, edit, delete and share them (vCard, plain text or QR code), place calls, and get reminders about birthdays — in English and Russian, on phones, tablets and foldables, in light and dark themes.

| Fold, dark theme | Phone, light theme, details |
|---|---|
| ![Fold dark](docs/screenshots/fold-dark.png) | ![Phone light details](docs/screenshots/phone-light-details.png) |

## Features

- Contact list: search by name (including structured name components: given/family/middle/prefix/suffix and their phonetics), phone, email, nickname, relation names, company, address, website, note and group names; favorites filter; group (label) filter from the header chip; account filter; alphabetical grouping with fast scroller; pull-to-refresh; phoneless contacts stay visible; multi-select mode (long press) with select all, batch delete (with confirmation and undo via a snackbar), batch vCard sharing and batch add to a group; import from a .vcf file with a preview of duplicates and invalid entries, export of the selected contacts to a .vcf file; the list is read page by page, so the first rows show up immediately on a large base
- Contact details: phones, emails, instant messengers, structured addresses (street, city, region, postcode, country), websites, organization (company, title, department), note, birthday, nickname, relations (family, friends, etc.), events (anniversaries and custom dates with labels), the account the contact belongs to and group memberships (labels with account ownership); the primary phone and email are used as the subtitle; call (with confirmation), SMS, email, IM, map, open link and copy actions; "add to calendar" for a birthday and an event (the next occurrence of the date); vCard, plain text and QR code sharing; favorites toggle; copy and move to another account
- Contact editor: create, update and delete contacts; account picker for new contacts (local storage or a synced account) that remembers the last used account; existing contacts show their account and only offer groups from it; structured name — a single full-name field or given/family/middle/prefix/suffix components with phonetic variants, toggled with an animated chevron; multi-raw contacts are updated consistently (deterministic target raw contact, group membership rows of all raw contacts reconciled); photo pick from the gallery or the camera with cropping and preview, removal and a round write to the provider; dynamic typed rows for phones, emails, instant messengers, addresses, websites, relations and events (with custom labels, optional year for dates and a shared date picker); group membership picker filtered by the contact's account, with inline creation of a new group in that account; nickname field; validation of email, phone, URL and date with a clear message (the values are normalized before they are written: the domain of an email is lower cased, the formatting of a phone is dropped, a site gets the https scheme), an unsaved-changes guard and a warning with a path to merge a likely duplicate
- Groups (labels): a dedicated screen to create, rename, delete and merge groups with member counts and account labels, opened from the list filter or the filter chip menu; members can be removed from the group screen; group pickers and the group filter distinguish same-name groups from different accounts
- Duplicates: a dedicated screen (list overflow menu) that finds likely duplicates by normalized phone, email or full name, shows name-only matches with a warning, lets you pick the primary contact and merges all data, labels and the photo into it while keeping favorites; the merge is reversible — the sources stay in the provider and can be unmerged
- Settings (list overflow menu): theme override (system/light/dark), dynamic color toggle, show or hide contact photos, app language (system/English/Russian), contact sorting (full name or last name, with matching letter sections), list density (comfortable/compact), favorites first, the default account for new contacts, birthday and event reminders (on/off plus how many days in advance), app lock with a PIN code, hiding the window in the recent apps list, resetting all settings, and an "About" section with the version and a privacy note
- Privacy: the PIN lock covers the whole app and asks again when the app is genuinely backgrounded; the PIN is stored as a PBKDF2-HMAC-SHA256 hash with a random salt, not in clear text; after a few wrong attempts the input is blocked for a while; the window can be hidden from the recent apps list with `FLAG_SECURE`
- Reminders: a daily local notification lists the birthdays and events that are coming up; tapping the notification opens the contact; reminders are restored after reboot and request `POST_NOTIFICATIONS` on Android 13+
- Home screen: launcher shortcuts (new contact, favorites) and a widget with the favorite contacts
- Favorites are stored as `STARRED` rows in the provider, so they survive a sync; Material You dynamic color; adaptive two-pane layout on wide screens

## Architecture

Multi-module Clean Architecture with a build-enforced dependency rule — `:domain` (pure JVM) ← `:data` (android-library) ← `:app`:

```
ContactsApp/
├── domain/   # pure models, repository interfaces, use cases, ports (+ fast JVM tests)
├── data/     # ContactsContract sources (datasource/read/, datasource/write/), repositories, vCard codec
└── app/      # presentation/, di/, ui/
    ├── presentation/   # screens, ViewModels, UiState (contacts/, details/, editor/, groups/, duplicates/,
    │                   # settings/, reminders/, widget/, navigation/; shared helpers in common/:
    │                   # permissions/, call/, share/, calendar/, format/)
    ├── di/             # Metro dependency graph (needs :data on classpath for codegen)
    └── ui/             # design system: theme tokens + components
```

- **DI — Metro** (compile-time, no KSP/KAPT) instead of Hilt: smaller generated code, faster builds.
- **Navigation — Navigation 3** (`NavDisplay` + `NavBackStack`) with Material adaptive `ListDetailSceneStrategy` for tablets/foldables and custom slide/fade transition specs.
- **State** — single `UiState` + sealed `Loadable<T>` (Loading/Empty/Content/Error) per screen.
- **vCard** — разбор и сборка .vcf живут в `:data` за портом `VCardCodec` из `:domain`: presentation не знает формата, а UI получает готовый текст (share/export) или формы контактов (import); методы порта suspend — разбор и сборка больших файлов не идут на главном потоке; поддерживаются фото, категории, grouped-поля, значения в quoted-printable и структурированные адреса.
- **Background work** — AlarmManager для напоминаний и RemoteViews-виджет: оба получают репозитории из графа приложения и работают без запуска интерфейса.
- **Static analysis** — Detekt + ktlint applied to every module (`detekt`, `ktlintCheck`).

## Stack

| Library | Version |
|---|---|
| Kotlin / AGP / Gradle | 2.4.21 / 9.4.1 / 9.8.2 |
| Compose BOM / compileSdk / minSdk | 2026.09.00 / 37 / 26 |
| Metro DI | 1.4.5 |
| Navigation3 / Adaptive | 1.2.0 / 1.4.0-alpha03 |
| Coil / DataStore / kotlinx.serialization | 3.6.3 / 1.2.1 / 1.12.0 |
| ZXing (QR codes) | 3.5.3 |
| Detekt / ktlint-gradle | 2.0.0-alpha.6 / 14.2.0 |

Android permissions used: `READ_CONTACTS` (list), `WRITE_CONTACTS` (editing and contact mutations), `CALL_PHONE` (direct calls, with dialer fallback), `POST_NOTIFICATIONS` (birthday and event reminders).

## Build & verify

```bash
./gradlew :app:assembleDebug          # debug APK
./gradlew :domain:test :data:testDebugUnitTest :app:testDebugUnitTest  # 754 unit tests
./gradlew :app:connectedDebugAndroidTest  # 102 Compose UI tests (needs a running emulator)
./gradlew detekt ktlintCheck          # static analysis, all modules
```
