package com.m4isper.contactsapp.data.datasource.read

import android.provider.ContactsContract.CommonDataKinds.Email
import android.provider.ContactsContract.CommonDataKinds.Event
import android.provider.ContactsContract.CommonDataKinds.Note
import android.provider.ContactsContract.CommonDataKinds.Phone
import android.provider.ContactsContract.CommonDataKinds.StructuredPostal
import com.m4isper.contactsapp.domain.model.AddressKind
import com.m4isper.contactsapp.domain.model.Birthday
import com.m4isper.contactsapp.domain.model.EmailAddress
import com.m4isper.contactsapp.domain.model.EmailKind
import com.m4isper.contactsapp.domain.model.Organization
import com.m4isper.contactsapp.domain.model.PhoneKind
import com.m4isper.contactsapp.domain.model.PhoneNumber
import com.m4isper.contactsapp.domain.model.PostalAddress
import com.m4isper.contactsapp.domain.model.Website
import android.provider.ContactsContract.CommonDataKinds.Organization as OrganizationData
import android.provider.ContactsContract.CommonDataKinds.Website as WebsiteData

internal fun mapPhoneKind(type: Int): PhoneKind =
    when (type) {
        Phone.TYPE_MOBILE -> PhoneKind.MOBILE
        Phone.TYPE_HOME -> PhoneKind.HOME
        Phone.TYPE_WORK,
        Phone.TYPE_WORK_MOBILE,
        -> PhoneKind.WORK
        else -> PhoneKind.OTHER
    }

internal fun mapEmailKind(type: Int): EmailKind =
    when (type) {
        Email.TYPE_HOME -> EmailKind.HOME
        Email.TYPE_WORK -> EmailKind.WORK
        else -> EmailKind.OTHER
    }

internal fun mapAddressKind(type: Int): AddressKind =
    when (type) {
        StructuredPostal.TYPE_HOME -> AddressKind.HOME
        StructuredPostal.TYPE_WORK -> AddressKind.WORK
        else -> AddressKind.OTHER
    }

internal fun customLabelOf(
    type: Int,
    label: String?,
    customType: Int,
): String? = label?.takeIf { type == customType && it.isNotBlank() }

internal fun phoneNumbersOf(rows: List<DataRow>): List<PhoneNumber> =
    rows
        .filter { it.mimetype == Phone.CONTENT_ITEM_TYPE }
        .mapNotNull { row ->
            row.data1?.trim()?.takeIf { it.isNotEmpty() }?.let { number ->
                PhoneNumber(
                    number = number,
                    kind = mapPhoneKind(row.data2Int),
                    customLabel = customLabelOf(row.data2Int, row.data3, Phone.TYPE_CUSTOM),
                )
            }
        }.distinctBy { it.number }

internal fun emailAddressesOf(rows: List<DataRow>): List<EmailAddress> =
    rows
        .filter { it.mimetype == Email.CONTENT_ITEM_TYPE }
        .mapNotNull { row ->
            row.data1?.trim()?.takeIf { it.isNotEmpty() }?.let { address ->
                EmailAddress(
                    address = address,
                    kind = mapEmailKind(row.data2Int),
                    customLabel = customLabelOf(row.data2Int, row.data3, Email.TYPE_CUSTOM),
                )
            }
        }.distinctBy { it.address }

internal fun postalAddressesOf(rows: List<DataRow>): List<PostalAddress> =
    rows
        .filter { it.mimetype == StructuredPostal.CONTENT_ITEM_TYPE }
        .mapNotNull { row ->
            row.data1?.trim()?.takeIf { it.isNotEmpty() }?.let { address ->
                PostalAddress(
                    address = address,
                    kind = mapAddressKind(row.data2Int),
                    customLabel = customLabelOf(row.data2Int, row.data3, StructuredPostal.TYPE_CUSTOM),
                )
            }
        }.distinctBy { it.address }

internal fun websitesOf(rows: List<DataRow>): List<Website> =
    rows
        .filter { it.mimetype == WebsiteData.CONTENT_ITEM_TYPE }
        .mapNotNull { row -> row.data1?.trim()?.takeIf { it.isNotEmpty() } }
        .distinct()
        .map { Website(it) }

internal fun organizationOf(rows: List<DataRow>): Organization? {
    val row = rows.firstOrNull { it.mimetype == OrganizationData.CONTENT_ITEM_TYPE }
    val company = row?.data1.orEmpty().trim()
    val title = row?.data4.orEmpty().trim()
    return if (company.isEmpty() && title.isEmpty()) {
        null
    } else {
        Organization(company = company, title = title)
    }
}

internal fun noteOf(rows: List<DataRow>): String? =
    rows
        .firstOrNull { it.mimetype == Note.CONTENT_ITEM_TYPE }
        ?.data1
        ?.trim()
        ?.takeIf { it.isNotEmpty() }

internal fun birthdayOf(rows: List<DataRow>): Birthday? =
    rows
        .firstOrNull { it.mimetype == Event.CONTENT_ITEM_TYPE && it.data2Int == Event.TYPE_BIRTHDAY }
        ?.let { Birthday.parse(it.data1) }
