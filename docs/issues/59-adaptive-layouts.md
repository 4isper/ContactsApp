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

- [x] Settings, import, and details content are centered and capped at a readable max width on expanded windows; no field spans more than ~640 dp.
- [x] Groups↔details and duplicates review use a two-pane layout on expanded widths and the existing full-screen behavior on compact widths.
- [x] The editor uses a two-column form at ≥ 840 dp without losing field order or validation focus behavior.
- [x] Breakpoint logic lives in one shared helper; no new `screenWidthDp` comparisons are added in leaf composables.
- [x] Previews/screenshot tests exist for compact, medium, and expanded widths; the existing phone tests still pass unchanged.
- [x] Rotating a phone in the editor does not lose scroll position or unsaved input.

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

## Decisions

- **One helper, two shapes of adaptivity.** `ui/adaptive/WindowWidthClass.kt` maps `currentWindowAdaptiveInfoV2().windowSizeClass` to `COMPACT`/`MEDIUM`/`EXPANDED` at the Material breakpoints (600 and 840 dp). Screens that branch ask for a class; screens that only need a cap do not branch at all. `ReadingColumn` is the layout-only half: centered, `widthIn(max = contentMaxWidth).fillMaxWidth()`, no threshold. The 640 dp value lives in `Spacing.contentMaxWidth` instead of a local dp. No screen gained a `screenWidthDp` comparison; the only remaining code with that name is `ContactCard`'s trailing-button budget (issue 52), which derives the value from `containerSize`, not `Configuration`.
- **Details keeps list-detail; the column lives inside the pane.** Contacts↔details already used `ListDetailSceneStrategy`, so details only needed `ReadingColumn` around its list. On a 840+ dp window the card is capped at 640 dp inside the detail pane instead of stretching to 1200 dp.
- **A scene key per list↔detail pair.** Every `listPane`/`detailPane` record used the default `sceneKey = Unit`. `ListDetailSceneStrategy` walks the back stack backwards while the key matches, so `ContactsListKey`, `GroupsKey`, and `DuplicatesKey` were one scene and the last-visited list's placeholder won. The manual run showed Groups offering "Select a contact from the list". `SCENE_CONTACTS`/`SCENE_GROUPS`/`SCENE_DUPLICATES` split them.
- **Duplicate review is a destination, not dialog state.** `DuplicateReviewKey(targetId, sourceIds)` carries ids only; the pane resolves the group from the shared `DuplicatesViewModel` and reloads when the stack is restored before the list. That gives the review a rotation/back story identical to contacts, and `MergePreviewContent` is shared with the compact dialog so both render the same rows. Compact keeps the dialog because a second pane does not fit.
- **Editor columns.** Left: photo, account, error banner, name, nickname, groups, then phones/emails/addresses/etc. Right: organization, birthday, events, relations, note, delete. Each column owns a `LazyListState` and a `FormItemIndices`; the error effect scrolls and focuses the left list first and falls back to the right. On compact `EditorBody` emits the same two item groups into one list, so the order is unchanged. The identity rows moved to `items/EditorIdentityItems.kt` to keep `EditorForm` under detekt's method limit.
- **Tests set the window, not the content width.** `Modifier.requiredWidth(1000.dp)` does not widen the layout window: the column capped at 640 dp but stayed centered in the real 411 dp window (observed left = −114 dp). The width tests use `DeviceConfigurationOverride.WindowSize(DpSize(...))`, which overrides `LocalWindowInfo.containerSize`, so `currentWindowAdaptiveInfoV2` and `getUnclippedBoundsInRoot` see 1000 dp. dp assertions carry a 0.5 dp tolerance because the forced density rounds.
- **Previews render real content.** `WidthPreviews.kt` calls `ContactListContent`, `DetailsContent`, `EditorBody` with the real item builders, `SettingsSection`/`SettingsOption`/`SettingsSwitch` inside `ReadingColumn`, and `GroupPickerDialog`, each at 320/600/840 dp with font scale 2.0 on the 320 variant. `ContactListContent` became `internal` for this; the editor preview shows the actual two-column split at 840 dp.

## Verification

- `./gradlew :app:compileDebugKotlin :app:compileDebugAndroidTestKotlin :app:testDebugUnitTest :app:detekt :app:ktlintCheck` — green on the final tree.
- `WindowWidthClassTest` — COMPACT below 600, MEDIUM at 600–839, EXPANDED from 840 (JVM).
- `ReadingColumnTest` — 1000 dp window → 640 dp column at left 180 dp; 400 dp window → column fills it.
- `DetailsContentWidthTest` — the details list is 640 dp wide, left 180 dp, in a 1000 dp window.
- `ContactEditorUiTest` — 29/29, including four new cases: two columns plus field order at 1000 dp, one list at 411 dp, an invalid email still blocking save at expanded width, and a phone-sized `StateRestorationTester` rotation that restores both the scroll position and the typed nickname.
- Full instrumented suite at 411 dp (`wm size 1002x2152`) — 142 tests, 139 passed. The two `FavoritesStarredTest` failures are the stale starred contact id 263 on this AVD (same as issues 57/58; not deleted without authorization). The third failure was `ReadingColumnTest` reporting a Glance widget-receiver NPE (`CoroutineBroadcastReceiver.goAsync` on a background thread) that the runner attributed to the running test; the class passes standalone (2/2), and the editor class was re-run after the test edit (29/29).
- Manual at 851 dp (foldable AVD unfolded, rail visible): contacts↔details two-pane; Groups shows "No groups yet" + "Select a group from the list"; Duplicates shows its groups + "Select a duplicate group to review", and Merge opens the review pane with "Will be added to Event reader: No new fields" and a Merge button; settings rows measure 1502 px at density 390 (616 dp card inside the 640 dp column) and center in the pane after the rail; details caps inside its pane; the editor renders the two columns above with Save still in the top bar; a `.vcf` intent renders the import preview as a centered 640 dp card (no rail on that destination).
- Compact behavior is covered by the same full suite at 411 dp: every phone test passed unchanged, and the compact editor test asserts the single `editor_form` list with no right column.
- Not hand-checked: medium width (600 dp) beyond the previews, live multi-window resizing, tablet 10" hardware, and TalkBack order in the two-column editor. The previews cover 600 dp, and the tests pin both sides of the 840 dp breakpoint.
