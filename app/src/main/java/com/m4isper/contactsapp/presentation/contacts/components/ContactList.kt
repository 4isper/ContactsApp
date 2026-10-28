package com.m4isper.contactsapp.presentation.contacts.components

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.m4isper.contactsapp.R
import com.m4isper.contactsapp.domain.model.contact.AccountSyncInfo
import com.m4isper.contactsapp.domain.model.contact.ContactAccount
import com.m4isper.contactsapp.domain.model.contact.ContactDetails
import com.m4isper.contactsapp.domain.model.contact.ContactGroup
import com.m4isper.contactsapp.domain.model.contact.PhoneNumber
import com.m4isper.contactsapp.presentation.common.Loadable
import com.m4isper.contactsapp.presentation.common.call.ConfirmCallDialog
import com.m4isper.contactsapp.presentation.common.call.rememberCallHandler
import com.m4isper.contactsapp.presentation.common.format.phoneText
import com.m4isper.contactsapp.presentation.common.groups.GroupPickerSheet
import com.m4isper.contactsapp.presentation.common.share.openSms
import com.m4isper.contactsapp.presentation.contacts.ContactsEvent
import com.m4isper.contactsapp.presentation.contacts.ContactsFileAction
import com.m4isper.contactsapp.presentation.contacts.ContactsListAction
import com.m4isper.contactsapp.presentation.contacts.ContactsNavigationAction
import com.m4isper.contactsapp.presentation.contacts.ContactsUiAction
import com.m4isper.contactsapp.presentation.contacts.ContactsUiState
import com.m4isper.contactsapp.presentation.contacts.effects.ExportSummary
import com.m4isper.contactsapp.presentation.contacts.effects.handleContactsEvent
import com.m4isper.contactsapp.presentation.contacts.effects.rememberVCardLaunchers
import com.m4isper.contactsapp.ui.components.ConfirmDialog
import com.m4isper.contactsapp.ui.components.HapticImpact
import com.m4isper.contactsapp.ui.components.performHapticFeedback

private data class QuickCallTarget(
    val name: String,
    val phone: PhoneNumber,
)

@Composable
internal fun ContactList(
    uiState: ContactsUiState,
    onAction: (ContactsUiAction) -> Unit,
    modifier: Modifier = Modifier,
    groups: List<ContactGroup> = emptyList(),
    accounts: List<ContactAccount> = emptyList(),
    profileContactId: Long? = null,
    accountSyncInfos: List<AccountSyncInfo> = emptyList(),
    onCreateContact: (() -> Unit)? = null,
    onImportClick: (() -> Unit)? = null,
) {
    val ctx = LocalContext.current
    // Листья получают свои лямбды, а контракт экрана остаётся один.
    val onSearchQueryChange: (String) -> Unit = { onAction(ContactsListAction.SearchQueryChanged(it)) }
    val onSearchOpened: () -> Unit = { onAction(ContactsListAction.SearchOpened) }
    val onSearchClosed: () -> Unit = { onAction(ContactsListAction.SearchClosed) }
    val onFavoritesOnlyChange: () -> Unit = { onAction(ContactsListAction.FavoritesOnlyToggled) }
    val onClearSearch: () -> Unit = { onSearchQueryChange("") }
    val onContactClick: (Long) -> Unit = { onAction(ContactsNavigationAction.ContactClicked(it)) }
    val onAddClick: () -> Unit = { onAction(ContactsNavigationAction.AddClicked) }
    val onRetry: () -> Unit = { onAction(ContactsFileAction.Retry) }
    val onRefresh: () -> Unit = { onAction(ContactsFileAction.Refresh) }
    val onRetrySync: () -> Unit = { onAction(ContactsFileAction.RetrySync) }
    val onContactSelected: (Long) -> Unit = { onAction(ContactsListAction.ContactSelected(it)) }
    val onClearSelection: () -> Unit = { onAction(ContactsListAction.ClearSelection) }
    val onSelectAll: (List<Long>) -> Unit = { onAction(ContactsListAction.SelectAll(it)) }
    val onDeleteSelected: () -> Unit = { onAction(ContactsFileAction.DeleteSelected) }
    val onUndoDelete: () -> Unit = { onAction(ContactsFileAction.UndoDelete) }
    val onDeleteWindowClosed: () -> Unit = { onAction(ContactsFileAction.DeleteWindowClosed) }
    val onShareSelected: () -> Unit = { onAction(ContactsFileAction.ShareSelected) }
    val onImportText: (String) -> Unit = { onAction(ContactsFileAction.ImportText(it)) }
    val onExportRequested: () -> Unit = { onAction(ContactsFileAction.ExportRequested) }
    val onExportSelectionRequested: () -> Unit = { onAction(ContactsFileAction.ExportSelectionRequested) }
    val onEventHandled: () -> Unit = { onAction(ContactsFileAction.EventHandled) }
    val onGroupSelected: (Long?) -> Unit = { onAction(ContactsListAction.GroupSelected(it)) }
    val onAccountSelected: (ContactAccount?) -> Unit = { onAction(ContactsListAction.AccountSelected(it)) }
    val onClearFilters: () -> Unit = {
        clearAllFilters(
            showFavoritesOnly = uiState.showFavoritesOnly,
            onSearchQueryChange = onSearchQueryChange,
            onFavoritesOnlyChange = onFavoritesOnlyChange,
            onGroupSelected = onGroupSelected,
            onAccountSelected = onAccountSelected,
        )
    }
    val onCreateGroup: (String) -> Unit = { onAction(ContactsListAction.CreateGroup(it)) }
    val onManageGroupsClick: () -> Unit = { onAction(ContactsNavigationAction.ManageGroupsClicked) }
    val onAddSelectedToGroup: (Set<Long>) -> Unit = { onAction(ContactsListAction.AddSelectedToGroup(it)) }
    val showFavoriteQuickAction = rememberShowFavoriteQuickAction()
    val onDuplicatesClick: () -> Unit = { onAction(ContactsNavigationAction.DuplicatesClicked) }
    val onRecentsClick: () -> Unit = { onAction(ContactsNavigationAction.RecentsClicked) }
    val onSettingsClick: () -> Unit = { onAction(ContactsNavigationAction.SettingsClicked) }
    val onToggleFavorite: (Long) -> Unit = { contactId ->
        performHapticFeedback(ctx, HapticImpact.Light)
        onAction(ContactsListAction.ToggleFavorite(contactId))
    }
    val onToggleFavoriteSelected: () -> Unit = {
        performHapticFeedback(ctx, HapticImpact.Light)
        onAction(ContactsListAction.ToggleFavoriteSelected)
    }
    val onProfileClick: (Long) -> Unit = { onAction(ContactsNavigationAction.ProfileClicked(it)) }

    var showDeleteDialog by remember { mutableStateOf(false) }
    var showGroupPicker by remember { mutableStateOf(false) }
    var pendingGroupIds by remember { mutableStateOf(emptySet<Long>()) }
    var pendingExportUri by remember { mutableStateOf<Uri?>(null) }
    var exportInProgress by remember { mutableStateOf(false) }
    var exportSummary by remember { mutableStateOf<ExportSummary?>(null) }
    var pendingQuickCall by remember { mutableStateOf<QuickCallTarget?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val requestCall = rememberCallHandler()

    val vCardLaunchers =
        rememberVCardLaunchers(
            onImportText = onImportText,
            onExportSelected = { uri ->
                pendingExportUri = uri
                onExportRequested()
            },
            onExportSelectionSelected = { uri ->
                pendingExportUri = uri
                onExportSelectionRequested()
            },
        )

    val event = uiState.event
    LaunchedEffect(event) {
        if (event != null) {
            val exportUri = pendingExportUri
            pendingExportUri = null
            exportInProgress = event is ContactsEvent.ExportReady
            val summary = handleContactsEvent(ctx, event, exportUri, onEventHandled)
            exportInProgress = false
            if (summary != null) exportSummary = summary
        }
    }

    // Выход из режима выделения закрывает пикер групп: контекст действия пропал.
    LaunchedEffect(uiState.isSelectionMode) {
        if (!uiState.isSelectionMode) showGroupPicker = false
    }

    // Назад из поиска закрывает поиск, а не экран; выделение важнее, если они совпали.
    BackHandler(enabled = uiState.isSearchMode && !uiState.isSelectionMode) {
        onSearchClosed()
    }

    DeleteUndoEffect(
        snackbarHostState = snackbarHostState,
        pendingDeleteIds = uiState.pendingDeleteIds,
        deleteClosing = uiState.deleteClosing,
        onUndoDelete = onUndoDelete,
        onWindowClosed = onDeleteWindowClosed,
    )

    val visibleIds = (uiState.content as? Loadable.Content)?.data?.map { it.contactId }.orEmpty()
    val allSelected = visibleIds.isNotEmpty() && uiState.selection.containsAll(visibleIds)
    val allFavorite =
        uiState.selection.isNotEmpty() && uiState.selection.all { it in uiState.favorites }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.testTag("undo_snackbar"),
            )
        },
        topBar = {
            ContactListTopBar(
                uiState = uiState,
                allSelected = allSelected,
                allFavorite = allFavorite,
                visibleIds = visibleIds,
                profileContactId = profileContactId,
                onClearSelection = onClearSelection,
                onSelectAll = onSelectAll,
                onToggleFavoriteSelected = onToggleFavoriteSelected,
                onAddToGroup = {
                    pendingGroupIds = emptySet()
                    showGroupPicker = true
                },
                onShare = onShareSelected,
                onExportSelection = { vCardLaunchers.startExportSelection() },
                onDelete = { showDeleteDialog = true },
                onSettingsClick = onSettingsClick,
                onRecentsClick = onRecentsClick,
                onDuplicatesClick = onDuplicatesClick,
                onGroupsClick = onManageGroupsClick,
                onImportClick = { vCardLaunchers.startImport(ctx) },
                onExportClick = { vCardLaunchers.startExport() },
                onProfileClick = onProfileClick,
            )
        },
        floatingActionButton = {
            if (!uiState.isSelectionMode) {
                FloatingActionButton(
                    onClick = onAddClick,
                    modifier = Modifier.testTag("add_contact_fab"),
                ) {
                    Icon(
                        Icons.Filled.Add,
                        contentDescription = stringResource(R.string.editor_title_new),
                    )
                }
            }
        },
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
        ) {
            ExportStatusBanner(
                inProgress = exportInProgress,
                summary = exportSummary,
                onDismiss = { exportSummary = null },
            )
            if (!uiState.isSelectionMode) {
                FilterHeader(
                    searchQuery = uiState.searchQuery,
                    isSearchMode = uiState.isSearchMode,
                    showFavoritesOnly = uiState.showFavoritesOnly,
                    onSearchQueryChange = onSearchQueryChange,
                    onSearchExpandedChange = { onSearchOpened() },
                    onSearchClose = onSearchClosed,
                    onFavoritesOnlyChange = onFavoritesOnlyChange,
                    groups = groups,
                    selectedGroupId = uiState.selectedGroupId,
                    onGroupSelected = onGroupSelected,
                    accounts = accounts,
                    selectedAccount = uiState.selectedAccount,
                    onAccountSelected = onAccountSelected,
                    accountSyncInfos = accountSyncInfos,
                    onRetrySync = onRetrySync,
                    onCreateGroup = onCreateGroup,
                    onManageGroupsClick = onManageGroupsClick,
                )
            }
            ContactListBody(
                uiState = uiState,
                onContactClick = { contactId ->
                    if (uiState.isSelectionMode) {
                        onContactSelected(contactId)
                    } else {
                        onContactClick(contactId)
                    }
                },
                onContactLongClick = onContactSelected,
                onRetry = onRetry,
                onRefresh = onRefresh,
                onCallContact = { contact -> pendingQuickCall = contact.toQuickCallTarget() },
                onSmsContact = { contact ->
                    contact.primaryPhone?.let { openSms(ctx, it.number) }
                },
                onToggleFavorite = onToggleFavorite,
                onCreateContact = onCreateContact,
                onImportClick = onImportClick ?: { vCardLaunchers.startImport(ctx) },
                onClearSearch = onClearSearch,
                onClearFilters = onClearFilters,
                showFavoriteQuickAction = showFavoriteQuickAction,
            )
        }
    }

    ContactListDialogs(
        showDeleteDialog = showDeleteDialog,
        selectionSize = uiState.selection.size,
        onDismissDelete = { showDeleteDialog = false },
        onConfirmDelete = {
            showDeleteDialog = false
            onDeleteSelected()
        },
        pendingQuickCall = pendingQuickCall,
        onDismissQuickCall = { pendingQuickCall = null },
        onConfirmQuickCall = { phone ->
            pendingQuickCall = null
            requestCall(phone.number)
        },
        showGroupPicker = showGroupPicker,
        groups = groups,
        pendingGroupIds = pendingGroupIds,
        onToggleGroup = { groupId -> pendingGroupIds = pendingGroupIds.toggle(groupId) },
        onCreateGroup = onCreateGroup,
        onConfirmGroups = {
            showGroupPicker = false
            onAddSelectedToGroup(pendingGroupIds)
        },
        onDismissGroups = { showGroupPicker = false },
    )
}

/** Верхняя панель списка: выделение, обычный режим или поиск без панели. */
@Composable
private fun ContactListTopBar(
    uiState: ContactsUiState,
    allSelected: Boolean,
    allFavorite: Boolean,
    visibleIds: List<Long>,
    profileContactId: Long?,
    onClearSelection: () -> Unit,
    onSelectAll: (List<Long>) -> Unit,
    onToggleFavoriteSelected: () -> Unit,
    onAddToGroup: () -> Unit,
    onShare: () -> Unit,
    onExportSelection: () -> Unit,
    onDelete: () -> Unit,
    onSettingsClick: () -> Unit,
    onRecentsClick: () -> Unit,
    onDuplicatesClick: () -> Unit,
    onGroupsClick: () -> Unit,
    onImportClick: () -> Unit,
    onExportClick: () -> Unit,
    onProfileClick: (Long) -> Unit,
) {
    when {
        uiState.isSelectionMode ->
            SelectionTopBar(
                count = uiState.selection.size,
                allSelected = allSelected,
                allFavorite = allFavorite,
                onClose = onClearSelection,
                onSelectAll = { onSelectAll(visibleIds) },
                onToggleFavorite = onToggleFavoriteSelected,
                onAddToGroup = onAddToGroup,
                onShare = onShare,
                onExport = onExportSelection,
                onDelete = onDelete,
            )
        !uiState.isSearchMode ->
            HomeTopBar(
                onSettingsClick = onSettingsClick,
                onRecentsClick = onRecentsClick,
                onDuplicatesClick = onDuplicatesClick,
                onGroupsClick = onGroupsClick,
                onImportClick = onImportClick,
                onExportClick = onExportClick,
                onSelectAllClick = { onSelectAll(visibleIds) },
                profileContactId = profileContactId,
                onProfileClick = { profileContactId?.let(onProfileClick) },
            )
    }
}

@Composable
private fun ContactListDialogs(
    showDeleteDialog: Boolean,
    selectionSize: Int,
    onDismissDelete: () -> Unit,
    onConfirmDelete: () -> Unit,
    pendingQuickCall: QuickCallTarget?,
    onDismissQuickCall: () -> Unit,
    onConfirmQuickCall: (PhoneNumber) -> Unit,
    showGroupPicker: Boolean,
    groups: List<ContactGroup>,
    pendingGroupIds: Set<Long>,
    onToggleGroup: (Long) -> Unit,
    onCreateGroup: (String) -> Unit,
    onConfirmGroups: () -> Unit,
    onDismissGroups: () -> Unit,
) {
    if (showDeleteDialog) {
        ConfirmDialog(
            title =
                pluralStringResource(
                    R.plurals.batch_delete_title,
                    selectionSize,
                    selectionSize,
                ),
            text = stringResource(R.string.editor_delete_message),
            confirmLabel = stringResource(R.string.action_delete),
            onConfirm = onConfirmDelete,
            onDismiss = onDismissDelete,
            danger = true,
            confirmTestTag = "confirm_batch_delete",
        )
    }

    pendingQuickCall?.let { target ->
        ConfirmCallDialog(
            contactName = target.name,
            phoneText = phoneText(target.phone),
            onConfirm = { onConfirmQuickCall(target.phone) },
            onDismiss = onDismissQuickCall,
        )
    }

    if (showGroupPicker) {
        GroupPickerSheet(
            title = stringResource(R.string.action_add_to_group),
            groups = groups,
            selectedIds = pendingGroupIds,
            confirmLabel = stringResource(R.string.action_add),
            onToggle = onToggleGroup,
            onCreateGroup = onCreateGroup,
            onConfirm = onConfirmGroups,
            onDismiss = onDismissGroups,
        )
    }
}

private fun Set<Long>.toggle(groupId: Long): Set<Long> = if (groupId in this) this - groupId else this + groupId

/** Сбрасывает все фильтры списка одним действием; избранное выключается только если включено. */
private fun clearAllFilters(
    showFavoritesOnly: Boolean,
    onSearchQueryChange: (String) -> Unit,
    onFavoritesOnlyChange: () -> Unit,
    onGroupSelected: (Long?) -> Unit,
    onAccountSelected: (ContactAccount?) -> Unit,
) {
    onSearchQueryChange("")
    if (showFavoritesOnly) onFavoritesOnlyChange()
    onGroupSelected(null)
    onAccountSelected(null)
}

private fun ContactDetails.toQuickCallTarget(): QuickCallTarget? =
    primaryPhone?.let { QuickCallTarget(name = name, phone = it) }
