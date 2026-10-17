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
import com.m4isper.contactsapp.presentation.common.PermissionRequest
import com.m4isper.contactsapp.presentation.common.hasPermission
import com.m4isper.contactsapp.presentation.common.openAppSettings
import dev.zacsweers.metrox.viewmodel.metroViewModel

@Composable
fun ContactsScreen(
    onContactClick: (Long) -> Unit,
    onAddClick: () -> Unit,
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
            onShareSelected = viewModel::onShareSelected,
            onImportText = viewModel.files::onImportText,
            onExportRequested = viewModel.files::onExportRequested,
            onEventHandled = viewModel::onEventHandled,
            modifier = modifier,
        )
    }
}
