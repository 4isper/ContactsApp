package com.m4isper.contactsapp.presentation.contacts

import android.Manifest
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.m4isper.contactsapp.presentation.common.permissions.PermissionRequest
import com.m4isper.contactsapp.presentation.common.permissions.hasPermission
import com.m4isper.contactsapp.presentation.common.permissions.openAppSettings
import com.m4isper.contactsapp.presentation.import.ImportPreviewViewModel
import com.m4isper.contactsapp.presentation.widget.ContactsWidgetProvider
import dev.zacsweers.metrox.viewmodel.metroViewModel
import kotlinx.coroutines.launch

@Composable
internal fun ContactsScreen(
    onContactClick: (Long) -> Unit,
    onAddClick: () -> Unit,
    onGroupsClick: () -> Unit,
    onDuplicatesClick: () -> Unit,
    onRecentsClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onImportPreviewReady: () -> Unit,
    onProfileClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
    showFavorites: Boolean = false,
    viewModel: ContactsViewModel = metroViewModel(),
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(showFavorites) {
        // Ярлык «Избранное» сразу включает фильтр избранного.
        if (showFavorites) viewModel.filters.onShowFavoritesOnly()
    }
    val importViewModel: ImportPreviewViewModel = metroViewModel()

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
        viewModel.selection.clear()
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
            onSearchQueryChange = viewModel.filters::onSearchQueryChange,
            onFavoritesOnlyChange = viewModel.filters::onToggleFavoritesOnly,
            onContactClick = onContactClick,
            onAddClick = onAddClick,
            onRetry = viewModel::refresh,
            onRefresh = viewModel::refresh,
            onContactSelected = viewModel.selection::onContactSelected,
            onClearSelection = viewModel.selection::clear,
            onSelectAll = viewModel.selection::selectAll,
            onDeleteSelected = viewModel::onDeleteSelected,
            onUndoDelete = viewModel::onUndoDelete,
            onDeleteWindowClosed = viewModel::onDeleteWindowClosed,
            onShareSelected = { scope.launch { viewModel.onShareSelected() } },
            onImportText = { text ->
                importViewModel.prepare(text)
                onImportPreviewReady()
            },
            onExportRequested = viewModel.files::onExportRequested,
            onExportSelectionRequested = viewModel::onExportSelection,
            onEventHandled = viewModel::onEventHandled,
            groups = uiState.groups,
            onGroupSelected = viewModel.filters::onGroupSelected,
            accounts = uiState.accounts,
            onAccountSelected = viewModel.filters::onAccountSelected,
            onCreateGroup = { title -> scope.launch { viewModel.groups.onCreateGroup(title) } },
            onManageGroupsClick = onGroupsClick,
            onAddSelectedToGroup = { groupIds -> scope.launch { viewModel.groups.onAddSelectedToGroup(groupIds) } },
            onDuplicatesClick = onDuplicatesClick,
            onRecentsClick = onRecentsClick,
            onSettingsClick = onSettingsClick,
            onToggleFavorite = { contactId ->
                scope.launch { viewModel.favorites.onToggleFavorite(contactId) }
                ContactsWidgetProvider.refresh(context)
            },
            onToggleFavoriteSelected = {
                scope.launch { viewModel.onToggleSelectedFavorite() }
                ContactsWidgetProvider.refresh(context)
            },
            profileContactId = uiState.profileContactId,
            onProfileClick = { contactId -> onProfileClick(contactId) },
            modifier = modifier,
        )
    }
}
