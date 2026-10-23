package com.m4isper.contactsapp.presentation.editor

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.m4isper.contactsapp.R
import com.m4isper.contactsapp.domain.model.contact.ContactDetails
import com.m4isper.contactsapp.domain.model.contact.FormError
import com.m4isper.contactsapp.presentation.common.groups.GroupPickerDialog
import com.m4isper.contactsapp.presentation.common.message
import com.m4isper.contactsapp.presentation.editor.components.AccountPickerDialog
import com.m4isper.contactsapp.presentation.editor.components.AccountRow
import com.m4isper.contactsapp.presentation.editor.components.SectionHeader
import com.m4isper.contactsapp.presentation.editor.dialogs.BirthdayPickerDialog
import com.m4isper.contactsapp.presentation.editor.dialogs.CropPhotoDialog
import com.m4isper.contactsapp.presentation.editor.edits.withBirthday
import com.m4isper.contactsapp.presentation.editor.edits.withDisplayName
import com.m4isper.contactsapp.presentation.editor.edits.withEventDate
import com.m4isper.contactsapp.presentation.editor.edits.withFamilyName
import com.m4isper.contactsapp.presentation.editor.edits.withGivenName
import com.m4isper.contactsapp.presentation.editor.edits.withMiddleName
import com.m4isper.contactsapp.presentation.editor.edits.withNamePrefix
import com.m4isper.contactsapp.presentation.editor.edits.withNameSuffix
import com.m4isper.contactsapp.presentation.editor.edits.withNickname
import com.m4isper.contactsapp.presentation.editor.edits.withPhoneticFamilyName
import com.m4isper.contactsapp.presentation.editor.edits.withPhoneticGivenName
import com.m4isper.contactsapp.presentation.editor.edits.withPhoneticMiddleName
import com.m4isper.contactsapp.presentation.editor.edits.withPhoto
import com.m4isper.contactsapp.presentation.editor.edits.withToggledGroup
import com.m4isper.contactsapp.presentation.editor.edits.withToggledNameMode
import com.m4isper.contactsapp.presentation.editor.edits.withoutPhoto
import com.m4isper.contactsapp.presentation.editor.items.editorContactItems
import com.m4isper.contactsapp.presentation.editor.items.editorExtraItems
import com.m4isper.contactsapp.presentation.editor.rows.GroupMembershipRow
import com.m4isper.contactsapp.presentation.editor.rows.NameEditor
import com.m4isper.contactsapp.presentation.editor.rows.PhotoEditor
import com.m4isper.contactsapp.ui.theme.LocalSpacing

@Composable
internal fun EditorForm(
    viewModel: ContactEditorViewModel,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalSpacing.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val edit: ((EditorUiState) -> EditorUiState) -> Unit = viewModel::update
    var showDatePicker by remember { mutableStateOf(false) }
    var showGroupPicker by remember { mutableStateOf(false) }
    var showAccountPicker by remember { mutableStateOf(false) }
    var eventPickerRowId by remember { mutableStateOf<Long?>(null) }
    val context = LocalContext.current
    var cropSource by remember { mutableStateOf<Uri?>(null) }

    cropSource?.let { source ->
        CropPhotoDialog(
            source = source,
            onConfirm = { cropped ->
                cropSource = null
                edit { it.withPhoto(cropped.toString()) }
            },
            onDismiss = { cropSource = null },
        )
    }

    if (uiState.similarContacts.isNotEmpty()) {
        SimilarContactsDialog(
            contacts = uiState.similarContacts,
            onConfirm = viewModel::onSaveConfirmed,
            onDismiss = viewModel::onSaveDismissed,
        )
    }

    LazyColumn(
        modifier =
            modifier
                .fillMaxSize()
                .imePadding()
                .testTag("editor_form"),
    ) {
        item(key = "photo") {
            EditorPhotoSection(
                name = uiState.effectiveName,
                photoUri = uiState.photoUri,
                context = context,
                onCrop = { cropSource = it },
                onRemove = { edit { it.withoutPhoto() } },
            )
        }
        accountItems(uiState = uiState, onClick = { showAccountPicker = true })
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
                onDisplayNameChange = { value -> edit { it.withDisplayName(value) } },
                onGivenNameChange = { value -> edit { it.withGivenName(value) } },
                onFamilyNameChange = { value -> edit { it.withFamilyName(value) } },
                onMiddleNameChange = { value -> edit { it.withMiddleName(value) } },
                onPrefixChange = { value -> edit { it.withNamePrefix(value) } },
                onSuffixChange = { value -> edit { it.withNameSuffix(value) } },
                onPhoneticGivenChange = { value -> edit { it.withPhoneticGivenName(value) } },
                onPhoneticMiddleChange = { value -> edit { it.withPhoneticMiddleName(value) } },
                onPhoneticFamilyChange = { value -> edit { it.withPhoneticFamilyName(value) } },
                onToggleMode = { edit { it.withToggledNameMode() } },
            )
        }
        item(key = "nickname-header") {
            SectionHeader(titleRes = R.string.editor_section_nickname)
        }
        item(key = "nickname") {
            OutlinedTextField(
                value = uiState.nickname.nickname,
                onValueChange = { value -> edit { it.withNickname(value) } },
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
        item(key = "groups-header") {
            SectionHeader(titleRes = R.string.editor_section_groups)
        }
        item(key = "groups") {
            GroupMembershipRow(
                selected = uiState.selectedGroups,
                onClick = { showGroupPicker = true },
            )
        }
        editorContactItems(uiState = uiState, edit = edit)
        editorExtraItems(
            uiState = uiState,
            edit = edit,
            onBirthdayClick = { showDatePicker = true },
            onEventDateClick = { eventPickerRowId = it },
        )
        if (uiState.formError != null || uiState.error != null || uiState.deleteFailed) {
            item(key = "error") {
                Text(
                    text = formMessage(uiState),
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
                edit { it.withBirthday(birthday) }
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
                edit { it.withEventDate(eventPickerRow.rowId, birthday) }
                eventPickerRowId = null
            },
            onDismiss = { eventPickerRowId = null },
        )
    }

    if (showGroupPicker) {
        GroupPickerDialog(
            title = stringResource(R.string.editor_section_groups),
            groups = uiState.availableGroups,
            selectedIds = uiState.selectedGroupIds,
            confirmLabel = stringResource(R.string.action_done),
            onToggle = { groupId -> edit { it.withToggledGroup(groupId) } },
            onCreateGroup = viewModel::onCreateGroup,
            onConfirm = { showGroupPicker = false },
            onDismiss = { showGroupPicker = false },
        )
    }

    if (showAccountPicker) {
        AccountPickerDialog(
            accounts = uiState.accounts,
            selected = uiState.selectedAccount,
            onSelect = { account ->
                viewModel.onAccountSelected(account)
                showAccountPicker = false
            },
            onDismiss = { showAccountPicker = false },
        )
    }
}

/**
 * Новому контакту — выбор аккаунта, если есть из чего выбирать;
 * существующему — аккаунт только для чтения.
 */
private fun LazyListScope.accountItems(
    uiState: EditorUiState,
    onClick: () -> Unit,
) {
    if (uiState.isNew) {
        if (uiState.accounts.size <= 1) return
        item(key = "account-header") {
            SectionHeader(titleRes = R.string.editor_section_account)
        }
        item(key = "account") {
            AccountRow(account = uiState.selectedAccount, onClick = onClick)
        }
    } else {
        val account = uiState.selectedAccount ?: return
        item(key = "account-header") {
            SectionHeader(titleRes = R.string.editor_section_account)
        }
        item(key = "account") {
            AccountRow(account = account, onClick = null)
        }
    }
}

/**
 * Фото контакта: галерея, камера и обрезка. Снимок камеры и выбранный файл
 * попадают в один редактор обрезки.
 */
@Composable
private fun EditorPhotoSection(
    name: String,
    photoUri: String?,
    context: android.content.Context,
    onCrop: (Uri) -> Unit,
    onRemove: () -> Unit,
) {
    val photoPicker =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.PickVisualMedia()) { uri ->
            uri?.let { onCrop(it) }
        }
    var cameraUri by remember { mutableStateOf<Uri?>(null) }
    val cameraLauncher =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.TakePicture()) { saved ->
            if (saved) cameraUri?.let { onCrop(it) }
        }
    PhotoEditor(
        name = name,
        photoUri = photoUri,
        onPick = {
            photoPicker.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
            )
        },
        onTakePhoto = {
            val uri = cameraOutputUri(context)
            cameraUri = uri
            cameraLauncher.launch(uri)
        },
        onRemove = onRemove,
    )
}

/**
 * Предупреждение о дубле: похожие контакты и выбор — сохранить всё равно или отменить.
 */
@Composable
private fun SimilarContactsDialog(
    contacts: List<ContactDetails>,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val spacing = LocalSpacing.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.editor_duplicate_title)) },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.editor_duplicate_message),
                    style = MaterialTheme.typography.bodyMedium,
                )
                contacts.forEach { contact ->
                    Text(
                        text = contact.name,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(top = spacing.s),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                modifier = Modifier.testTag("editor_duplicate_confirm"),
            ) {
                Text(stringResource(R.string.editor_duplicate_save_anyway))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
        modifier = Modifier.testTag("editor_duplicate_dialog"),
    )
}

/** Файл для снимка камеры: провайдеру отдаём URI через FileProvider. */
private fun cameraOutputUri(context: android.content.Context): Uri {
    val file = java.io.File(context.cacheDir, "camera_${System.currentTimeMillis()}.jpg")
    return androidx.core.content.FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file,
    )
}

/** Текст ошибки поля: показываем проблему рядом с полем. */
@Composable
internal fun FormError.fieldMessage(): String =
    stringResource(
        when (this) {
            FormError.EMPTY_FORM -> R.string.form_error_empty
            FormError.INVALID_PHONE -> R.string.form_error_phone
            FormError.INVALID_EMAIL -> R.string.form_error_email
            FormError.INVALID_URL -> R.string.form_error_url
            FormError.INVALID_DATE -> R.string.form_error_date
        },
    )

/** Текст ошибки формы: удаление, валидация или сообщение исключения. */
@Composable
private fun formMessage(uiState: EditorUiState): String =
    when {
        uiState.deleteFailed -> stringResource(R.string.batch_delete_failed)
        uiState.formError == FormError.EMPTY_FORM -> stringResource(R.string.form_error_empty)
        else -> uiState.error?.message().orEmpty()
    }
