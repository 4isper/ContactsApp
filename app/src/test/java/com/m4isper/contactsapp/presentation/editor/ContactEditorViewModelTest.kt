package com.m4isper.contactsapp.presentation.editor

import com.m4isper.contactsapp.domain.model.ContactForm
import com.m4isper.contactsapp.domain.repository.ContactsRepository
import com.m4isper.contactsapp.domain.repository.FavoritesRepository
import com.m4isper.contactsapp.domain.usecase.CreateContactUseCase
import com.m4isper.contactsapp.domain.usecase.DeleteContactUseCase
import com.m4isper.contactsapp.domain.usecase.GetContactFormUseCase
import com.m4isper.contactsapp.domain.usecase.UpdateContactUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private class FakeEditorRepository :
    ContactsRepository,
    FavoritesRepository {
    val created = mutableListOf<ContactForm>()

    override suspend fun getContacts() = emptyList<com.m4isper.contactsapp.domain.model.ContactDetails>()

    override suspend fun getContactDetails(contactId: Long) = null

    override suspend fun getContactForm(contactId: Long): ContactForm? = null

    override suspend fun createContact(form: ContactForm) {
        created += form
    }

    override suspend fun updateContact(
        contactId: Long,
        form: ContactForm,
    ) = Unit

    override suspend fun deleteContact(contactId: Long) = Unit

    override fun observeFavorites(): Flow<Set<Long>> = MutableStateFlow(emptySet<Long>()).asStateFlow()

    override suspend fun toggleFavorite(contactId: Long) = Unit

    override suspend fun removeFavorite(contactId: Long) = Unit
}

@OptIn(ExperimentalCoroutinesApi::class)
class ContactEditorViewModelTest {
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `save closes screen on JVM`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val repository = FakeEditorRepository()
            val viewModel =
                ContactEditorViewModel(
                    GetContactFormUseCase(repository),
                    CreateContactUseCase(repository),
                    UpdateContactUseCase(repository),
                    DeleteContactUseCase(repository, repository),
                )
            backgroundScope.launch { viewModel.uiState.collect { } }
            viewModel.load(null)
            advanceUntilIdle()

            viewModel.onNameChange("Anna")
            viewModel.onSave()
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.closeScreen)
            assertEquals(1, repository.created.size)
        }
}
