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
import com.m4isper.contactsapp.domain.model.contact.EditableWebsite
import com.m4isper.contactsapp.domain.model.contact.WebsiteKind
import com.m4isper.contactsapp.presentation.common.format.websiteKindLabel
import com.m4isper.contactsapp.presentation.editor.EditorRow
import com.m4isper.contactsapp.presentation.editor.components.CompactFieldLabel
import com.m4isper.contactsapp.presentation.editor.components.CompactFieldRow
import com.m4isper.contactsapp.presentation.editor.components.DeleteFieldButton
import com.m4isper.contactsapp.presentation.editor.components.FieldRowMenu
import com.m4isper.contactsapp.presentation.editor.components.KindMenu
import com.m4isper.contactsapp.ui.theme.LocalSpacing

@Composable
internal fun WebsiteEditorRow(
    row: EditorRow<EditableWebsite>,
    error: String? = null,
    focusRequester: FocusRequester? = null,
    onWebsiteChange: (Long, String) -> Unit,
    onKindChange: (Long, WebsiteKind) -> Unit,
    onCustomLabelChange: (Long, String) -> Unit,
    onRemove: (Long) -> Unit,
) {
    val spacing = LocalSpacing.current
    val deleteLabel = stringResource(R.string.action_delete_website)
    CompactFieldRow(
        testTag = "editor_website_row_${row.rowId}",
        kind = {
            KindMenu(
                current = row.value.kind,
                options = WebsiteKind.entries,
                label = { websiteKindLabel(it) },
                onSelect = { onKindChange(row.rowId, it) },
            )
        },
        value = { fieldModifier ->
            OutlinedTextField(
                value = row.value.url,
                onValueChange = { onWebsiteChange(row.rowId, it) },
                label = { CompactFieldLabel(R.string.editor_website_label) },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                keyboardOptions =
                    KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Next,
                    ),
                isError = error != null,
                supportingText = error?.let { { Text(it) } },
                modifier =
                    (focusRequester?.let { Modifier.focusRequester(it) } ?: Modifier)
                        .then(fieldModifier)
                        .testTag("editor_website_field"),
            )
        },
        trailing = {
            if (row.value.url.isBlank()) {
                DeleteFieldButton(
                    testTag = "editor_website_delete_${row.rowId}",
                    label = deleteLabel,
                    onClick = { onRemove(row.rowId) },
                )
            } else {
                FieldRowMenu(
                    prefix = "website",
                    rowId = row.rowId,
                    deleteLabel = deleteLabel,
                    onRemove = { onRemove(row.rowId) },
                )
            }
        },
    ) {
        if (row.value.kind == WebsiteKind.OTHER) {
            Spacer(modifier = Modifier.height(spacing.xs))
            OutlinedTextField(
                value = row.value.customLabel.orEmpty(),
                onValueChange = { onCustomLabelChange(row.rowId, it) },
                label = { Text(stringResource(R.string.editor_kind_label)) },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
