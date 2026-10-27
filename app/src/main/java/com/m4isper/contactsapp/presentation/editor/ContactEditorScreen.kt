package com.m4isper.contactsapp.presentation.editor

import android.Manifest
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
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
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.m4isper.contactsapp.R
import com.m4isper.contactsapp.presentation.common.messageRes
import com.m4isper.contactsapp.presentation.common.permissions.PermissionRequest
import com.m4isper.contactsapp.presentation.common.permissions.hasPermission
import com.m4isper.contactsapp.presentation.common.permissions.openAppSettings
import com.m4isper.contactsapp.presentation.editor.components.EditorTopBar
import com.m4isper.contactsapp.ui.components.ConfirmDialog
import com.m4isper.contactsapp.ui.components.HapticImpact
import com.m4isper.contactsapp.ui.components.performHapticFeedback
import dev.zacsweers.metrox.viewmodel.metroViewModel

@Composable
fun ContactEditorScreen(
    contactId: Long?,
    onDone: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    sessionId: Long = 0L,
    viewModel: ContactEditorViewModel = metroViewModel(),
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val errorText = uiState.error?.let { stringResource(it.messageRes()) }
    val checkFieldsMessage = stringResource(R.string.editor_check_fields)

    var hasWritePermission by remember {
        mutableStateOf(hasPermission(context, Manifest.permission.WRITE_CONTACTS))
    }
    var showDiscardDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val writePermissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
        ) { isGranted ->
            hasWritePermission = isGranted
        }

    LaunchedEffect(contactId, sessionId, hasWritePermission) {
        if (hasWritePermission) {
            viewModel.load(contactId, sessionId)
        }
    }

    LaunchedEffect(uiState.closeScreen) {
        if (uiState.closeScreen) onDone()
    }

    LifecycleResumeEffect(Unit) {
        hasWritePermission = hasPermission(context, Manifest.permission.WRITE_CONTACTS)
        onPauseOrDispose { /* no-op */ }
    }

    fun tryBack() {
        if (viewModel.isDirty()) {
            showDiscardDialog = true
        } else {
            onBack()
        }
    }

    // Системный и предиктивный back тоже проходят через guard несохранённых изменений.
    BackHandler(enabled = hasWritePermission) {
        tryBack()
    }

    if (!hasWritePermission) {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            topBar = {
                EditorTopBar(
                    title = stringResource(R.string.app_name),
                    saveEnabled = false,
                    onBack = onBack,
                    onSave = {},
                    showSave = false,
                )
            },
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        ) { innerPadding ->
            PermissionRequest(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                rationale = stringResource(R.string.permission_write_rationale),
                onGrantClick = {
                    writePermissionLauncher.launch(Manifest.permission.WRITE_CONTACTS)
                },
                onSettingsClick = { openAppSettings(context) },
            )
        }
        return
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            EditorTopBar(
                title =
                    if (uiState.isNew) {
                        stringResource(R.string.editor_title_new)
                    } else {
                        uiState.effectiveName.ifBlank { stringResource(R.string.editor_title_new) }
                    },
                saveEnabled = !uiState.isBusy,
                onBack = ::tryBack,
                onSave = {
                    performHapticFeedback(context, HapticImpact.Medium)
                    viewModel.onSave()
                },
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { innerPadding ->
        EditorForm(
            viewModel = viewModel,
            onDeleteClick = { showDeleteDialog = true },
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
        )
    }

    LaunchedEffect(errorText) {
        if (errorText != null) {
            snackbarHostState.showSnackbar(
                message = errorText,
                duration = SnackbarDuration.Long,
            )
        }
    }

    // Блокированное сохранение: короткая подсказка, даже если невалидное поле за экраном.
    val fieldsBlocked = uiState.firstErrorRowIds.isNotEmpty()
    LaunchedEffect(fieldsBlocked) {
        if (fieldsBlocked) {
            snackbarHostState.showSnackbar(
                message = checkFieldsMessage,
                duration = SnackbarDuration.Long,
            )
        }
    }

    if (showDiscardDialog) {
        ConfirmDialog(
            title = stringResource(R.string.editor_discard_title),
            text = stringResource(R.string.editor_discard_message),
            confirmLabel = stringResource(R.string.action_discard),
            onConfirm = {
                showDiscardDialog = false
                onBack()
            },
            onDismiss = { showDiscardDialog = false },
        )
    }

    if (showDeleteDialog) {
        ConfirmDialog(
            title = stringResource(R.string.editor_delete_title),
            text = stringResource(R.string.editor_delete_message),
            confirmLabel = stringResource(R.string.action_delete),
            onConfirm = {
                showDeleteDialog = false
                performHapticFeedback(context, HapticImpact.Medium)
                viewModel.onDelete()
            },
            onDismiss = { showDeleteDialog = false },
            danger = true,
            confirmTestTag = "confirm_delete_button",
        )
    }
}
