package com.m4isper.contactsapp.data.datasource.write.contacts

import android.provider.ContactsContract.CommonDataKinds.StructuredName
import com.m4isper.contactsapp.domain.model.contact.EditableName
import com.m4isper.contactsapp.domain.model.contact.NameMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class NameActionsTestExtra {
    @Test
    fun decideNameActions_deleteWhenEmpty() {
        val a = decideNameActions(listOf(1L, 2L), true)
        assertEquals(listOf(1L, 2L), a.deleteIds)
        assertTrue(a.updateIds.isEmpty())
        assertEquals(false, a.insertNew)
    }

    @Test
    fun decideNameActions_insertWhenNoExisting() {
        val a = decideNameActions(emptyList(), false)
        assertTrue(a.deleteIds.isEmpty())
        assertTrue(a.updateIds.isEmpty())
        assertEquals(true, a.insertNew)
    }

    @Test
    fun decideNameActions_updateWhenExisting() {
        val a = decideNameActions(listOf(1L), false)
        assertTrue(a.deleteIds.isEmpty())
        assertEquals(listOf(1L), a.updateIds)
        assertEquals(false, a.insertNew)
    }

    @Test
    fun structuredNameValues_displayMode() {
        val cv = structuredNameValues(EditableName(display = "John", mode = NameMode.DISPLAY, given = "J"))
        assertEquals("John", cv.getAsString(StructuredName.DISPLAY_NAME))
    }

    @Test
    fun structuredNameValues_structuredMode() {
        val cv = structuredNameValues(EditableName(display = "John", mode = NameMode.STRUCTURED, given = "J"))
        assertEquals("J", cv.getAsString(StructuredName.GIVEN_NAME))
    }
}
