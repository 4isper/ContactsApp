// Подавление осознанное: по одной маленькой функции на тип ContentValues.
@file:Suppress("TooManyFunctions")

package com.m4isper.contactsapp.data.datasource.write.contacts

import com.m4isper.contactsapp.data.datasource.ContactsContractLegacy

import android.content.ContentValues
import android.provider.ContactsContract
import android.provider.ContactsContract.CommonDataKinds.Email
import android.provider.ContactsContract.CommonDataKinds.Event
import android.provider.ContactsContract.CommonDataKinds.Note
import android.provider.ContactsContract.CommonDataKinds.Organization
import android.provider.ContactsContract.CommonDataKinds.Phone
import android.provider.ContactsContract.CommonDataKinds.Photo
import android.provider.ContactsContract.CommonDataKinds.SipAddress
import android.provider.ContactsContract.CommonDataKinds.StructuredPostal
import android.provider.ContactsContract.CommonDataKinds.Website
import com.m4isper.contactsapp.data.format.birthdayToString
import com.m4isper.contactsapp.domain.model.contact.AddressKind
import com.m4isper.contactsapp.domain.model.contact.Birthday
import com.m4isper.contactsapp.domain.model.contact.EditableAddress
import com.m4isper.contactsapp.domain.model.contact.EditableEmail
import com.m4isper.contactsapp.domain.model.contact.EditableOrganization
import com.m4isper.contactsapp.domain.model.contact.EditablePhone
import com.m4isper.contactsapp.domain.model.contact.EditableSipAddress
import com.m4isper.contactsapp.domain.model.contact.EditableWebsite
import com.m4isper.contactsapp.domain.model.contact.EmailKind
import com.m4isper.contactsapp.domain.model.contact.PhoneKind
import com.m4isper.contactsapp.domain.model.contact.SipKind
import com.m4isper.contactsapp.domain.model.contact.WebsiteKind

internal fun phoneTypeInt(phone: EditablePhone): Int =
    if (phone.kind == PhoneKind.OTHER && !phone.customLabel.isNullOrBlank()) {
        Phone.TYPE_CUSTOM
    } else {
        when (phone.kind) {
            PhoneKind.MOBILE -> Phone.TYPE_MOBILE
            PhoneKind.HOME -> Phone.TYPE_HOME
            PhoneKind.WORK -> Phone.TYPE_WORK
            PhoneKind.WORK_MOBILE -> Phone.TYPE_WORK_MOBILE
            PhoneKind.FAX -> Phone.TYPE_FAX_WORK
            PhoneKind.PAGER -> Phone.TYPE_PAGER
            PhoneKind.CALLBACK -> Phone.TYPE_CALLBACK
            PhoneKind.COMPANY_MAIN -> Phone.TYPE_COMPANY_MAIN
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
            EmailKind.MOBILE -> Email.TYPE_MOBILE
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

internal fun websiteTypeInt(website: EditableWebsite): Int =
    if (website.kind == WebsiteKind.OTHER && !website.customLabel.isNullOrBlank()) {
        Website.TYPE_CUSTOM
    } else {
        when (website.kind) {
            WebsiteKind.HOMEPAGE -> Website.TYPE_HOMEPAGE
            WebsiteKind.HOME -> Website.TYPE_HOME
            WebsiteKind.WORK -> Website.TYPE_WORK
            WebsiteKind.BLOG -> Website.TYPE_BLOG
            WebsiteKind.PROFILE -> Website.TYPE_PROFILE
            WebsiteKind.FTP -> Website.TYPE_FTP
            WebsiteKind.OTHER -> Website.TYPE_OTHER
        }
    }

internal fun sipTypeInt(sip: EditableSipAddress): Int =
    if (sip.kind == SipKind.OTHER && !sip.customLabel.isNullOrBlank()) {
        ContactsContractLegacy.Sip.TYPE_CUSTOM
    } else {
        when (sip.kind) {
            SipKind.HOME -> ContactsContractLegacy.Sip.TYPE_HOME
            SipKind.WORK -> ContactsContractLegacy.Sip.TYPE_WORK
            SipKind.OTHER -> ContactsContractLegacy.Sip.TYPE_OTHER
        }
    }

internal fun phoneValues(phone: EditablePhone): ContentValues =
    ContentValues().apply {
        put(Phone.NUMBER, phone.number)
        put(Phone.TYPE, phoneTypeInt(phone))
        put(Phone.LABEL, phone.customLabel.takeIf { phone.kind == PhoneKind.OTHER && !it.isNullOrBlank() })
        put(ContactsContract.Data.IS_PRIMARY, if (phone.isPrimary) 1 else 0)
    }

internal fun emailValues(email: EditableEmail): ContentValues =
    ContentValues().apply {
        put(Email.ADDRESS, email.address)
        put(Email.TYPE, emailTypeInt(email))
        put(Email.LABEL, email.customLabel.takeIf { email.kind == EmailKind.OTHER && !it.isNullOrBlank() })
        put(ContactsContract.Data.IS_PRIMARY, if (email.isPrimary) 1 else 0)
    }

/**
 * Адрес пишем структурированными частями. [EditableAddress.address] остаётся
 * fallback для старых контактов, у которых была заполнена только одна строка.
 */
internal fun addressValues(address: EditableAddress): ContentValues =
    ContentValues().apply {
        val street = address.streetLine
        val formatted = formattedAddress(address)
        put(StructuredPostal.STREET, street.takeIf { it.isNotBlank() })
        put(StructuredPostal.FORMATTED_ADDRESS, formatted.takeIf { it.isNotBlank() })
        put(StructuredPostal.TYPE, addressTypeInt(address))
        put(
            StructuredPostal.LABEL,
            address.customLabel.takeIf { address.kind == AddressKind.OTHER && !it.isNullOrBlank() },
        )
        putOrNull(StructuredPostal.POBOX, address.poBox)
        putOrNull(StructuredPostal.NEIGHBORHOOD, address.neighborhood)
        putOrNull(StructuredPostal.CITY, address.city)
        putOrNull(StructuredPostal.REGION, address.region)
        putOrNull(StructuredPostal.POSTCODE, address.postcode)
        putOrNull(StructuredPostal.COUNTRY, address.country)
    }

internal fun websiteValues(website: EditableWebsite): ContentValues =
    ContentValues().apply {
        put(Website.URL, website.url)
        put(Website.TYPE, websiteTypeInt(website))
        put(
            Website.LABEL,
            website.customLabel.takeIf { website.kind == WebsiteKind.OTHER && !it.isNullOrBlank() },
        )
    }

internal fun sipValues(sip: EditableSipAddress): ContentValues =
    ContentValues().apply {
        put(ContactsContractLegacy.Sip.SIP_ADDRESS, sip.address)
        put(ContactsContractLegacy.Sip.TYPE, sipTypeInt(sip))
        put(
            ContactsContractLegacy.Sip.LABEL,
            sip.customLabel.takeIf { sip.kind == SipKind.OTHER && !it.isNullOrBlank() },
        )
    }

internal fun organizationValues(organization: EditableOrganization): ContentValues =
    ContentValues().apply {
        put(Organization.COMPANY, organization.company.takeIf { it.isNotBlank() })
        put(Organization.TITLE, organization.title.takeIf { it.isNotBlank() })
        put(Organization.DEPARTMENT, organization.department.takeIf { it.isNotBlank() })
        put(Organization.JOB_DESCRIPTION, organization.jobDescription.takeIf { it.isNotBlank() })
        put(Organization.OFFICE_LOCATION, organization.officeLocation.takeIf { it.isNotBlank() })
    }

private fun formattedAddress(address: EditableAddress): String =
    listOf(
        address.poBox,
        address.streetLine,
        address.neighborhood,
        address.city,
        address.region,
        address.postcode,
        address.country,
    ).map { it.trim() }
        .filter { it.isNotEmpty() }
        .joinToString(", ")

private fun ContentValues.putOrNull(
    key: String,
    value: String,
) {
    if (value.isBlank()) putNull(key) else put(key, value)
}

internal fun noteValues(note: String): ContentValues =
    ContentValues().apply {
        put(Note.NOTE, note)
    }

internal fun eventValues(birthday: Birthday): ContentValues =
    ContentValues().apply {
        put(Event.START_DATE, birthdayToString(birthday))
        put(Event.TYPE, Event.TYPE_BIRTHDAY)
    }

internal fun photoValues(photo: ByteArray): ContentValues =
    ContentValues().apply {
        put(Photo.PHOTO, photo)
    }
