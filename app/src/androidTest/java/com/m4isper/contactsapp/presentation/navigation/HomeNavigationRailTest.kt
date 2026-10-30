package com.m4isper.contactsapp.presentation.navigation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.m4isper.contactsapp.ui.theme.ContactsAppTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeNavigationRailTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun railShowsEveryTopLevelDestinationAndReportsSelection() {
        val selected = mutableListOf<TopLevelDestination>()
        compose.setContent {
            ContactsAppTheme(dynamicColor = false) {
                HomeNavigationRail(
                    current = TopLevelDestination.Contacts,
                    onSelect = { selected += it },
                )
            }
        }

        TopLevelDestination.entries.forEach { destination ->
            compose.onNodeWithTag("rail_${destination.name.lowercase()}").assertIsDisplayed()
        }
        compose.onNodeWithTag("rail_contacts").assertIsSelected()

        compose.onNodeWithTag("rail_groups").performClick()

        assertEquals(listOf(TopLevelDestination.Groups), selected)
    }
}
