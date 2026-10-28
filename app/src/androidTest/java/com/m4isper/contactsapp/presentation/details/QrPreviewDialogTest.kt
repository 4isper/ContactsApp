package com.m4isper.contactsapp.presentation.details

import android.graphics.Bitmap
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.m4isper.contactsapp.presentation.common.share.qrCodeBitmap
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicBoolean

@RunWith(AndroidJUnit4::class)
class QrPreviewDialogTest {
    @get:Rule
    val compose = createComposeRule()

    private fun awaitImage() {
        compose.waitUntil(timeoutMillis = 5_000) {
            compose.onAllNodesWithTag("qr_preview_image").fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun showsPlaceholderUntilTheCodeIsEncoded() {
        val encoded = CompletableDeferred<Bitmap?>()
        compose.setContent {
            QrPreviewDialog(
                contactName = "Anna",
                vCardText = VCARD,
                onShare = {},
                onDismiss = {},
                encode = { encoded.await() },
            )
        }

        compose.onNodeWithTag("qr_preview_placeholder").assertIsDisplayed()
        compose.onNodeWithTag("qr_preview_share").assertDoesNotExist()

        encoded.complete(qrCodeBitmap(VCARD, sizePx = SMALL_QR))
        compose.waitForIdle()

        compose.onNodeWithTag("qr_preview_placeholder").assertDoesNotExist()
        compose.onNodeWithTag("qr_preview_image").assertIsDisplayed()
    }

    @Test
    fun showsNameAndScanHint() {
        compose.setContent {
            QrPreviewDialog(
                contactName = "Anna",
                vCardText = VCARD,
                onShare = {},
                onDismiss = {},
                encode = { qrCodeBitmap(it, sizePx = SMALL_QR) },
            )
        }
        awaitImage()

        compose.onNodeWithText("Anna").assertIsDisplayed()
        compose.onNodeWithText("Point another phone's camera at this code to add the contact").assertIsDisplayed()
    }

    @Test
    fun shareButtonInvokesShare() {
        val shared = AtomicBoolean(false)
        compose.setContent {
            QrPreviewDialog(
                contactName = "Anna",
                vCardText = VCARD,
                onShare = { shared.set(true) },
                onDismiss = {},
                encode = { qrCodeBitmap(it, sizePx = SMALL_QR) },
            )
        }
        awaitImage()

        compose.onNodeWithTag("qr_preview_share").performClick()

        assertTrue(shared.get())
    }

    @Test
    fun closeButtonInvokesDismiss() {
        val dismissed = AtomicBoolean(false)
        compose.setContent {
            QrPreviewDialog(
                contactName = "Anna",
                vCardText = VCARD,
                onShare = {},
                onDismiss = { dismissed.set(true) },
                encode = { qrCodeBitmap(it, sizePx = SMALL_QR) },
            )
        }
        awaitImage()

        compose.onNodeWithTag("qr_preview_close").performClick()

        assertTrue(dismissed.get())
    }

    @Test
    fun failedEncodingShowsErrorWithoutShare() {
        compose.setContent {
            QrPreviewDialog(
                contactName = "Anna",
                vCardText = VCARD,
                onShare = {},
                onDismiss = {},
                encode = { null },
            )
        }
        compose.waitUntil(timeoutMillis = 5_000) {
            compose.onAllNodesWithTag("qr_preview_placeholder").fetchSemanticsNodes().isEmpty()
        }

        compose.onAllNodesWithText("Couldn't share the contact").assertCountEquals(1)
        compose.onNodeWithTag("qr_preview_share").assertDoesNotExist()
        compose.onNodeWithTag("qr_preview_close").assertIsDisplayed()
    }

    private companion object {
        const val VCARD = "BEGIN:VCARD\nFN:Anna\nEND:VCARD"
        const val SMALL_QR = 64
    }
}
