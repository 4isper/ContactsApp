package com.m4isper.contactsapp.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.m4isper.contactsapp.domain.model.settings.ThemeMode
import com.m4isper.contactsapp.domain.usecase.settings.ObserveSettingsUseCase
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** Настройки, нужные всему приложению: тема, фотографии, защита и язык. */
data class AppUiSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = true,
    val showPhotos: Boolean = true,
    val appLockEnabled: Boolean = false,
    val appLockPinHash: String = "",
    val secureWindow: Boolean = false,
    val appLanguage: String = "",
)

/** Настройки приложения для корня: читаются до отрисовки навигации. */
@Inject
@ViewModelKey
@ContributesIntoMap(AppScope::class)
class AppSettingsViewModel(
    observeSettingsUseCase: ObserveSettingsUseCase,
) : ViewModel() {
    val settings: StateFlow<AppUiSettings> =
        observeSettingsUseCase()
            .map { settings ->
                AppUiSettings(
                    themeMode = settings.themeMode,
                    dynamicColor = settings.dynamicColor,
                    showPhotos = settings.showPhotos,
                    appLockEnabled = settings.appLockEnabled,
                    appLockPinHash = settings.appLockPinHash,
                    secureWindow = settings.secureWindow,
                    appLanguage = settings.appLanguage,
                )
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                // До первой эмиссии DataStore считаем защиту включённой: так контакты
                // не показываются до загрузки реальных настроек.
                initialValue = AppUiSettings(appLockEnabled = true),
            )
}
