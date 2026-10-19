package com.m4isper.contactsapp.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.m4isper.contactsapp.domain.model.ThemeMode
import com.m4isper.contactsapp.domain.usecase.ObserveSettingsUseCase
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** Тема для корня приложения: читается до отрисовки навигации. */
@Inject
@ViewModelKey
@ContributesIntoMap(AppScope::class)
class ThemeViewModel(
    observeSettingsUseCase: ObserveSettingsUseCase,
) : ViewModel() {
    val themeMode: StateFlow<ThemeMode> =
        observeSettingsUseCase()
            .map { it.themeMode }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = ThemeMode.SYSTEM,
            )
}
