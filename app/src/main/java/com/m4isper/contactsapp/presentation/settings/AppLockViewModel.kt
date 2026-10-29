package com.m4isper.contactsapp.presentation.settings

import androidx.lifecycle.ViewModel
import com.m4isper.contactsapp.domain.repository.settings.PrivacySettings
import com.m4isper.contactsapp.domain.security.AppLockAttempts
import com.m4isper.contactsapp.domain.security.PinHasher
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.time.Duration

/**
 * Защита приложения: показан PIN-экран, пока контакт не разблокирован.
 * Проверка PIN идёт в фоне: хэш считается медленно и не должен блокировать интерфейс.
 */
@Inject
@ViewModelKey
@ContributesIntoMap(AppScope::class)
class AppLockViewModel(
    private val privacySettings: PrivacySettings,
    private val pinHasher: PinHasher,
) : ViewModel() {
    private val unlocked = MutableStateFlow(false)
    val isUnlocked: StateFlow<Boolean> = unlocked.asStateFlow()
    private val lockWait = MutableStateFlow(Duration.ZERO)

    /** Сколько осталось ждать до следующей попытки ввода PIN. */
    val lockRemaining: StateFlow<Duration> = lockWait.asStateFlow()
    private val attempts = AppLockAttempts()

    /**
     * Проверяет PIN и снимает блокировку.
     * @return true, если PIN верный и приложение разблокировано.
     */
    suspend fun unlock(
        pin: String,
        hash: String,
    ): Boolean {
        if (attempts.isLocked()) {
            publishLockWait()
            return false
        }
        val ok = withContext(Dispatchers.Default) { pinHasher.verify(pin, hash) }
        if (ok) {
            attempts.registerSuccess()
            lockWait.value = Duration.ZERO
            unlocked.value = true
            migrateLegacyHash(pin, hash)
        } else {
            attempts.registerFailure()
            publishLockWait()
        }
        return ok
    }

    /** Биометрическая разблокировка: снимает блокировку и сбрасывает бюджет попыток PIN. */
    fun unlockBiometric() {
        attempts.registerSuccess()
        lockWait.value = Duration.ZERO
        unlocked.value = true
    }

    /** Снова блокирует приложение: уход в фон. */
    fun lock() {
        unlocked.value = false
    }

    /** Старый хэш SHA-256 заменяем на PBKDF2 после первого успешного ввода. */
    private suspend fun migrateLegacyHash(
        pin: String,
        hash: String,
    ) {
        if (!pinHasher.needsMigration(hash)) return
        withContext(Dispatchers.Default) {
            privacySettings.setAppLock(true, pinHasher.hash(pin))
        }
    }

    private fun publishLockWait() {
        lockWait.value = attempts.remainingLock()
    }
}
