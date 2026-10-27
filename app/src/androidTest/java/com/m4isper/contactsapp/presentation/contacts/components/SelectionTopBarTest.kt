package com.m4isper.contactsapp.presentation.contacts.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.m4isper.contactsapp.R
import com.m4isper.contactsapp.ui.theme.ContactsAppTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicBoolean

@RunWith(AndroidJUnit4::class)
class SelectionTopBarTest {
    @get:Rule
    val compose = createComposeRule()

    private fun setSelectionBar(
        count: Int = 3,
        fontScale: Float = 1f,
        onDelete: () -> Unit = {},
    ) {
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density = density.density, fontScale = fontScale),
            ) {
                ContactsAppTheme(dynamicColor = false) {
                    Box(modifier = Modifier.width(320.dp)) {
                        SelectionTopBar(
                            count = count,
                            allSelected = false,
                            allFavorite = false,
                            onClose = {},
                            onSelectAll = {},
                            onToggleFavorite = {},
                            onAddToGroup = {},
                            onShare = {},
                            onExport = {},
                            onDelete = onDelete,
                        )
                    }
                }
            }
        }
    }

    @Test
    fun compactWidthKeepsSelectedCountVisibleAtLargeFontScale() {
        setSelectionBar(count = 3, fontScale = 2f)
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val expectedTitle = context.resources.getQuantityString(R.plurals.selection_count, 3, 3)

        compose.onNodeWithTag("selection_title").assertIsDisplayed()
        compose.onNodeWithContentDescription(expectedTitle).assertIsDisplayed()
    }

    @Test
    fun overflowExposesEverySecondarySelectionAction() {
        setSelectionBar()

        compose.onNodeWithTag("selection_more_button").performClick()

        listOf(
            "add_to_group_button",
            "share_selected_button",
            "export_selected_button",
            "delete_selected_button",
        ).forEach { tag ->
            compose.onNodeWithTag(tag).assertIsDisplayed()
        }
    }

    @Test
    fun deleteActionDispatchesFromOverflow() {
        val deleted = AtomicBoolean(false)
        setSelectionBar(onDelete = { deleted.set(true) })

        compose.onNodeWithTag("selection_more_button").performClick()
        compose.onNodeWithTag("delete_selected_button").performClick()

        assertTrue(deleted.get())
    }
}
