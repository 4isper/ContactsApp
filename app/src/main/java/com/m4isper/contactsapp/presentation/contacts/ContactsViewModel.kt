package com.m4isper.contactsapp.presentation.contacts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.m4isper.contactsapp.domain.model.ContactDetails
import com.m4isper.contactsapp.domain.model.NameParts
import com.m4isper.contactsapp.domain.usecase.GetContactsUseCase
import com.m4isper.contactsapp.domain.usecase.ObserveFavoritesUseCase
import com.m4isper.contactsapp.presentation.common.Loadable
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

data class ContactsUiState(
    val searchQuery: String = "",
    val showFavoritesOnly: Boolean = false,
    val favorites: Set<Long> = emptySet(),
    val isRefreshing: Boolean = false,
    val content: Loadable<List<ContactDetails>> = Loadable.Loading,
) {
    val isSearching: Boolean
        get() = searchQuery.isNotBlank()
}

internal const val SUBSCRIBE_TIMEOUT_MILLIS = 5_000L

private data class ContactsLoad(
    val all: List<ContactDetails>,
    val loading: Boolean,
    val query: String,
    val failure: Throwable?,
)

@Inject
@ViewModelKey
@ContributesIntoMap(AppScope::class)
class ContactsViewModel(
    private val getContactsUseCase: GetContactsUseCase,
    observeFavoritesUseCase: ObserveFavoritesUseCase,
) : ViewModel() {
    private val allContacts = MutableStateFlow<List<ContactDetails>>(emptyList())
    private val isLoading = MutableStateFlow(false)
    private val searchQueryFlow = MutableStateFlow("")
    private val loadFailure = MutableStateFlow<Throwable?>(null)
    private val favoritesOnlyFlow = MutableStateFlow(false)

    private val favoritesFlow: StateFlow<Set<Long>> =
        observeFavoritesUseCase()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(SUBSCRIBE_TIMEOUT_MILLIS),
                initialValue = emptySet(),
            )

    val uiState: StateFlow<ContactsUiState> =
        combine(
            combine(allContacts, isLoading, searchQueryFlow, loadFailure, ::ContactsLoad),
            favoritesFlow,
            favoritesOnlyFlow,
        ) { load, favorites, favoritesOnly ->
            val trimmed = load.query.trim()
            val visible =
                load.all
                    .filter { !favoritesOnly || it.contactId in favorites }
                    .let { list ->
                        if (trimmed.isBlank()) {
                            list
                        } else {
                            val q = trimmed.lowercase(Locale.ROOT)
                            list.filter { contact ->
                                contact.name.lowercase(Locale.ROOT).contains(q) ||
                                    contact.phones.any { it.number.contains(trimmed) } ||
                                    contact.emails.any { it.address.lowercase(Locale.ROOT).contains(q) } ||
                                    contact.nameParts?.matches(q) == true
                            }
                        }
                    }
            val content =
                when {
                    load.failure != null -> Loadable.Error(load.failure.message)
                    load.loading && visible.isEmpty() -> Loadable.Loading
                    visible.isEmpty() -> Loadable.Empty
                    else -> Loadable.Content(visible)
                }
            ContactsUiState(
                searchQuery = load.query,
                showFavoritesOnly = favoritesOnly,
                favorites = favorites,
                isRefreshing = load.loading,
                content = content,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(SUBSCRIBE_TIMEOUT_MILLIS),
            initialValue = ContactsUiState(),
        )

    fun onSearchQueryChange(query: String) {
        searchQueryFlow.value = query
    }

    fun onToggleFavoritesOnly() {
        favoritesOnlyFlow.value = !favoritesOnlyFlow.value
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

/** Совпадение поискового запроса с любым компонентом структурированного имени. */
private fun NameParts.matches(query: String): Boolean =
    listOf(given, family, middle, prefix, suffix, phoneticGiven, phoneticMiddle, phoneticFamily)
        .any { it.lowercase(Locale.ROOT).contains(query) }
