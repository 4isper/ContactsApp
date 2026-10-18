package com.m4isper.contactsapp.presentation.common

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import com.m4isper.contactsapp.R
import java.io.File

private const val SHARE_TAG = "ContactsShare"

/** Шарит один контакт: vCard-текст собирает слой данных, сюда приходит готовым. */
internal fun shareContact(
    context: Context,
    vCardText: String,
) = shareContacts(context, vCardText)

internal fun shareContacts(
    context: Context,
    vCardText: String,
) {
    if (vCardText.isBlank()) {
        showShareFailed(context)
        return
    }
    runCatching {
        // Одно фиксированное имя: повторные шаринги перезаписывают файл, кэш не растёт.
        val file = File(context.cacheDir, "shared_contacts.vcf")
        file.writeText(vCardText)
        val uri =
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file,
            )
        val sendIntent =
            Intent(Intent.ACTION_SEND).apply {
                type = "text/vcard"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        context.startActivity(
            Intent
                .createChooser(sendIntent, context.getString(R.string.share_contact))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }.onFailure { e ->
        logw(SHARE_TAG, "shareContacts failed", e)
        showShareFailed(context)
    }
}

private fun showShareFailed(context: Context) {
    Toast
        .makeText(
            context,
            context.getString(R.string.share_failed),
            Toast.LENGTH_SHORT,
        ).show()
}
