package com.m4isper.contactsapp.presentation.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.m4isper.contactsapp.domain.model.ContactDetails
import com.m4isper.contactsapp.domain.model.ContactGroup
import com.m4isper.contactsapp.domain.usecase.GetContactDetailsUseCase
import com.m4isper.contactsapp.domain.usecase.GetContactGroupsUseCase
import com.m4isper.contactsapp.domain.usecase.ObserveFavoritesUseCase
import com.m4isper.contactsapp.domain.usecase.ToggleFavoriteUseCase
import com.m4isper.contactsapp.domain.vcard.VCardCodec
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

private const val DETAILS_TAG = "ContactDetails"

data class DetailsUiState(
    val content: Loadable<ContactDetails> = Loadable.Loading,
    val isFavorite: Boolean = false,
    val groups: List<ContactGroup> = emptyList(),
)

@Inject
@ViewModelKey
@ContributesIntoMap(AppScope::class)
class ContactDetailsViewModel(
    private val getContactDetailsUseCase: GetContactDetailsUseCase,
    private val getContactGroupsUseCase: GetContactGroupsUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    observeFavoritesUseCase: ObserveFavoritesUseCase,
    private val vCardCodec: VCardCodec,
) : ViewModel() {
    private val selectedContactId = MutableStateFlow<Long?>(null)
    private val loadedDetails = MutableStateFlow<ContactDetails?>(null)
    private val groupsFlow = MutableStateFlow<List<ContactGroup>>(emptyList())
    private val isLoading = MutableStateFlow(false)
    private val loadFailure = MutableStateFlow<Throwable?>(null)

    private val favoritesFlow: StateFlow<Set<Long>> =
        observeFavoritesUseCase()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(SUBSCRIBE_TIMEOUT_MILLIS),
                initialValue = emptySet(),
            )

    val uiState: StateFlow<DetailsUiState> =
        combine(
            loadedDetails,
            isLoading,
            loadFailure,
            favoritesFlow,
            groupsFlow,
        ) { details, loading, failure, favorites, groups ->
            val content =
                when {
                    failure != null -> Loadable.Error(failure.message)
                    loading && details == null -> Loadable.Loading
                    details == null -> Loadable.Empty
                    else -> Loadable.Content(details)
                }
            DetailsUiState(
                content = content,
                isFavorite = details?.contactId in favorites,
                groups = groups,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(SUBSCRIBE_TIMEOUT_MILLIS),
            initialValue = DetailsUiState(),
        )

    fun load(contactId: Long) {
        if (selectedContactId.value == contactId && (loadedDetails.value != null || isLoading.value)) return
        selectedContactId.value = contactId
        loadedDetails.value = null
        groupsFlow.value = emptyList()
        refresh()
    }

    // Catch-all осознанный: сбой записи избранного не должен ронять экран.
    @Suppress("TooGenericExceptionCaught")
    fun toggleFavorite() {
        val contactId = selectedContactId.value ?: return
        viewModelScope.launch {
            try {
                toggleFavoriteUseCase(contactId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logw(DETAILS_TAG, "toggleFavorite failed", e)
            }
        }
    }

    /**
     * Готовый vCard-текст загруженного контакта для share-интента; null, пока контакт не загружен.
     * suspend: сборка текста уходит на фоновый диспетчер.
     */
    suspend fun vCardText(): String? = loadedDetails.value?.let { vCardCodec.encode(listOf(it)) }

    // Catch-all осознанный: любая ошибка загрузки превращается в Error-стейт.
    @Suppress("TooGenericExceptionCaught")
    fun refresh() {
        val contactId = selectedContactId.value ?: return
        if (isLoading.value) return
        viewModelScope.launch {
            isLoading.value = true
            loadFailure.value = null
            try {
                val details = getContactDetailsUseCase(contactId)
                loadedDetails.value = details
                groupsFlow.value = if (details != null) loadGroups(contactId) else emptyList()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                loadFailure.value = e
            } finally {
                isLoading.value = false
            }
        }
    }

    /** Сбой чтения групп не отменяет показ контакта: детали важнее состава групп. */
    @Suppress("TooGenericExceptionCaught")
    private suspend fun loadGroups(contactId: Long): List<ContactGroup> =
        try {
            getContactGroupsUseCase(contactId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logw(DETAILS_TAG, "load groups failed", e)
            emptyList()
        }
}
