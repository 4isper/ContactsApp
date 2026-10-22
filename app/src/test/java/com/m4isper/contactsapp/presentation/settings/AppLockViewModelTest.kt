package com.m4isper.contactsapp.presentation.settings

import com.m4isper.contactsapp.FakeSettingsRepository
import com.m4isper.contactsapp.domain.model.AppLockAttempts
import com.m4isper.contactsapp.domain.model.hashAppLockPin
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration

class AppLockViewModelTest {
    private val repository = FakeSettingsRepository()

    private fun viewModel() = AppLockViewModel(repository)

    @Test
    fun `correct pin unlocks the app`() =
        runTest {
            val viewModel = viewModel()
            val hash = hashAppLockPin("1234")

            assertTrue(viewModel.unlock("1234", hash))
            assertTrue(viewModel.isUnlocked.value)
        }

    @Test
    fun `wrong pin keeps the app locked`() =
        runTest {
            val viewModel = viewModel()
            val hash = hashAppLockPin("1234")

            assertFalse(viewModel.unlock("9999", hash))
            assertFalse(viewModel.isUnlocked.value)
        }

    @Test
    fun `lock blocks the app again`() =
        runTest {
            val viewModel = viewModel()
            val hash = hashAppLockPin("1234")
            viewModel.unlock("1234", hash)

            viewModel.lock()

            assertFalse(viewModel.isUnlocked.value)
        }

    @Test
    fun `empty hash never unlocks`() =
        runTest {
            val viewModel = viewModel()

            assertFalse(viewModel.unlock("1234", ""))
            assertFalse(viewModel.isUnlocked.value)
            assertEquals(false, viewModel.isUnlocked.value)
        }

    @Test
    fun `too many wrong pins block the input`() =
        runTest {
            val viewModel = viewModel()
            val hash = hashAppLockPin("1234")

            repeat(AppLockAttempts.DEFAULT_MAX_ATTEMPTS) {
                assertFalse(viewModel.unlock("9999", hash))
            }

            assertTrue(viewModel.lockRemaining.value > Duration.ZERO)
            // Даже верный PIN не проходит, пока действует блокировка.
            assertFalse(viewModel.unlock("1234", hash))
            assertFalse(viewModel.isUnlocked.value)
        }

    @Test
    fun `a legacy hash is replaced after a successful unlock`() =
        runTest {
            val viewModel = viewModel()
            val legacyHash = "078e8919d760dd638c367b6b18b351300915f5fd23689f15ce1ba4d598cdea04"

            assertTrue(viewModel.unlock("1234", legacyHash))

            assertEquals(1, repository.appLocks.size)
            val saved = repository.appLocks.single()
            assertTrue(saved.second.startsWith("pbkdf2-sha256\$"))
        }

    @Test
    fun `a new hash is not rewritten`() =
        runTest {
            val viewModel = viewModel()
            val hash = hashAppLockPin("1234")

            assertTrue(viewModel.unlock("1234", hash))

            assertTrue(repository.appLocks.isEmpty())
        }
}
