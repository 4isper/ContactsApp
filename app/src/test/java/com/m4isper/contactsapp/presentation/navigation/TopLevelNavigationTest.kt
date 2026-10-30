package com.m4isper.contactsapp.presentation.navigation

import androidx.navigation3.runtime.NavKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TopLevelNavigationTest {
    @Test
    fun `maps top level keys and ignores nested screens`() {
        assertEquals(TopLevelDestination.Contacts, topLevelDestinationOf(ContactsListKey))
        assertEquals(TopLevelDestination.Groups, topLevelDestinationOf(GroupsKey))
        assertEquals(TopLevelDestination.Recents, topLevelDestinationOf(RecentsKey))
        assertEquals(TopLevelDestination.Duplicates, topLevelDestinationOf(DuplicatesKey))
        assertEquals(TopLevelDestination.Settings, topLevelDestinationOf(SettingsKey))
        assertNull(topLevelDestinationOf(ContactDetailsKey(1)))
        assertNull(topLevelDestinationOf(GroupDetailKey(4)))
        assertNull(topLevelDestinationOf(DuplicateReviewKey(targetId = 1, sourceIds = listOf(2))))
        assertNull(topLevelDestinationOf(null))
    }

    @Test
    fun `rail appears only on medium and wider windows at a top level`() {
        assertFalse(
            showNavigationRail(
                isMediumOrWider = false,
                destination = TopLevelDestination.Contacts,
            ),
        )
        assertFalse(showNavigationRail(isMediumOrWider = true, destination = null))
        assertTrue(
            showNavigationRail(
                isMediumOrWider = true,
                destination = TopLevelDestination.Groups,
            ),
        )
    }

    @Test
    fun `top level switch collapses nested destinations`() {
        val backStack = mutableListOf<NavKey>(ContactsListKey, GroupsKey, GroupDetailKey(4))

        navigateToTopLevel(backStack, TopLevelDestination.Settings)

        assertEquals(listOf(ContactsListKey, SettingsKey), backStack)
    }

    @Test
    fun `contacts destination returns to the root`() {
        val backStack = mutableListOf<NavKey>(ContactsListKey, GroupsKey)

        navigateToTopLevel(backStack, TopLevelDestination.Contacts)

        assertEquals(listOf(ContactsListKey), backStack)
    }

    @Test
    fun `switching to the same top level keeps a single entry`() {
        val backStack = mutableListOf<NavKey>(ContactsListKey, GroupsKey)

        navigateToTopLevel(backStack, TopLevelDestination.Groups)

        assertEquals(listOf(ContactsListKey, GroupsKey), backStack)
    }
}
