package com.m4isper.contactsapp.data.datasource

import android.provider.ContactsContract.CommonDataKinds.Email
import android.provider.ContactsContract.CommonDataKinds.Event
import android.provider.ContactsContract.CommonDataKinds.Note
import android.provider.ContactsContract.CommonDataKinds.Organization
import android.provider.ContactsContract.CommonDataKinds.Phone
import android.provider.ContactsContract.CommonDataKinds.StructuredName
import android.provider.ContactsContract.CommonDataKinds.StructuredPostal
import android.provider.ContactsContract.CommonDataKinds.Website

internal fun header(
    id: Long,
    name: String,
    photoUri: String? = null,
) = ContactHeaderRow(contactId = id, name = name, photoUri = photoUri)

internal fun phoneRow(
    contactId: Long,
    phone: String,
    type: Int = Phone.TYPE_MOBILE,
    label: String? = null,
) = DataRow(
    contactId = contactId,
    mimetype = Phone.CONTENT_ITEM_TYPE,
    data1 = phone,
    data2 = type.toString(),
    data3 = label,
)

internal fun emailRow(
    contactId: Long,
    address: String,
    type: Int = Email.TYPE_HOME,
    label: String? = null,
) = DataRow(
    contactId = contactId,
    mimetype = Email.CONTENT_ITEM_TYPE,
    data1 = address,
    data2 = type.toString(),
    data3 = label,
)

internal fun addressRow(
    contactId: Long,
    address: String,
    type: Int = StructuredPostal.TYPE_HOME,
) = DataRow(
    contactId = contactId,
    mimetype = StructuredPostal.CONTENT_ITEM_TYPE,
    data1 = address,
    data2 = type.toString(),
)

internal fun websiteRow(
    contactId: Long,
    url: String,
) = DataRow(
    contactId = contactId,
    mimetype = Website.CONTENT_ITEM_TYPE,
    data1 = url,
)

internal fun organizationRow(
    contactId: Long,
    company: String,
    title: String,
) = DataRow(
    contactId = contactId,
    mimetype = Organization.CONTENT_ITEM_TYPE,
    data1 = company,
    data4 = title,
)

internal fun noteRow(
    contactId: Long,
    note: String,
) = DataRow(
    contactId = contactId,
    mimetype = Note.CONTENT_ITEM_TYPE,
    data1 = note,
)

internal fun birthdayRow(
    contactId: Long,
    raw: String,
    type: Int = Event.TYPE_BIRTHDAY,
) = DataRow(
    contactId = contactId,
    mimetype = Event.CONTENT_ITEM_TYPE,
    data1 = raw,
    data2 = type.toString(),
)

internal fun nameRow(
    contactId: Long,
    display: String? = null,
    given: String? = null,
    family: String? = null,
    prefix: String? = null,
    middle: String? = null,
    suffix: String? = null,
    phoneticGiven: String? = null,
    phoneticMiddle: String? = null,
    phoneticFamily: String? = null,
) = DataRow(
    contactId = contactId,
    mimetype = StructuredName.CONTENT_ITEM_TYPE,
    data1 = display,
    data2 = given,
    data3 = family,
    data4 = prefix,
    data5 = middle,
    data6 = suffix,
    data7 = phoneticGiven,
    data8 = phoneticMiddle,
    data9 = phoneticFamily,
)
