package com.m4isper.contactsapp.presentation.editor.items

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.m4isper.contactsapp.R
import com.m4isper.contactsapp.presentation.editor.EditorUiState
import com.m4isper.contactsapp.presentation.editor.FormItemIndices
import com.m4isper.contactsapp.presentation.editor.components.SectionHeader
import com.m4isper.contactsapp.presentation.editor.edits.withDisplayName
import com.m4isper.contactsapp.presentation.editor.edits.withFamilyName
import com.m4isper.contactsapp.presentation.editor.edits.withGivenName
import com.m4isper.contactsapp.presentation.editor.edits.withMiddleName
import com.m4isper.contactsapp.presentation.editor.edits.withNamePrefix
import com.m4isper.contactsapp.presentation.editor.edits.withNameSuffix
import com.m4isper.contactsapp.presentation.editor.edits.withNickname
import com.m4isper.contactsapp.presentation.editor.edits.withPhoneticFamilyName
import com.m4isper.contactsapp.presentation.editor.edits.withPhoneticGivenName
import com.m4isper.contactsapp.presentation.editor.edits.withPhoneticMiddleName
import com.m4isper.contactsapp.presentation.editor.edits.withToggledNameMode
import com.m4isper.contactsapp.presentation.editor.formMessage
import com.m4isper.contactsapp.presentation.editor.rows.GroupMembershipRow
import com.m4isper.contactsapp.presentation.editor.rows.NameEditor
import com.m4isper.contactsapp.ui.theme.LocalSpacing

/** Баннер ошибки: удаление, валидация формы или ошибки полей. */
internal fun LazyListScope.errorItem(
    uiState: EditorUiState,
    itemIndices: FormItemIndices,
) {
    if (uiState.formError == null && !uiState.deleteFailed && uiState.fieldErrors.isEmpty()) return
    itemIndices.register("error")
    item(key = "error") {
        Text(
            text = formMessage(uiState),
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium,
            modifier =
                Modifier.padding(
                    horizontal = LocalSpacing.current.m,
                    vertical = LocalSpacing.current.s,
                ),
        )
    }
}

/** Имя во всех вариантах: отображаемое, части, фонетика и переключатель режима. */
internal fun LazyListScope.nameItem(
    uiState: EditorUiState,
    edit: ((EditorUiState) -> EditorUiState) -> Unit,
    itemIndices: FormItemIndices,
) {
    itemIndices.register("name")
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
}

/** Ник: короткое имя для списков и поиска. */
internal fun LazyListScope.nicknameItem(
    uiState: EditorUiState,
    edit: ((EditorUiState) -> EditorUiState) -> Unit,
    itemIndices: FormItemIndices,
) {
    itemIndices.register("nickname-header")
    item(key = "nickname-header") {
        SectionHeader(titleRes = R.string.editor_section_nickname)
    }
    itemIndices.register("nickname")
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
                    .padding(horizontal = LocalSpacing.current.m)
                    .testTag("editor_nickname_field"),
        )
    }
}

/** Группы: одна строка-сводка, выбор открывается в диалоге. */
internal fun LazyListScope.groupsItem(
    uiState: EditorUiState,
    itemIndices: FormItemIndices,
    onGroupsClick: () -> Unit,
) {
    itemIndices.register("groups-header")
    item(key = "groups-header") {
        SectionHeader(titleRes = R.string.editor_section_groups)
    }
    itemIndices.register("groups")
    item(key = "groups") {
        GroupMembershipRow(
            selected = uiState.selectedGroups,
            onClick = onGroupsClick,
        )
    }
}
