package com.m4isper.contactsapp.presentation.common.account

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.m4isper.contactsapp.R
import com.m4isper.contactsapp.domain.model.contact.ContactAccount

/** Короткое имя аккаунта: локальное хранилище, почта или тип. */
@Composable
internal fun contactAccountTitle(account: ContactAccount): String =
    if (account.isLocal) {
        stringResource(R.string.editor_account_local)
    } else {
        account.name?.takeIf { it.isNotBlank() } ?: account.type.orEmpty()
    }

/** Пояснение к аккаунту: «хранится на устройстве», Google, Exchange или тип. */
@Composable
internal fun contactAccountSubtitle(account: ContactAccount): String =
    when {
        account.isLocal -> stringResource(R.string.editor_account_local_hint)
        account.type?.contains("google", ignoreCase = true) == true ->
            stringResource(R.string.editor_account_google)
        account.type?.contains("exchange", ignoreCase = true) == true ->
            stringResource(R.string.editor_account_exchange)
        else -> account.type.orEmpty()
    }

/** Стабильный ключ аккаунта для тестов: локальный и пара type/name. */
internal fun ContactAccount.testKey(): String = if (isLocal) "local" else "${type.orEmpty()}/${name.orEmpty()}"

/** Однострочная подпись для списков: «user@gmail.com (Google)» или «Локальный». */
@Composable
internal fun contactAccountLabel(account: ContactAccount): String {
    val title = contactAccountTitle(account)
    if (account.isLocal) return title
    val type = contactAccountSubtitle(account)
    return if (type.isBlank() || type == title) {
        title
    } else {
        stringResource(R.string.account_with_type, title, type)
    }
}
