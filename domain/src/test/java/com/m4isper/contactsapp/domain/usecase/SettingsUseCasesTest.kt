package com.m4isper.contactsapp.domain.usecase

import com.m4isper.contactsapp.domain.model.AppSettings
import com.m4isper.contactsapp.domain.model.ContactSortOrder
import com.m4isper.contactsapp.domain.model.ListDensity
import com.m4isper.contactsapp.domain.model.ThemeMode
import com.m4isper.contactsapp.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

private class StubSettingsRepository(
    initial: AppSettings = AppSettings(),
) : SettingsRepository {
    private val state = MutableStateFlow(initial)

    val themeModes = mutableListOf<ThemeMode>()
    val sortOrders = mutableListOf<ContactSortOrder>()
    val densities = mutableListOf<ListDensity>()

    override fun observeSettings(): Flow<AppSettings> = state

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
}

class SettingsUseCasesTest {
    @Test
    fun `observe settings returns repository flow`() =
        runTest {
            val expected =
                AppSettings(
                    themeMode = ThemeMode.DARK,
                    sortOrder = ContactSortOrder.BY_FAMILY,
                    listDensity = ListDensity.COMPACT,
                )
            val repository = StubSettingsRepository(expected)

            assertEquals(expected, ObserveSettingsUseCase(repository)().first())
        }

    @Test
    fun `set theme mode delegates to repository`() =
        runTest {
            val repository = StubSettingsRepository()

            SetThemeModeUseCase(repository)(ThemeMode.LIGHT)

            assertEquals(listOf(ThemeMode.LIGHT), repository.themeModes)
        }

    @Test
    fun `set sort order delegates to repository`() =
        runTest {
            val repository = StubSettingsRepository()

            SetSortOrderUseCase(repository)(ContactSortOrder.BY_FAMILY)

            assertEquals(listOf(ContactSortOrder.BY_FAMILY), repository.sortOrders)
        }

    @Test
    fun `set list density delegates to repository`() =
        runTest {
            val repository = StubSettingsRepository()

            SetListDensityUseCase(repository)(ListDensity.COMPACT)

            assertEquals(listOf(ListDensity.COMPACT), repository.densities)
        }
}
