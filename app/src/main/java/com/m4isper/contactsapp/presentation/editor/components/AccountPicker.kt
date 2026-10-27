package com.m4isper.contactsapp.presentation.editor.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.m4isper.contactsapp.R
import com.m4isper.contactsapp.domain.model.contact.ContactAccount
import com.m4isper.contactsapp.presentation.common.account.contactAccountSubtitle
import com.m4isper.contactsapp.presentation.common.account.contactAccountTitle
import com.m4isper.contactsapp.presentation.common.account.testKey
import com.m4isper.contactsapp.ui.theme.LocalSpacing

/** Строка аккаунта: интерактивная для нового контакта, только для чтения у существующего.
 * [isError] подсвечивает строку, если сохранение в этот аккаунт заблокировано. */
@Composable
internal fun AccountRow(
    account: ContactAccount?,
    onClick: (() -> Unit)?,
    isError: Boolean = false,
) {
    val spacing = LocalSpacing.current
    val selected = account ?: ContactAccount.Local
    val clickModifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    Card(
        colors =
            CardDefaults.cardColors(
                containerColor =
                    if (isError) {
                        MaterialTheme.colorScheme.errorContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerLow
                    },
            ),
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.m, vertical = spacing.xs)
                .then(clickModifier)
                .testTag("editor_account_row"),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(spacing.m),
        ) {
            Icon(
                Icons.Filled.AccountCircle,
                contentDescription = null,
                tint =
                    if (isError) {
                        MaterialTheme.colorScheme.onErrorContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
            )
            Column(
                modifier =
                    Modifier
                        .weight(1f)
                        .padding(horizontal = spacing.s),
            ) {
                Text(
                    text = contactAccountTitle(selected),
                    style = MaterialTheme.typography.bodyLarge,
                    color =
                        if (isError) {
                            MaterialTheme.colorScheme.onErrorContainer
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                )
                Text(
                    text = contactAccountSubtitle(selected),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (onClick != null) {
                Icon(
                    Icons.Filled.Edit,
                    contentDescription = stringResource(R.string.editor_account_choose),
                    tint =
                        if (isError) {
                            MaterialTheme.colorScheme.onErrorContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                )
            }
        }
    }
}

/** Диалог выбора: локальное хранение или один из синхронизируемых аккаунтов. */
@Composable
internal fun AccountPickerDialog(
    accounts: List<ContactAccount>,
    selected: ContactAccount?,
    onSelect: (ContactAccount) -> Unit,
    onDismiss: () -> Unit,
) {
    val spacing = LocalSpacing.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.editor_account_title)) },
        text = {
            Column(
                modifier =
                    Modifier
                        .selectableGroup()
                        .heightIn(max = 420.dp)
                        .verticalScroll(rememberScrollState()),
            ) {
                accounts.forEach { account ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = spacing.s)
                                .testTag("account_option_${account.testKey()}")
                                .selectable(
                                    selected = account == selected,
                                    role = Role.RadioButton,
                                    onClick = { onSelect(account) },
                                ),
                    ) {
                        RadioButton(
                            selected = account == selected,
                            onClick = null,
                        )
                        Column(
                            modifier = Modifier.padding(start = spacing.s),
                        ) {
                            Text(
                                text = contactAccountTitle(account),
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            Text(
                                text = contactAccountSubtitle(account),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
        modifier = Modifier.testTag("account_picker_dialog"),
    )
}
