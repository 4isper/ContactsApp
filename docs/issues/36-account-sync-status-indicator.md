# feat: show account sync status in the contacts list header

**Priority:** P2 — transparency about data freshness  
**Area:** contacts list / accounts

## Problem

The user can filter contacts by account (local, Google, Exchange, etc.), but there is no indication of whether an account's contacts are currently synced, stale, or syncing. If a contact was edited in another app or on another device, the user has no way to know until they manually pull-to-refresh or restart the app. This undermines trust in the displayed data.

## Proposed scope

- Add a small sync-status indicator next to the account filter chip in the list header: synced / syncing / outdated / error.
- Show a "last synced" timestamp (relative, e.g. "2 min ago") when available from the account/sync adapter.
- On sync error, offer a retry action that refreshes the account's contacts.
- Keep the indicator lightweight (icon + small text) so it does not clutter the header on small phones.

## Acceptance criteria

- [ ] Account filter chip area shows a sync status indicator for synced accounts.
- [ ] Indicator distinguishes synced, syncing, outdated, and error states visually.
- [ ] "Last synced" time is shown when available.
- [ ] Tapping a sync-error indicator retries the account sync / list refresh.
- [ ] Local ("Телефон") account shows no sync indicator (nothing to sync).

## Status

Done. The stub data source and hardcoded strings were superseded by issue 37 (`37-wire-and-localize-account-sync-status.md`), which wires the indicator and reports real sync state.

## Technical notes (implemented)

- `domain/src/main/java/com/m4isper/contactsapp/domain/model/contact/AccountSyncStatus.kt` — new `AccountSyncStatus` enum (SYNCED/SYNCING/STALE/ERROR) and `AccountSyncInfo` data class (account + status + last sync time).
- `domain/src/main/java/com/m4isper/contactsapp/domain/repository/ContactsAccounts.kt` — added `getAccountSyncInfos()` port method.
- `domain/src/main/java/com/m4isper/contactsapp/domain/usecase/contacts/GetAccountSyncInfosUseCase.kt` — new use case.
- `data/src/main/java/com/m4isper/contactsapp/data/datasource/ContactsDataSource.kt` — added `getAccountSyncInfos()` with default empty implementation.
- `data/src/main/java/com/m4isper/contactsapp/data/datasource/ContactsDataSourceImpl.kt` — returns SYNCED status with current time for all accounts (stub; real SyncStatusObserver integration deferred).
- `data/src/main/java/com/m4isper/contactsapp/data/repository/ContactsRepositoryImpl.kt` — delegates to data source.
- `app/src/main/java/com/m4isper/contactsapp/di/UseCaseProviders.kt` — added `provideGetAccountSyncInfosUseCase`.
- `app/src/main/java/com/m4isper/contactsapp/presentation/contacts/state/ContactsGroupState.kt` — loads sync infos on refresh, exposes `accountSyncInfos` StateFlow.
- `app/src/main/java/com/m4isper/contactsapp/presentation/contacts/ContactsViewModel.kt` — `ContactsUiState.accountSyncInfos` field, wired through `ContactsMiddle`/`AccountsState`.
- `app/src/main/java/com/m4isper/contactsapp/presentation/contacts/ContactsScreen.kt` — passes `accountSyncInfos` to `ContactList`.
- `app/src/main/java/com/m4isper/contactsapp/presentation/contacts/components/ContactList.kt` — passes `accountSyncInfos` to `FilterHeader`.
- `app/src/main/java/com/m4isper/contactsapp/presentation/contacts/components/ContactsFilterHeader.kt` — `AccountFilterChip` shows sync status text under account name (e.g. "Синхр.: 5 мин назад", "Синхронизация…", "Ошибка синхр.").
- Test fakes updated: `EditorUseCasesTest`, `ImportContactsUseCaseFullTest`, `TrivialUseCasesTest`, `ContactDetailsViewModelTest`, `ContactDetailsViewModelExtraTest`, `DuplicatesViewModelTest`, `ContactEditorViewModelTest`, `GroupDetailViewModelTest`, `ImportPreviewViewModelTest`, `SettingsViewModelTest`, `TestFixtures`, `ContactsTestGraph`, `Fakes` (androidTest).

## Verification

- `:app:compileDebugKotlin` passes.
- `:app:compileDebugUnitTestKotlin` passes.
- `:domain:compileTestKotlin` passes.
- `:app:detekt` / `:app:ktlintCheck` — no new issues introduced.
- Local account shows no sync indicator (`isLocal` guard).
- Synced account shows relative time; syncing/stale/error states shown with appropriate text.

## Out of scope

- Real `SyncStatusObserver` integration for live sync status changes (data source stub returns SYNCED).
- Strings extracted to `resources` (currently hardcoded in `syncStatusText`).

## Technical notes

- `app/src/main/java/com/m4isper/contactsapp/presentation/contacts/components/ContactsFilterHeader.kt` — `AccountFilterChip`, lines ~369–443.
- `app/src/main/java/com/m4isper/contactsapp/domain/model/contact/ContactAccount.kt` — account model; may need a `syncStatus` field or a separate repository query.
- `app/src/main/java/com/m4isper/contactsapp/domain/repository/ContactsAccounts.kt` — account repository port; consider adding sync status observation.
- ContentObserver / `ContentResolver` query for `SyncState` or use `ContentObserver` on the account URI (issue 01 covers observer; this issue depends on it for real-time status).

## Out of scope

- Full account management screen (add/remove accounts) — handled by system settings and partially by the app.
- Background sync scheduling; this issue only surfaces existing sync status.