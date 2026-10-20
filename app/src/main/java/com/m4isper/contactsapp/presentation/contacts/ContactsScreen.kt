package com.m4isper.contactsapp.presentation.contacts

import android.Manifest
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.m4isper.contactsapp.presentation.common.permissions.PermissionRequest
import com.m4isper.contactsapp.presentation.common.permissions.hasPermission
import com.m4isper.contactsapp.presentation.common.permissions.openAppSettings
import dev.zacsweers.metrox.viewmodel.metroViewModel

@Composable
fun ContactsScreen(
    onContactClick: (Long) -> Unit,
    onAddClick: () -> Unit,
    onGroupsClick: () -> Unit,
    onDuplicatesClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ContactsViewModel = metroViewModel(),
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var hasReadPermission by remember {
        mutableStateOf(hasPermission(context, Manifest.permission.READ_CONTACTS))
    }

    val readPermissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
        ) { isGranted ->
            hasReadPermission = isGranted
            if (isGranted) {
                viewModel.refresh()
            }
        }

    LifecycleResumeEffect(context) {
        syncContactsPermission(context, viewModel) { hasReadPermission = it }
        onPauseOrDispose { /* no-op */ }
    }

    LifecycleResumeEffect(hasReadPermission) {
        if (hasReadPermission) {
            viewModel.startObservingChanges()
        } else {
            viewModel.stopObservingChanges()
        }
        onPauseOrDispose { viewModel.stopObservingChanges() }
    }

    BackHandler(enabled = uiState.isSelectionMode) {
        viewModel.onClearSelection()
    }

    if (!hasReadPermission) {
        PermissionRequest(
            modifier = modifier,
            onGrantClick = {
                readPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
            },
            onSettingsClick = { openAppSettings(context) },
        )
    } else {
        ContactList(
            uiState = uiState,
            onSearchQueryChange = viewModel::onSearchQueryChange,
            onFavoritesOnlyChange = viewModel::onToggleFavoritesOnly,
            onContactClick = onContactClick,
            onAddClick = onAddClick,
            onRetry = viewModel::refresh,
            onRefresh = viewModel::refresh,
            onContactSelected = viewModel::onContactSelected,
            onClearSelection = viewModel::onClearSelection,
            onSelectAll = viewModel::onSelectAll,
            onDeleteSelected = viewModel::onDeleteSelected,
            onUndoDelete = viewModel::onUndoDelete,
            onDeleteWindowClosed = viewModel::onDeleteWindowClosed,
            onShareSelected = viewModel::onShareSelected,
            onImportText = viewModel::onImportText,
            onExportRequested = viewModel::onExportRequested,
            onEventHandled = viewModel::onEventHandled,
            groups = uiState.groups,
            onGroupSelected = viewModel::onGroupSelected,
            onCreateGroup = viewModel::onCreateGroup,
            onManageGroupsClick = onGroupsClick,
            onAddSelectedToGroup = viewModel::onAddSelectedToGroup,
            onDuplicatesClick = onDuplicatesClick,
            onSettingsClick = onSettingsClick,
            modifier = modifier,
        )
    }
}
