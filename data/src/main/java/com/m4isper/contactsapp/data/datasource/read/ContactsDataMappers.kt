// Подавление осознанное: по одной маленькой функции на тип data-строки.
@file:Suppress("TooManyFunctions")

package com.m4isper.contactsapp.data.datasource.read

import com.m4isper.contactsapp.data.datasource.ContactsContractLegacy

import android.provider.ContactsContract.CommonDataKinds.Email
import android.provider.ContactsContract.CommonDataKinds.Event
import android.provider.ContactsContract.CommonDataKinds.Note
import android.provider.ContactsContract.CommonDataKinds.Phone
import android.provider.ContactsContract.CommonDataKinds.StructuredPostal
import com.m4isper.contactsapp.data.format.birthdayFromString
import com.m4isper.contactsapp.domain.model.contact.AddressKind
import com.m4isper.contactsapp.domain.model.contact.Birthday
import com.m4isper.contactsapp.domain.model.contact.EmailAddress
import com.m4isper.contactsapp.domain.model.contact.EmailKind
import com.m4isper.contactsapp.domain.model.contact.Organization
import com.m4isper.contactsapp.domain.model.contact.PhoneKind
import com.m4isper.contactsapp.domain.model.contact.PhoneNumber
import com.m4isper.contactsapp.domain.model.contact.PostalAddress
import com.m4isper.contactsapp.domain.model.contact.SipAddress
import com.m4isper.contactsapp.domain.model.contact.SipKind
import com.m4isper.contactsapp.domain.model.contact.Website
import com.m4isper.contactsapp.domain.model.contact.WebsiteKind
import android.provider.ContactsContract.CommonDataKinds.Organization as OrganizationData
import android.provider.ContactsContract.CommonDataKinds.SipAddress as SipAddressData
import android.provider.ContactsContract.CommonDataKinds.Website as WebsiteData

internal fun mapPhoneKind(type: Int): PhoneKind =
    when (type) {
        Phone.TYPE_MOBILE -> PhoneKind.MOBILE
        Phone.TYPE_HOME -> PhoneKind.HOME
        Phone.TYPE_WORK -> PhoneKind.WORK
        Phone.TYPE_WORK_MOBILE -> PhoneKind.WORK_MOBILE
        Phone.TYPE_FAX_HOME,
        Phone.TYPE_FAX_WORK,
        -> PhoneKind.FAX
        Phone.TYPE_PAGER,
        Phone.TYPE_WORK_PAGER,
        -> PhoneKind.PAGER
        Phone.TYPE_CALLBACK -> PhoneKind.CALLBACK
        Phone.TYPE_COMPANY_MAIN -> PhoneKind.COMPANY_MAIN
        else -> PhoneKind.OTHER
    }

internal fun mapEmailKind(type: Int): EmailKind =
    when (type) {
        Email.TYPE_HOME -> EmailKind.HOME
        Email.TYPE_WORK -> EmailKind.WORK
        Email.TYPE_MOBILE -> EmailKind.MOBILE
        else -> EmailKind.OTHER
    }

internal fun mapWebsiteKind(type: Int): WebsiteKind =
    when (type) {
        WebsiteData.TYPE_HOMEPAGE -> WebsiteKind.HOMEPAGE
        WebsiteData.TYPE_HOME -> WebsiteKind.HOME
        WebsiteData.TYPE_WORK -> WebsiteKind.WORK
        WebsiteData.TYPE_BLOG -> WebsiteKind.BLOG
        WebsiteData.TYPE_PROFILE -> WebsiteKind.PROFILE
        WebsiteData.TYPE_FTP -> WebsiteKind.FTP
        else -> WebsiteKind.OTHER
    }

internal fun mapSipKind(type: Int): SipKind =
    when (type) {
        ContactsContractLegacy.Sip.TYPE_HOME -> SipKind.HOME
        ContactsContractLegacy.Sip.TYPE_WORK -> SipKind.WORK
        else -> SipKind.OTHER
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
                    isPrimary = row.isPrimary,
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
                    isPrimary = row.isPrimary,
                )
            }
        }.distinctBy { it.address }

internal fun postalAddressesOf(rows: List<DataRow>): List<PostalAddress> =
    rows
        .filter { it.mimetype == StructuredPostal.CONTENT_ITEM_TYPE }
        .mapNotNull { row ->
            postalAddressOf(row)?.takeIf { it.displayAddress.isNotBlank() }
        }.distinctBy { it.displayAddress }

internal fun websitesOf(rows: List<DataRow>): List<Website> =
    rows
        .filter { it.mimetype == WebsiteData.CONTENT_ITEM_TYPE }
        .mapNotNull { row ->
            row.data1?.trim()?.takeIf { it.isNotEmpty() }?.let { url ->
                Website(
                    url = url,
                    kind = mapWebsiteKind(row.data2Int),
                    customLabel = customLabelOf(row.data2Int, row.data3, WebsiteData.TYPE_CUSTOM),
                )
            }
        }.distinctBy { it.url }

internal fun sipAddressesOf(rows: List<DataRow>): List<SipAddress> =
    rows
        .filter { it.mimetype == ContactsContractLegacy.Sip.CONTENT_ITEM_TYPE }
        .mapNotNull { row ->
            row.data1?.trim()?.takeIf { it.isNotEmpty() }?.let { address ->
                SipAddress(
                    address = address,
                    kind = mapSipKind(row.data2Int),
                    customLabel = customLabelOf(row.data2Int, row.data3, ContactsContractLegacy.Sip.TYPE_CUSTOM),
                )
            }
        }.distinctBy { it.address }

internal fun organizationOf(rows: List<DataRow>): Organization? {
    val row = rows.firstOrNull { it.mimetype == OrganizationData.CONTENT_ITEM_TYPE } ?: return null
    val company = row.data1.orEmpty().trim()
    val title = row.data4.orEmpty().trim()
    val department = row.data5.orEmpty().trim()
    val jobDescription = row.data6.orEmpty().trim()
    val officeLocation = row.data7.orEmpty().trim()
    return if (listOf(company, title, department, jobDescription, officeLocation).all { it.isEmpty() }) {
        null
    } else {
        Organization(
            company = company,
            title = title,
            department = department,
            jobDescription = jobDescription,
            officeLocation = officeLocation,
        )
    }
}

private fun postalAddressOf(row: DataRow): PostalAddress? {
    val address = row.data1.orEmpty().trim()
    val street = row.data4.orEmpty().trim()
    val poBox = row.data5.orEmpty().trim()
    val neighborhood = row.data6.orEmpty().trim()
    val city = row.data7.orEmpty().trim()
    val region = row.data8.orEmpty().trim()
    val postcode = row.data9.orEmpty().trim()
    val country = row.data10.orEmpty().trim()
    val parts = listOf(address, street, poBox, neighborhood, city, region, postcode, country)
    if (parts.all { it.isEmpty() }) return null
    return PostalAddress(
        address = address,
        kind = mapAddressKind(row.data2Int),
        customLabel = customLabelOf(row.data2Int, row.data3, StructuredPostal.TYPE_CUSTOM),
        street = street,
        poBox = poBox,
        neighborhood = neighborhood,
        city = city,
        region = region,
        postcode = postcode,
        country = country,
    )
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
        ?.let { birthdayFromString(it.data1) }
