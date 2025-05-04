package com.m4isper.contactsapp

import android.content.Context
import android.provider.ContactsContract

data class Contact(
    val name: String,
    val phone: String,
)

fun getContacts(context: Context): List<Contact>{
    val contacts = mutableListOf<Contact>()
    val resolver = context.contentResolver
    val cursor = resolver.query(
        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
        null, null, null, null
    )

    cursor?.use {
        val nameIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
        val phoneIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

        while (it.moveToNext()) {
            val name = it.getString(nameIndex) ?: "No Name"
            val phone = it.getString(phoneIndex) ?: "No Phone"

            contacts.add(Contact(name, phone))
        }
    }

    return contacts
}