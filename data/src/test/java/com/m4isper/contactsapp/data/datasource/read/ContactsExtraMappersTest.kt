package com.m4isper.contactsapp.data.datasource.read

import android.provider.ContactsContract.CommonDataKinds.Event
import com.m4isper.contactsapp.domain.model.contact.Birthday
import com.m4isper.contactsapp.domain.model.contact.EventKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ContactsExtraMappersTest {
    @Test
    fun `parseable event date maps to parsed date`() {
        val event = editableEventOf(existingEvent(raw = "2015-06-01"))

        assertEquals(Birthday(day = 1, month = 6, year = 2015), event.date)
        assertNull(event.unparsedDate)
        assertEquals(EventKind.ANNIVERSARY, event.kind)
    }

    @Test
    fun `unparsed event date is preserved as raw value`() {
        val event = editableEventOf(existingEvent(raw = "1985-03-0X"))

        assertNull(event.date)
        assertEquals("1985-03-0X", event.unparsedDate)
    }

    private fun existingEvent(
        raw: String,
        dataId: Long = 1,
        type: Int = Event.TYPE_ANNIVERSARY,
        label: String? = null,
    ) = ExistingEvent(dataId = dataId, rawContactId = 10, raw = raw, type = type, label = label)
}
