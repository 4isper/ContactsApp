package com.m4isper.contactsapp.presentation.settings

import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.m4isper.contactsapp.FakeAndroidRepository
import com.m4isper.contactsapp.FakeSettingsRepository
import com.m4isper.contactsapp.domain.model.AppSettings
import com.m4isper.contactsapp.domain.model.ContactSortOrder
import com.m4isper.contactsapp.domain.model.ListDensity
import com.m4isper.contactsapp.domain.model.ThemeMode
import com.m4isper.contactsapp.domain.usecase.GetContactAccountsUseCase
import com.m4isper.contactsapp.domain.usecase.ObserveSettingsUseCase
import com.m4isper.contactsapp.domain.usecase.SetDefaultContactAccountUseCase
import com.m4isper.contactsapp.domain.usecase.SetFavoritesFirstUseCase
import com.m4isper.contactsapp.domain.usecase.SetListDensityUseCase
import com.m4isper.contactsapp.domain.usecase.SetSortOrderUseCase
import com.m4isper.contactsapp.domain.usecase.SetThemeModeUseCase
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicBoolean

@RunWith(AndroidJUnit4::class)
class SettingsUiTest {
    @get:Rule
    val compose = createComposeRule()

    private fun viewModel(repository: FakeSettingsRepository) =
        SettingsViewModel(
            ObserveSettingsUseCase(repository),
            SetThemeModeUseCase(repository),
            SetSortOrderUseCase(repository),
            SetListDensityUseCase(repository),
            SetFavoritesFirstUseCase(repository),
            SetDefaultContactAccountUseCase(repository),
            GetContactAccountsUseCase(FakeAndroidRepository()),
        )

    @Test
    fun showsStoredSettingsSelected() {
        val repository =
            FakeSettingsRepository(
                AppSettings(
                    themeMode = ThemeMode.DARK,
                    sortOrder = ContactSortOrder.BY_FAMILY,
                    listDensity = ListDensity.COMPACT,
                    favoritesFirst = false,
                ),
            )
        compose.setContent {
            SettingsScreen(onBack = {}, viewModel = viewModel(repository))
        }

        compose.onNodeWithTag("theme_DARK").assertIsSelected()
        compose.onNodeWithTag("sort_BY_FAMILY").assertIsSelected()
        compose.onNodeWithTag("density_COMPACT").assertIsSelected()
        compose.onNodeWithTag("favorites_first_switch").assertIsOff()
    }

    @Test
    fun selectingOptionsPersistsThem() {
        val repository = FakeSettingsRepository()
        compose.setContent {
            SettingsScreen(onBack = {}, viewModel = viewModel(repository))
        }

        compose.onNodeWithTag("theme_LIGHT").performClick()
        compose.onNodeWithTag("sort_BY_FAMILY").performClick()
        compose.onNodeWithTag("density_COMPACT").performClick()
        compose.onNodeWithTag("favorites_first_switch").performClick()

        compose.waitUntil(timeoutMillis = 5_000) {
            repository.themeModes.isNotEmpty() &&
                repository.sortOrders.isNotEmpty() &&
                repository.densities.isNotEmpty() &&
                repository.favoritesFirstValues.isNotEmpty()
        }
        assertEquals(listOf(ThemeMode.LIGHT), repository.themeModes)
        assertEquals(listOf(ContactSortOrder.BY_FAMILY), repository.sortOrders)
        assertEquals(listOf(ListDensity.COMPACT), repository.densities)
        assertEquals(listOf(false), repository.favoritesFirstValues)
    }

    @Test
    fun backButtonInvokesCallback() {
        val backPressed = AtomicBoolean(false)
        compose.setContent {
            SettingsScreen(
                onBack = { backPressed.set(true) },
                viewModel = viewModel(FakeSettingsRepository()),
            )
        }

        compose.onNodeWithTag("settings_back_button").performClick()

        assertEquals(true, backPressed.get())
    }
}
