package com.m4isper.contactsapp.presentation.recents

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.m4isper.contactsapp.domain.model.ContactDetails
import com.m4isper.contactsapp.domain.usecase.GetContactsUseCase
import com.m4isper.contactsapp.domain.usecase.ObserveFavoritesUseCase
import com.m4isper.contactsapp.domain.usecase.ToggleFavoriteUseCase
import com.m4isper.contactsapp.presentation.common.Loadable
import com.m4isper.contactsapp.presentation.common.logw
import com.m4isper.contactsapp.presentation.contacts.SUBSCRIBE_TIMEOUT_MILLIS
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

private const val RECENTS_TAG = "RecentsScreen"

data class RecentsUiState(
    val content: Loadable<List<ContactDetails>> = Loadable.Loading,
    val favorites: Set<Long> = emptySet(),
)

@Inject
@ViewModelKey
@ContributesIntoMap(AppScope::class)
class RecentsViewModel(
    private val getContactsUseCase: GetContactsUseCase,
    observeFavoritesUseCase: ObserveFavoritesUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
) : ViewModel() {
    private val content = MutableStateFlow<Loadable<List<ContactDetails>>>(Loadable.Loading)

    private val favoritesFlow: StateFlow<Set<Long>> =
        observeFavoritesUseCase()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(SUBSCRIBE_TIMEOUT_MILLIS),
                initialValue = emptySet(),
            )

    val uiState: StateFlow<RecentsUiState> =
        combine(content, favoritesFlow) { loadable, favorites ->
            RecentsUiState(content = loadable, favorites = favorites)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(SUBSCRIBE_TIMEOUT_MILLIS),
            initialValue = RecentsUiState(),
        )

    // Catch-all осознанный: любая ошибка загрузки превращается в Error-стейт.
    @Suppress("TooGenericExceptionCaught") // Осознанно: сбой чтения показывается пустым списком.
    fun load() {
        viewModelScope.launch {
            try {
                val recents = recentContacts(getContactsUseCase())
                content.value = if (recents.isEmpty()) Loadable.Empty else Loadable.Content(recents)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logw(RECENTS_TAG, "load recents failed", e)
                content.value = Loadable.Error(e.message)
            }
        }
    }

    fun clear() {
        content.value = Loadable.Empty
    }

    @Suppress("TooGenericExceptionCaught") // Осознанно: сбой правки показывается тостом.
    fun toggleFavorite(contactId: Long) {
        viewModelScope.launch {
            try {
                toggleFavoriteUseCase(contactId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logw(RECENTS_TAG, "toggle favorite failed", e)
            }
        }
    }
}
