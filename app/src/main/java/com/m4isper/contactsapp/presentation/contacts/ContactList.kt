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
import com.m4isper.contactsapp.domain.model.ContactGroup
import com.m4isper.contactsapp.presentation.common.Loadable
import com.m4isper.contactsapp.presentation.common.groups.GroupPickerDialog
import com.m4isper.contactsapp.ui.components.ConfirmDialog

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
    onCreateGroup: (String) -> Unit = {},
    onManageGroupsClick: () -> Unit = {},
    onAddSelectedToGroup: (Set<Long>) -> Unit = {},
    onDuplicatesClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
) {
    val context = LocalContext.current
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showGroupPicker by remember { mutableStateOf(false) }
    var pendingGroupIds by remember { mutableStateOf(emptySet<Long>()) }
    var pendingExportUri by remember { mutableStateOf<Uri?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

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
                    onClose = onClearSelection,
                    onSelectAll = { onSelectAll(visibleIds) },
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
                    onCreateGroup = onCreateGroup,
                    onManageGroupsClick = onManageGroupsClick,
                    onDuplicatesClick = onDuplicatesClick,
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
            )
        }
    }

    if (showDeleteDialog) {
        ConfirmDialog(
            title =
                pluralStringResource(
                    R.plurals.batch_delete_title,
                    uiState.selection.size,
                    uiState.selection.size,
                ),
            text = stringResource(R.string.editor_delete_message),
            confirmLabel = stringResource(R.string.action_delete),
            onConfirm = {
                showDeleteDialog = false
                onDeleteSelected()
            },
            onDismiss = { showDeleteDialog = false },
            danger = true,
            confirmTestTag = "confirm_batch_delete",
        )
    }

    if (showGroupPicker) {
        GroupPickerDialog(
            title = stringResource(R.string.action_add_to_group),
            groups = groups,
            selectedIds = pendingGroupIds,
            confirmLabel = stringResource(R.string.action_add),
            onToggle = { groupId ->
                pendingGroupIds =
                    if (groupId in pendingGroupIds) pendingGroupIds - groupId else pendingGroupIds + groupId
            },
            onCreateGroup = onCreateGroup,
            onConfirm = {
                showGroupPicker = false
                onAddSelectedToGroup(pendingGroupIds)
            },
            onDismiss = { showGroupPicker = false },
        )
    }
}
