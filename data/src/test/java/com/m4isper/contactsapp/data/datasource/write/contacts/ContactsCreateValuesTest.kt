package com.m4isper.contactsapp.data.datasource.write.contacts

import android.provider.ContactsContract.CommonDataKinds.Email
import android.provider.ContactsContract.CommonDataKinds.GroupMembership
import android.provider.ContactsContract.CommonDataKinds.Organization
import android.provider.ContactsContract.CommonDataKinds.Phone
import android.provider.ContactsContract.CommonDataKinds.Photo
import com.m4isper.contactsapp.domain.model.contact.ContactForm
import com.m4isper.contactsapp.domain.model.contact.EditableEmail
import com.m4isper.contactsapp.domain.model.contact.EditableOrganization
import com.m4isper.contactsapp.domain.model.contact.EditablePhone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ContactsCreateValuesTest {
    @Test
    fun emptyFormProducesEmptyList() {
        assertEquals(emptyList<Pair<String, android.content.ContentValues>>(), contactDataValues(ContactForm(), null))
    }

    @Test
    fun phoneIsIncluded() {
        val form = ContactForm(phones = listOf(EditablePhone(number = "123")))
        val result = contactDataValues(form, null)
        assertTrue(result.any { it.first == Phone.CONTENT_ITEM_TYPE })
    }

    @Test
    fun emailIsIncluded() {
        val form = ContactForm(emails = listOf(EditableEmail(address = "a@b.c")))
        val result = contactDataValues(form, null)
        assertTrue(result.any { it.first == Email.CONTENT_ITEM_TYPE })
    }

    @Test
    fun organizationIsIncludedWhenNonEmpty() {
        val form = ContactForm(organization = EditableOrganization(company = "Acme"))
        val result = contactDataValues(form, null)
        assertTrue(result.any { it.first == Organization.CONTENT_ITEM_TYPE })
    }

    @Test
    fun groupMembershipIsIncluded() {
        val form = ContactForm(groups = listOf(1L, 2L))
        val result = contactDataValues(form, null)
        assertEquals(2, result.count { it.first == GroupMembership.CONTENT_ITEM_TYPE })
    }

    @Test
    fun photoIsIncludedWhenProvided() {
        val form = ContactForm()
        val result = contactDataValues(form, byteArrayOf(1, 2, 3))
        assertTrue(result.any { it.first == Photo.CONTENT_ITEM_TYPE })
    }
}
