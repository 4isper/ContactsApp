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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.m4isper.contactsapp.R
import com.m4isper.contactsapp.domain.model.contact.EditableEmail
import com.m4isper.contactsapp.domain.model.contact.EmailKind
import com.m4isper.contactsapp.presentation.common.format.emailKindLabel
import com.m4isper.contactsapp.presentation.editor.EditorRow
import com.m4isper.contactsapp.presentation.editor.components.CompactFieldLabel
import com.m4isper.contactsapp.presentation.editor.components.CompactFieldRow
import com.m4isper.contactsapp.presentation.editor.components.DeleteFieldButton
import com.m4isper.contactsapp.presentation.editor.components.FieldRowMenu
import com.m4isper.contactsapp.presentation.editor.components.KindMenu
import com.m4isper.contactsapp.presentation.editor.components.ReorderHandle
import com.m4isper.contactsapp.ui.theme.LocalSpacing

@Composable
internal fun EmailEditorRow(
    row: EditorRow<EditableEmail>,
    error: String? = null,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    focusRequester: FocusRequester? = null,
    onEmailChange: (Long, String) -> Unit,
    onKindChange: (Long, EmailKind) -> Unit,
    onCustomLabelChange: (Long, String) -> Unit,
    onSetPrimary: (Long) -> Unit,
    onMoveUp: (Long) -> Unit,
    onMoveDown: (Long) -> Unit,
    onRemove: (Long) -> Unit,
) {
    val spacing = LocalSpacing.current
    val deleteLabel = stringResource(R.string.action_delete_email)
    CompactFieldRow(
        testTag = "editor_email_row_${row.rowId}",
        dragHandle = {
            ReorderHandle(
                testTag = "editor_email_drag_${row.rowId}",
                canMoveUp = canMoveUp,
                canMoveDown = canMoveDown,
                onMoveUp = { onMoveUp(row.rowId) },
                onMoveDown = { onMoveDown(row.rowId) },
            )
        },
        kind = {
            KindMenu(
                current = row.value.kind,
                options = EmailKind.entries,
                label = { emailKindLabel(it) },
                onSelect = { onKindChange(row.rowId, it) },
            )
        },
        value = { fieldModifier ->
            OutlinedTextField(
                value = row.value.address,
                onValueChange = { onEmailChange(row.rowId, it) },
                label = { CompactFieldLabel(R.string.editor_email_label) },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                keyboardOptions =
                    KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next,
                    ),
                isError = error != null,
                supportingText = error?.let { { Text(it) } },
                modifier =
                    (focusRequester?.let { Modifier.focusRequester(it) } ?: Modifier)
                        .then(fieldModifier)
                        .testTag("editor_email_field"),
            )
        },
        trailing = {
            if (row.value.address.isBlank()) {
                DeleteFieldButton(
                    testTag = "editor_email_delete_${row.rowId}",
                    label = deleteLabel,
                    onClick = { onRemove(row.rowId) },
                )
            } else {
                FieldRowMenu(
                    prefix = "email",
                    rowId = row.rowId,
                    isPrimary = row.value.isPrimary,
                    canMoveUp = canMoveUp,
                    canMoveDown = canMoveDown,
                    deleteLabel = deleteLabel,
                    onSetPrimary = { onSetPrimary(row.rowId) },
                    onMoveUp = { onMoveUp(row.rowId) },
                    onMoveDown = { onMoveDown(row.rowId) },
                    onRemove = { onRemove(row.rowId) },
                )
            }
        },
    ) {
        if (row.value.kind == EmailKind.OTHER) {
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
