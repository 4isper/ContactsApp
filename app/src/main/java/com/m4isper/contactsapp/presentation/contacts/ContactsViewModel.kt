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
    data class Contacts(val items: List<ContactDetails>, val isSearching: Boolean) : ContactsContent
    data class Empty(val isSearching: Boolean) : ContactsContent
    data class Error(val message: String?) : ContactsContent
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

    private val _allContacts = MutableStateFlow<List<ContactDetails>>(emptyList())
    private val _isLoading = MutableStateFlow(false)
    private val _searchQuery = MutableStateFlow("")
    private val _failure = MutableStateFlow<Throwable?>(null)

    val uiState: StateFlow<ContactsUiState> =
        combine(_allContacts, _isLoading, _searchQuery, _failure) { all, loading, query, failure ->
            val trimmed = query.trim()
            val visible = if (trimmed.isBlank()) {
                all
            } else {
                val q = trimmed.lowercase(Locale.ROOT)
                all.filter { contact ->
                    contact.name.lowercase(Locale.ROOT).contains(q) ||
                        contact.phones.any { it.number.contains(trimmed) }
                }
            }
            val content = when {
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
        _searchQuery.value = query
    }

    fun refresh() {
        if (_isLoading.value) return
        viewModelScope.launch {
            _isLoading.value = true
            _failure.value = null
            try {
                _allContacts.value = getContactsUseCase()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _failure.value = e
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clear() {
        _allContacts.value = emptyList()
    }
}
