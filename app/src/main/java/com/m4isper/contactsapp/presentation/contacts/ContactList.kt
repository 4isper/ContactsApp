package com.m4isper.contactsapp.presentation.contacts

import android.net.Uri
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
import com.m4isper.contactsapp.domain.model.ContactAccount
import com.m4isper.contactsapp.domain.model.ContactDetails
import com.m4isper.contactsapp.domain.model.ContactGroup
import com.m4isper.contactsapp.domain.model.PhoneNumber
import com.m4isper.contactsapp.presentation.common.Loadable
import com.m4isper.contactsapp.presentation.common.call.ConfirmCallDialog
import com.m4isper.contactsapp.presentation.common.call.rememberCallHandler
import com.m4isper.contactsapp.presentation.common.format.phoneText
import com.m4isper.contactsapp.presentation.common.groups.GroupPickerDialog
import com.m4isper.contactsapp.presentation.common.share.openSms
import com.m4isper.contactsapp.ui.components.ConfirmDialog

private data class QuickCallTarget(
    val name: String,
    val phone: PhoneNumber,
)

@Composable
internal fun ContactList(
    uiState: ContactsUiState,
    onSearchQueryChange: (String) -> Unit,
    onFavoritesOnlyChange: () -> Unit,
    onContactClick: (Long) -> Unit,
    onAddClick: () -> Unit,
    onRetry: () -> Unit,
    onRefresh: () -> Unit,
    onContactSelected: (Long) -> Unit,
    onClearSelection: () -> Unit,
    onSelectAll: (List<Long>) -> Unit,
    onDeleteSelected: () -> Unit,
    onUndoDelete: () -> Unit,
    onDeleteWindowClosed: () -> Unit,
    onShareSelected: () -> Unit,
    onImportText: (String) -> Unit,
    onExportRequested: () -> Unit,
    onEventHandled: () -> Unit,
    modifier: Modifier = Modifier,
    groups: List<ContactGroup> = emptyList(),
    onGroupSelected: (Long?) -> Unit = {},
    accounts: List<ContactAccount> = emptyList(),
    onAccountSelected: (ContactAccount?) -> Unit = {},
    onCreateGroup: (String) -> Unit = {},
    onManageGroupsClick: () -> Unit = {},
    onAddSelectedToGroup: (Set<Long>) -> Unit = {},
    onDuplicatesClick: () -> Unit = {},
    onRecentsClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onToggleFavorite: (Long) -> Unit = {},
    onToggleFavoriteSelected: () -> Unit = {},
) {
    val context = LocalContext.current
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showGroupPicker by remember { mutableStateOf(false) }
    var pendingGroupIds by remember { mutableStateOf(emptySet<Long>()) }
    var pendingExportUri by remember { mutableStateOf<Uri?>(null) }
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
        )

    val event = uiState.event
    LaunchedEffect(event) {
        if (event != null) {
            val exportUri = pendingExportUri
            pendingExportUri = null
            handleContactsEvent(context, event, exportUri, onEventHandled)
        }
    }

    // Выход из режима выделения закрывает пикер групп: контекст действия пропал.
    LaunchedEffect(uiState.isSelectionMode) {
        if (!uiState.isSelectionMode) showGroupPicker = false
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
            if (uiState.isSelectionMode) {
                SelectionTopBar(
                    count = uiState.selection.size,
                    allSelected = allSelected,
                    allFavorite = allFavorite,
                    onClose = onClearSelection,
                    onSelectAll = { onSelectAll(visibleIds) },
                    onToggleFavorite = onToggleFavoriteSelected,
                    onAddToGroup = {
                        pendingGroupIds = emptySet()
                        showGroupPicker = true
                    },
                    onShare = onShareSelected,
                    onDelete = { showDeleteDialog = true },
                )
            }
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
            if (!uiState.isSelectionMode) {
                FilterHeader(
                    searchQuery = uiState.searchQuery,
                    showFavoritesOnly = uiState.showFavoritesOnly,
                    onSearchQueryChange = onSearchQueryChange,
                    onFavoritesOnlyChange = onFavoritesOnlyChange,
                    onImportClick = { vCardLaunchers.startImport(context) },
                    onExportClick = { vCardLaunchers.startExport() },
                    groups = groups,
                    selectedGroupId = uiState.selectedGroupId,
                    onGroupSelected = onGroupSelected,
                    accounts = accounts,
                    selectedAccount = uiState.selectedAccount,
                    onAccountSelected = onAccountSelected,
                    onCreateGroup = onCreateGroup,
                    onManageGroupsClick = onManageGroupsClick,
                    onDuplicatesClick = onDuplicatesClick,
                    onRecentsClick = onRecentsClick,
                    onSettingsClick = onSettingsClick,
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
                    contact.primaryPhone?.let { openSms(context, it.number) }
                },
                onToggleFavorite = onToggleFavorite,
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
        GroupPickerDialog(
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

private fun ContactDetails.toQuickCallTarget(): QuickCallTarget? =
    primaryPhone?.let { QuickCallTarget(name = name, phone = it) }
