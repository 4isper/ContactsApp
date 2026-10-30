package com.m4isper.contactsapp.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.m4isper.contactsapp.R
import com.m4isper.contactsapp.domain.model.contact.Birthday
import com.m4isper.contactsapp.domain.model.contact.ContactDetails
import com.m4isper.contactsapp.domain.model.contact.ContactGroup
import com.m4isper.contactsapp.domain.model.contact.EditableBirthday
import com.m4isper.contactsapp.domain.model.contact.EditableEmail
import com.m4isper.contactsapp.domain.model.contact.EditableNickname
import com.m4isper.contactsapp.domain.model.contact.EditableNote
import com.m4isper.contactsapp.domain.model.contact.EditableOrganization
import com.m4isper.contactsapp.domain.model.contact.EditablePhone
import com.m4isper.contactsapp.domain.model.contact.EmailAddress
import com.m4isper.contactsapp.domain.model.contact.EmailKind
import com.m4isper.contactsapp.domain.model.contact.Organization
import com.m4isper.contactsapp.domain.model.contact.PhoneKind
import com.m4isper.contactsapp.domain.model.contact.PhoneNumber
import com.m4isper.contactsapp.presentation.common.groups.GroupPickerSheet
import com.m4isper.contactsapp.presentation.details.DetailsContent
import com.m4isper.contactsapp.presentation.editor.EditorBody
import com.m4isper.contactsapp.presentation.editor.EditorRow
import com.m4isper.contactsapp.presentation.editor.EditorUiState
import com.m4isper.contactsapp.presentation.editor.FormItemIndices
import com.m4isper.contactsapp.presentation.editor.items.editorContactItems
import com.m4isper.contactsapp.presentation.editor.items.editorExtraItems
import com.m4isper.contactsapp.presentation.editor.items.groupsItem
import com.m4isper.contactsapp.presentation.editor.items.nameItem
import com.m4isper.contactsapp.presentation.editor.items.nicknameItem
import com.m4isper.contactsapp.presentation.settings.SettingsOption
import com.m4isper.contactsapp.presentation.settings.SettingsSection
import com.m4isper.contactsapp.presentation.settings.SettingsSwitch
import com.m4isper.contactsapp.ui.adaptive.ReadingColumn
import com.m4isper.contactsapp.ui.adaptive.WindowWidthClass
import com.m4isper.contactsapp.ui.adaptive.rememberWindowWidthClass
import com.m4isper.contactsapp.ui.theme.ContactsAppTheme
import com.m4isper.contactsapp.ui.theme.LocalSpacing

private val WidthPreviewDetails =
    ContactDetails(
        contactId = 1,
        name = "Anna Ivanova",
        phones =
            listOf(
                PhoneNumber("+7 900 111-11-11", PhoneKind.MOBILE, isPrimary = true),
                PhoneNumber("+7 495 222-22-22", PhoneKind.WORK),
            ),
        emails = listOf(EmailAddress("anna@example.com", EmailKind.HOME)),
        organization = Organization(company = "ACME", title = "Staff Engineer"),
        birthday = Birthday(day = 21, month = 7, year = 1990),
        nickname = "anna",
    )

private val WidthPreviewGroups =
    listOf(
        ContactGroup(id = 1, title = "Friends"),
        ContactGroup(id = 2, title = "Work"),
    )

private val WidthPreviewEditorState =
    EditorUiState(
        displayName = "Anna Ivanova",
        phones = listOf(EditorRow(1, EditablePhone(number = "+7 900 111-11-11"))),
        emails = listOf(EditorRow(2, EditableEmail(address = "anna@example.com"))),
        nickname = EditableNickname(nickname = "anna"),
        allGroups = WidthPreviewGroups,
        selectedGroupIds = setOf(1),
        organization = EditableOrganization(company = "ACME", title = "Staff Engineer"),
        birthday = EditableBirthday(value = Birthday(day = 21, month = 7, year = 1990)),
        note = EditableNote(note = "Met at DevConf"),
    )

@Composable
private fun WidthPreview(content: @Composable () -> Unit) {
    ContactsAppTheme(darkTheme = false, dynamicColor = false) {
        content()
    }
}

@Preview(name = "List 320", widthDp = 320, heightDp = 640, fontScale = 2f, showBackground = true)
@Preview(name = "List 600", widthDp = 600, heightDp = 800, showBackground = true)
@Preview(name = "List 840", widthDp = 840, heightDp = 900, showBackground = true)
@Composable
internal fun ContactListWidthPreview() {
    WidthPreview { ContactListContent() }
}

@Preview(name = "Details 320", widthDp = 320, heightDp = 640, fontScale = 2f, showBackground = true)
@Preview(name = "Details 600", widthDp = 600, heightDp = 800, showBackground = true)
@Preview(name = "Details 840", widthDp = 840, heightDp = 900, showBackground = true)
@Composable
internal fun DetailsWidthPreview() {
    WidthPreview {
        DetailsContent(
            details = WidthPreviewDetails,
            isFavorite = true,
            listState = rememberLazyListState(),
            onCallClick = {},
            onSmsClick = {},
            onEmailClick = {},
            onCopyClick = {},
            onToggleFavorite = {},
            onHeroNameBottom = {},
        )
    }
}

@Preview(name = "Editor 320", widthDp = 320, heightDp = 640, fontScale = 2f, showBackground = true)
@Preview(name = "Editor 600", widthDp = 600, heightDp = 800, showBackground = true)
@Preview(name = "Editor 840", widthDp = 840, heightDp = 900, showBackground = true)
@Composable
internal fun EditorWidthPreview() {
    WidthPreview {
        val state = WidthPreviewEditorState
        val edit: ((EditorUiState) -> EditorUiState) -> Unit = {}
        val indices = FormItemIndices()
        EditorBody(
            twoColumns = rememberWindowWidthClass() == WindowWidthClass.EXPANDED,
            left = {
                nameItem(state, edit, indices)
                nicknameItem(state, edit, indices)
                groupsItem(state, indices, onGroupsClick = {})
                editorContactItems(state, edit, indices) { FocusRequester() }
            },
            right = {
                editorExtraItems(
                    uiState = state,
                    edit = edit,
                    itemIndices = indices,
                    focusFor = { FocusRequester() },
                    onBirthdayClick = {},
                    onEventDateClick = {},
                    onBirthdayAddYear = {},
                    onEventAddYear = {},
                )
            },
        )
    }
}

@Preview(name = "Settings 320", widthDp = 320, heightDp = 640, fontScale = 2f, showBackground = true)
@Preview(name = "Settings 600", widthDp = 600, heightDp = 800, showBackground = true)
@Preview(name = "Settings 840", widthDp = 840, heightDp = 900, showBackground = true)
@Composable
internal fun SettingsWidthPreview() {
    WidthPreview {
        ReadingColumn {
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = LocalSpacing.current.s),
            ) {
                SettingsSection(title = stringResource(R.string.settings_theme)) {
                    SettingsOption("System", selected = false, onSelect = {}, testTag = "preview_theme_system")
                    SettingsOption("Light", selected = true, onSelect = {}, testTag = "preview_theme_light")
                    SettingsOption("Dark", selected = false, onSelect = {}, testTag = "preview_theme_dark")
                }
                SettingsSection(title = stringResource(R.string.settings_privacy)) {
                    SettingsSwitch(
                        label = stringResource(R.string.settings_secure_window),
                        description = stringResource(R.string.settings_secure_window_desc),
                        checked = true,
                        onCheckedChange = {},
                        testTag = "preview_secure_window",
                    )
                }
            }
        }
    }
}

@Preview(name = "Group picker 320", widthDp = 320, heightDp = 640, fontScale = 2f, showBackground = true)
@Preview(name = "Group picker 840", widthDp = 840, heightDp = 900, showBackground = true)
@Composable
internal fun GroupPickerWidthPreview() {
    WidthPreview {
        GroupPickerSheet(
            title = stringResource(R.string.editor_section_groups),
            groups = WidthPreviewGroups,
            selectedIds = setOf(1),
            confirmLabel = stringResource(R.string.action_done),
            onToggle = {},
            onCreateGroup = {},
            onConfirm = {},
            onDismiss = {},
        )
    }
}
