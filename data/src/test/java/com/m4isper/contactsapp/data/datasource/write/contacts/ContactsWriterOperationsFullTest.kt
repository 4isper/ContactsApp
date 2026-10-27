package com.m4isper.contactsapp.data.datasource.write.contacts

import android.provider.ContactsContract
import com.m4isper.contactsapp.domain.model.contact.EditableEmail
import com.m4isper.contactsapp.domain.model.contact.EditablePhone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ContactsWriterOperationsFullTest {
    @Test
    fun dataInsert_createsBuilder() {
        val builder = dataInsert(ContactsContract.Data.MIMETYPE, rawContactBackRef = 0)
        assertTrue(builder is android.content.ContentProviderOperation.Builder)
    }

    @Test
    fun dataUpdate_createsBuilder() {
        val builder = dataUpdate(1L)
        assertTrue(builder is android.content.ContentProviderOperation.Builder)
    }

    @Test
    fun deleteDataOperation_createsOperation() {
        val op = deleteDataOperation(1L)
        assertTrue(op is android.content.ContentProviderOperation)
    }

    @Test
    fun phoneTypeInt_mapsAllKinds() {
        assertEquals(
            android.provider.ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE,
            phoneTypeInt(
                EditablePhone(number = "1", kind = com.m4isper.contactsapp.domain.model.contact.PhoneKind.MOBILE),
            ),
        )
        assertEquals(
            android.provider.ContactsContract.CommonDataKinds.Phone.TYPE_HOME,
            phoneTypeInt(
                EditablePhone(number = "1", kind = com.m4isper.contactsapp.domain.model.contact.PhoneKind.HOME),
            ),
        )
        assertEquals(
            android.provider.ContactsContract.CommonDataKinds.Phone.TYPE_CUSTOM,
            phoneTypeInt(
                EditablePhone(
                    number = "1",
                    kind = com.m4isper.contactsapp.domain.model.contact.PhoneKind.OTHER,
                    customLabel = "x",
                ),
            ),
        )
    }

    @Test
    fun emailTypeInt_mapsAllKinds() {
        assertEquals(
            android.provider.ContactsContract.CommonDataKinds.Email.TYPE_HOME,
            emailTypeInt(
                EditableEmail(address = "a@b.c", kind = com.m4isper.contactsapp.domain.model.contact.EmailKind.HOME),
            ),
        )
        assertEquals(
            android.provider.ContactsContract.CommonDataKinds.Email.TYPE_CUSTOM,
            emailTypeInt(
                EditableEmail(
                    address = "a@b.c",
                    kind = com.m4isper.contactsapp.domain.model.contact.EmailKind.OTHER,
                    customLabel = "x",
                ),
            ),
        )
    }
}
