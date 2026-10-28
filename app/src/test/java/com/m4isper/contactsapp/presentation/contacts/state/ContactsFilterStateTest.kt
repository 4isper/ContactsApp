package com.m4isper.contactsapp.presentation.contacts.state

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ContactsFilterStateTest {
    private val filters = ContactsFilterState(ContactsSelectionState())

    @Test
    fun `search opens on focus`() {
        filters.onSearchActivated()

        assertTrue(filters.searchActive.value)
    }

    @Test
    fun `closing search keeps the query and filters`() {
        filters.onSearchActivated()
        filters.onToggleFavoritesOnly()
        filters.onSearchQueryChange("An")

        filters.onSearchClosed()

        assertFalse(filters.searchActive.value)
        assertEquals("An", filters.searchQuery.value)
        assertTrue(filters.favoritesOnly.value)
    }

    @Test
    fun `clearing the query restores the browse list with filters intact`() {
        filters.onSearchActivated()
        filters.onToggleFavoritesOnly()
        filters.onGroupSelected(3L)
        filters.onSearchQueryChange("An")

        filters.onSearchQueryChange("")

        assertEquals("", filters.searchQuery.value)
        assertTrue(filters.favoritesOnly.value)
        assertEquals(3L, filters.selectedGroupId.value)
    }
}
