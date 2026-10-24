package com.m4isper.contactsapp.presentation.editor

import android.Manifest
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
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
    sessionId: Long = 0L,
    onDone: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ContactEditorViewModel = metroViewModel(),
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

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
