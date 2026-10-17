package com.m4isper.contactsapp.presentation.common

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import com.m4isper.contactsapp.R
import com.m4isper.contactsapp.domain.model.AddressKind
import com.m4isper.contactsapp.domain.model.ContactDetails
import com.m4isper.contactsapp.domain.model.EmailKind
import com.m4isper.contactsapp.domain.model.ImProtocol
import com.m4isper.contactsapp.domain.model.InstantMessage
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

internal fun vcardEmailType(kind: EmailKind): String =
    when (kind) {
        EmailKind.HOME -> "HOME"
        EmailKind.WORK -> "WORK"
        EmailKind.OTHER -> "OTHER"
    }

internal fun vcardAddressType(kind: AddressKind): String =
    when (kind) {
        AddressKind.HOME -> "HOME"
        AddressKind.WORK -> "WORK"
        AddressKind.OTHER -> "OTHER"
    }

/** Имена сервисов, которые понимает vCard-импортёр Android (VCardUtils). */
internal fun vcardImService(im: InstantMessage): String =
    when (im.protocol) {
        ImProtocol.AIM -> "AIM"
        ImProtocol.MSN -> "MSN"
        ImProtocol.YAHOO -> "Yahoo"
        ImProtocol.SKYPE -> "Skype"
        ImProtocol.QQ -> "QQ"
        ImProtocol.GOOGLE_TALK -> "Google Talk"
        ImProtocol.ICQ -> "ICQ"
        ImProtocol.JABBER -> "Jabber"
        ImProtocol.CUSTOM -> im.customProtocol?.takeIf { it.isNotBlank() } ?: "Custom"
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
        details.nameParts?.let { parts ->
            appendLine(
                "N:${escapeVCard(parts.family)};${escapeVCard(parts.given)};" +
                    "${escapeVCard(parts.middle)};${escapeVCard(parts.prefix)};${escapeVCard(parts.suffix)}",
            )
        }
        details.phones.forEach { phone ->
            appendLine("TEL;TYPE=${vcardType(phone.kind)}:${phone.number}")
        }
        details.emails.forEach { email ->
            appendLine("EMAIL;TYPE=${vcardEmailType(email.kind)}:${email.address}")
        }
        details.instantMessages.forEach { im ->
            appendLine("IMPP;X-SERVICE-TYPE=${vcardImService(im)}:${escapeVCard(im.address)}")
        }
        details.addresses.forEach { address ->
            appendLine("ADR;TYPE=${vcardAddressType(address.kind)}:;;${escapeVCard(address.address)};;;;")
        }
        details.websites.forEach { website ->
            appendLine("URL:${website.url}")
        }
        details.organization?.let { organization ->
            if (organization.company.isNotBlank()) {
                appendLine("ORG:${escapeVCard(organization.company)}")
            }
            if (organization.title.isNotBlank()) {
                appendLine("TITLE:${escapeVCard(organization.title)}")
            }
        }
        details.note?.let { note -> appendLine("NOTE:${escapeVCard(note)}") }
        details.birthday?.let { birthday -> appendLine("BDAY:${birthday.toStorageString()}") }
        appendLine("END:VCARD")
    }

/** Склеенный поток из нескольких vCard: валидный формат для файла .vcf с несколькими контактами. */
internal fun buildVCards(contacts: List<ContactDetails>): String =
    contacts.joinToString(separator = "") { buildVCard(it) }

internal fun shareContact(
    context: Context,
    details: ContactDetails,
) = shareContacts(context, listOf(details))

internal fun shareContacts(
    context: Context,
    contacts: List<ContactDetails>,
) {
    if (contacts.isEmpty()) {
        showShareFailed(context)
        return
    }
    runCatching {
        // Одно фиксированное имя: повторные шаринги перезаписывают файл, кэш не растёт.
        val file = File(context.cacheDir, "shared_contacts.vcf")
        file.writeText(buildVCards(contacts))
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
