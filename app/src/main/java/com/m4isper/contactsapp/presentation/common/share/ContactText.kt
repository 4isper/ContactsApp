package com.m4isper.contactsapp.presentation.common.share

import com.m4isper.contactsapp.domain.model.ContactDetails

/**
 * Контакт обычным текстом: имя, организация, телефоны и почта.
 * Такой формат понимают мессенджеры и заметки, в отличие от vCard.
 */
internal fun contactAsText(details: ContactDetails): String {
    val lines = mutableListOf<String>()
    lines += details.name
    details.organization?.let { organization ->
        val company = organization.company.trim()
        if (company.isNotEmpty()) lines += company
    }
    details.phones.forEach { lines += it.number }
    details.emails.forEach { lines += it.address }
    details.websites.forEach { lines += it.url }
    return lines.joinToString("\n")
}
