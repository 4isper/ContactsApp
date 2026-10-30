package com.m4isper.contactsapp.ui.adaptive

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.WindowSize
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.width
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.m4isper.contactsapp.ui.theme.ContactsAppTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReadingColumnTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun expandedWidthCapsAndCentersContent() {
        compose.setContent {
            ContactsAppTheme(dynamicColor = false) {
                // Окно шириной 1000 dp: на такой ширине колонка должна упереться в предел.
                DeviceConfigurationOverride(DeviceConfigurationOverride.WindowSize(DpSize(1000.dp, 800.dp))) {
                    ReadingColumn {
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxSize()
                                    .testTag("content"),
                        )
                    }
                }
            }
        }

        val bounds = compose.onNodeWithTag("content").getUnclippedBoundsInRoot()
        assertEquals(640f, bounds.width.value, 0.5f)
        // Колонка по центру окна: (1000 − 640) / 2.
        assertEquals(180f, bounds.left.value, 0.5f)
    }

    @Test
    fun narrowWidthFillsTheWindow() {
        compose.setContent {
            ContactsAppTheme(dynamicColor = false) {
                Box(
                    modifier =
                        Modifier
                            .requiredWidth(400.dp)
                            .requiredHeight(800.dp),
                ) {
                    ReadingColumn {
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxSize()
                                    .testTag("content"),
                        )
                    }
                }
            }
        }

        compose.onNodeWithTag("content").assertWidthIsEqualTo(400.dp)
    }
}
