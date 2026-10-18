package com.m4isper.contactsapp.presentation.editor

import android.Manifest
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ButtonDefaults
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
import com.m4isper.contactsapp.presentation.common.permissions.PermissionRequest
import com.m4isper.contactsapp.presentation.common.permissions.hasPermission
import com.m4isper.contactsapp.presentation.common.permissions.openAppSettings
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

    LaunchedEffect(contactId, hasWritePermission) {
        if (hasWritePermission) {
            viewModel.load(contactId)
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
                            uiState.effectiveName.ifBlank { stringResource(R.string.editor_title_new) }
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
                viewModel.onDelete()
            },
            onDismiss = { showDeleteDialog = false },
            danger = true,
            confirmTestTag = "confirm_delete_button",
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditorForm(
    viewModel: ContactEditorViewModel,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalSpacing.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showDatePicker by remember { mutableStateOf(false) }
    var eventPickerRowId by remember { mutableStateOf<Long?>(null) }
    val photoPicker =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.PickVisualMedia()) { uri ->
            uri?.let { viewModel.onPhotoPicked(it.toString()) }
        }

    LazyColumn(
        modifier =
            modifier
                .fillMaxSize()
                .testTag("editor_form"),
    ) {
        item(key = "photo") {
            PhotoEditor(
                name = uiState.effectiveName,
                photoUri = uiState.photoUri,
                onPick = {
                    photoPicker.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                    )
                },
                onRemove = viewModel::onPhotoRemoved,
            )
        }
        item(key = "name") {
            NameEditor(
                mode = uiState.nameMode,
                displayName = uiState.displayName,
                given = uiState.givenName,
                family = uiState.familyName,
                middle = uiState.middleName,
                prefix = uiState.namePrefix,
                suffix = uiState.nameSuffix,
                phoneticGiven = uiState.phoneticGivenName,
                phoneticMiddle = uiState.phoneticMiddleName,
                phoneticFamily = uiState.phoneticFamilyName,
                onDisplayNameChange = viewModel::onDisplayNameChange,
                onGivenNameChange = viewModel::onGivenNameChange,
                onFamilyNameChange = viewModel::onFamilyNameChange,
                onMiddleNameChange = viewModel::onMiddleNameChange,
                onPrefixChange = viewModel::onNamePrefixChange,
                onSuffixChange = viewModel::onNameSuffixChange,
                onPhoneticGivenChange = viewModel::onPhoneticGivenNameChange,
                onPhoneticMiddleChange = viewModel::onPhoneticMiddleNameChange,
                onPhoneticFamilyChange = viewModel::onPhoneticFamilyNameChange,
                onToggleMode = viewModel::onToggleNameMode,
            )
        }
        item(key = "nickname-header") {
            SectionHeader(titleRes = R.string.editor_section_nickname)
        }
        item(key = "nickname") {
            OutlinedTextField(
                value = uiState.nickname.nickname,
                onValueChange = viewModel::onNicknameChange,
                label = { Text(stringResource(R.string.editor_nickname_label)) },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = spacing.m)
                        .testTag("editor_nickname_field"),
            )
        }
        items(
            items = uiState.phones,
            key = { it.rowId },
        ) { row ->
            PhoneEditorRow(
                row = row,
                canRemove = uiState.phones.size > 1,
                onPhoneChange = viewModel::onPhoneChange,
                onKindChange = viewModel::onKindChange,
                onCustomLabelChange = viewModel::onCustomLabelChange,
                onRemove = viewModel::onRemovePhone,
            )
        }
        item(key = "add-phone") {
            OutlinedButton(
                onClick = viewModel::onAddPhone,
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
        item(key = "emails-header") {
            SectionHeader(
                titleRes = R.string.editor_section_emails,
                onAdd = viewModel::onAddEmail,
                addLabelRes = R.string.editor_add_email,
                addTestTag = "editor_add_email_button",
            )
        }
        items(
            items = uiState.emails,
            key = { it.rowId },
        ) { row ->
            EmailEditorRow(
                row = row,
                onEmailChange = viewModel::onEmailChange,
                onKindChange = viewModel::onEmailKindChange,
                onCustomLabelChange = viewModel::onEmailCustomLabelChange,
                onRemove = viewModel::onRemoveEmail,
            )
        }
        item(key = "instant-messages-header") {
            SectionHeader(
                titleRes = R.string.editor_section_instant_messages,
                onAdd = viewModel::onAddIm,
                addLabelRes = R.string.editor_add_im,
                addTestTag = "editor_add_im_button",
            )
        }
        items(
            items = uiState.instantMessages,
            key = { it.rowId },
        ) { row ->
            ImEditorRow(
                row = row,
                onImChange = viewModel::onImChange,
                onProtocolChange = viewModel::onImProtocolChange,
                onCustomProtocolChange = viewModel::onImCustomProtocolChange,
                onRemove = viewModel::onRemoveIm,
            )
        }
        item(key = "addresses-header") {
            SectionHeader(
                titleRes = R.string.editor_section_addresses,
                onAdd = viewModel::onAddAddress,
                addLabelRes = R.string.editor_add_address,
                addTestTag = "editor_add_address_button",
            )
        }
        items(
            items = uiState.addresses,
            key = { it.rowId },
        ) { row ->
            AddressEditorRow(
                row = row,
                onAddressChange = viewModel::onAddressChange,
                onKindChange = viewModel::onAddressKindChange,
                onCustomLabelChange = viewModel::onAddressCustomLabelChange,
                onRemove = viewModel::onRemoveAddress,
            )
        }
        item(key = "websites-header") {
            SectionHeader(
                titleRes = R.string.editor_section_websites,
                onAdd = viewModel::onAddWebsite,
                addLabelRes = R.string.editor_add_website,
                addTestTag = "editor_add_website_button",
            )
        }
        items(
            items = uiState.websites,
            key = { it.rowId },
        ) { row ->
            WebsiteEditorRow(
                row = row,
                onWebsiteChange = viewModel::onWebsiteChange,
                onRemove = viewModel::onRemoveWebsite,
            )
        }
        item(key = "organization-header") {
            SectionHeader(titleRes = R.string.editor_section_organization)
        }
        item(key = "organization") {
            Column(modifier = Modifier.padding(horizontal = spacing.m)) {
                OutlinedTextField(
                    value = uiState.organization.company,
                    onValueChange = viewModel::onOrganizationCompanyChange,
                    label = { Text(stringResource(R.string.editor_organization_company)) },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .testTag("editor_company_field"),
                )
                Spacer(modifier = Modifier.height(spacing.s))
                OutlinedTextField(
                    value = uiState.organization.title,
                    onValueChange = viewModel::onOrganizationTitleChange,
                    label = { Text(stringResource(R.string.editor_organization_title)) },
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
                            .testTag("editor_title_field"),
                )
            }
        }
        item(key = "birthday-header") {
            SectionHeader(titleRes = R.string.editor_section_birthday)
        }
        item(key = "birthday") {
            BirthdayEditorRow(
                birthday = uiState.birthday,
                onPickClick = { showDatePicker = true },
                onBirthdayChange = viewModel::onBirthdayChange,
            )
        }
        item(key = "events-header") {
            SectionHeader(
                titleRes = R.string.editor_section_events,
                onAdd = viewModel::onAddEvent,
                addLabelRes = R.string.editor_add_event,
                addTestTag = "editor_add_event_button",
            )
        }
        items(
            items = uiState.events,
            key = { it.rowId },
        ) { row ->
            EventEditorRow(
                row = row,
                onKindChange = viewModel::onEventKindChange,
                onDatePickClick = { eventPickerRowId = it },
                onDateChange = viewModel::onEventDateChange,
                onCustomLabelChange = viewModel::onEventCustomLabelChange,
                onRemove = viewModel::onRemoveEvent,
            )
        }
        item(key = "relations-header") {
            SectionHeader(
                titleRes = R.string.editor_section_relations,
                onAdd = viewModel::onAddRelation,
                addLabelRes = R.string.editor_add_relation,
                addTestTag = "editor_add_relation_button",
            )
        }
        items(
            items = uiState.relations,
            key = { it.rowId },
        ) { row ->
            RelationEditorRow(
                row = row,
                onNameChange = viewModel::onRelationNameChange,
                onKindChange = viewModel::onRelationKindChange,
                onCustomLabelChange = viewModel::onRelationCustomLabelChange,
                onRemove = viewModel::onRemoveRelation,
            )
        }
        item(key = "note-header") {
            SectionHeader(titleRes = R.string.editor_section_note)
        }
        item(key = "note") {
            OutlinedTextField(
                value = uiState.note.note,
                onValueChange = viewModel::onNoteChange,
                label = { Text(stringResource(R.string.editor_note_label)) },
                minLines = 3,
                shape = MaterialTheme.shapes.medium,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = spacing.m)
                        .testTag("editor_note_field"),
            )
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
                    modifier = Modifier.padding(horizontal = spacing.m, vertical = spacing.s),
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

    if (showDatePicker) {
        BirthdayPickerDialog(
            birthday = uiState.birthday.value,
            onConfirmed = { birthday ->
                viewModel.onBirthdayChange(birthday)
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false },
        )
    }

    val eventPickerRow = uiState.events.firstOrNull { it.rowId == eventPickerRowId }
    if (eventPickerRow != null) {
        BirthdayPickerDialog(
            birthday = eventPickerRow.value.date,
            confirmTestTag = "editor_event_date_confirm",
            onConfirmed = { birthday ->
                viewModel.onEventDateChange(eventPickerRow.rowId, birthday)
                eventPickerRowId = null
            },
            onDismiss = { eventPickerRowId = null },
        )
    }
}
