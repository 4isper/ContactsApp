package com.m4isper.contactsapp.presentation.navigation

import androidx.navigation3.runtime.NavKey
import org.junit.Assert.assertEquals
import org.junit.Test

class NavigateToDetailsTest {
    @Test
    fun `pushes details onto list`() {
        val backStack = mutableListOf<NavKey>(ContactsListKey)

        navigateToDetails(backStack, 7)

        assertEquals(
            listOf(ContactsListKey, ContactDetailsKey(7)),
            backStack,
        )
    }

    @Test
    fun `replaces open details instead of stacking`() {
        val backStack = mutableListOf<NavKey>(ContactsListKey, ContactDetailsKey(7))

        navigateToDetails(backStack, 8)

        assertEquals(
            listOf(ContactsListKey, ContactDetailsKey(8)),
            backStack,
        )
    }
}
