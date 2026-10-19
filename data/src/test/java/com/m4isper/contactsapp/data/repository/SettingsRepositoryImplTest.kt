package com.m4isper.contactsapp.data.repository

import com.m4isper.contactsapp.data.datasource.SettingsDataSource
import com.m4isper.contactsapp.domain.model.AppSettings
import com.m4isper.contactsapp.domain.model.ContactSortOrder
import com.m4isper.contactsapp.domain.model.ListDensity
import com.m4isper.contactsapp.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

private class FakeSettingsDataSource(
    initial: AppSettings = AppSettings(),
) : SettingsDataSource {
    private val state = MutableStateFlow(initial)

    val themeModes = mutableListOf<String>()
    val sortOrders = mutableListOf<String>()
    val densities = mutableListOf<String>()

    override val settings: Flow<AppSettings> = state

    override suspend fun setThemeMode(mode: String) {
        themeModes += mode
    }

    override suspend fun setSortOrder(order: String) {
        sortOrders += order
    }

    override suspend fun setListDensity(density: String) {
        densities += density
    }
}

class SettingsRepositoryImplTest {
    @Test
    fun `observe settings delegates to data source`() =
        runBlocking {
            val expected =
                AppSettings(
                    themeMode = ThemeMode.DARK,
                    sortOrder = ContactSortOrder.BY_FAMILY,
                    listDensity = ListDensity.COMPACT,
                )
            val repository = SettingsRepositoryImpl(FakeSettingsDataSource(expected))

            assertEquals(expected, repository.observeSettings().first())
        }

    @Test
    fun `set theme mode stores enum name`() =
        runBlocking {
            val dataSource = FakeSettingsDataSource()

            SettingsRepositoryImpl(dataSource).setThemeMode(ThemeMode.LIGHT)

            assertEquals(listOf("LIGHT"), dataSource.themeModes)
        }

    @Test
    fun `set sort order stores enum name`() =
        runBlocking {
            val dataSource = FakeSettingsDataSource()

            SettingsRepositoryImpl(dataSource).setSortOrder(ContactSortOrder.BY_FAMILY)

            assertEquals(listOf("BY_FAMILY"), dataSource.sortOrders)
        }

    @Test
    fun `set list density stores enum name`() =
        runBlocking {
            val dataSource = FakeSettingsDataSource()

            SettingsRepositoryImpl(dataSource).setListDensity(ListDensity.COMPACT)

            assertEquals(listOf("COMPACT"), dataSource.densities)
        }
}
