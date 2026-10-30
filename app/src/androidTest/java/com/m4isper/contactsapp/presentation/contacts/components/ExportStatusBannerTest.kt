package com.m4isper.contactsapp.presentation.contacts.components

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.m4isper.contactsapp.presentation.contacts.effects.ExportSummary
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExportStatusBannerTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun showsProgressWhileExporting() {
        compose.setContent {
            ExportStatusBanner(inProgress = true, summary = null, onDismiss = {})
        }

        compose.onNodeWithTag("export_progress").assertIsDisplayed()
    }

    @Test
    fun summaryStaysUntilDismissed() {
        val summary = mutableStateOf<ExportSummary?>(ExportSummary("Exported 2 contacts"))
        compose.setContent {
            ExportStatusBanner(
                inProgress = false,
                summary = summary.value,
                onDismiss = { summary.value = null },
            )
        }

        compose.onNodeWithText("Exported 2 contacts").assertIsDisplayed()
        compose.onNodeWithTag("export_summary_close").performClick()

        compose.onNodeWithTag("export_summary").assertDoesNotExist()
    }

    @Test
    fun failedSummaryIsStillReadable() {
        compose.setContent {
            ExportStatusBanner(
                inProgress = false,
                summary = ExportSummary("Export failed", failed = true),
                onDismiss = {},
            )
        }

        compose.onNodeWithTag("export_summary").assertIsDisplayed()
        compose.onNodeWithText("Export failed").assertIsDisplayed()
    }
}
