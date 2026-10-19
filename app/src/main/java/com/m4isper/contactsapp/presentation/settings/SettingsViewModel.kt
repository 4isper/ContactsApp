package com.m4isper.contactsapp.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.m4isper.contactsapp.domain.model.AppSettings
import com.m4isper.contactsapp.domain.model.ContactSortOrder
import com.m4isper.contactsapp.domain.model.ListDensity
import com.m4isper.contactsapp.domain.model.ThemeMode
import com.m4isper.contactsapp.domain.usecase.ObserveSettingsUseCase
import com.m4isper.contactsapp.domain.usecase.SetListDensityUseCase
import com.m4isper.contactsapp.domain.usecase.SetSortOrderUseCase
import com.m4isper.contactsapp.domain.usecase.SetThemeModeUseCase
import com.m4isper.contactsapp.presentation.common.logw
import com.m4isper.contactsapp.presentation.contacts.SUBSCRIBE_TIMEOUT_MILLIS
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val SETTINGS_TAG = "SettingsScreen"

data class SettingsUiState(
    val settings: AppSettings = AppSettings(),
)

@Inject
@ViewModelKey
@ContributesIntoMap(AppScope::class)
class SettingsViewModel(
    observeSettingsUseCase: ObserveSettingsUseCase,
    private val setThemeModeUseCase: SetThemeModeUseCase,
    private val setSortOrderUseCase: SetSortOrderUseCase,
    private val setListDensityUseCase: SetListDensityUseCase,
) : ViewModel() {
    val uiState: StateFlow<SettingsUiState> =
        observeSettingsUseCase()
            .map(::SettingsUiState)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(SUBSCRIBE_TIMEOUT_MILLIS),
                initialValue = SettingsUiState(),
            )

    fun onThemeModeSelected(mode: ThemeMode) = update { setThemeModeUseCase(mode) }

    fun onSortOrderSelected(order: ContactSortOrder) = update { setSortOrderUseCase(order) }

    fun onListDensitySelected(density: ListDensity) = update { setListDensityUseCase(density) }

    // Catch-all осознанный: ошибка записи настроек не должна ронять экран.
    @Suppress("TooGenericExceptionCaught")
    private fun update(block: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logw(SETTINGS_TAG, "update settings failed", e)
            }
        }
    }
}
