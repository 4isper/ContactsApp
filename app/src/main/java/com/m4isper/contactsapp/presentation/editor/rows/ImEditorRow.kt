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
import com.m4isper.contactsapp.domain.model.contact.EditableInstantMessage
import com.m4isper.contactsapp.domain.model.contact.ImProtocol
import com.m4isper.contactsapp.presentation.common.format.imProtocolLabel
import com.m4isper.contactsapp.presentation.editor.EditorRow
import com.m4isper.contactsapp.presentation.editor.components.CompactFieldLabel
import com.m4isper.contactsapp.presentation.editor.components.CompactFieldRow
import com.m4isper.contactsapp.presentation.editor.components.DeleteFieldButton
import com.m4isper.contactsapp.presentation.editor.components.FieldRowMenu
import com.m4isper.contactsapp.presentation.editor.components.KindMenu
import com.m4isper.contactsapp.ui.theme.LocalSpacing

@Composable
internal fun ImEditorRow(
    row: EditorRow<EditableInstantMessage>,
    onImChange: (Long, String) -> Unit,
    onProtocolChange: (Long, ImProtocol) -> Unit,
    onCustomProtocolChange: (Long, String) -> Unit,
    onRemove: (Long) -> Unit,
) {
    val spacing = LocalSpacing.current
    val deleteLabel = stringResource(R.string.action_delete_im)
    CompactFieldRow(
        testTag = "editor_im_row_${row.rowId}",
        kind = {
            KindMenu(
                current = row.value.protocol,
                options = ImProtocol.entries,
                label = { imProtocolLabel(it) },
                onSelect = { onProtocolChange(row.rowId, it) },
            )
        },
        value = { fieldModifier ->
            OutlinedTextField(
                value = row.value.address,
                onValueChange = { onImChange(row.rowId, it) },
                label = { CompactFieldLabel(R.string.editor_im_label) },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                keyboardOptions =
                    KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next,
                    ),
                modifier =
                    fieldModifier
                        .testTag("editor_im_field"),
            )
        },
        trailing = {
            if (row.value.address.isBlank()) {
                DeleteFieldButton(
                    testTag = "editor_im_delete_${row.rowId}",
                    label = deleteLabel,
                    onClick = { onRemove(row.rowId) },
                )
            } else {
                FieldRowMenu(
                    prefix = "im",
                    rowId = row.rowId,
                    deleteLabel = deleteLabel,
                    onRemove = { onRemove(row.rowId) },
                )
            }
        },
    ) {
        if (row.value.protocol == ImProtocol.CUSTOM) {
            Spacer(modifier = Modifier.height(spacing.xs))
            OutlinedTextField(
                value = row.value.customProtocol.orEmpty(),
                onValueChange = { onCustomProtocolChange(row.rowId, it) },
                label = { Text(stringResource(R.string.editor_im_service)) },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                keyboardOptions =
                    KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Done,
                    ),
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .testTag("editor_im_service_field"),
            )
        }
    }
}
