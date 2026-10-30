package com.m4isper.contactsapp.presentation.settings

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Duration
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/** Экран блокировки: пад, автоотправка полной длины, очистка после ошибки и блокировка попыток. */
@RunWith(AndroidJUnit4::class)
class AppLockScreenUiTest {
    @get:Rule
    val compose = createComposeRule()

    private fun tapPin(vararg digits: Int) {
        digits.forEach { digit ->
            compose.onNodeWithTag("app_lock_key_$digit").performClick()
        }
    }

    private fun dotsState(): String {
        val node = compose.onNodeWithTag("app_lock_dots").fetchSemanticsNode()
        return node.config[SemanticsProperties.StateDescription]
    }

    @Test
    fun enteringTheFullPinSubmitsAutomatically() {
        val calls = AtomicInteger(0)
        val unlocked = AtomicBoolean(false)
        compose.setContent {
            AppLockScreen(
                onUnlock = { pin ->
                    calls.incrementAndGet()
                    unlocked.set(pin == "1234")
                    true
                },
                pinLength = 4,
            )
        }

        tapPin(1, 2, 3, 4)
        compose.waitUntil(timeoutMillis = 5_000) { unlocked.get() }

        assertEquals(1, calls.get())
    }

    @Test
    fun wrongPin_clearsDotsAndShowsError() {
        val attempts = AtomicInteger(0)
        compose.setContent {
            AppLockScreen(
                onUnlock = { _ ->
                    attempts.incrementAndGet()
                    false
                },
                pinLength = 4,
            )
        }

        tapPin(9, 9, 9, 9)
        compose.waitUntil(timeoutMillis = 5_000) { attempts.get() == 1 }
        compose.waitForIdle()

        compose.onNodeWithText("Wrong PIN code").assertIsDisplayed()
        // Точки очищены: состояние строки снова «0 из 4».
        assertEquals("0 of 4 digits", dotsState())

        // Очистка не блокирует повторную попытку.
        tapPin(9, 9, 9, 9)
        compose.waitUntil(timeoutMillis = 5_000) { attempts.get() == 2 }
        assertEquals(2, attempts.get())
    }

    @Test
    fun backspaceRemovesTheLastDigit() {
        compose.setContent {
            AppLockScreen(onUnlock = { true }, pinLength = 6)
        }

        tapPin(1, 2, 3)
        compose.waitForIdle()
        assertEquals("3 of 6 digits", dotsState())

        compose.onNodeWithTag("app_lock_key_delete").performClick()
        compose.waitForIdle()
        assertEquals("2 of 6 digits", dotsState())
    }

    @Test
    fun legacyMode_showsLengthHintOnShortSubmit() {
        // Установка до обновления: длина неизвестна, отправка вручную.
        compose.setContent {
            AppLockScreen(onUnlock = { true })
        }

        tapPin(1, 2, 3)
        compose.onNodeWithTag("app_lock_unlock_button").assertIsEnabled()
        compose.onNodeWithTag("app_lock_unlock_button").performClick()

        // Короткая попытка не молчит: подсказка о длине подсвечивается.
        compose.onNodeWithTag("app_lock_length_hint").assertIsDisplayed()
        compose.onNodeWithText("4–8 digits").assertIsDisplayed()
    }

    @Test
    fun legacyMode_unlockButtonSubmitsWhenLongEnough() {
        val unlocked = AtomicBoolean(false)
        compose.setContent {
            AppLockScreen(
                onUnlock = { pin ->
                    unlocked.set(pin == "1234")
                    true
                },
            )
        }

        tapPin(1, 2, 3, 4)
        compose.onNodeWithTag("app_lock_unlock_button").performClick()
        compose.waitUntil(timeoutMillis = 5_000) { unlocked.get() }

        assertTrue(unlocked.get())
    }

    @Test
    fun lockout_disablesPadAndCountsDown() {
        compose.setContent {
            AppLockScreen(
                onUnlock = { true },
                pinLength = 4,
                lockedFor = Duration.ofSeconds(5),
            )
        }

        compose.onNodeWithTag("app_lock_key_1").assertIsNotEnabled()
        compose.onNodeWithText("Too many attempts. Try again in 5 s").assertIsDisplayed()
    }

    @Test
    fun lockoutEnds_enablesPad() {
        compose.setContent {
            AppLockScreen(
                onUnlock = { true },
                pinLength = 4,
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

        compose.onNodeWithTag("app_lock_key_1").assertIsEnabled()
    }
}
