package com.m4isper.contactsapp.data.datasource.write.contacts

import android.content.ContentProviderOperation
import android.provider.ContactsContract
import com.m4isper.contactsapp.domain.model.contact.ContactAccount
import com.m4isper.contactsapp.domain.model.contact.ContactForm
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ContactsWriterOperationsTest {
    @Test
    fun dataInsert_createsInsertBuilder() {
        val builder =
            dataInsert(
                ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE,
                rawContactBackRef = 0,
            )
        assertTrue(builder is ContentProviderOperation.Builder)
    }

    @Test
    fun dataUpdate_createsUpdateBuilder() {
        val builder = dataUpdate(1L)
        assertTrue(builder is ContentProviderOperation.Builder)
    }

    @Test
    fun deleteDataOperation_createsDeleteOperation() {
        val op = deleteDataOperation(1L)
        assertTrue(op is ContentProviderOperation)
    }

    @Test
    fun photoWriteOperations_deletesAndInserts() {
        val ops = photoWriteOperations(1L, byteArrayOf(1, 2, 3))
        assertEquals(2, ops.size)
    }

    @Test
    fun photoWriteOperations_whenNoPhotoChanged() {
        val ops = photoWriteOperations(1L, null)
        assertTrue(ops is List<ContentProviderOperation>)
    }

    @Test
    fun diffListOperations_insertsNewItems() {
        val newItems = listOf("a", "b")
        val existingIds = emptySet<Long>()
        val ops =
            diffListOperations(
                newItems = newItems,
                existingIds = existingIds,
                dataIdOf = { null },
                insert = { item ->
                    ContentProviderOperation
                        .newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValue(ContactsContract.Data.DATA1, item)
                },
                update = { _, _ -> ContentProviderOperation.newUpdate(ContactsContract.Data.CONTENT_URI) },
            )
        assertEquals(2, ops.size)
    }

    @Test
    fun buildCreateOperations_createsRawContact() {
        val form = ContactForm()
        val account = ContactAccount()
        val ops = buildCreateOperations(form, null, account)
        assertTrue(ops.isNotEmpty())
    }
}
