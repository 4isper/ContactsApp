package com.m4isper.contactsapp.presentation.navigation

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract

/**
 * Режим выбора контакта: программа ждёт URI контакта. Отдаём lookup-URI,
 * а если его нет — обычный URI с id, который тоже читается провайдером.
 */
internal fun Context.pickContactResult(contactId: Long) {
    val activity = this as? Activity ?: return
    activity.setResult(Activity.RESULT_OK, Intent().setData(contactUriOf(contactId)))
    activity.finish()
}

/** URI контакта для результата выбора. */
private fun Context.contactUriOf(contactId: Long): Uri {
    val lookupKey =
        contentResolver
            .query(
                ContactsContract.Contacts.CONTENT_URI,
                arrayOf(ContactsContract.Contacts.LOOKUP_KEY),
                "${ContactsContract.Contacts._ID}=?",
                arrayOf(contactId.toString()),
                null,
            )?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
    return lookupKey
        ?.let { ContactsContract.Contacts.getLookupUri(contactId, it) }
        ?: Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, contactId.toString())
}
