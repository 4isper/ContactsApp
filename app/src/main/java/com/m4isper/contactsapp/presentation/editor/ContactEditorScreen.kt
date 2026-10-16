package com.m4isper.contactsapp.presentation.editor

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.m4isper.contactsapp.R
import com.m4isper.contactsapp.domain.model.FormError
import com.m4isper.contactsapp.domain.model.PhoneKind
import com.m4isper.contactsapp.presentation.common.PermissionRequest
import com.m4isper.contactsapp.presentation.common.hasPermission
import com.m4isper.contactsapp.presentation.common.openAppSettings
import com.m4isper.contactsapp.ui.components.ConfirmDialog
import com.m4isper.contactsapp.ui.theme.LocalSpacing
import dev.zacsweers.metrox.viewmodel.metroViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactEditorScreen(
    contactId: Long?,
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

    fun refreshPermission() {
        hasWritePermission = hasPermission(context, Manifest.permission.WRITE_CONTACTS)
    }

    LaunchedEffect(contactId) {
        if (hasPermission(context, Manifest.permission.WRITE_CONTACTS)) {
            viewModel.load(contactId)
        }
    }

    LaunchedEffect(uiState.closeScreen) {
        if (uiState.closeScreen) onDone()
    }

    LifecycleResumeEffect(Unit) {
        refreshPermission()
        onPauseOrDispose { /* no-op */ }
    }

    fun tryBack() {
        if (viewModel.isDirty()) {
            showDiscardDialog = true
        } else {
            onBack()
        }
    }

    if (!hasWritePermission) {
        PermissionRequest(
            modifier = modifier,
            rationale = stringResource(R.string.permission_write_rationale),
            onGrantClick = {
                writePermissionLauncher.launch(Manifest.permission.WRITE_CONTACTS)
            },
            onSettingsClick = { openAppSettings(context) },
        )
        return
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (uiState.isNew) {
                            stringResource(R.string.editor_title_new)
                        } else {
                            uiState.name.ifBlank { stringResource(R.string.editor_title_new) }
                        },
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = ::tryBack,
                        modifier = Modifier.testTag("editor_back_button"),
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
                actions = {
                    TextButton(
                        onClick = viewModel::onSave,
                        enabled = !uiState.isBusy,
                        modifier = Modifier.testTag("editor_save_button"),
                    ) {
                        Text(stringResource(R.string.editor_save))
                    }
                },
            )
        },
    ) { innerPadding ->
        EditorForm(
            uiState = uiState,
            onNameChange = viewModel::onNameChange,
            onPhoneChange = viewModel::onPhoneChange,
            onKindChange = viewModel::onKindChange,
            onCustomLabelChange = viewModel::onCustomLabelChange,
            onAddPhone = viewModel::onAddPhone,
            onRemovePhone = viewModel::onRemovePhone,
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
                viewModel.onDelete()
            },
            onDismiss = { showDeleteDialog = false },
            danger = true,
            confirmTestTag = "confirm_delete_button",
        )
    }
}

@Composable
private fun EditorForm(
    uiState: EditorUiState,
    onNameChange: (String) -> Unit,
    onPhoneChange: (Long, String) -> Unit,
    onKindChange: (Long, PhoneKind) -> Unit,
    onCustomLabelChange: (Long, String) -> Unit,
    onAddPhone: () -> Unit,
    onRemovePhone: (Long) -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalSpacing.current
    LazyColumn(modifier = modifier.fillMaxSize()) {
        item(key = "name") {
            OutlinedTextField(
                value = uiState.name,
                onValueChange = onNameChange,
                label = { Text(stringResource(R.string.editor_name_label)) },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                keyboardOptions =
                    KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next,
                    ),
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = spacing.m)
                        .padding(top = spacing.m)
                        .testTag("editor_name_field"),
            )
        }
        items(
            items = uiState.phones,
            key = { it.rowId },
        ) { row ->
            PhoneEditorRow(
                row = row,
                canRemove = uiState.phones.size > 1,
                onPhoneChange = onPhoneChange,
                onKindChange = onKindChange,
                onCustomLabelChange = onCustomLabelChange,
                onRemove = onRemovePhone,
            )
        }
        item(key = "add") {
            OutlinedButton(
                onClick = onAddPhone,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = spacing.m, vertical = spacing.s)
                        .testTag("editor_add_phone_button"),
            ) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(spacing.s))
                Text(stringResource(R.string.editor_add_phone))
            }
        }
        if (uiState.formError != null || uiState.errorMessage != null) {
            item(key = "error") {
                Text(
                    text =
                        when (uiState.formError) {
                            FormError.EMPTY_FORM ->
                                stringResource(R.string.form_error_empty)
                            FormError.BLANK_PHONE ->
                                stringResource(R.string.form_error_blank_phone)
                            null -> uiState.errorMessage.orEmpty()
                        },
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = spacing.m),
                )
            }
        }
        if (!uiState.isNew) {
            item(key = "delete") {
                OutlinedButton(
                    onClick = onDeleteClick,
                    colors =
                        ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error,
                        ),
                    border =
                        BorderStroke(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.error,
                        ),
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(spacing.m)
                            .testTag("editor_delete_button"),
                ) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = null,
                    )
                    Spacer(modifier = Modifier.width(spacing.s))
                    Text(stringResource(R.string.editor_delete))
                }
            }
        }
    }
}

@Composable
private fun PhoneEditorRow(
    row: EditorPhoneRow,
    canRemove: Boolean,
    onPhoneChange: (Long, String) -> Unit,
    onKindChange: (Long, PhoneKind) -> Unit,
    onCustomLabelChange: (Long, String) -> Unit,
    onRemove: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalSpacing.current
    var kindMenuOpen by remember { mutableStateOf(false) }
    Card(
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ),
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.m, vertical = spacing.xs),
    ) {
        Column(modifier = Modifier.padding(spacing.m)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = row.number,
                    onValueChange = { onPhoneChange(row.rowId, it) },
                    label = { Text(stringResource(R.string.editor_phone_label)) },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    keyboardOptions =
                        KeyboardOptions(
                            keyboardType = KeyboardType.Phone,
                            imeAction = ImeAction.Next,
                        ),
                    modifier =
                        Modifier
                            .weight(1f)
                            .testTag("editor_phone_field"),
                )
                TextButtonOrMenu(
                    kind = row.kind,
                    expanded = kindMenuOpen,
                    onExpandedChange = { kindMenuOpen = it },
                    onKindChange = {
                        onKindChange(row.rowId, it)
                        kindMenuOpen = false
                    },
                )
                if (canRemove) {
                    IconButton(onClick = { onRemove(row.rowId) }) {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = stringResource(R.string.action_delete),
                        )
                    }
                }
            }
            if (row.kind == PhoneKind.OTHER) {
                Spacer(modifier = Modifier.height(spacing.s))
                OutlinedTextField(
                    value = row.customLabel,
                    onValueChange = { onCustomLabelChange(row.rowId, it) },
                    label = { Text(stringResource(R.string.editor_kind_label)) },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    keyboardOptions =
                        KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Done,
                        ),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun TextButtonOrMenu(
    kind: PhoneKind,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onKindChange: (PhoneKind) -> Unit,
) {
    Box {
        TextButton(onClick = { onExpandedChange(true) }) {
            Text(kindLabel(kind = kind))
            Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) },
        ) {
            PhoneKind.entries.forEach { option ->
                DropdownMenuItem(
                    text = { Text(kindLabel(kind = option)) },
                    onClick = { onKindChange(option) },
                )
            }
        }
    }
}

@Composable
private fun kindLabel(kind: PhoneKind): String =
    stringResource(
        when (kind) {
            PhoneKind.MOBILE -> R.string.phone_type_mobile
            PhoneKind.HOME -> R.string.phone_type_home
            PhoneKind.WORK -> R.string.phone_type_work
            PhoneKind.OTHER -> R.string.phone_type_other
        },
    )
