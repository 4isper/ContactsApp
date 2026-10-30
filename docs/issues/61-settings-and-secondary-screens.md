# feat: refresh settings and the secondary screens with a scannable, consistent style

**Priority:** P3 — settings is a plain radio list and the utility screens look unfinished compared to the contact list
**Area:** settings / duplicates / recents / groups / empty states

## Problem

- `presentation/settings/SettingsScreen.kt` is a stack of cards with plain text radio rows (`SettingsOption`): theme, sort order, list density, and language each expand into 3–4 full rows, so four independent settings consume most of the screen. There are no leading icons, no current-value summary on a collapsed row, no grouping beyond card titles, and the reset action is a bare `TextButton` outside the card stack. The PIN row is a switch without context (no "enabled since", no change PIN entry).
- The utility screens (Duplicates, Recents, Groups, Import) reuse a generic list with bare top bars and per-row icon buttons. Their empty states are the shared `EmptyState` (`ui/components/States.kt`) with a Material icon, a title, and a full-width button — consistent but flat, with no brand voice and no distinct illustration or accent per screen.
- Action labels and tone vary between screens ("OK", "Save", "Done"-style mixes); some one-off strings were added per feature rather than reusing `action_*` resources.
- Import/export is launched from the home overflow (issue 56) with no progress or result summary; on a large vCard the user sees nothing until the snackbar appears.
- There is no "About" section (version, licenses, source), which users expect in settings.

## Proposed scope

- Settings: convert each multi-choice setting to a single row with icon + title + current value, opening a bottom sheet or using a segmented control; switches for booleans with a supporting line; group into Appearance / Behavior / Security / Data / About. Move "Reset settings" into a "Danger zone" section with a confirmation dialog. Show PIN state as "Enabled · 4–8 digits" with a "Change PIN" action.
- Give each section a short supporting description; keep rows at 56–64 dp and aligned with the same horizontal padding as the home list.
- Utility screens: consistent top bar (title + optional primary action), list rows aligned to the home row tokens (issue 57), empty states with a tinted icon container, a one-line explanation, and one primary action.
- Export/import: show determinate/indeterminate progress while processing and a result summary (imported/skipped/errors) before returning; reuse the existing snackbar/event channel.
- Add an About section with version and open-source licenses entry point.
- Audit action strings for sentence case and a single verb per intent; reuse `action_*` resources where they already exist.

## Acceptance criteria

- [x] Theme, sort, density, and language are each one collapsed row with the current value visible; changing one takes at most two taps and returns to settings with the new value shown.
- [x] Reset is in a clearly separated destructive section and requires confirmation.
- [x] PIN section shows its current state and offers change/disable without hunting.
- [x] Duplicates, Recents, and Groups empty states share the new component and each has a screen-appropriate action; no screen still uses a bare icon + button with no context line.
- [x] Import/export shows progress and a summary, and can be dismissed without losing the result text.
- [x] About section exists; all new strings are localized in en and ru; TalkBack order is logical and switches announce their state.

## Technical notes

- Shared tokens and rules: `docs/design/visual-rules.md` (issue 54); reuse the row and type tokens.
- `app/src/main/java/com/m4isper/contactsapp/presentation/settings/SettingsScreen.kt` — `SettingsSection`/`SettingsOption` (radio list), reset `TextButton`, PIN switch.
- `app/src/main/java/com/m4isper/contactsapp/presentation/duplicates/`, `presentation/recents/`, `presentation/groups/`, `presentation/import/` — top bars, rows, and empty states.
- `app/src/main/java/com/m4isper/contactsapp/ui/components/States.kt` — `EmptyState`; extend it (icon container, supporting text) rather than forking per screen.
- `app/src/main/java/com/m4isper/contactsapp/ui/components/ConfirmDialog.kt` — reuse for the reset/destructive confirmation.
- Strings: `app/src/main/res/values/strings.xml`, `values-ru/strings.xml`; reuse `action_*` where possible; the settings labels are produced by `labelRes()` mappers in the settings model — keep those as the single source.
- Issue 56 owns the home entry point to settings. Leave that slot in the new layout.
- Version name comes from `BuildConfig`/`PackageManager`; no new dependency for the About section.

## Out of scope

- New settings (backup scheduling, sync interval, etc.).
- Redesigning the contact list or details (issues 57–58).
- Migrating preferences storage.

## Decisions

- **One value row, one sheet.** `SettingsChoiceRow` (`SettingsChoice.kt`) shows a tinted leading icon, the setting name, the current value, and a chevron at 56 dp minimum height. Tapping it opens `SettingsChoiceSheet` (shared `AppBottomSheet`, radio rows); tapping an option applies it and closes the sheet in the same tap, so a change is two taps and lands back on settings with the new value. Theme, density, language, sort order, and the default account when there is more than one all use this row; the option test tags (`theme_*`, `sort_*`, `density_*`, `language_*`, `default_account_*`) are unchanged, so the existing tests only needed the new second tap.
- **Sections by intent.** Appearance (theme, density, language, dynamic color, photos), Behavior (sort, favorites first, default account), Reminders, Security (lock, PIN state, secure window), Danger zone, About. Section titles are `titleSmall` on `surfaceContainerLow` cards — the row alignment and corner treatment match the issue 54/57 list surfaces.
- **Reset is its own red card.** `SettingsDangerZone` is a separate `errorContainer` card outside the section stack, with the reset label and its consequence on one 56 dp row; the `ConfirmDialog` (`confirm_reset_settings`) still gates it.
- **PIN state is a line, not a switch label.** `SettingsPinState` reads "Enabled · 4–8 digits" (tertiary) or "Disabled" under the lock switch, and offers `app_lock_change_pin` only when a code exists. Change PIN reuses `PinSetupDialog` with a title override instead of adding a second dialog.
- **About is its own section.** Version from `PackageManager`, an `Open-source licenses` row that opens a sheet listing the Apache-2.0 libraries (Kotlin/Coroutines/Serialization, AndroidX and Compose, Metro, Coil, ZXing), and the existing on-device privacy note.
- **EmptyState grew a supporting line.** `EmptyState` now puts the icon in a `primaryContainer` circle, adds an optional `description` (`bodyMedium`, `onSurfaceVariant`), keeps the merged semantics for TalkBack, and caps the content at `contentMaxWidth`. Groups passes `groups_empty_desc` and `groups_create_action` (opens the create sheet); Recents and Duplicates pass their descriptions and `action_browse_contacts` (`onBack`). The contacts list's own empty variants already carry a specific title plus a clear CTA from issue 56 and were left as they are.
- **Progress lives next to the content, not in a snackbar.** Import shows an indeterminate `LinearProgressIndicator` under the top bar whenever `isImporting`, disables the decision chips, and replaces the preview with a result screen as soon as `outcome` is set — a result card at the bottom of a 500-entry list would never be seen. Export shows an inline `ExportStatusBanner` above the list: a progress row while the vCard is written, then a persistent summary card with a close button. A snackbar was rejected because `SnackbarHostState` serializes the delete-undo snackbar behind it; a toast was rejected because the result would time out and be lost.
- **Locale parity, one label fix.** Every new string exists in `values` and `values-ru`. The reminder switch now reads "Birthdays and events" instead of repeating the section title "Reminders"; the description and test tags are unchanged.

## Verification

- `./gradlew :app:compileDebugKotlin :app:compileDebugAndroidTestKotlin :app:testDebugUnitTest :app:detekt :app:ktlintCheck` — green on the final tree. detekt required moving `ExportSummary` into its own file (single-declaration file-name rule) and renaming `SettingsRows.kt` to `SettingsChoice.kt` (matching declaration name).
- `ImportPreviewViewModelTest` (JVM) — a delayed `createContact` leaves `isImporting` true with `canImport` false after `runCurrent()`, and the state settles to the outcome after the delay.
- `SettingsUiTest` — 9/9. Stored values render on the four collapsed rows (read in the unmerged tree, and the merged row text contains the current value so TalkBack reads name and value together), a change is row → option and the row shows the new value, reset sits behind `confirm_reset_settings`, the enabled PIN state shows the change action and a new 4-digit code saves, and the licenses sheet lists the third-party libraries.
- `GroupsUiTest` (new) — the empty state shows its description and action, and `groups_create_action` opens the create sheet; confirming "Work" records the group and the list shows it.
- `RecentsUiTest`, `DuplicatesUiTest` — the empty states show their descriptions and `action_browse_contacts` invokes `onBack`.
- `ImportPreviewUiTest` — 4/4, including a delayed create: the progress indicator is displayed, the decision chips are disabled, and the result card replaces the preview list after the outcome. The multiple-duplicate merge path still requires an explicit target.
- `ExportStatusBannerTest` (new) — 3/3: progress row, summary text stays until the close button clears it, and the failed summary keeps its error styling.
- Full instrumented suite at 411 dp (`wm size 1002x2152`, performance class excluded) — 157 tests, 153 passed. Failures: the two known `FavoritesStarredTest` cases from the stale starred contact id 263 on this AVD (unchanged since issues 57–60; not deleted without authorization), `ReadingColumnTest` with the known Glance receiver `PendingResult.finish()` NPE (passes standalone, 2/2), and `ContactEditorUiTest.birthdayPickerCancel_keepsNoYearValue` with the known window-focus `RootViewWithoutFocusException`; that method passes alone (1/1), and issue 60's full run had it green.
- Manual at phone width 411 dp and font scale 2.0: the settings screen scrolls without clipping; each choice row reads name + current value; theme changed in two taps to Dark, survived `am force-stop` plus relaunch, and the row showed "Dark" afterwards; dark mode renders every section including the red danger zone; the PIN area shows "Disabled"; the About section shows version and the licenses row (`docs/screenshots/phone-light-settings.png`, `phone-light-settings-bottom.png`). The Groups empty state shows the tinted icon, explanation, and Create group action (`phone-light-groups-empty.png`).
- Manual import/export: a generated 500-contact `.vcf` opens the preview with "Contacts in file: 500" and 500 decision rows; importing it showed the progress bar and produced the result screen; the 500 contacts were then removed through search + select all + delete, restoring the 58 pre-existing contacts. A non-vCard file lands on "No contacts found in the file". Exporting the selection to Downloads wrote `selected-contacts.vcf` and left "Exported 58 contacts" above the list until dismissed (`phone-light-export-summary.png`).
- Not hand-checked: a live TalkBack session (switch state and merged row text are asserted in the instrumented tests), RTL, and process death via the developer "Don't keep activities" switch — persistence was checked with a force-stop relaunch.
