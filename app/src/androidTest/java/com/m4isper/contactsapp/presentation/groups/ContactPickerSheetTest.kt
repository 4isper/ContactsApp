package com.m4isper.contactsapp.presentation.groups

import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.WindowSize
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.m4isper.contactsapp.testDetails
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Пикер участников в нижнем листе: подтверждение остаётся на виду над списком. */
@RunWith(AndroidJUnit4::class)
class ContactPickerSheetTest {
    @get:Rule
    val compose = createComposeRule()

    private val contacts = (1..12).map { testDetails(it.toLong(), "Contact $it", "11$it") }

    @Test
    fun confirmStaysPinnedWhileTheListScrollsInAShortWindow() {
        var confirmed: List<Long>? = null
        compose.setContent {
            // Окно высотой с телефонную часть над клавиатурой: лист не должен прятать подтверждение.
            DeviceConfigurationOverride(DeviceConfigurationOverride.WindowSize(DpSize(320.dp, 420.dp))) {
                ContactPickerSheet(
                    contacts = contacts,
                    onConfirm = { confirmed = it },
                    onDismiss = {},
                )
            }
        }

        compose.onNodeWithTag("contact_picker_confirm").assertIsDisplayed()
        compose.onNodeWithTag("contact_picker_list").performScrollToNode(hasTestTag("contact_picker_row_12"))
        compose.onNodeWithTag("contact_picker_row_12").performClick()
        compose.onNodeWithTag("contact_picker_confirm").assertIsDisplayed().performClick()

        assertEquals(listOf(12L), confirmed)
    }

    @Test
    fun confirmStaysPinnedUnderSearchResults() {
        compose.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.WindowSize(DpSize(320.dp, 420.dp))) {
                ContactPickerSheet(
                    contacts = contacts,
                    onConfirm = {},
                    onDismiss = {},
                )
            }
        }

        compose.onNodeWithTag("search_field").performTextInput("Contact 5")

        compose.onNodeWithTag("contact_picker_row_5").assertIsDisplayed()
        compose.onNodeWithTag("contact_picker_confirm").assertIsDisplayed()
    }
}
