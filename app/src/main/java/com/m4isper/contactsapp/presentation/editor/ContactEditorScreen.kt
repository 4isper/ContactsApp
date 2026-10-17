package com.m4isper.contactsapp.presentation.editor

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
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
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
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
import com.m4isper.contactsapp.domain.model.AddressKind
import com.m4isper.contactsapp.domain.model.Birthday
import com.m4isper.contactsapp.domain.model.EditableBirthday
import com.m4isper.contactsapp.domain.model.EmailKind
import com.m4isper.contactsapp.domain.model.FormError
import com.m4isper.contactsapp.domain.model.PhoneKind
import com.m4isper.contactsapp.presentation.common.PermissionRequest
import com.m4isper.contactsapp.presentation.common.addressKindLabel
import com.m4isper.contactsapp.presentation.common.birthdayText
import com.m4isper.contactsapp.presentation.common.emailKindLabel
import com.m4isper.contactsapp.presentation.common.hasPermission
import com.m4isper.contactsapp.presentation.common.openAppSettings
import com.m4isper.contactsapp.presentation.common.phoneKindLabel
import com.m4isper.contactsapp.ui.components.ConfirmDialog
import com.m4isper.contactsapp.ui.components.ContactAvatar
import com.m4isper.contactsapp.ui.theme.LocalSpacing
import dev.zacsweers.metrox.viewmodel.metroViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

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
                onDisplayNameChange = viewModel::onDisplayNameChange,
                onGivenNameChange = viewModel::onGivenNameChange,
                onFamilyNameChange = viewModel::onFamilyNameChange,
                onMiddleNameChange = viewModel::onMiddleNameChange,
                onPrefixChange = viewModel::onNamePrefixChange,
                onSuffixChange = viewModel::onNameSuffixChange,
                onToggleMode = viewModel::onToggleNameMode,
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
}

@Composable
private fun PhotoEditor(
    name: String,
    photoUri: String?,
    onPick: () -> Unit,
    onRemove: () -> Unit,
) {
    val spacing = LocalSpacing.current
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(spacing.m),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ContactAvatar(
            name = name,
            photoUri = photoUri,
            size = spacing.avatarDetails,
            textStyle = MaterialTheme.typography.headlineMedium,
            testTag = "editor_photo",
        )
        Spacer(modifier = Modifier.height(spacing.xs))
        Row {
            TextButton(
                onClick = onPick,
                modifier = Modifier.testTag("editor_photo_pick"),
            ) {
                Text(
                    stringResource(
                        if (photoUri == null) R.string.editor_add_photo else R.string.editor_change_photo,
                    ),
                )
            }
            if (photoUri != null) {
                TextButton(
                    onClick = onRemove,
                    modifier = Modifier.testTag("editor_photo_remove"),
                ) {
                    Text(
                        text = stringResource(R.string.editor_remove_photo),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(
    titleRes: Int,
    onAdd: (() -> Unit)? = null,
    addLabelRes: Int? = null,
    addTestTag: String? = null,
) {
    val spacing = LocalSpacing.current
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.m)
                .padding(top = spacing.m, bottom = spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(titleRes),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        if (onAdd != null) {
            IconButton(
                onClick = onAdd,
                modifier = if (addTestTag != null) Modifier.testTag(addTestTag) else Modifier,
            ) {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = addLabelRes?.let { stringResource(it) },
                )
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
) {
    val spacing = LocalSpacing.current
    Card(
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ),
        modifier =
            Modifier
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
                KindMenu(
                    current = row.kind,
                    options = PhoneKind.entries,
                    label = { phoneKindLabel(it) },
                    onSelect = { onKindChange(row.rowId, it) },
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
private fun EmailEditorRow(
    row: EditorEmailRow,
    onEmailChange: (Long, String) -> Unit,
    onKindChange: (Long, EmailKind) -> Unit,
    onCustomLabelChange: (Long, String) -> Unit,
    onRemove: (Long) -> Unit,
) {
    val spacing = LocalSpacing.current
    Card(
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ),
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.m, vertical = spacing.xs),
    ) {
        Column(modifier = Modifier.padding(spacing.m)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = row.address,
                    onValueChange = { onEmailChange(row.rowId, it) },
                    label = { Text(stringResource(R.string.editor_email_label)) },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    keyboardOptions =
                        KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next,
                        ),
                    modifier =
                        Modifier
                            .weight(1f)
                            .testTag("editor_email_field"),
                )
                KindMenu(
                    current = row.kind,
                    options = EmailKind.entries,
                    label = { emailKindLabel(it) },
                    onSelect = { onKindChange(row.rowId, it) },
                )
                IconButton(onClick = { onRemove(row.rowId) }) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = stringResource(R.string.action_delete),
                    )
                }
            }
            if (row.kind == EmailKind.OTHER) {
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
private fun AddressEditorRow(
    row: EditorAddressRow,
    onAddressChange: (Long, String) -> Unit,
    onKindChange: (Long, AddressKind) -> Unit,
    onCustomLabelChange: (Long, String) -> Unit,
    onRemove: (Long) -> Unit,
) {
    val spacing = LocalSpacing.current
    Card(
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ),
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.m, vertical = spacing.xs),
    ) {
        Column(modifier = Modifier.padding(spacing.m)) {
            OutlinedTextField(
                value = row.address,
                onValueChange = { onAddressChange(row.rowId, it) },
                label = { Text(stringResource(R.string.editor_address_label)) },
                minLines = 2,
                shape = MaterialTheme.shapes.medium,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .testTag("editor_address_field"),
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                KindMenu(
                    current = row.kind,
                    options = AddressKind.entries,
                    label = { addressKindLabel(it) },
                    onSelect = { onKindChange(row.rowId, it) },
                )
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = { onRemove(row.rowId) }) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = stringResource(R.string.action_delete),
                    )
                }
            }
            if (row.kind == AddressKind.OTHER) {
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
private fun WebsiteEditorRow(
    row: EditorWebsiteRow,
    onWebsiteChange: (Long, String) -> Unit,
    onRemove: (Long) -> Unit,
) {
    val spacing = LocalSpacing.current
    Card(
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ),
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.m, vertical = spacing.xs),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(spacing.m),
        ) {
            OutlinedTextField(
                value = row.url,
                onValueChange = { onWebsiteChange(row.rowId, it) },
                label = { Text(stringResource(R.string.editor_website_label)) },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                keyboardOptions =
                    KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Next,
                    ),
                modifier =
                    Modifier
                        .weight(1f)
                        .testTag("editor_website_field"),
            )
            IconButton(onClick = { onRemove(row.rowId) }) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = stringResource(R.string.action_delete),
                )
            }
        }
    }
}

@Composable
private fun <T> KindMenu(
    current: T,
    options: List<T>,
    label: @Composable (T) -> String,
    onSelect: (T) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { expanded = true }) {
            Text(label(current))
            Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(label(option)) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun BirthdayEditorRow(
    birthday: EditableBirthday,
    onPickClick: () -> Unit,
    onBirthdayChange: (Birthday?) -> Unit,
) {
    val spacing = LocalSpacing.current
    val value = birthday.value
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.m),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(
                onClick = onPickClick,
                modifier = Modifier.testTag("editor_birthday_button"),
            ) {
                Text(
                    text =
                        if (value == null) {
                            stringResource(R.string.editor_birthday_add)
                        } else {
                            birthdayText(value)
                        },
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            if (value != null) {
                IconButton(
                    onClick = { onBirthdayChange(null) },
                    modifier = Modifier.testTag("editor_birthday_remove"),
                ) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = stringResource(R.string.editor_birthday_remove),
                    )
                }
            }
        }
        if (value != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.editor_birthday_no_year),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(modifier = Modifier.width(spacing.s))
                Switch(
                    checked = value.year == null,
                    onCheckedChange = { noYear ->
                        onBirthdayChange(value.copy(year = if (noYear) null else LocalDate.now().year))
                    },
                    modifier = Modifier.testTag("editor_birthday_switch"),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BirthdayPickerDialog(
    birthday: Birthday?,
    onConfirmed: (Birthday) -> Unit,
    onDismiss: () -> Unit,
) {
    val pickerState =
        rememberDatePickerState(
            initialSelectedDateMillis =
                birthday?.let { value ->
                    LocalDate
                        .of(value.year ?: DEFAULT_PICKER_YEAR, value.month, value.day)
                        .atStartOfDay(ZoneOffset.UTC)
                        .toInstant()
                        .toEpochMilli()
                },
        )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        val date =
                            Instant
                                .ofEpochMilli(millis)
                                .atZone(ZoneOffset.UTC)
                                .toLocalDate()
                        onConfirmed(
                            Birthday(
                                day = date.dayOfMonth,
                                month = date.monthValue,
                                year = date.year,
                            ),
                        )
                    }
                },
                modifier = Modifier.testTag("editor_birthday_confirm"),
            ) {
                Text(stringResource(R.string.editor_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    ) {
        DatePicker(state = pickerState, showModeToggle = false, title = null)
    }
}

private const val DEFAULT_PICKER_YEAR = 2000
