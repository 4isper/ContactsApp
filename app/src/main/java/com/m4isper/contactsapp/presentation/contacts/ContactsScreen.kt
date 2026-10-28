package com.m4isper.contactsapp.presentation.contacts

import android.Manifest
import android.content.Context
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
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.m4isper.contactsapp.R
import com.m4isper.contactsapp.presentation.common.permissions.PermissionRequest
import com.m4isper.contactsapp.presentation.common.permissions.hasPermission
import com.m4isper.contactsapp.presentation.common.permissions.openAppSettings
import com.m4isper.contactsapp.presentation.common.permissions.openSettingsIfPermanentlyDenied
import com.m4isper.contactsapp.presentation.contacts.components.ContactList
import com.m4isper.contactsapp.presentation.contacts.effects.syncContactsPermission
import com.m4isper.contactsapp.presentation.import.ImportPreviewViewModel
import com.m4isper.contactsapp.presentation.widget.ContactsWidgetProvider
import dev.zacsweers.metrox.viewmodel.metroViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Suppress("CyclomaticComplexMethod")
@Composable
internal fun ContactsScreen(
    onNavigation: (ContactsNavigationAction) -> Unit,
    onImportPreviewReady: () -> Unit,
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
    var hasWritePermission by remember {
        mutableStateOf(hasPermission(context, Manifest.permission.WRITE_CONTACTS))
    }
    var pendingWriteAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    val writePermissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
        ) { isGranted ->
            hasWritePermission = isGranted
            if (isGranted) {
                pendingWriteAction?.invoke()
                pendingWriteAction = null
            } else {
                openSettingsIfPermanentlyDenied(context, Manifest.permission.WRITE_CONTACTS)
            }
        }

    val readPermissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
        ) { isGranted ->
            hasReadPermission = isGranted
            if (isGranted) {
                viewModel.refresh()
                if (!hasPermission(context, Manifest.permission.WRITE_CONTACTS)) {
                    writePermissionLauncher.launch(Manifest.permission.WRITE_CONTACTS)
                }
            } else {
                // Постоянный отказ: диалог больше не покажется — ведём в настройки.
                openSettingsIfPermanentlyDenied(context, Manifest.permission.READ_CONTACTS)
            }
        }

    LaunchedEffect(hasWritePermission) {
        if (hasWritePermission) {
            pendingWriteAction?.invoke()
            pendingWriteAction = null
        }
    }

    LifecycleResumeEffect(context) {
        syncContactsPermission(context, viewModel) { hasReadPermission = it }
        hasWritePermission = hasPermission(context, Manifest.permission.WRITE_CONTACTS)
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

    // Переход в режим выделения и выход из него озвучиваются для TalkBack:
    // announceForAccessibility доступен на всех поддерживаемых API через View.
    val accessibilityView = LocalView.current
    LaunchedEffect(uiState.isSelectionMode) {
        val message =
            if (uiState.isSelectionMode) {
                val count = uiState.selection.size
                accessibilityView.resources.getQuantityString(
                    R.plurals.selection_mode_announce,
                    count,
                    count,
                )
            } else {
                accessibilityView.resources.getString(R.string.selection_mode_exited)
            }
        accessibilityView.announceForAccessibility(message)
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
        val actions =
            ContactsActionScope(
                viewModel = viewModel,
                scope = scope,
                context = context,
                importViewModel = importViewModel,
                onImportPreviewReady = onImportPreviewReady,
                onNavigation = onNavigation,
            )
        val handleAction = { action: ContactsUiAction ->
            when (action) {
                is ContactsListAction.ToggleFavorite ->
                    if (hasWritePermission) {
                        actions.handle(action)
                    } else {
                        pendingWriteAction = { actions.handle(action) }
                        writePermissionLauncher.launch(Manifest.permission.WRITE_CONTACTS)
                    }

                ContactsListAction.ToggleFavoriteSelected ->
                    if (hasWritePermission) {
                        actions.handle(action)
                    } else {
                        pendingWriteAction = { actions.handle(action) }
                        writePermissionLauncher.launch(Manifest.permission.WRITE_CONTACTS)
                    }

                ContactsFileAction.DeleteSelected ->
                    if (hasWritePermission) {
                        actions.handle(action)
                    } else {
                        pendingWriteAction = { actions.handle(action) }
                        writePermissionLauncher.launch(Manifest.permission.WRITE_CONTACTS)
                    }

                else -> actions.handle(action)
            }
        }
        val onCreateContact = { handleAction(ContactsNavigationAction.AddClicked) }
        ContactList(
            uiState = uiState,
            onAction = handleAction,
            groups = uiState.groups,
            accounts = uiState.accounts,
            profileContactId = uiState.profileContactId,
            accountSyncInfos = uiState.accountSyncInfos,
            onCreateContact = onCreateContact,
        )
    }
}

/** Всё, что нужно экрану для разбора действия списка. */
private class ContactsActionScope(
    val viewModel: ContactsViewModel,
    val scope: CoroutineScope,
    val context: Context,
    val importViewModel: ImportPreviewViewModel,
    val onImportPreviewReady: () -> Unit,
    val onNavigation: (ContactsNavigationAction) -> Unit,
)

/** Разбор действия экрана: сценарии состояния, файлы и переходы. */
private fun ContactsActionScope.handle(action: ContactsUiAction) {
    when (action) {
        is ContactsListAction -> handleState(action)
        is ContactsFileAction -> handleFile(action)
        is ContactsNavigationAction -> onNavigation(action)
    }
}

/** Фильтры, выделение, избранное и группы. */
private fun ContactsActionScope.handleState(action: ContactsListAction) {
    when (action) {
        is ContactsListAction.SearchQueryChanged -> viewModel.filters.onSearchQueryChange(action.query)
        ContactsListAction.FavoritesOnlyToggled -> viewModel.filters.onToggleFavoritesOnly()
        is ContactsListAction.ContactSelected -> viewModel.selection.onContactSelected(action.contactId)
        ContactsListAction.ClearSelection -> viewModel.selection.clear()
        is ContactsListAction.SelectAll -> viewModel.selection.selectAll(action.contactIds)
        is ContactsListAction.GroupSelected -> viewModel.filters.onGroupSelected(action.groupId)
        is ContactsListAction.AccountSelected -> viewModel.filters.onAccountSelected(action.account)
        is ContactsListAction.CreateGroup -> scope.launch { viewModel.groups.onCreateGroup(action.title) }
        is ContactsListAction.AddSelectedToGroup ->
            scope.launch { viewModel.groups.onAddSelectedToGroup(action.groupIds) }
        is ContactsListAction.ToggleFavorite -> toggleFavorite(action.contactId)
        ContactsListAction.ToggleFavoriteSelected -> toggleFavoriteSelected()
    }
}

/** Файлы, удаление и обновление списка. */
private fun ContactsActionScope.handleFile(action: ContactsFileAction) {
    when (action) {
        ContactsFileAction.Retry -> viewModel.refresh()
        ContactsFileAction.Refresh -> viewModel.refresh()
        ContactsFileAction.RetrySync -> viewModel.refresh(reportSyncResult = true)
        ContactsFileAction.DeleteSelected -> viewModel.onDeleteSelected()
        ContactsFileAction.UndoDelete -> viewModel.onUndoDelete()
        ContactsFileAction.DeleteWindowClosed -> viewModel.onDeleteWindowClosed()
        ContactsFileAction.ShareSelected -> scope.launch { viewModel.onShareSelected() }
        is ContactsFileAction.ImportText -> {
            importViewModel.prepare(action.text)
            onImportPreviewReady()
        }
        ContactsFileAction.ExportRequested -> viewModel.files.onExportRequested()
        ContactsFileAction.ExportSelectionRequested -> viewModel.onExportSelection()
        ContactsFileAction.EventHandled -> viewModel.onEventHandled()
    }
}

/** Звезда в строке списка: виджет показывает избранное, его тоже обновляем. */
private fun ContactsActionScope.toggleFavorite(contactId: Long) {
    scope.launch { viewModel.favorites.onToggleFavorite(contactId) }
    ContactsWidgetProvider.refresh(context)
}

/** Пакетное избранное: виджет показывает избранное, его тоже обновляем. */
private fun ContactsActionScope.toggleFavoriteSelected() {
    scope.launch { viewModel.onToggleSelectedFavorite() }
    ContactsWidgetProvider.refresh(context)
}
