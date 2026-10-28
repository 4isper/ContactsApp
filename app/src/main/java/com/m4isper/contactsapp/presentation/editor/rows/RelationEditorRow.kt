package com.m4isper.contactsapp.presentation.editor.rows

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import com.m4isper.contactsapp.domain.model.contact.EditableRelation
import com.m4isper.contactsapp.domain.model.contact.RelationKind
import com.m4isper.contactsapp.presentation.common.format.relationKindLabel
import com.m4isper.contactsapp.presentation.editor.EditorRow
import com.m4isper.contactsapp.presentation.editor.components.CompactFieldLabel
import com.m4isper.contactsapp.presentation.editor.components.CompactFieldRow
import com.m4isper.contactsapp.presentation.editor.components.DeleteFieldButton
import com.m4isper.contactsapp.presentation.editor.components.FieldRowMenu
import com.m4isper.contactsapp.presentation.editor.components.KindMenu
import com.m4isper.contactsapp.ui.theme.LocalSpacing

@Composable
internal fun RelationEditorRow(
    row: EditorRow<EditableRelation>,
    onNameChange: (Long, String) -> Unit,
    onKindChange: (Long, RelationKind) -> Unit,
    onCustomLabelChange: (Long, String) -> Unit,
    onRemove: (Long) -> Unit,
) {
    val spacing = LocalSpacing.current
    val deleteLabel = stringResource(R.string.action_delete_relation)
    CompactFieldRow(
        testTag = "editor_relation_row_${row.rowId}",
        kind = {
            KindMenu(
                current = row.value.kind,
                options = RelationKind.entries,
                label = { relationKindLabel(it) },
                onSelect = { onKindChange(row.rowId, it) },
            )
        },
        value = { fieldModifier ->
            OutlinedTextField(
                value = row.value.name,
                onValueChange = { onNameChange(row.rowId, it) },
                label = { CompactFieldLabel(R.string.editor_relation_label) },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                keyboardOptions =
                    KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next,
                    ),
                modifier =
                    fieldModifier
                        .testTag("editor_relation_field"),
            )
        },
        trailing = {
            if (row.value.name.isBlank()) {
                DeleteFieldButton(
                    testTag = "editor_relation_delete_${row.rowId}",
                    label = deleteLabel,
                    onClick = { onRemove(row.rowId) },
                )
            } else {
                FieldRowMenu(
                    prefix = "relation",
                    rowId = row.rowId,
                    deleteLabel = deleteLabel,
                    onRemove = { onRemove(row.rowId) },
                )
            }
        },
    ) {
        if (row.value.kind == RelationKind.OTHER) {
            Spacer(modifier = Modifier.height(spacing.xs))
            OutlinedTextField(
                value = row.value.customLabel.orEmpty(),
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
