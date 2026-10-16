package com.m4isper.contactsapp.presentation.common

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import com.m4isper.contactsapp.R
import com.m4isper.contactsapp.domain.model.ContactDetails
import com.m4isper.contactsapp.domain.model.PhoneKind
import java.io.File

private const val SHARE_TAG = "ContactsShare"

internal fun vcardType(kind: PhoneKind): String =
    when (kind) {
        PhoneKind.MOBILE -> "CELL"
        PhoneKind.HOME -> "HOME"
        PhoneKind.WORK -> "WORK"
        PhoneKind.OTHER -> "VOICE"
    }

private fun escapeVCard(value: String): String =
    value
        .replace("\\", "\\\\")
        .replace(";", "\\;")
        .replace(",", "\\,")
        .replace("\n", "\\n")

internal fun buildVCard(details: ContactDetails): String =
    buildString {
        appendLine("BEGIN:VCARD")
        appendLine("VERSION:3.0")
        appendLine("FN:${escapeVCard(details.name)}")
        details.phones.forEach { phone ->
            appendLine("TEL;TYPE=${vcardType(phone.kind)}:${phone.number}")
        }
        appendLine("END:VCARD")
    }

internal fun shareContact(
    context: Context,
    details: ContactDetails,
) {
    runCatching {
        // Одно фиксированное имя: повторные шаринги перезаписывают файл, кэш не растёт.
        val file = File(context.cacheDir, "shared_contact.vcf")
        file.writeText(buildVCard(details))
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
        logw(SHARE_TAG, "shareContact failed", e)
        Toast
            .makeText(
                context,
                context.getString(R.string.share_failed),
                Toast.LENGTH_SHORT,
            ).show()
    }
}
