package com.m4isper.contactsapp.presentation.groups

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.m4isper.contactsapp.R
import com.m4isper.contactsapp.domain.contacts.filterContacts
import com.m4isper.contactsapp.domain.model.contact.ContactDetails
import com.m4isper.contactsapp.ui.components.AppBottomSheet
import com.m4isper.contactsapp.ui.components.SearchField
import com.m4isper.contactsapp.ui.theme.LocalSpacing

/**
 * Выбор контактов для добавления в группу: поиск и отметки.
 * [excludedIds] убирает уже состоящих в группе: добавлять их заново нельзя.
 */
@Composable
internal fun ContactPickerSheet(
    contacts: List<ContactDetails>,
    onConfirm: (List<Long>) -> Unit,
    onDismiss: () -> Unit,
    excludedIds: Set<Long> = emptySet(),
) {
    val spacing = LocalSpacing.current
    var query by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf(emptySet<Long>()) }
    val eligible = remember(contacts, excludedIds) { contacts.filterNot { it.contactId in excludedIds } }
    val visible = remember(eligible, query) { filterContacts(eligible, query) }

    AppBottomSheet(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("contact_picker_dialog"),
    ) {
        Text(
            text = stringResource(R.string.group_add_members),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(top = spacing.s, bottom = spacing.xs),
        )
        SearchField(
            value = query,
            onValueChange = { query = it },
        )
        val emptyMessage =
            when {
                contacts.isEmpty() -> stringResource(R.string.picker_no_contacts)
                eligible.isEmpty() -> stringResource(R.string.picker_all_members)
                visible.isEmpty() -> stringResource(R.string.contacts_search_empty, query)
                else -> null
            }
        if (emptyMessage != null) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .weight(1f),
            ) {
                PickerMessage(text = emptyMessage)
                if (contacts.isNotEmpty() && eligible.isNotEmpty()) {
                    TextButton(
                        onClick = { query = "" },
                        modifier = Modifier.padding(top = spacing.xs),
                    ) {
                        Text(stringResource(R.string.action_clear_search))
                    }
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(vertical = spacing.xs),
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .weight(1f)
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
        Row(
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(top = spacing.s),
        ) {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
            TextButton(
                onClick = { onConfirm(selected.toList()) },
                enabled = selected.isNotEmpty(),
                modifier = Modifier.testTag("contact_picker_confirm"),
            ) {
                Text(
                    text =
                        if (selected.isEmpty()) {
                            stringResource(R.string.group_add_members_action)
                        } else {
                            pluralStringResource(
                                R.plurals.group_add_members_count,
                                selected.size,
                                selected.size,
                            )
                        },
                )
            }
        }
    }
}

/** Текст пустого состояния пикера: без списка действий. */
@Composable
private fun PickerMessage(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier =
            Modifier
                .padding(vertical = 24.dp)
                .testTag("contact_picker_empty"),
    )
}
