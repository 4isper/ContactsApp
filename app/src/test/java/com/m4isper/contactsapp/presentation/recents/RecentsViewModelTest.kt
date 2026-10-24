package com.m4isper.contactsapp.presentation.recents

import com.m4isper.contactsapp.domain.model.contact.ContactDetails
import com.m4isper.contactsapp.domain.usecase.contacts.GetContactsUseCase
import com.m4isper.contactsapp.domain.usecase.contacts.ObserveFavoritesUseCase
import com.m4isper.contactsapp.domain.usecase.contacts.ToggleFavoriteUseCase
import com.m4isper.contactsapp.presentation.common.Loadable
import com.m4isper.contactsapp.testing.FakeContactsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RecentsViewModelTest {
    private val testScheduler = TestCoroutineScheduler()
    private val testDispatcher = StandardTestDispatcher(testScheduler)

    private val repository =
        FakeContactsRepository(
            contacts =
                listOf(
                    ContactDetails(contactId = 1L, name = "Anna", lastContacted = 100L),
                ),
        )

    private lateinit var viewModel: RecentsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel =
            RecentsViewModel(
                getContactsUseCase = GetContactsUseCase(repository),
                observeFavoritesUseCase = ObserveFavoritesUseCase(repository),
                toggleFavoriteUseCase = ToggleFavoriteUseCase(repository),
            )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun loadPopulatesRecents() =
        runTest(testScheduler) {
            backgroundScope.launch { viewModel.uiState.collect { } }
            viewModel.load()
            advanceUntilIdle()
            val state = viewModel.uiState.value
            assertTrue(state.content is Loadable.Content<*>)
        }

    @Test
    fun clearEmptiesContent() =
        runTest(testScheduler) {
            backgroundScope.launch { viewModel.uiState.collect { } }
            viewModel.load()
            advanceUntilIdle()
            viewModel.clear()
            advanceUntilIdle()
            assertEquals(Loadable.Empty, viewModel.uiState.value.content)
        }
}
