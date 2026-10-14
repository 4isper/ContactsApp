package com.m4isper.contactsapp

import android.content.Context
import android.provider.ContactsContract

data class Contact(
    val id: Long,
    val name: String,
    val phone: String,
)

private val CONTACT_PROJECTION = arrayOf(
    ContactsContract.CommonDataKinds.Phone._ID,
    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
    ContactsContract.CommonDataKinds.Phone.NUMBER,
)

private val CONTACT_SORT_ORDER =
    "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"

fun getContacts(context: Context): List<Contact> {
    val contacts = mutableListOf<Contact>()
    val resolver = context.contentResolver
    val cursor = resolver.query(
        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
        CONTACT_PROJECTION,
        null,
        null,
        CONTACT_SORT_ORDER,
    )

    cursor?.use {
        val idIndex =
            it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone._ID)
        val nameIndex =
            it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
        val phoneIndex =
            it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)

        while (it.moveToNext()) {
            val id = it.getLong(idIndex)
            val name = it.getString(nameIndex)?.trim().orEmpty()
            val phone = it.getString(phoneIndex)?.trim().orEmpty()

            if (name.isBlank() || phone.isBlank()) continue

            contacts.add(Contact(id = id, name = name, phone = phone))
        }
    }

    return contacts.distinctBy { it.id to it.phone }
}
