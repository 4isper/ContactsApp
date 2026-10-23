package com.m4isper.contactsapp.presentation.duplicates

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.m4isper.contactsapp.domain.duplicates.DuplicateGroup
import com.m4isper.contactsapp.domain.duplicates.findDuplicateGroups
import com.m4isper.contactsapp.domain.model.common.AppError
import com.m4isper.contactsapp.domain.model.contact.ContactLink
import com.m4isper.contactsapp.domain.usecase.contacts.GetContactsUseCase
import com.m4isper.contactsapp.domain.usecase.contacts.MergeContactsUseCase
import com.m4isper.contactsapp.domain.usecase.contacts.UnmergeContactsUseCase
import com.m4isper.contactsapp.presentation.common.Loadable
import com.m4isper.contactsapp.presentation.common.safeCall
import com.m4isper.contactsapp.presentation.contacts.SUBSCRIBE_TIMEOUT_MILLIS
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val DUPLICATES_TAG = "DuplicatesScreen"

data class DuplicatesUiState(
    val content: Loadable<List<DuplicateGroup>> = Loadable.Loading,
    val isMerging: Boolean = false,
    val event: DuplicatesEvent? = null,
    /** Связь последнего слияния: по ней слияние можно отменить. */
    val lastLink: ContactLink? = null,
)

/** Одноразовые события экрана: тост об успешном или неудавшемся слиянии. */
sealed interface DuplicatesEvent {
    data object Merged : DuplicatesEvent

    data object MergeFailed : DuplicatesEvent

    /** Слияние отменено: контакты снова видны отдельно. */
    data object Unmerged : DuplicatesEvent

    /** Отменить слияние не удалось. */
    data object UnmergeFailed : DuplicatesEvent
}

@Inject
@ViewModelKey
@ContributesIntoMap(AppScope::class)
class DuplicatesViewModel(
    private val getContactsUseCase: GetContactsUseCase,
    private val mergeContactsUseCase: MergeContactsUseCase,
    private val unmergeContactsUseCase: UnmergeContactsUseCase,
) : ViewModel() {
    private val content = MutableStateFlow<Loadable<List<DuplicateGroup>>>(Loadable.Loading)
    private val isMerging = MutableStateFlow(false)
    private val event = MutableStateFlow<DuplicatesEvent?>(null)
    private val lastLink = MutableStateFlow<ContactLink?>(null)

    val uiState: StateFlow<DuplicatesUiState> =
        combine(content, isMerging, event, lastLink) { loadable, merging, eventValue, link ->
            DuplicatesUiState(content = loadable, isMerging = merging, event = eventValue, lastLink = link)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(SUBSCRIBE_TIMEOUT_MILLIS),
            initialValue = DuplicatesUiState(),
        )

    // Catch-all осознанный: любая ошибка загрузки превращается в Error-стейт.
    fun load() {
        viewModelScope.launch {
            safeCall(DUPLICATES_TAG, "load duplicates failed") {
                val groups = findDuplicateGroups(getContactsUseCase())
                content.value = if (groups.isEmpty()) Loadable.Empty else Loadable.Content(groups)
            }.onFailure { failure ->
                content.value = Loadable.Error(AppError.from(failure))
            }
        }
    }

    // Catch-all осознанный: ошибка слияния показывается тостом, список перезагружается.
    fun onMerge(
        targetId: Long,
        sourceIds: List<Long>,
    ) {
        if (isMerging.value || sourceIds.isEmpty()) return
        viewModelScope.launch {
            isMerging.value = true
            try {
                safeCall(DUPLICATES_TAG, "merge contacts failed") {
                    val link = mergeContactsUseCase(targetId, sourceIds)
                    lastLink.value = link
                    event.value = DuplicatesEvent.Merged
                }.onFailure { failure ->
                    event.value = DuplicatesEvent.MergeFailed
                }
            } finally {
                isMerging.value = false
            }
            load()
        }
    }

    /** Отменяет последнее слияние: контакты снова видны отдельно. */
    fun onUndoMerge() {
        val link = lastLink.value ?: return
        if (isMerging.value) return
        viewModelScope.launch {
            isMerging.value = true
            try {
                safeCall(DUPLICATES_TAG, "unmerge contacts failed") {
                    unmergeContactsUseCase(link)
                    lastLink.value = null
                    event.value = DuplicatesEvent.Unmerged
                }.onFailure { failure ->
                    event.value = DuplicatesEvent.UnmergeFailed
                }
            } finally {
                isMerging.value = false
            }
            load()
        }
    }

    fun onEventHandled() {
        event.value = null
    }
}
