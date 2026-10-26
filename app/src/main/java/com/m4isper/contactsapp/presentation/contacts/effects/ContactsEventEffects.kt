package com.m4isper.contactsapp.presentation.contacts.effects

import android.content.Context
import android.net.Uri
import android.widget.Toast
import com.m4isper.contactsapp.R
import com.m4isper.contactsapp.presentation.common.share.shareContacts
import com.m4isper.contactsapp.presentation.contacts.ContactsEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Обрабатывает одноразовые события списка: шаринг, тосты и запись экспортированного .vcf. */
internal suspend fun handleContactsEvent(
    context: Context,
    event: ContactsEvent,
    exportUri: Uri?,
    onEventHandled: () -> Unit,
) {
    val toast = { resId: Int -> Toast.makeText(context, resId, Toast.LENGTH_SHORT).show() }
    when (event) {
        is ContactsEvent.Share -> shareContacts(context, event.content)
        ContactsEvent.DeleteFailed -> toast(R.string.batch_delete_failed)
        is ContactsEvent.GroupAddFinished,
        ContactsEvent.GroupActionFailed,
        -> handleGroupEvent(context, event, toast)
        is ContactsEvent.ExportReady -> {
            if (exportUri != null && writeVCardFile(context, exportUri, event.content)) {
                showPluralToast(context, R.plurals.export_success, event.count)
            } else {
                toast(R.string.export_failed)
            }
        }
        ContactsEvent.ExportEmpty -> toast(R.string.export_empty)
        ContactsEvent.ExportFailed -> toast(R.string.export_failed)
        is ContactsEvent.SyncRetryFinished ->
            toast(
                if (event.success) {
                    R.string.account_sync_retry_success
                } else {
                    R.string.account_sync_retry_failed
                },
            )
    }
    onEventHandled()
}

/** События добавления в группу: тост об успехе или ошибке. */
private fun handleGroupEvent(
    context: Context,
    event: ContactsEvent,
    toast: (Int) -> Unit,
) {
    when (event) {
        is ContactsEvent.GroupAddFinished -> showPluralToast(context, R.plurals.group_add_success, event.count)
        // Вызывается только для событий групп, здесь остаётся GroupActionFailed.
        else -> toast(R.string.groups_action_failed)
    }
}

private fun showPluralToast(
    context: Context,
    resId: Int,
    count: Int,
) {
    val message = context.resources.getQuantityString(resId, count, count)
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
}

private suspend fun writeVCardFile(
    context: Context,
    uri: Uri,
    content: String,
): Boolean =
    runCatching {
        withContext(Dispatchers.IO) {
            val stream = context.contentResolver.openOutputStream(uri)
            if (stream == null) error("no output stream for $uri")
            stream.use { it.write(content.toByteArray()) }
        }
    }.isSuccess
