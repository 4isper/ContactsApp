package com.m4isper.contactsapp

import com.m4isper.contactsapp.domain.model.AppSettings
import com.m4isper.contactsapp.domain.model.ContactAccount
import com.m4isper.contactsapp.domain.model.ContactSortOrder
import com.m4isper.contactsapp.domain.model.ListDensity
import com.m4isper.contactsapp.domain.model.ThemeMode
import com.m4isper.contactsapp.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Фейк репозитория настроек: in-memory состояние и запись вызовов для проверок. */
class FakeSettingsRepository(
    initial: AppSettings = AppSettings(),
) : SettingsRepository {
    private val state = MutableStateFlow(initial)

    /** Текущие настройки; тесты могут менять их отсюда, чтобы проверить реакцию UI. */
    val settings: StateFlow<AppSettings> = state

    val themeModes = mutableListOf<ThemeMode>()
    val sortOrders = mutableListOf<ContactSortOrder>()
    val densities = mutableListOf<ListDensity>()
    val lastContactAccounts = mutableListOf<ContactAccount>()

    override fun observeSettings(): StateFlow<AppSettings> = state

    override suspend fun setThemeMode(mode: ThemeMode) {
        themeModes += mode
        state.value = state.value.copy(themeMode = mode)
    }

    override suspend fun setSortOrder(order: ContactSortOrder) {
        sortOrders += order
        state.value = state.value.copy(sortOrder = order)
    }

    override suspend fun setListDensity(density: ListDensity) {
        densities += density
        state.value = state.value.copy(listDensity = density)
    }

    override suspend fun setLastContactAccount(account: ContactAccount) {
        lastContactAccounts += account
        state.value = state.value.copy(lastContactAccount = account)
    }
}
