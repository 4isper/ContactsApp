# feat: make every screen adaptive, not just contacts↔details

**Priority:** P2 — on tablets and foldables, settings, editor, groups, duplicates, recents, and import all stretch a phone layout across 1200 dp
**Area:** adaptive layout / navigation shell / content width

## Problem

- `presentation/navigation/ContactsNavHost.kt` applies `ListDetailSceneStrategy` only to Contacts↔ContactDetails. Groups, Settings, Editor, Duplicates, Recents, and Import open full-screen even when there is enough width for two panes. A foldable user editing a contact loses the list they came from.
- No screen constrains content width. On an expanded window, `presentation/settings/SettingsScreen.kt` renders a single column of full-width radio rows, and `presentation/editor/EditorScreen.kt` stretches text fields to 1000 dp+ — one-line inputs a meter long. `DetailsContent` has the same problem.
- Layout decisions are made inconsistently: list-detail uses adaptive APIs, but other screens would need to fall back on `screenWidthDp` checks, which are scattered and easy to get wrong with insets and multi-window.
- Landscape phones run the editor as one long scrolling column; there is no two-column option even when height is the scarce dimension.
- No previews or screenshot tests at medium/expanded widths, so regressions on large screens go unnoticed.

## Proposed scope

- Constrain reading-column screens (settings, import, empty/error states, dialogs' content) to a max width (roughly 600–640 dp) centered in the window; constrain form fields (editor) to a narrower max width and align the primary action to the column, not the window edge.
- Extend two-pane navigation: Groups↔Group details and Duplicates list↔merge review should use `ListDetailSceneStrategy` (or `SupportingPaneSceneStrategy` for supporting content). Keep the editor as a full-screen destination but use a two-column form at ≥ 840 dp.
- Centralize breakpoints in one place (a small adaptive helper or reuse `currentWindowAdaptiveInfoV2` + `WindowSizeClass`) so screens do not each re-derive `screenWidthDp`.
- Add `@Preview` variants at 320/600/840 dp and font scale 2.0 for the list, details, editor, settings, and one dialog; add screenshot tests if the project's preview-screenshot tooling is enabled.
- Do not introduce a second navigation shell here; the rail/suite belongs to issue 56. This issue covers pane strategy and content width.

## Acceptance criteria

- [ ] Settings, import, and details content are centered and capped at a readable max width on expanded windows; no field spans more than ~640 dp.
- [ ] Groups↔details and duplicates review use a two-pane layout on expanded widths and the existing full-screen behavior on compact widths.
- [ ] The editor uses a two-column form at ≥ 840 dp without losing field order or validation focus behavior.
- [ ] Breakpoint logic lives in one shared helper; no new `screenWidthDp` comparisons are added in leaf composables.
- [ ] Previews/screenshot tests exist for compact, medium, and expanded widths; the existing phone tests still pass unchanged.
- [ ] Rotating a phone in the editor does not lose scroll position or unsaved input.

## Technical notes

- Shared tokens and rules: `docs/design/visual-rules.md` (issue 54); add width tokens to `ui/theme/Spacing.kt` rather than raw dp.
- `app/src/main/java/com/m4isper/contactsapp/presentation/navigation/ContactsNavHost.kt` — scene strategies and destination registration.
- `app/src/main/java/com/m4isper/contactsapp/presentation/editor/EditorScreen.kt` / `EditorForm.kt` — long `LazyColumn` of field cards; the two-column split lands here.
- `app/src/main/java/com/m4isper/contactsapp/presentation/settings/SettingsScreen.kt` — `verticalScroll` column; wrap in a width-constrained centered container.
- `app/src/main/java/com/m4isper/contactsapp/presentation/groups/` — group details and picker; groups↔details pane.
- Compose: `Modifier.widthIn(max = …)` + `Alignment.TopCenter` inside the scaffold; avoid hard-coded dp — add the width tokens to `ui/theme/Spacing.kt`.
- `currentWindowAdaptiveInfoV2` and `calculatePaneScaffoldDirective` are already used by the list-detail setup; reuse them rather than adding new window-metrics code.
- Tests: Compose UI tests can set the window size via `DeviceConfigurationOverride`; add at least compact/expanded cases for the editor and settings.

## Out of scope

- Navigation rail/suite and the home chrome (issue 56).
- Supporting Android Auto/TV/XR form factors.
- New editor features; only its layout changes.

## Verification

- Manual: phone portrait/landscape, foldable unfolded, tablet 10"; multi-window; font scale 2.0.
- `./gradlew :app:compileDebugKotlin :app:compileDebugAndroidTestKotlin :app:testDebugUnitTest :app:detekt :app:ktlintCheck`.
- Screenshot comparisons at the three widths for settings, editor, details, groups.
