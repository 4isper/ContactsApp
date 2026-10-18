package com.m4isper.contactsapp.data.datasource.write

import android.content.ContentValues
import android.provider.ContactsContract.CommonDataKinds.Email
import android.provider.ContactsContract.CommonDataKinds.Event
import android.provider.ContactsContract.CommonDataKinds.Note
import android.provider.ContactsContract.CommonDataKinds.Organization
import android.provider.ContactsContract.CommonDataKinds.Phone
import android.provider.ContactsContract.CommonDataKinds.Photo
import android.provider.ContactsContract.CommonDataKinds.StructuredPostal
import android.provider.ContactsContract.CommonDataKinds.Website
import com.m4isper.contactsapp.domain.model.AddressKind
import com.m4isper.contactsapp.domain.model.Birthday
import com.m4isper.contactsapp.domain.model.EditableAddress
import com.m4isper.contactsapp.domain.model.EditableEmail
import com.m4isper.contactsapp.domain.model.EditableOrganization
import com.m4isper.contactsapp.domain.model.EditablePhone
import com.m4isper.contactsapp.domain.model.EditableWebsite
import com.m4isper.contactsapp.domain.model.EmailKind
import com.m4isper.contactsapp.domain.model.PhoneKind

internal fun phoneTypeInt(phone: EditablePhone): Int =
    if (phone.kind == PhoneKind.OTHER && !phone.customLabel.isNullOrBlank()) {
        Phone.TYPE_CUSTOM
    } else {
        when (phone.kind) {
            PhoneKind.MOBILE -> Phone.TYPE_MOBILE
            PhoneKind.HOME -> Phone.TYPE_HOME
            PhoneKind.WORK -> Phone.TYPE_WORK
            PhoneKind.OTHER -> Phone.TYPE_OTHER
        }
    }

internal fun emailTypeInt(email: EditableEmail): Int =
    if (email.kind == EmailKind.OTHER && !email.customLabel.isNullOrBlank()) {
        Email.TYPE_CUSTOM
    } else {
        when (email.kind) {
            EmailKind.HOME -> Email.TYPE_HOME
            EmailKind.WORK -> Email.TYPE_WORK
            EmailKind.OTHER -> Email.TYPE_OTHER
        }
    }

internal fun addressTypeInt(address: EditableAddress): Int =
    if (address.kind == AddressKind.OTHER && !address.customLabel.isNullOrBlank()) {
        StructuredPostal.TYPE_CUSTOM
    } else {
        when (address.kind) {
            AddressKind.HOME -> StructuredPostal.TYPE_HOME
            AddressKind.WORK -> StructuredPostal.TYPE_WORK
            AddressKind.OTHER -> StructuredPostal.TYPE_OTHER
        }
    }

internal fun phoneValues(phone: EditablePhone): ContentValues =
    ContentValues().apply {
        put(Phone.NUMBER, phone.number)
        put(Phone.TYPE, phoneTypeInt(phone))
        put(Phone.LABEL, phone.customLabel.takeIf { phone.kind == PhoneKind.OTHER && !it.isNullOrBlank() })
    }

internal fun emailValues(email: EditableEmail): ContentValues =
    ContentValues().apply {
        put(Email.ADDRESS, email.address)
        put(Email.TYPE, emailTypeInt(email))
        put(Email.LABEL, email.customLabel.takeIf { email.kind == EmailKind.OTHER && !it.isNullOrBlank() })
    }

/**
 * Адрес пишем одной строкой в STREET и FORMATTED_ADDRESS,
 * а структурированные части очищаем, чтобы провайдер не подмешал старые City/Country.
 */
internal fun addressValues(address: EditableAddress): ContentValues =
    ContentValues().apply {
        put(StructuredPostal.STREET, address.address)
        put(StructuredPostal.FORMATTED_ADDRESS, address.address)
        put(StructuredPostal.TYPE, addressTypeInt(address))
        put(
            StructuredPostal.LABEL,
            address.customLabel.takeIf { address.kind == AddressKind.OTHER && !it.isNullOrBlank() },
        )
        putNull(StructuredPostal.POBOX)
        putNull(StructuredPostal.NEIGHBORHOOD)
        putNull(StructuredPostal.CITY)
        putNull(StructuredPostal.REGION)
        putNull(StructuredPostal.POSTCODE)
        putNull(StructuredPostal.COUNTRY)
    }

internal fun websiteValues(website: EditableWebsite): ContentValues =
    ContentValues().apply {
        put(Website.URL, website.url)
        put(Website.TYPE, Website.TYPE_HOMEPAGE)
    }

internal fun organizationValues(organization: EditableOrganization): ContentValues =
    ContentValues().apply {
        put(Organization.COMPANY, organization.company.takeIf { it.isNotBlank() })
        put(Organization.TITLE, organization.title.takeIf { it.isNotBlank() })
    }

internal fun noteValues(note: String): ContentValues =
    ContentValues().apply {
        put(Note.NOTE, note)
    }

internal fun eventValues(birthday: Birthday): ContentValues =
    ContentValues().apply {
        put(Event.START_DATE, birthday.toStorageString())
        put(Event.TYPE, Event.TYPE_BIRTHDAY)
    }

internal fun photoValues(photo: ByteArray): ContentValues =
    ContentValues().apply {
        put(Photo.PHOTO, photo)
    }
