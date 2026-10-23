package com.m4isper.contactsapp.data.datasource

import android.content.Context
import com.m4isper.contactsapp.domain.model.contact.ContactAccount
import com.m4isper.contactsapp.domain.model.contact.ContactForm
import com.m4isper.contactsapp.domain.model.contact.EditableName
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ContactsWriterDataSourceImplTest {
    @Test
    fun createContact_throwsWhenProviderEmpty() =
        runBlocking {
            val context = RuntimeEnvironment.getApplication() as Context
            val ds = ContactsWriterDataSourceImpl(context)
            val form =
                ContactForm(
                    name =
                        EditableName(
                            display = "John",
                            mode = com.m4isper.contactsapp.domain.model.contact.NameMode.DISPLAY,
                        ),
                )
            try {
                ds.createContact(form, ContactAccount.Local)
                assertTrue(false) // должно было бросить исключение
            } catch (e: IllegalStateException) {
                assertEquals("raw contact not created", e.message)
            }
        }

    @Test
    fun updateContact_doesNotThrowOnEmptyProvider() =
        runBlocking {
            val context = RuntimeEnvironment.getApplication() as Context
            val ds = ContactsWriterDataSourceImpl(context)
            val form =
                ContactForm(
                    name =
                        EditableName(
                            display = "John",
                            mode = com.m4isper.contactsapp.domain.model.contact.NameMode.DISPLAY,
                        ),
                )
            ds.updateContact(
                1L,
                com.m4isper.contactsapp.data.datasource.read
                    .ContactRows(),
                form,
            )
            assertTrue(true)
        }

    @Test
    fun loadContactRows_returnsEmptyOnEmptyProvider() =
        runBlocking {
            val context = RuntimeEnvironment.getApplication() as Context
            val ds = ContactsWriterDataSourceImpl(context)
            val rows = ds.loadContactRows(1L)
            assertTrue(rows.names.isEmpty())
            assertTrue(rows.phones.isEmpty())
        }

    @Test
    fun loadRawContactId_returnsNullOnEmptyProvider() =
        runBlocking {
            val context = RuntimeEnvironment.getApplication() as Context
            val ds = ContactsWriterDataSourceImpl(context)
            val id = ds.loadRawContactId(1L)
            assertNull(id)
        }
}
