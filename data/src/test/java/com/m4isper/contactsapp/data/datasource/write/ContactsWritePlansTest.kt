package com.m4isper.contactsapp.data.datasource.write

import android.provider.ContactsContract.CommonDataKinds.Event
import com.m4isper.contactsapp.data.datasource.read.ExistingBirthday
import com.m4isper.contactsapp.data.datasource.read.ExistingEvent
import com.m4isper.contactsapp.domain.model.Birthday
import com.m4isper.contactsapp.domain.model.EditableEvent
import com.m4isper.contactsapp.domain.model.EventKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ContactsWritePlansTest {
    private val date = Birthday(day = 1, month = 6, year = 2015)

    private fun existingEvent(
        dataId: Long = 1,
        raw: String = "2015-06-01",
        type: Int = Event.TYPE_ANNIVERSARY,
        label: String? = null,
    ) = ExistingEvent(dataId = dataId, rawContactId = 10, raw = raw, type = type, label = label)

    @Test
    fun `dated event updates matching row`() {
        val ops =
            planEventWriteOps(
                existing = listOf(existingEvent(dataId = 1)),
                events = listOf(EditableEvent(dataId = 1, date = date)),
            )

        val op = ops.single() as EventWriteOp.Update
        assertEquals(1L, op.dataId)
        assertEquals(date, op.date)
    }

    @Test
    fun `dated event without dataId is inserted`() {
        val event = EditableEvent(date = date)

        val ops = planEventWriteOps(existing = emptyList(), events = listOf(event))

        assertEquals(listOf(EventWriteOp.Insert(event, date)), ops)
    }

    @Test
    fun `event missing from form is deleted`() {
        val ops = planEventWriteOps(existing = listOf(existingEvent(dataId = 7)), events = emptyList())

        assertEquals(listOf(EventWriteOp.Delete(7L)), ops)
    }

    @Test
    fun `event with both date and unparsedDate is treated as dated`() {
        val ops =
            planEventWriteOps(
                existing = listOf(existingEvent(dataId = 4, raw = "1985-03-0X", type = Event.TYPE_OTHER)),
                events = listOf(EditableEvent(dataId = 4, date = date, unparsedDate = "1985-03-0X")),
            )

        val op = ops.single() as EventWriteOp.Update
        assertEquals(date, op.date)
    }

    @Test
    fun `untouched unparsed event produces no operations`() {
        val ops =
            planEventWriteOps(
                existing = listOf(existingEvent(dataId = 4, raw = "1985-03-0X", type = Event.TYPE_OTHER)),
                events =
                    listOf(
                        EditableEvent(
                            dataId = 4,
                            kind = EventKind.OTHER,
                            unparsedDate = "1985-03-0X",
                        ),
                    ),
            )

        assertTrue(ops.isEmpty())
    }

    @Test
    fun `unparsed event with changed kind is rewritten preserving raw date`() {
        val ops =
            planEventWriteOps(
                existing = listOf(existingEvent(dataId = 4, raw = "1985-03-0X", type = Event.TYPE_OTHER)),
                events =
                    listOf(
                        EditableEvent(
                            dataId = 4,
                            kind = EventKind.ANNIVERSARY,
                            unparsedDate = "1985-03-0X",
                        ),
                    ),
            )

        val op = ops.single() as EventWriteOp.UpdatePreserved
        assertEquals(4L, op.dataId)
        assertEquals("1985-03-0X", op.rawDate)
    }

    @Test
    fun `unparsed event with changed label is rewritten preserving raw date`() {
        val ops =
            planEventWriteOps(
                existing = listOf(existingEvent(dataId = 4, raw = "1985-03-0X", type = Event.TYPE_OTHER)),
                events =
                    listOf(
                        EditableEvent(
                            dataId = 4,
                            kind = EventKind.OTHER,
                            customLabel = "Годовщина",
                            unparsedDate = "1985-03-0X",
                        ),
                    ),
            )

        val op = ops.single() as EventWriteOp.UpdatePreserved
        assertEquals("Годовщина", op.event.customLabel)
    }

    @Test
    fun `unparsed event removed from form is deleted`() {
        val ops =
            planEventWriteOps(
                existing = listOf(existingEvent(dataId = 4, raw = "1985-03-0X", type = Event.TYPE_OTHER)),
                events = emptyList(),
            )

        assertEquals(listOf(EventWriteOp.Delete(4L)), ops)
    }

    @Test
    fun `cleared birthday deletes parsed row`() {
        assertEquals(
            BirthdayWriteOp.Delete(1L),
            planBirthdayWrite(existingBirthday(raw = "1990-05-24"), value = null),
        )
    }

    @Test
    fun `unparsed birthday is preserved when form has no value`() {
        assertEquals(
            BirthdayWriteOp.None,
            planBirthdayWrite(existingBirthday(raw = "1985-03-0X"), value = null),
        )
    }

    @Test
    fun `unchanged birthday is not rewritten`() {
        assertEquals(
            BirthdayWriteOp.None,
            planBirthdayWrite(
                existingBirthday(raw = "1990-05-24"),
                value = Birthday(day = 24, month = 5, year = 1990),
            ),
        )
    }

    @Test
    fun `changed birthday updates row`() {
        assertEquals(
            BirthdayWriteOp.Update(1L, Birthday(day = 25, month = 5, year = 1990)),
            planBirthdayWrite(
                existingBirthday(raw = "1990-05-24"),
                value = Birthday(day = 25, month = 5, year = 1990),
            ),
        )
    }

    @Test
    fun `unparsed birthday replaced by new value updates row`() {
        assertEquals(
            BirthdayWriteOp.Update(1L, Birthday(day = 1, month = 1, year = 2000)),
            planBirthdayWrite(
                existingBirthday(raw = "1985-03-0X"),
                value = Birthday(day = 1, month = 1, year = 2000),
            ),
        )
    }

    @Test
    fun `new birthday is inserted`() {
        assertEquals(
            BirthdayWriteOp.Insert(Birthday(day = 1, month = 1, year = 2000)),
            planBirthdayWrite(existing = null, value = Birthday(day = 1, month = 1, year = 2000)),
        )
    }

    @Test
    fun `no existing row and no value produce no operations`() {
        assertEquals(BirthdayWriteOp.None, planBirthdayWrite(existing = null, value = null))
    }

    private fun existingBirthday(
        raw: String,
        dataId: Long = 1,
    ) = ExistingBirthday(dataId = dataId, rawContactId = 10, raw = raw)
}
