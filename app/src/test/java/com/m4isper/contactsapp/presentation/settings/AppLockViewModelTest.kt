package com.m4isper.contactsapp.presentation.settings

import com.m4isper.contactsapp.FakeSettingsRepository
import com.m4isper.contactsapp.data.security.Pbkdf2PinHasher
import com.m4isper.contactsapp.domain.security.AppLockAttempts
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration

class AppLockViewModelTest {
    private val repository = FakeSettingsRepository()
    private val hasher = Pbkdf2PinHasher()

    private fun viewModel() = AppLockViewModel(repository, hasher)

    @Test
    fun `correct pin unlocks the app`() =
        runTest {
            val viewModel = viewModel()
            val hash = hasher.hash("1234")

            assertTrue(viewModel.unlock("1234", hash))
            assertTrue(viewModel.isUnlocked.value)
        }

    @Test
    fun `wrong pin keeps the app locked`() =
        runTest {
            val viewModel = viewModel()
            val hash = hasher.hash("1234")

            assertFalse(viewModel.unlock("9999", hash))
            assertFalse(viewModel.isUnlocked.value)
        }

    @Test
    fun `lock blocks the app again`() =
        runTest {
            val viewModel = viewModel()
            val hash = hasher.hash("1234")
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
            val hash = hasher.hash("1234")

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
    fun `a new hash with a known length is not rewritten`() =
        runTest {
            val viewModel = viewModel()
            val hash = hasher.hash("1234")

            assertTrue(viewModel.unlock("1234", hash, storedLength = 4))

            assertTrue(repository.appLocks.isEmpty())
        }

    @Test
    fun `the first unlock stores the pin length for legacy installs`() =
        runTest {
            val viewModel = viewModel()
            val hash = hasher.hash("1234")

            // Установка до обновления: длина не сохранена.
            assertTrue(viewModel.unlock("1234", hash, storedLength = 0))

            val saved = repository.appLocks.single()
            assertEquals(4, saved.third)
        }

    @Test
    fun `a known length is not rewritten`() =
        runTest {
            val viewModel = viewModel()
            val hash = hasher.hash("1234")

            assertTrue(viewModel.unlock("1234", hash, storedLength = 4))

            assertTrue(repository.appLocks.isEmpty())
        }

    @Test
    fun `biometric success unlocks without a pin`() {
        val viewModel = viewModel()

        viewModel.unlockBiometric()

        assertTrue(viewModel.isUnlocked.value)
        assertTrue(repository.appLocks.isEmpty())
    }

    @Test
    fun `biometric success clears the pin lockout`() =
        runTest {
            val viewModel = viewModel()
            val hash = hasher.hash("1234")

            repeat(AppLockAttempts.DEFAULT_MAX_ATTEMPTS) {
                viewModel.unlock("9999", hash)
            }
            assertTrue(viewModel.lockRemaining.value > Duration.ZERO)

            viewModel.unlockBiometric()

            assertTrue(viewModel.isUnlocked.value)
            assertEquals(Duration.ZERO, viewModel.lockRemaining.value)
            // Следующий PIN-ввод снова принимается: бюджет попыток сброшен.
            viewModel.lock()
            assertTrue(viewModel.unlock("1234", hash))
        }
}
