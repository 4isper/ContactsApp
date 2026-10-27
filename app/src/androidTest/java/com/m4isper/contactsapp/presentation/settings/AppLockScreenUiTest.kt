package com.m4isper.contactsapp.presentation.settings

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Duration
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/** Экран блокировки: IME-Done, очистка после ошибки, показ PIN и блокировка попыток. */
@RunWith(AndroidJUnit4::class)
class AppLockScreenUiTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun imeDone_triggersUnlockOnce() {
        val calls = AtomicInteger(0)
        val unlocked = AtomicBoolean(false)
        compose.setContent {
            AppLockScreen(
                onUnlock = { pin ->
                    calls.incrementAndGet()
                    unlocked.set(pin == "1234")
                    true
                },
            )
        }

        compose.onNodeWithTag("app_lock_pin_field").performTextInput("1234")
        compose.onNodeWithTag("app_lock_pin_field").performImeAction()
        compose.waitUntil(timeoutMillis = 5_000) { unlocked.get() }

        assertEquals(1, calls.get())
    }

    @Test
    fun wrongPin_clearsFieldAndShowsError() {
        val attemptFinished = AtomicBoolean(false)
        compose.setContent {
            AppLockScreen(
                onUnlock = { _ ->
                    attemptFinished.set(true)
                    false
                },
            )
        }

        compose.onNodeWithTag("app_lock_pin_field").performTextInput("9999")
        compose.onNodeWithTag("app_lock_pin_field").performImeAction()
        compose.waitUntil(timeoutMillis = 5_000) { attemptFinished.get() }
        compose.waitForIdle()

        compose.onNodeWithText("Wrong PIN code").assertIsDisplayed()
        // Поле очищено: маска показывает «•», поэтому смотрим EditableText напрямую.
        val field = compose.onNodeWithTag("app_lock_pin_field").fetchSemanticsNode()
        assertEquals("", field.config[SemanticsProperties.EditableText].toString())
        compose.onNodeWithTag("app_lock_unlock_button").assertIsNotEnabled()
    }

    @Test
    fun showPinToggle_switchesVisibility() {
        compose.setContent {
            AppLockScreen(onUnlock = { true })
        }

        compose.onNodeWithContentDescription("Show PIN code").performClick()
        compose.onNodeWithContentDescription("Hide PIN code").assertIsDisplayed()

        compose.onNodeWithContentDescription("Hide PIN code").performClick()
        compose.onNodeWithContentDescription("Show PIN code").assertIsDisplayed()
    }

    @Test
    fun lockout_disablesInputAndCountsDown() {
        compose.setContent {
            AppLockScreen(
                onUnlock = { true },
                lockedFor = Duration.ofSeconds(5),
            )
        }

        compose.onNodeWithTag("app_lock_pin_field").assertIsNotEnabled()
        compose.onNodeWithTag("app_lock_unlock_button").assertIsNotEnabled()
        compose.onNodeWithText("Too many attempts. Try again in 5 s").assertIsDisplayed()
    }

    @Test
    fun lockoutEnds_unlocksInput() {
        compose.setContent {
            AppLockScreen(
                onUnlock = { true },
                lockedFor = Duration.ofSeconds(1),
            )
        }

        compose.waitUntil(timeoutMillis = 5_000) {
            compose
                .onAllNodesWithText("Too many attempts. Try again in 1 s")
                .fetchSemanticsNodes()
                .isEmpty()
        }
        compose.waitForIdle()

        compose.onNodeWithTag("app_lock_pin_field").assertIsEnabled()
        compose.onNodeWithTag("app_lock_pin_field").performTextInput("1234")
        compose.onNodeWithTag("app_lock_unlock_button").assertIsEnabled()
    }
}
