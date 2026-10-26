# fix: request contact-write permission when the action happens, not on screen entry

**Priority:** P1 — permission prompts appear before any write, while some writes still fail silently
**Area:** permissions / contact details, groups, duplicates

## Problem

The permission flow is inconsistent after issue 24:

- `ContactDetailsScreen` toggles the favorite by calling `viewModel.toggleFavorite()` directly (`ContactDetailsScreen.kt` lines ~115–118). It never checks or requests `WRITE_CONTACTS`. `ContactDetailsViewModel.toggleFavorite()` then calls the use case inside `safeCall` and only logs the failure (`ContactDetailsViewModel.kt` lines ~135–143). On a fresh install without write permission, tapping the heart can do nothing with no message.
- `GroupsScreen`, `GroupDetailScreen`, and `DuplicatesScreen` launch a `WRITE_CONTACTS` request in a `LaunchedEffect` as soon as the screen opens (`GroupsScreen.kt` lines ~75–82, `GroupDetailScreen.kt` lines ~81–88, `DuplicatesScreen.kt` lines ~81–87). A user who only wants to browse groups or duplicates gets a system permission dialog out of context. If permission was previously denied with "don't ask again", the launch is a no-op and there is no rationale, no settings route, and no indication that later write actions will fail. The launcher callbacks are also empty comments (`/* no-op */`).
- Group actions (create, rename, delete, merge, add/remove member) and duplicate merges do not check permission before calling the write use cases, so denial turns into generic failure toasts or silent no-ops.
- Because permission state is not tracked on these screens, granting permission in system settings and returning to the screen does not reliably re-enable the flow.

## Proposed scope

- Adopt a single, contextual write-permission gate for contact mutations, following the pattern already used by `ContactsScreen`:
  - When the user taps a write action and permission is missing, request it.
  - If granted, retry the pending action exactly once.
  - If denied, do not perform the action; show a short rationale with a "Open settings" action where useful.
- Remove the entry-time `LaunchedEffect` requests from Groups, Group Detail, and Duplicates. Browsing must not trigger a permission prompt.
- Gate group create/rename/delete/merge, add/remove member, and duplicate merge at the moment of the action. Pass the pending action through to the permission callback, as `ContactsScreen` does for favorite/delete.
- Fix the details favorite path: gate it on `WRITE_CONTACTS`, surface a snackbar on write failure (not just a log), and refresh the favorite state after success. Keep widget refresh only after a successful write.
- Check `hasPermission` again on resume so a user returning from Settings sees the updated state; do not cache a denied state forever.
- Make denial accessible: announce the refusal and explain why the action did not happen. Do not show a generic "action failed" toast for a permission problem.
- Add haptic feedback only when the action is actually attempted (or clearly document the existing list behavior); the details toggle currently has no haptic feedback at all, unlike the list.

## Acceptance criteria

- [ ] Opening Groups, Group Detail, or Duplicates never shows a permission dialog by itself.
- [ ] Tapping favorite in contact details without write permission requests it, then applies the toggle after a grant.
- [ ] If favorite permission is denied, the heart state does not change and a clear snackbar with a settings action appears.
- [ ] Group and duplicate write actions are gated; denied actions do not reach the data layer.
- [ ] Returning from system settings refreshes permission state and allows the action without restarting the screen.
- [ ] A denied permission never results in an unhandled `SecurityException` or a silent no-op.
- [ ] User-facing permission strings exist in English and Russian.

## Technical notes

- `app/src/main/java/com/m4isper/contactsapp/presentation/details/ContactDetailsScreen.kt` lines ~84–127 — favorite call site.
- `app/src/main/java/com/m4isper/contactsapp/presentation/details/ContactDetailsViewModel.kt` lines ~135–143 — swallowed failure.
- `app/src/main/java/com/m4isper/contactsapp/presentation/groups/GroupsScreen.kt` lines ~75–90.
- `app/src/main/java/com/m4isper/contactsapp/presentation/groups/GroupDetailScreen.kt` lines ~81–90, ~176–186, and the top-bar actions.
- `app/src/main/java/com/m4isper/contactsapp/presentation/duplicates/DuplicatesScreen.kt` lines ~81–87, ~152–165.
- `app/src/main/java/com/m4isper/contactsapp/presentation/contacts/ContactsScreen.kt` lines ~129–157 — the existing pending-action pattern to reuse.
- `app/src/main/java/com/m4isper/contactsapp/presentation/common/permissions/` — `hasPermission`, `PermissionRequest`, `openAppSettings`.
- `docs/issues/24-write-contacts-permissions.md` — this is a UX follow-up: issue 24 added requests, but some are mistimed and one path is still missing.

## Out of scope

- Requesting `WRITE_CONTACTS` for the editor; it already has a dedicated permission screen.
- Broad permission onboarding or changes to the system permission model.
- The notification permission flow (covered separately).

## Verification

- Manual fresh install, deny, grant, and "don't ask again" for each entry point.
- Verify groups/duplicates remain browseable before any write.
- Verify favorite toggling with and without permission, including failure injection from the data source.
- `./gradlew :app:compileDebugKotlin :app:testDebugUnitTest`.
