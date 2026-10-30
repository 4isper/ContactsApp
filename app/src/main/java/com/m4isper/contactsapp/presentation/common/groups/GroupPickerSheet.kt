package com.m4isper.contactsapp.presentation.common.groups

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.m4isper.contactsapp.R
import com.m4isper.contactsapp.domain.model.contact.ContactGroup
import com.m4isper.contactsapp.presentation.common.account.contactAccountLabel
import com.m4isper.contactsapp.ui.components.AppBottomSheet
import com.m4isper.contactsapp.ui.theme.LocalSpacing

/**
 * Мультивыбор групп с созданием новой прямо в листе.
 * Лист управляет только отображением; выбор и создание идут через колбэки.
 */
@Composable
internal fun GroupPickerSheet(
    title: String,
    groups: List<ContactGroup>,
    selectedIds: Set<Long>,
    confirmLabel: String,
    onToggle: (Long) -> Unit,
    onCreateGroup: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalSpacing.current
    var newGroupName by rememberSaveable { mutableStateOf("") }
    AppBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
        )
        if (groups.isEmpty()) {
            Text(
                text = stringResource(R.string.groups_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = spacing.m),
            )
        } else {
            Column(
                modifier =
                    Modifier
                        .padding(top = spacing.s)
                        .heightIn(max = 320.dp)
                        .verticalScroll(rememberScrollState())
                        .selectableGroup(),
            ) {
                groups.forEach { group ->
                    GroupCheckRow(
                        title = group.title,
                        accountLabel = contactAccountLabel(group.account),
                        checked = group.id in selectedIds,
                        onToggle = { onToggle(group.id) },
                    )
                }
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(top = spacing.m),
        ) {
            OutlinedTextField(
                value = newGroupName,
                onValueChange = { newGroupName = it },
                label = { Text(stringResource(R.string.groups_new)) },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                modifier =
                    Modifier
                        .weight(1f)
                        .testTag("new_group_field"),
            )
            IconButton(
                onClick = {
                    onCreateGroup(newGroupName.trim())
                    newGroupName = ""
                },
                enabled = newGroupName.isNotBlank(),
                modifier = Modifier.testTag("new_group_confirm"),
            ) {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = stringResource(R.string.action_create),
                )
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
                onClick = onConfirm,
                modifier = Modifier.testTag("group_picker_confirm"),
            ) {
                Text(confirmLabel)
            }
        }
    }
}

@Composable
private fun GroupCheckRow(
    title: String,
    accountLabel: String,
    checked: Boolean,
    onToggle: () -> Unit,
) {
    val spacing = LocalSpacing.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            Modifier
                .fillMaxWidth()
                .toggleable(
                    value = checked,
                    role = Role.Checkbox,
                    onValueChange = { onToggle() },
                ).padding(vertical = spacing.xs)
                .testTag("group_check_row_$title"),
    ) {
        Checkbox(checked = checked, onCheckedChange = null)
        Column(modifier = Modifier.padding(start = spacing.s)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = accountLabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
