package com.m4isper.contactsapp.presentation.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.m4isper.contactsapp.domain.model.ContactDetails
import com.m4isper.contactsapp.domain.usecase.GetContactDetailsUseCase
import com.m4isper.contactsapp.presentation.common.Loadable
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

data class DetailsUiState(
    val content: Loadable<ContactDetails> = Loadable.Loading,
)

@Inject
@ViewModelKey
@ContributesIntoMap(AppScope::class)
class ContactDetailsViewModel(
    private val getContactDetailsUseCase: GetContactDetailsUseCase,
) : ViewModel() {
    private val selectedContactId = MutableStateFlow<Long?>(null)
    private val loadedDetails = MutableStateFlow<ContactDetails?>(null)
    private val isLoading = MutableStateFlow(false)
    private val loadFailure = MutableStateFlow<Throwable?>(null)

    val uiState: StateFlow<DetailsUiState> =
        combine(loadedDetails, isLoading, loadFailure) { details, loading, failure ->
            val content =
                when {
                    failure != null -> Loadable.Error(failure.message)
                    loading && details == null -> Loadable.Loading
                    details == null -> Loadable.Empty
                    else -> Loadable.Content(details)
                }
            DetailsUiState(content)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(SUBSCRIBE_TIMEOUT_MILLIS),
            initialValue = DetailsUiState(),
        )

    fun load(contactId: Long) {
        if (selectedContactId.value == contactId && (loadedDetails.value != null || isLoading.value)) return
        selectedContactId.value = contactId
        loadedDetails.value = null
        refresh()
    }

    // Catch-all осознанный: любая ошибка загрузки превращается в Error-стейт.
    @Suppress("TooGenericExceptionCaught")
    fun refresh() {
        val contactId = selectedContactId.value ?: return
        if (isLoading.value) return
        viewModelScope.launch {
            isLoading.value = true
            loadFailure.value = null
            try {
                loadedDetails.value = getContactDetailsUseCase(contactId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                loadFailure.value = e
            } finally {
                isLoading.value = false
            }
        }
    }
}
