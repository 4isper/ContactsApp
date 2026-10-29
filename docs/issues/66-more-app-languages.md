# feat: expand localization beyond English and Russian

**Priority:** P2 — the language picker exists and the mechanism works for any BCP-47 tag, but only `values` (English) and `values-ru` ship, and the override has gaps outside MainActivity
**Area:** i18n / settings / widget

## Status

Done.

What changed:

- Four full locale catalogs — `values-es`, `values-de`, `values-fr`, `values-pt-rBR` (463 keys each: 445 strings + 18 plurals), with locale-correct quotation marks, plural rules, and placeholder order; brand and protocol names stay untranslated.
- The override gap is closed by one shared decision: `Context.withAppLanguage()` (extracted from MainActivity's private wrap) is now applied to the favorites widget and the reminder notification in addition to the activity. On Android 13+ a non-empty system per-app choice wins — the system has already applied it to the context, and a manual wrap would fight it.
- `SystemAppLanguage` wraps `LocaleManager`: the in-app pick is mirrored into the system (`setApplicationLocales`), so the system per-app language page shows the same choice; on 33+ the screen skips the manual `recreate()` because the system recreates activities itself. Reset clears the mirror. After a system-page change, MainActivity syncs the stored language before wrapping (so "system default" doesn't resurrect the old pick) and updates the settings row's stored value.
- `locales_config.xml` + `android:localeConfig` — the app now appears in the Android 13+ per-app language page. The picker offers system/English/Russian/Spanish/German/French/Português (Brasil).
- Parity is enforced, not trusted: `MissingTranslation` is promoted to an error for `:app`, and `AppLocalesSmokeTest` renders every shipped locale through the wrap (per-locale settings title, plural resolution).

Verification:

- Key parity checked mechanically (all four locales carry the same 463 keys) and by `:app:lintDebug`, which now fails on a missing translation — proven by mutation: removing one es key fails the build, restoring it passes.
- JVM: `AppLanguageWrapTest` (stored language localizes a non-activity context, empty store passes through, system choice suppresses the wrap via `ShadowLocaleManager`), mirror and reset-clear tests in `SettingsViewModelTest`; full suites, detekt, and ktlint green.
- Connected: `SettingsUiTest` 11/11 on the fold AVD, including the language sheet offering all shipped languages.
- Manual on the fold AVD (Android 17): `cmd locale set-app-locales … es` (what the system page does) → cold start renders Spanish end to end ("Contactos", "Ajustes", the permission primer), and the settings row shows "Idioma — Español" after the sync; picking Deutsch in-app mirrors `[de]` into the system and the window re-creates without a double restart; picking "System language" clears the system choice, falls back to English (the device language), and the row shows "System language".

Known limitations:

- Below Android 13 the reverse direction (an external change of the app language) does not exist by design; the stored preference is the only source there.
- The first release of each new locale is machine-assisted and human-reviewed in one pass; further string changes are guarded by the lint gate.

## Problem

The app is translated into English and Russian only, and the in-app language override is incomplete:

- `app/src/main/res` contains only `values` and `values-ru`; `APP_LANGUAGES` in `SettingsScreen.kt` offers system/English/Russian.
- The override is applied exclusively in `MainActivity.attachBaseContext` via a manual `createConfigurationContext` wrap. Surfaces that build their own UI from the application context do not get it: the favorites widget (`ContactsWidgetProvider` builds RemoteViews from app resources) and the reminder notification (`BirthdayReminderReceiver`) render in the **system** language even when the user picked another in-app language.
- There is no `android:localeConfig` / `locales_config.xml`, so on Android 13+ the app does not appear in the system per-app language page, and the in-app picker does not sync with the platform setting.
- The roadmap README ("Not covered by the roadmap") documents the missing translations.

## Proposed scope

- Add a first batch of locales — proposed: `values-es`, `values-de`, `values-fr`, `values-pt-rBR` (each locale is an independent landing unit; the final set is an implementation decision, but every entry below must hold for each shipped locale).
- Translate plurals and formatted strings correctly per locale (plural rules, placeholder order); untranslated keys fall back to the default (English) locale automatically.
- Register `locales_config.xml` and `android:localeConfig` in the manifest for Android 13+. On API 33+, prefer routing the picker through `LocaleManager.setApplicationLocales` so the in-app setting and the system per-app language stay one source of truth; keep the manual `attachBaseContext` wrap for API 26–32.
- Close the override gap: apply the stored language when the widget builds RemoteViews and when `BirthdayReminderReceiver` builds the notification (wrap the application context with `SharedPrefsAppLanguageStore.language` the same way MainActivity does), or adopt one shared context-wrapping helper for all three surfaces.
- Extend `APP_LANGUAGES` (and `LANGUAGE_LABELS`) with the new tags.
- Add a locale-parity gate: lint with `MissingTranslation` promoted to an error for `:app`, so a future default-locale key cannot silently skip a shipped locale.

## Acceptance criteria

- [ ] Every shipped locale renders the main screens (list, details, editor, groups, duplicates, settings, app lock) without English fallback leaks on visible strings.
- [ ] The widget title/empty text and the reminder notification follow the in-app language choice, not the system locale.
- [ ] On Android 13+ the app appears in the system per-app language page; changing the language from either path keeps both in sync.
- [ ] Switching languages still recreates the activity with the new locale (existing behavior, regression-checked).
- [ ] Plural and formatted strings are grammatically correct in each new locale (spot check).
- [ ] `./gradlew :app:lintDebug` fails on a missing translation in any shipped locale.

## Technical notes

- `app/src/main/java/com/m4isper/contactsapp/MainActivity.kt` — `String.wrap` is private here; extract it if the widget/notification reuse it.
- `app/src/main/java/com/m4isper/contactsapp/presentation/settings/SettingsScreen.kt` — `APP_LANGUAGES` / `LANGUAGE_LABELS` are hardcoded lists; new tags are added there.
- `app/src/main/java/com/m4isper/contactsapp/presentation/settings/AppLanguageStore.kt` — the store already holds any BCP-47 tag; no change needed.
- `app/src/main/java/com/m4isper/contactsapp/presentation/widget/ContactsWidgetProvider.kt` and `app/src/main/java/com/m4isper/contactsapp/presentation/reminders/BirthdayReminderReceiver.kt` — the surfaces that ignore the override today.
- `app/src/main/AndroidManifest.xml` — `android:localeConfig` attribute plus `res/xml/locales_config.xml`.
- `gradle/libs.versions.toml` — no new dependencies needed for the manual path; `appcompat` is only required if the AppCompatDelegate per-app locales route is chosen instead of `LocaleManager` + manual wrap.

## Out of scope

- RTL locales (Arabic, Hebrew): the code uses start/end alignment, but a dedicated layout and icon-flip audit is its own issue.
- Translations without a review pass (machine-generated strings land only if someone reads them first).
- Localizing the store listing or the app's play-policy data-safety texts.
