package com.m4isper.contactsapp.presentation.groups

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.m4isper.contactsapp.R
import com.m4isper.contactsapp.domain.contacts.filterContacts
import com.m4isper.contactsapp.domain.model.ContactDetails
import com.m4isper.contactsapp.ui.components.SearchField
import com.m4isper.contactsapp.ui.theme.LocalSpacing

/**
 * Выбор контактов для добавления в группу: поиск и отметки.
 * Пустой список означает, что добавлять нечего.
 */
@Composable
internal fun ContactPickerDialog(
    contacts: List<ContactDetails>,
    onConfirm: (List<Long>) -> Unit,
    onDismiss: () -> Unit,
) {
    val spacing = LocalSpacing.current
    var query by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf(emptySet<Long>()) }
    val visible = remember(contacts, query) { filterContacts(contacts, query) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.group_add_members)) },
        text = {
            Column {
                SearchField(
                    value = query,
                    onValueChange = { query = it },
                )
                LazyColumn(
                    modifier =
                        Modifier
                            .heightIn(max = 320.dp)
                            .testTag("contact_picker_list"),
                ) {
                    items(visible, key = { it.contactId }) { contact ->
                        val checked = contact.contactId in selected
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .testTag("contact_picker_row_${contact.contactId}")
                                    .toggleable(
                                        value = checked,
                                        role = Role.Checkbox,
                                        onValueChange = { isChecked ->
                                            selected =
                                                if (isChecked) {
                                                    selected + contact.contactId
                                                } else {
                                                    selected - contact.contactId
                                                }
                                        },
                                    ).padding(horizontal = spacing.m, vertical = spacing.xs),
                        ) {
                            Checkbox(
                                checked = checked,
                                onCheckedChange = null,
                            )
                            Text(
                                text = contact.name,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(start = spacing.m),
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(selected.toList()) },
                enabled = selected.isNotEmpty(),
                modifier = Modifier.testTag("contact_picker_confirm"),
            ) {
                Text(stringResource(R.string.group_add_members_action))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
        modifier = Modifier.testTag("contact_picker_dialog"),
    )
}
