package com.m4isper.contactsapp.presentation.recents

import com.m4isper.contactsapp.domain.model.ContactDetails
import java.util.Locale

/**
 * Недавние контакты по данным устройства: звонки и сообщения из любых приложений,
 * а не только из этого. Сортировка — от самого свежего контакта.
 */
internal fun recentContacts(contacts: List<ContactDetails>): List<ContactDetails> =
    contacts
        .filter { (it.lastContacted ?: 0L) > 0L }
        .sortedWith(
            compareByDescending<ContactDetails> { it.lastContacted ?: 0L }
                .thenBy { it.name.lowercase(Locale.ROOT) },
        )
