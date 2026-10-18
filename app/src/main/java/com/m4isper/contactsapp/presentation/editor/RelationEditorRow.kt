package com.m4isper.contactsapp.presentation.editor

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.m4isper.contactsapp.R
import com.m4isper.contactsapp.domain.model.EditableRelation
import com.m4isper.contactsapp.domain.model.RelationKind
import com.m4isper.contactsapp.presentation.common.format.relationKindLabel
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
                    value = row.value.name,
                    onValueChange = { onNameChange(row.rowId, it) },
                    label = { Text(stringResource(R.string.editor_relation_label)) },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    keyboardOptions =
                        KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Next,
                        ),
                    modifier =
                        Modifier
                            .weight(1f)
                            .testTag("editor_relation_field"),
                )
                KindMenu(
                    current = row.value.kind,
                    options = RelationKind.entries,
                    label = { relationKindLabel(it) },
                    onSelect = { onKindChange(row.rowId, it) },
                )
                IconButton(onClick = { onRemove(row.rowId) }) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = stringResource(R.string.action_delete),
                    )
                }
            }
            if (row.value.kind == RelationKind.OTHER) {
                Spacer(modifier = Modifier.height(spacing.s))
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
}
