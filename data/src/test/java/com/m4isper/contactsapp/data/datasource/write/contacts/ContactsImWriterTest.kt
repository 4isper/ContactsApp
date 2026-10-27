package com.m4isper.contactsapp.data.datasource.write.contacts

import android.provider.ContactsContract.CommonDataKinds.Im
import com.m4isper.contactsapp.data.datasource.read.ContactRows
import com.m4isper.contactsapp.domain.model.contact.ContactForm
import com.m4isper.contactsapp.domain.model.contact.EditableInstantMessage
import com.m4isper.contactsapp.domain.model.contact.ImProtocol
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ContactsImWriterTest {
    @Test
    fun imProtocolInt_mapsEveryProtocol() {
        assertEquals(Im.PROTOCOL_AIM, imProtocolInt(ImProtocol.AIM))
        assertEquals(Im.PROTOCOL_MSN, imProtocolInt(ImProtocol.MSN))
        assertEquals(Im.PROTOCOL_YAHOO, imProtocolInt(ImProtocol.YAHOO))
        assertEquals(Im.PROTOCOL_SKYPE, imProtocolInt(ImProtocol.SKYPE))
        assertEquals(Im.PROTOCOL_QQ, imProtocolInt(ImProtocol.QQ))
        assertEquals(Im.PROTOCOL_GOOGLE_TALK, imProtocolInt(ImProtocol.GOOGLE_TALK))
        assertEquals(Im.PROTOCOL_ICQ, imProtocolInt(ImProtocol.ICQ))
        assertEquals(Im.PROTOCOL_JABBER, imProtocolInt(ImProtocol.JABBER))
        assertEquals(Im.PROTOCOL_CUSTOM, imProtocolInt(ImProtocol.CUSTOM))
    }

    @Test
    fun instantMessageValues_containsExpectedFields() {
        val cv =
            instantMessageValues(
                EditableInstantMessage(address = "user", protocol = ImProtocol.JABBER, customProtocol = "custom"),
                Im.TYPE_HOME,
            )
        assertEquals("user", cv.getAsString(Im.DATA))
        assertEquals(Im.TYPE_HOME, cv.getAsInteger(Im.TYPE))
        assertEquals(Im.PROTOCOL_JABBER, cv.getAsInteger(Im.PROTOCOL))
    }

    @Test
    fun instantMessageValues_customProtocolKeptOnlyForCustom() {
        val cv =
            instantMessageValues(
                EditableInstantMessage(address = "user", protocol = ImProtocol.CUSTOM, customProtocol = "proto"),
            )
        assertEquals("proto", cv.getAsString(Im.CUSTOM_PROTOCOL))
    }

    @Test
    fun imOperations_emptyWhenNoItems() {
        val ops = imOperations(1L, ContactRows(), ContactForm())
        assertTrue(ops.isEmpty())
    }
}
