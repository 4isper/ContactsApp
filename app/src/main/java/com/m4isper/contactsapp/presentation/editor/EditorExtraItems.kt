package com.m4isper.contactsapp.presentation.editor

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.m4isper.contactsapp.R
import com.m4isper.contactsapp.ui.theme.LocalSpacing

/** Организация, день рождения, события, связи и заметка. */
internal fun LazyListScope.editorExtraItems(
    uiState: EditorUiState,
    viewModel: ContactEditorViewModel,
    onBirthdayClick: () -> Unit,
    onEventDateClick: (Long) -> Unit,
) {
    organizationItems(uiState, viewModel)
    birthdayItems(uiState, viewModel, onBirthdayClick)
    eventItems(uiState, viewModel, onEventDateClick)
    relationItems(uiState, viewModel)
    noteItems(uiState, viewModel)
}

private fun LazyListScope.organizationItems(
    uiState: EditorUiState,
    viewModel: ContactEditorViewModel,
) {
    item(key = "organization-header") {
        SectionHeader(titleRes = R.string.editor_section_organization)
    }
    item(key = "organization") {
        val spacing = LocalSpacing.current
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
}

private fun LazyListScope.birthdayItems(
    uiState: EditorUiState,
    viewModel: ContactEditorViewModel,
    onBirthdayClick: () -> Unit,
) {
    item(key = "birthday-header") {
        SectionHeader(titleRes = R.string.editor_section_birthday)
    }
    item(key = "birthday") {
        BirthdayEditorRow(
            birthday = uiState.birthday,
            onPickClick = onBirthdayClick,
            onBirthdayChange = viewModel::onBirthdayChange,
        )
    }
}

private fun LazyListScope.eventItems(
    uiState: EditorUiState,
    viewModel: ContactEditorViewModel,
    onEventDateClick: (Long) -> Unit,
) {
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
            onDatePickClick = onEventDateClick,
            onDateChange = viewModel::onEventDateChange,
            onCustomLabelChange = viewModel::onEventCustomLabelChange,
            onRemove = viewModel::onRemoveEvent,
        )
    }
}

private fun LazyListScope.relationItems(
    uiState: EditorUiState,
    viewModel: ContactEditorViewModel,
) {
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
}

private fun LazyListScope.noteItems(
    uiState: EditorUiState,
    viewModel: ContactEditorViewModel,
) {
    item(key = "note-header") {
        SectionHeader(titleRes = R.string.editor_section_note)
    }
    item(key = "note") {
        val spacing = LocalSpacing.current
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
}
