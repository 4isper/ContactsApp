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

- [ ] Theme, sort, density, and language are each one collapsed row with the current value visible; changing one takes at most two taps and returns to settings with the new value shown.
- [ ] Reset is in a clearly separated destructive section and requires confirmation.
- [ ] PIN section shows its current state and offers change/disable without hunting.
- [ ] Duplicates, Recents, and Groups empty states share the new component and each has a screen-appropriate action; no screen still uses a bare icon + button with no context line.
- [ ] Import/export shows progress and a summary, and can be dismissed without losing the result text.
- [ ] About section exists; all new strings are localized in en and ru; TalkBack order is logical and switches announce their state.

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

## Verification

- Manual: every setting changed and persisted across process death; RTL if supported; font scale 2.0; TalkBack through switches and sheets.
- Import/export with a 500+ contact vCard and an invalid file.
- `./gradlew :app:compileDebugKotlin :app:compileDebugAndroidTestKotlin :app:testDebugUnitTest :app:detekt :app:ktlintCheck`.
