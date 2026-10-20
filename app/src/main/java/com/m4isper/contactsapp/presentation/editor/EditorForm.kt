package com.m4isper.contactsapp.presentation.editor

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.m4isper.contactsapp.R
import com.m4isper.contactsapp.domain.model.FormError
import com.m4isper.contactsapp.presentation.common.groups.GroupPickerDialog
import com.m4isper.contactsapp.ui.theme.LocalSpacing

@Composable
internal fun EditorForm(
    viewModel: ContactEditorViewModel,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalSpacing.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showDatePicker by remember { mutableStateOf(false) }
    var showGroupPicker by remember { mutableStateOf(false) }
    var showAccountPicker by remember { mutableStateOf(false) }
    var eventPickerRowId by remember { mutableStateOf<Long?>(null) }
    val photoPicker =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.PickVisualMedia()) { uri ->
            uri?.let { viewModel.onPhotoPicked(it.toString()) }
        }

    LazyColumn(
        modifier =
            modifier
                .fillMaxSize()
                .imePadding()
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
        item(key = "groups-header") {
            SectionHeader(titleRes = R.string.editor_section_groups)
        }
        item(key = "groups") {
            GroupMembershipRow(
                selected = uiState.selectedGroups,
                onClick = { showGroupPicker = true },
            )
        }
        editorContactItems(uiState = uiState, viewModel = viewModel)
        editorExtraItems(
            uiState = uiState,
            viewModel = viewModel,
            onBirthdayClick = { showDatePicker = true },
            onEventDateClick = { eventPickerRowId = it },
        )
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

    if (showGroupPicker) {
        GroupPickerDialog(
            title = stringResource(R.string.editor_section_groups),
            groups = uiState.allGroups,
            selectedIds = uiState.selectedGroupIds,
            confirmLabel = stringResource(R.string.action_done),
            onToggle = viewModel::onGroupToggled,
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

/** Секция аккаунта нужна только новому контакту, когда есть из чего выбирать. */
private fun LazyListScope.accountItems(
    uiState: EditorUiState,
    onClick: () -> Unit,
) {
    if (!uiState.isNew || uiState.accounts.size <= 1) return
    item(key = "account-header") {
        SectionHeader(titleRes = R.string.editor_section_account)
    }
    item(key = "account") {
        AccountRow(account = uiState.selectedAccount, onClick = onClick)
    }
}
