package com.m4isper.contactsapp.presentation.contacts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ContactsEmptyReasonTest {
    @Test
    fun `plain empty book maps to NoContacts`() {
        assertEquals(
            ContactsEmptyReason.NoContacts,
            contactsEmptyReason(
                searchQuery = "",
                showFavoritesOnly = false,
                groupName = null,
                accountName = null,
            ),
        )
    }

    @Test
    fun `favorites only maps to Favorites`() {
        assertEquals(
            ContactsEmptyReason.Favorites,
            contactsEmptyReason(
                searchQuery = "",
                showFavoritesOnly = true,
                groupName = null,
                accountName = null,
            ),
        )
    }

    @Test
    fun `group only maps to Group with its name`() {
        assertEquals(
            ContactsEmptyReason.Group("Family"),
            contactsEmptyReason(
                searchQuery = "",
                showFavoritesOnly = false,
                groupName = "Family",
                accountName = null,
            ),
        )
    }

    @Test
    fun `account only maps to Account with its name`() {
        assertEquals(
            ContactsEmptyReason.Account("user@gmail.com"),
            contactsEmptyReason(
                searchQuery = "",
                showFavoritesOnly = false,
                groupName = null,
                accountName = "user@gmail.com",
            ),
        )
    }

    @Test
    fun `search wins over other filters and reports them`() {
        val reason =
            contactsEmptyReason(
                searchQuery = "anna",
                showFavoritesOnly = true,
                groupName = "Family",
                accountName = null,
            )

        assertTrue(reason is ContactsEmptyReason.Search)
        assertEquals("anna", (reason as ContactsEmptyReason.Search).query)
        assertTrue(reason.hasOtherFilters)
    }

    @Test
    fun `search without other filters hides clear-filters action`() {
        val reason =
            contactsEmptyReason(
                searchQuery = "anna",
                showFavoritesOnly = false,
                groupName = null,
                accountName = null,
            )

        assertTrue(reason is ContactsEmptyReason.Search)
        assertFalse((reason as ContactsEmptyReason.Search).hasOtherFilters)
    }

    @Test
    fun `two active filters combine into CombinedFilters`() {
        assertEquals(
            ContactsEmptyReason.CombinedFilters,
            contactsEmptyReason(
                searchQuery = "",
                showFavoritesOnly = true,
                groupName = "Family",
                accountName = null,
            ),
        )
        assertEquals(
            ContactsEmptyReason.CombinedFilters,
            contactsEmptyReason(
                searchQuery = "",
                showFavoritesOnly = false,
                groupName = "Family",
                accountName = "user@gmail.com",
            ),
        )
    }

    @Test
    fun `blank search query is treated as no search`() {
        assertEquals(
            ContactsEmptyReason.Favorites,
            contactsEmptyReason(
                searchQuery = "   ",
                showFavoritesOnly = true,
                groupName = null,
                accountName = null,
            ),
        )
    }
}
