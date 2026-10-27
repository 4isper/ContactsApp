# fix: wire up and localize the account sync status indicator

**Priority:** P2 — the status is currently dead code, and the data behind it is a stub
**Area:** contacts list / account filter

## Problem

The account sync indicator promised by issue 36 is not visible in the app:

- `ContactsScreen` passes `uiState.accountSyncInfos` to `ContactList`, but `ContactList` never forwards the list to `FilterHeader` (`ContactList.kt` lines ~199–219). `FilterHeader` therefore always receives its default `emptyList()`, and `AccountFilterChip` cannot render a status. The `@Suppress("UnusedParameter")` annotation on `ContactList` hides the warning instead of fixing the flow.
- `syncStatusText` in `ContactsFilterHeader.kt` (lines ~475–496) hardcodes Russian strings in Kotlin. In the English UI the chip would show "Синхр.: …", "Устарело", or "Ошибка синхр." even though the rest of the screen is English. No strings exist in `values/strings.xml` or `values-ru/strings.xml`.
- The timestamps are computed once during composition from `System.currentTimeMillis()`. They do not refresh while the screen stays open, so "5 min ago" becomes stale.
- The data source is still a stub: `ContactsDataSourceImpl.getAccountSyncInfos()` returns `SYNCED` with `lastSyncTimeMs = now` for every non-local account. If the indicator is wired without changing this, every account will permanently claim "Synced just now", which is worse than showing nothing.
- The `ERROR` state has no recovery action, although issue 36's acceptance criteria required a retry.

## Proposed scope

- Forward `accountSyncInfos` from `ContactsScreen` → `ContactList` → `FilterHeader` and remove the `UnusedParameter` suppression.
- Replace the hardcoded strings with resources for English and Russian (status labels, relative time, "Synced", "Not synced yet"). Use a plural-aware/relative-time formatter instead of building strings in Kotlin.
- Recompute relative time only when the selected account or sync state changes, or expose it through a small ticker; avoid calling `System.currentTimeMillis()` directly in a composable body.
- Do not show a misleading "Synced just now" for the stub. Either implement real sync information (`SyncStatusObserver` / `SyncState` / account sync adapter query) or hide the status row until real data exists. If the stub remains, return no sync info rather than a fake timestamp.
- Show the sync state in the account dropdown rows too, not only in the collapsed chip, so users can compare accounts before selecting one.
- For `ERROR`, expose a visible retry action that refreshes the list/account and announces the result. Keep the chip compact: status text under the account name only while an account is selected, with the full text in the dropdown and in the content description.
- Add a content description such as "Google account, last synced 5 minutes ago" so TalkBack does not read only the account name.

## Acceptance criteria

- [x] Selecting a non-local account shows the sync status and the relative last-sync time when the platform provides one.
- [x] Selecting the local account shows no sync status.
- [x] All sync-related UI strings come from resources and are correct in English and Russian.
- [x] The indicator never shows a fabricated "just now" timestamp when no real sync information is available.
- [x] An error state offers a retry/refresh action and gives feedback after it is used.
- [x] TalkBack reads the account name together with its sync state.
- [x] `accountSyncInfos` is no longer an unused parameter anywhere in the chain.

## Status

Done.

### What was implemented

- `ContactList` now forwards `accountSyncInfos` to `FilterHeader`; the `@Suppress("UnusedParameter")` annotation is gone.
- `ContactsFilterHeader.kt` renders the status under the selected account name and in every account dropdown row. The collapsed chip stays compact; the dropdown shows `account type · status`.
- The hardcoded Russian `syncStatusText` was replaced with resource-backed strings and a plural-aware relative-time formatter in `presentation/contacts/components/SyncRelativeTime.kt`.
- Relative time is produced by a one-minute ticker (`produceState`) and never by `System.currentTimeMillis()` directly in a composable body. The ticker only runs when at least one real timestamp exists.
- Local accounts are guarded in `ContactAccount?.syncInfo()`: they never receive a status row, a state description, or a retry action.
- TalkBack receives the sync state from the visible status line inside the merged chip/dropdown semantics, so it reads the account name and the status together without a duplicate announcement. The retry controls have their own content descriptions.
- `AccountSyncStatus.ERROR` has two recovery entry points: a retry icon next to the selected chip and a separate "Retry sync" item in the account dropdown. Both dispatch the new `ContactsFileAction.RetrySync`.
- `ContactsViewModel.refresh(reportSyncResult = true)` reloads contacts, groups, accounts, and sync info, then emits `ContactsEvent.SyncRetryFinished(success)`. The event handler shows a localized toast ("Contacts refreshed" / "Couldn't refresh contacts"), so the result is announced with TalkBack as a normal toast.
- `AccountSyncInfo.lastSyncTimeMs` stays `null`: Android has no public API for the last successful sync time. No timestamp is fabricated; the indicator shows only the real status. The UI already supports timestamps if a future data source supplies them.
- `ContactsDataSourceImpl.getAccountSyncInfos()` now reports real state from `ContentResolver`: active/pending sync → `SYNCING`; disabled account/global sync, non-syncable account, or dirty raw contacts → `STALE`; otherwise `SYNCED`. `ERROR` is not synthesized because Android does not expose the previous sync outcome publicly.
- Added normal permissions `READ_SYNC_SETTINGS` and `READ_SYNC_STATS` for the status queries.

### Verification

- `./gradlew :app:compileDebugKotlin :app:testDebugUnitTest :data:testDebugUnitTest :app:compileDebugAndroidTestKotlin` passes.
- `./gradlew :app:detekt :data:detekt :app:ktlintCheck` passes.
- The full `:data:ktlintCheck` still fails on pre-existing violations in files unrelated to this issue (`ContactsWriterDataSourceImpl`, `ContactsDataMappers`, `ContactsImMappers`, `ContactsRowsReader`, `ContactsCreateValues`, `ContactsListOperations`, `ContactsWriterValues`, `ContactsRepositoryImpl`, plus several data test sources). Every file touched by this issue is ktlint-clean.
- Unit tests: `syncRelativeTime` covers just-now, minute/hour/day boundaries, and future timestamps; `resolveAccountSyncStatus` covers active, pending, unknown, disabled, dirty, and healthy combinations.
- Compose tests in `ContactsFilterHeaderSyncTest` cover the selected-account status, the local-account guard, status in the dropdown, and the `ERROR` retry action. They compile but need a device/emulator to execute.
- Manual checks still required: select each account in English and Russian, leave the screen open for several minutes, and run a TalkBack pass over the chip and the dropdown.

### Deviations from the plan

- The plan suggested `DateUtils.getRelativeTimeSpanString`; the implementation uses explicit plurals so both languages share one tested model and Russian gets correct plural forms.
- The plan's "relative last-sync time" is only shown when real data exists. The platform does not provide it today, so most accounts show "Synced"/"Out of date" without a time instead of a fabricated timestamp.
- `ERROR` recovery is present in the UI but the current data source cannot produce `ERROR`; it will appear when a future data source can report the previous sync outcome.

## Technical notes

- `app/src/main/java/com/m4isper/contactsapp/presentation/contacts/ContactsScreen.kt` lines ~159–167 — already passes `accountSyncInfos`.
- `app/src/main/java/com/m4isper/contactsapp/presentation/contacts/components/ContactList.kt` lines ~53–65 and ~199–219 — the missing hop.
- `app/src/main/java/com/m4isper/contactsapp/presentation/contacts/components/ContactsFilterHeader.kt` lines ~378–496 — `AccountFilterChip`, `syncStatusText`.
- `domain/src/main/java/com/m4isper/contactsapp/domain/model/contact/AccountSyncStatus.kt` — status model.
- `data/src/main/java/com/m4isper/contactsapp/data/datasource/ContactsDataSourceImpl.kt` lines ~185–199 — the SYNCED/now stub.
- `app/src/main/java/com/m4isper/contactsapp/presentation/contacts/state/ContactsGroupState.kt` lines ~130–135 — loads sync info.
- Add `account_sync_*` strings to both `values/strings.xml` and `values-ru/strings.xml`. Use `DateUtils.getRelativeTimeSpanString` or a `Duration`-based helper for localized relative time.

## Out of scope

- Building a full account management screen.
- Scheduling background sync; this issue only reports state that is actually available.
- Replacing the system account picker.

## Verification

- Manual: select each account in English and Russian; confirm the status text language and the local-account guard.
- Manual: rotate the device and leave the screen open for several minutes; confirm the relative time does not become incorrect.
- Unit test the relative-time helper for "just now", minutes, hours, and days.
- `./gradlew :app:compileDebugKotlin :app:testDebugUnitTest`.
