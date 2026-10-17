package com.m4isper.contactsapp.data.datasource

import android.content.ContentValues
import android.provider.ContactsContract.CommonDataKinds.Email
import android.provider.ContactsContract.CommonDataKinds.Event
import android.provider.ContactsContract.CommonDataKinds.Im
import android.provider.ContactsContract.CommonDataKinds.Nickname
import android.provider.ContactsContract.CommonDataKinds.Note
import android.provider.ContactsContract.CommonDataKinds.Organization
import android.provider.ContactsContract.CommonDataKinds.Phone
import android.provider.ContactsContract.CommonDataKinds.Photo
import android.provider.ContactsContract.CommonDataKinds.Relation
import android.provider.ContactsContract.CommonDataKinds.StructuredPostal
import android.provider.ContactsContract.CommonDataKinds.Website
import com.m4isper.contactsapp.domain.model.ContactForm

/** Пары mimetype–значения для создания контакта: пустые строки пропускаются. */
internal fun contactDataValues(
    form: ContactForm,
    photo: ByteArray?,
): List<Pair<String, ContentValues>> = contactListValues(form) + contactSingleValues(form, photo)

private fun contactListValues(form: ContactForm): List<Pair<String, ContentValues>> =
    buildList {
        form.phones
            .filter { it.number.isNotBlank() }
            .forEach { add(Phone.CONTENT_ITEM_TYPE to phoneValues(it)) }
        form.emails
            .filter { it.address.isNotBlank() }
            .forEach { add(Email.CONTENT_ITEM_TYPE to emailValues(it)) }
        form.instantMessages
            .filter { it.address.isNotBlank() }
            .forEach { add(Im.CONTENT_ITEM_TYPE to instantMessageValues(it)) }
        form.addresses
            .filter { it.address.isNotBlank() }
            .forEach { add(StructuredPostal.CONTENT_ITEM_TYPE to addressValues(it)) }
        form.websites
            .filter { it.url.isNotBlank() }
            .forEach { add(Website.CONTENT_ITEM_TYPE to websiteValues(it)) }
        form.relations
            .filter { it.name.isNotBlank() }
            .forEach { add(Relation.CONTENT_ITEM_TYPE to relationValues(it)) }
        form.events
            .mapNotNull { event -> event.date?.let { date -> event to date } }
            .forEach { (event, date) -> add(Event.CONTENT_ITEM_TYPE to contactEventValues(event, date)) }
    }

private fun contactSingleValues(
    form: ContactForm,
    photo: ByteArray?,
): List<Pair<String, ContentValues>> =
    buildList {
        if (!form.organization.isEmpty) {
            add(Organization.CONTENT_ITEM_TYPE to organizationValues(form.organization))
        }
        if (form.note.note.isNotBlank()) {
            add(Note.CONTENT_ITEM_TYPE to noteValues(form.note.note))
        }
        form.birthday.value?.let { add(Event.CONTENT_ITEM_TYPE to eventValues(it)) }
        if (form.nickname.nickname.isNotBlank()) {
            add(Nickname.CONTENT_ITEM_TYPE to nicknameValues(form.nickname.nickname))
        }
        if (photo != null) add(Photo.CONTENT_ITEM_TYPE to photoValues(photo))
    }
