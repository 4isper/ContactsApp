package com.m4isper.contactsapp.data.datasource.write

import com.m4isper.contactsapp.data.datasource.write.contacts.decideNameActions
import org.junit.Assert.assertEquals
import org.junit.Test

class NameActionsTest {
    @Test
    fun `empty name deletes all existing rows`() {
        val actions = decideNameActions(listOf(1L, 2L), nameIsEmpty = true)

        assertEquals(listOf(1L, 2L), actions.deleteIds)
        assertEquals(emptyList<Long>(), actions.updateIds)
        assertEquals(false, actions.insertNew)
    }

    @Test
    fun `non-empty name updates all existing rows across raws`() {
        val actions = decideNameActions(listOf(7L, 9L), nameIsEmpty = false)

        assertEquals(emptyList<Long>(), actions.deleteIds)
        assertEquals(listOf(7L, 9L), actions.updateIds)
        assertEquals(false, actions.insertNew)
    }

    @Test
    fun `non-empty name inserts when no rows exist`() {
        val actions = decideNameActions(emptyList(), nameIsEmpty = false)

        assertEquals(emptyList<Long>(), actions.deleteIds)
        assertEquals(emptyList<Long>(), actions.updateIds)
        assertEquals(true, actions.insertNew)
    }

    @Test
    fun `empty name with no rows does nothing`() {
        val actions = decideNameActions(emptyList(), nameIsEmpty = true)

        assertEquals(emptyList<Long>(), actions.deleteIds)
        assertEquals(emptyList<Long>(), actions.updateIds)
        assertEquals(false, actions.insertNew)
    }
}
