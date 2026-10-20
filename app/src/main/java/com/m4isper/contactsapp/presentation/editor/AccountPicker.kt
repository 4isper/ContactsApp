package com.m4isper.contactsapp.presentation.editor

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
import com.m4isper.contactsapp.domain.model.ContactAccount
import com.m4isper.contactsapp.ui.theme.LocalSpacing

/** Строка выбора аккаунта для нового контакта. */
@Composable
internal fun AccountRow(
    account: ContactAccount?,
    onClick: () -> Unit,
) {
    val spacing = LocalSpacing.current
    val selected = account ?: ContactAccount.Local
    Card(
        onClick = onClick,
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ),
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.m, vertical = spacing.xs)
                .testTag("editor_account_row"),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(spacing.m),
        ) {
            Icon(
                Icons.Filled.AccountCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Column(
                modifier =
                    Modifier
                        .weight(1f)
                        .padding(horizontal = spacing.s),
            ) {
                Text(
                    text = accountTitle(selected),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = accountSubtitle(selected),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                Icons.Filled.Edit,
                contentDescription = stringResource(R.string.editor_account_choose),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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
                                text = accountTitle(account),
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            Text(
                                text = accountSubtitle(account),
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

@Composable
private fun accountTitle(account: ContactAccount): String =
    if (account.isLocal) {
        stringResource(R.string.editor_account_local)
    } else {
        account.name.orEmpty()
    }

@Composable
private fun accountSubtitle(account: ContactAccount): String =
    when {
        account.isLocal -> stringResource(R.string.editor_account_local_hint)
        account.type?.contains("google", ignoreCase = true) == true -> stringResource(R.string.editor_account_google)
        account.type?.contains("exchange", ignoreCase = true) == true ->
            stringResource(R.string.editor_account_exchange)
        else -> account.type.orEmpty()
    }

/** Стабильный test tag: локальный аккаунт и пара type/name. */
private fun ContactAccount.testKey(): String = if (isLocal) "local" else "${type.orEmpty()}/${name.orEmpty()}"
