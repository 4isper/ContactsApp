package com.m4isper.contactsapp.domain.model

/** Группа (метка) контактов из таблицы [android.provider.ContactsContract.Groups]. */
data class ContactGroup(
    val id: Long,
    val title: String,
)
