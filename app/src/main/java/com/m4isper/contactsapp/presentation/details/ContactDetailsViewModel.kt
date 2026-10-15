package com.m4isper.contactsapp.presentation.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.m4isper.contactsapp.domain.model.ContactDetails
import com.m4isper.contactsapp.domain.usecase.GetContactDetailsUseCase
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

sealed interface DetailsContent {
    data object Loading : DetailsContent
    data class Content(val details: ContactDetails) : DetailsContent
    data object NotFound : DetailsContent
    data class Error(val message: String?) : DetailsContent
}

data class DetailsUiState(
    val content: DetailsContent = DetailsContent.Loading,
)

@Inject
@ViewModelKey
@ContributesIntoMap(AppScope::class)
class ContactDetailsViewModel(
    private val getContactDetailsUseCase: GetContactDetailsUseCase,
) : ViewModel() {

    private val _contactId = MutableStateFlow<Long?>(null)
    private val _details = MutableStateFlow<ContactDetails?>(null)
    private val _isLoading = MutableStateFlow(false)
    private val _failure = MutableStateFlow<Throwable?>(null)

    val uiState: StateFlow<DetailsUiState> =
        combine(_details, _isLoading, _failure) { details, loading, failure ->
            val content = when {
                failure != null -> DetailsContent.Error(failure.message)
                loading && details == null -> DetailsContent.Loading
                details == null -> DetailsContent.NotFound
                else -> DetailsContent.Content(details)
            }
            DetailsUiState(content)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(SUBSCRIBE_TIMEOUT_MILLIS),
            initialValue = DetailsUiState(),
        )

    fun load(contactId: Long) {
        if (_contactId.value == contactId && (_details.value != null || _isLoading.value)) return
        _contactId.value = contactId
        _details.value = null
        refresh()
    }

    fun refresh() {
        val contactId = _contactId.value ?: return
        if (_isLoading.value) return
        viewModelScope.launch {
            _isLoading.value = true
            _failure.value = null
            try {
                _details.value = getContactDetailsUseCase(contactId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _failure.value = e
            } finally {
                _isLoading.value = false
            }
        }
    }
}
