package com.m4isper.contactsapp.data.datasource.write.contacts

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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ContactsWriterValuesTest {
    @Test
    fun phoneTypeInt_mapsEveryKind() {
        val cases =
            listOf(
                PhoneKind.MOBILE to Phone.TYPE_MOBILE,
                PhoneKind.HOME to Phone.TYPE_HOME,
                PhoneKind.WORK to Phone.TYPE_WORK,
                PhoneKind.WORK_MOBILE to Phone.TYPE_WORK_MOBILE,
                PhoneKind.FAX to Phone.TYPE_FAX_WORK,
                PhoneKind.PAGER to Phone.TYPE_PAGER,
                PhoneKind.CALLBACK to Phone.TYPE_CALLBACK,
                PhoneKind.COMPANY_MAIN to Phone.TYPE_COMPANY_MAIN,
                PhoneKind.OTHER to Phone.TYPE_OTHER,
            )
        cases.forEach { (kind, expected) ->
            assertEquals(expected, phoneTypeInt(EditablePhone(number = "1", kind = kind)))
        }
        assertEquals(
            Phone.TYPE_CUSTOM,
            phoneTypeInt(EditablePhone(number = "1", kind = PhoneKind.OTHER, customLabel = "Custom")),
        )
    }

    @Test
    fun emailTypeInt_mapsEveryKind() {
        val cases =
            listOf(
                EmailKind.HOME to Email.TYPE_HOME,
                EmailKind.WORK to Email.TYPE_WORK,
                EmailKind.MOBILE to Email.TYPE_MOBILE,
                EmailKind.OTHER to Email.TYPE_OTHER,
            )
        cases.forEach { (kind, expected) ->
            assertEquals(expected, emailTypeInt(EditableEmail(address = "a@b.c", kind = kind)))
        }
        assertEquals(
            Email.TYPE_CUSTOM,
            emailTypeInt(EditableEmail(address = "a@b.c", kind = EmailKind.OTHER, customLabel = "Custom")),
        )
    }

    @Test
    fun addressTypeInt_mapsEveryKind() {
        assertEquals(
            StructuredPostal.TYPE_HOME,
            addressTypeInt(EditableAddress(address = "x", kind = AddressKind.HOME)),
        )
        assertEquals(
            StructuredPostal.TYPE_WORK,
            addressTypeInt(EditableAddress(address = "x", kind = AddressKind.WORK)),
        )
        assertEquals(
            StructuredPostal.TYPE_OTHER,
            addressTypeInt(EditableAddress(address = "x", kind = AddressKind.OTHER)),
        )
        assertEquals(
            StructuredPostal.TYPE_CUSTOM,
            addressTypeInt(EditableAddress(address = "x", kind = AddressKind.OTHER, customLabel = "Custom")),
        )
    }

    @Test
    fun websiteTypeInt_mapsEveryKind() {
        val cases =
            listOf(
                WebsiteKind.HOMEPAGE to Website.TYPE_HOMEPAGE,
                WebsiteKind.HOME to Website.TYPE_HOME,
                WebsiteKind.WORK to Website.TYPE_WORK,
                WebsiteKind.BLOG to Website.TYPE_BLOG,
                WebsiteKind.PROFILE to Website.TYPE_PROFILE,
                WebsiteKind.FTP to Website.TYPE_FTP,
                WebsiteKind.OTHER to Website.TYPE_OTHER,
            )
        cases.forEach { (kind, expected) ->
            assertEquals(expected, websiteTypeInt(EditableWebsite(url = "u", kind = kind)))
        }
        assertEquals(
            Website.TYPE_CUSTOM,
            websiteTypeInt(EditableWebsite(url = "u", kind = WebsiteKind.OTHER, customLabel = "Custom")),
        )
    }

    @Test
    fun sipTypeInt_mapsEveryKind() {
        val cases =
            listOf(
                SipKind.HOME to SipAddress.TYPE_HOME,
                SipKind.WORK to SipAddress.TYPE_WORK,
                SipKind.OTHER to SipAddress.TYPE_OTHER,
            )
        cases.forEach { (kind, expected) ->
            assertEquals(expected, sipTypeInt(EditableSipAddress(address = "s", kind = kind)))
        }
        assertEquals(
            SipAddress.TYPE_CUSTOM,
            sipTypeInt(EditableSipAddress(address = "s", kind = SipKind.OTHER, customLabel = "Custom")),
        )
    }

    @Test
    fun phoneValues_containsExpectedFields() {
        val cv = phoneValues(EditablePhone(number = "123", kind = PhoneKind.MOBILE, isPrimary = true))
        assertEquals("123", cv.getAsString(Phone.NUMBER))
        assertEquals(Phone.TYPE_MOBILE, cv.getAsInteger(Phone.TYPE))
        assertEquals(1, cv.getAsInteger(ContactsContract.Data.IS_PRIMARY))
    }

    @Test
    fun emailValues_containsExpectedFields() {
        val cv = emailValues(EditableEmail(address = "a@b.c", kind = EmailKind.WORK, isPrimary = true))
        assertEquals("a@b.c", cv.getAsString(Email.ADDRESS))
        assertEquals(Email.TYPE_WORK, cv.getAsInteger(Email.TYPE))
        assertEquals(1, cv.getAsInteger(ContactsContract.Data.IS_PRIMARY))
    }

    @Test
    fun addressValues_containsStructuredParts() {
        val cv =
            addressValues(
                EditableAddress(
                    address = "fallback",
                    kind = AddressKind.HOME,
                    street = "street",
                    city = "city",
                    country = "country",
                ),
            )
        assertEquals(StructuredPostal.TYPE_HOME, cv.getAsInteger(StructuredPostal.TYPE))
        assertEquals("street", cv.getAsString(StructuredPostal.STREET))
        assertEquals("city", cv.getAsString(StructuredPostal.CITY))
        assertEquals("country", cv.getAsString(StructuredPostal.COUNTRY))
    }

    @Test
    fun websiteValues_containsExpectedFields() {
        val cv = websiteValues(EditableWebsite(url = "https://example.com", kind = WebsiteKind.BLOG))
        assertEquals("https://example.com", cv.getAsString(Website.URL))
        assertEquals(Website.TYPE_BLOG, cv.getAsInteger(Website.TYPE))
    }

    @Test
    fun sipValues_containsExpectedFields() {
        val cv = sipValues(EditableSipAddress(address = "sip:user", kind = SipKind.WORK))
        assertEquals("sip:user", cv.getAsString(SipAddress.SIP_ADDRESS))
        assertEquals(SipAddress.TYPE_WORK, cv.getAsInteger(SipAddress.TYPE))
    }

    @Test
    fun organizationValues_containsExpectedFields() {
        val cv =
            organizationValues(
                EditableOrganization(
                    company = "Acme",
                    title = "Dev",
                    department = "R&D",
                    jobDescription = "coding",
                    officeLocation = "HQ",
                ),
            )
        assertEquals("Acme", cv.getAsString(Organization.COMPANY))
        assertEquals("Dev", cv.getAsString(Organization.TITLE))
        assertEquals("R&D", cv.getAsString(Organization.DEPARTMENT))
        assertEquals("coding", cv.getAsString(Organization.JOB_DESCRIPTION))
        assertEquals("HQ", cv.getAsString(Organization.OFFICE_LOCATION))
    }

    @Test
    fun noteValues_containsNote() {
        val cv = noteValues("hello")
        assertEquals("hello", cv.getAsString(Note.NOTE))
    }

    @Test
    fun photoValues_containsPhoto() {
        val bytes = byteArrayOf(1, 2, 3)
        val cv = photoValues(bytes)
        assertTrue(cv.get(Photo.PHOTO) is ByteArray)
        assertEquals(3, (cv.get(Photo.PHOTO) as ByteArray).size)
    }

    @Test
    fun eventValues_containsBirthday() {
        val cv = eventValues(Birthday(year = 2020, month = 1, day = 2))
        assertEquals(Event.TYPE_BIRTHDAY, cv.getAsInteger(Event.TYPE))
        assertNotNull(cv.getAsString(Event.START_DATE))
    }
}
