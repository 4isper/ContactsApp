package com.m4isper.contactsapp.presentation.contacts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.m4isper.contactsapp.domain.model.ContactDetails
import com.m4isper.contactsapp.domain.usecase.GetContactsUseCase
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Locale

sealed interface ContactsContent {
    data object Loading : ContactsContent

    data class Contacts(
        val items: List<ContactDetails>,
        val isSearching: Boolean,
    ) : ContactsContent

    data class Empty(
        val isSearching: Boolean,
    ) : ContactsContent

    data class Error(
        val message: String?,
    ) : ContactsContent
}

data class ContactsUiState(
    val searchQuery: String = "",
    val content: ContactsContent = ContactsContent.Loading,
)

internal const val SUBSCRIBE_TIMEOUT_MILLIS = 5_000L

@Inject
@ViewModelKey
@ContributesIntoMap(AppScope::class)
class ContactsViewModel(
    private val getContactsUseCase: GetContactsUseCase,
) : ViewModel() {
    private val allContacts = MutableStateFlow<List<ContactDetails>>(emptyList())
    private val isLoading = MutableStateFlow(false)
    private val searchQueryFlow = MutableStateFlow("")
    private val loadFailure = MutableStateFlow<Throwable?>(null)

    val uiState: StateFlow<ContactsUiState> =
        combine(allContacts, isLoading, searchQueryFlow, loadFailure) { all, loading, query, failure ->
            val trimmed = query.trim()
            val visible =
                if (trimmed.isBlank()) {
                    all
                } else {
                    val q = trimmed.lowercase(Locale.ROOT)
                    all.filter { contact ->
                        contact.name.lowercase(Locale.ROOT).contains(q) ||
                            contact.phones.any { it.number.contains(trimmed) }
                    }
                }
            val content =
                when {
                    failure != null -> ContactsContent.Error(failure.message)
                    loading && visible.isEmpty() -> ContactsContent.Loading
                    visible.isEmpty() -> ContactsContent.Empty(trimmed.isNotBlank())
                    else -> ContactsContent.Contacts(visible, trimmed.isNotBlank())
                }
            ContactsUiState(searchQuery = query, content = content)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(SUBSCRIBE_TIMEOUT_MILLIS),
            initialValue = ContactsUiState(),
        )

    fun onSearchQueryChange(query: String) {
        searchQueryFlow.value = query
    }

    // Catch-all осознанный: любая ошибка загрузки превращается в Error-стейт.
    @Suppress("TooGenericExceptionCaught")
    fun refresh() {
        if (isLoading.value) return
        viewModelScope.launch {
            isLoading.value = true
            loadFailure.value = null
            try {
                allContacts.value = getContactsUseCase()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                loadFailure.value = e
            } finally {
                isLoading.value = false
            }
        }
    }

    fun clear() {
        allContacts.value = emptyList()
    }
}
