package com.m4isper.contactsapp.presentation.settings

import androidx.lifecycle.ViewModel
import com.m4isper.contactsapp.domain.model.verifyAppLockPin
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Защита приложения: показан PIN-экран, пока контакт не разблокирован. */
@Inject
@ViewModelKey
@ContributesIntoMap(AppScope::class)
class AppLockViewModel : ViewModel() {
    private val unlocked = MutableStateFlow(false)
    val isUnlocked: StateFlow<Boolean> = unlocked.asStateFlow()

    /** Проверяет PIN и снимает блокировку. */
    fun unlock(
        pin: String,
        hash: String,
    ): Boolean {
        val ok = verifyAppLockPin(pin, hash)
        if (ok) unlocked.value = true
        return ok
    }

    /** Снова блокирует приложение: уход в фон. */
    fun lock() {
        unlocked.value = false
    }
}
