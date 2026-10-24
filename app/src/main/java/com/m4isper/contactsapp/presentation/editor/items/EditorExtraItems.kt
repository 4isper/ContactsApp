package com.m4isper.contactsapp.presentation.editor.items

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.m4isper.contactsapp.R
import com.m4isper.contactsapp.domain.model.contact.FormField
import com.m4isper.contactsapp.presentation.editor.EditorUiState
import com.m4isper.contactsapp.presentation.editor.fieldMessage
import com.m4isper.contactsapp.presentation.editor.components.SectionHeader
import com.m4isper.contactsapp.presentation.editor.edits.withAddedEvent
import com.m4isper.contactsapp.presentation.editor.edits.withAddedRelation
import com.m4isper.contactsapp.presentation.editor.edits.withBirthday
import com.m4isper.contactsapp.presentation.editor.edits.withEventDate
import com.m4isper.contactsapp.presentation.editor.edits.withEventKind
import com.m4isper.contactsapp.presentation.editor.edits.withEventLabel
import com.m4isper.contactsapp.presentation.editor.edits.withNote
import com.m4isper.contactsapp.presentation.editor.edits.withOrganizationCompany
import com.m4isper.contactsapp.presentation.editor.edits.withOrganizationDepartment
import com.m4isper.contactsapp.presentation.editor.edits.withOrganizationJobDescription
import com.m4isper.contactsapp.presentation.editor.edits.withOrganizationOfficeLocation
import com.m4isper.contactsapp.presentation.editor.edits.withOrganizationTitle
import com.m4isper.contactsapp.presentation.editor.edits.withRelationKind
import com.m4isper.contactsapp.presentation.editor.edits.withRelationLabel
import com.m4isper.contactsapp.presentation.editor.edits.withRelationName
import com.m4isper.contactsapp.presentation.editor.edits.withoutEvent
import com.m4isper.contactsapp.presentation.editor.edits.withoutRelation
import com.m4isper.contactsapp.presentation.editor.rows.BirthdayEditorRow
import com.m4isper.contactsapp.presentation.editor.rows.EventEditorRow
import com.m4isper.contactsapp.presentation.editor.rows.RelationEditorRow
import com.m4isper.contactsapp.ui.theme.LocalSpacing

/** Организация, день рождения, события, связи и заметка. */
internal fun LazyListScope.editorExtraItems(
    uiState: EditorUiState,
    edit: ((EditorUiState) -> EditorUiState) -> Unit,
    onBirthdayClick: () -> Unit,
    onEventDateClick: (Long) -> Unit,
) {
    organizationItems(uiState, edit)
    birthdayItems(uiState, edit, onBirthdayClick)
    eventItems(uiState, edit, onEventDateClick)
    relationItems(uiState, edit)
    noteItems(uiState, edit)
}

private fun LazyListScope.organizationItems(
    uiState: EditorUiState,
    edit: ((EditorUiState) -> EditorUiState) -> Unit,
) {
    item(key = "organization-header") {
        SectionHeader(titleRes = R.string.editor_section_organization)
    }
    item(key = "organization") {
        OrganizationFields(uiState, edit)
    }
}

@Composable
private fun OrganizationFields(
    uiState: EditorUiState,
    edit: ((EditorUiState) -> EditorUiState) -> Unit,
) {
    val spacing = LocalSpacing.current
    Column(modifier = Modifier.padding(horizontal = spacing.m)) {
        OrganizationField(
            value = uiState.organization.company,
            onValueChange = { value -> edit { it.withOrganizationCompany(value) } },
            labelRes = R.string.editor_organization_company,
            testTag = "editor_company_field",
        )
        Spacer(modifier = Modifier.height(spacing.s))
        OrganizationField(
            value = uiState.organization.title,
            onValueChange = { value -> edit { it.withOrganizationTitle(value) } },
            labelRes = R.string.editor_organization_title,
            testTag = "editor_title_field",
        )
        Spacer(modifier = Modifier.height(spacing.s))
        OrganizationField(
            value = uiState.organization.department,
            onValueChange = { value -> edit { it.withOrganizationDepartment(value) } },
            labelRes = R.string.editor_organization_department,
            testTag = "editor_department_field",
        )
        Spacer(modifier = Modifier.height(spacing.s))
        OrganizationField(
            value = uiState.organization.jobDescription,
            onValueChange = { value -> edit { it.withOrganizationJobDescription(value) } },
            labelRes = R.string.editor_organization_job_description,
            testTag = "editor_job_description_field",
        )
        Spacer(modifier = Modifier.height(spacing.s))
        OrganizationField(
            value = uiState.organization.officeLocation,
            onValueChange = { value -> edit { it.withOrganizationOfficeLocation(value) } },
            labelRes = R.string.editor_organization_office_location,
            testTag = "editor_office_location_field",
        )
    }
}

@Composable
private fun OrganizationField(
    value: String,
    onValueChange: (String) -> Unit,
    labelRes: Int,
    testTag: String,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(stringResource(labelRes)) },
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
                .testTag(testTag),
    )
}

private fun LazyListScope.birthdayItems(
    uiState: EditorUiState,
    edit: ((EditorUiState) -> EditorUiState) -> Unit,
    onBirthdayClick: () -> Unit,
) {
    item(key = "birthday-header") {
        SectionHeader(titleRes = R.string.editor_section_birthday)
    }
    item(key = "birthday") {
        BirthdayEditorRow(
            birthday = uiState.birthday,
            error = uiState.fieldErrors[FormField.EVENT]?.fieldMessage(),
            onPickClick = onBirthdayClick,
            onBirthdayChange = { value -> edit { it.withBirthday(value) } },
        )
    }
}

private fun LazyListScope.eventItems(
    uiState: EditorUiState,
    edit: ((EditorUiState) -> EditorUiState) -> Unit,
    onEventDateClick: (Long) -> Unit,
) {
    item(key = "events-header") {
        SectionHeader(
            titleRes = R.string.editor_section_events,
            onAdd = { edit { it.withAddedEvent() } },
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
            error = uiState.fieldErrors[FormField.EVENT]?.fieldMessage(),
            onKindChange = { rowId, kind -> edit { it.withEventKind(rowId, kind) } },
            onDatePickClick = onEventDateClick,
            onDateChange = { rowId, date -> edit { it.withEventDate(rowId, date) } },
            onCustomLabelChange = { rowId, value -> edit { it.withEventLabel(rowId, value) } },
            onRemove = { rowId -> edit { it.withoutEvent(rowId) } },
        )
    }
}

private fun LazyListScope.relationItems(
    uiState: EditorUiState,
    edit: ((EditorUiState) -> EditorUiState) -> Unit,
) {
    item(key = "relations-header") {
        SectionHeader(
            titleRes = R.string.editor_section_relations,
            onAdd = { edit { it.withAddedRelation() } },
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
            onNameChange = { rowId, value -> edit { it.withRelationName(rowId, value) } },
            onKindChange = { rowId, kind -> edit { it.withRelationKind(rowId, kind) } },
            onCustomLabelChange = {
                rowId,
                value,
                ->
                edit { it.withRelationLabel(rowId, value) }
            },
            onRemove = { rowId -> edit { it.withoutRelation(rowId) } },
        )
    }
}

private fun LazyListScope.noteItems(
    uiState: EditorUiState,
    edit: ((EditorUiState) -> EditorUiState) -> Unit,
) {
    item(key = "note-header") {
        SectionHeader(titleRes = R.string.editor_section_note)
    }
    item(key = "note") {
        val spacing = LocalSpacing.current
        OutlinedTextField(
            value = uiState.note.note,
            onValueChange = { value -> edit { it.withNote(value) } },
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
}
