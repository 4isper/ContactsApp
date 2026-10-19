package com.m4isper.contactsapp.presentation.duplicates

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.m4isper.contactsapp.domain.usecase.GetContactsUseCase
import com.m4isper.contactsapp.domain.usecase.MergeContactsUseCase
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

private const val DUPLICATES_TAG = "DuplicatesScreen"

data class DuplicatesUiState(
    val content: Loadable<List<DuplicateGroup>> = Loadable.Loading,
    val isMerging: Boolean = false,
    val event: DuplicatesEvent? = null,
)

/** Одноразовые события экрана: тост об успешном или неудавшемся слиянии. */
sealed interface DuplicatesEvent {
    data object Merged : DuplicatesEvent

    data object MergeFailed : DuplicatesEvent
}

@Inject
@ViewModelKey
@ContributesIntoMap(AppScope::class)
class DuplicatesViewModel(
    private val getContactsUseCase: GetContactsUseCase,
    private val mergeContactsUseCase: MergeContactsUseCase,
) : ViewModel() {
    private val content = MutableStateFlow<Loadable<List<DuplicateGroup>>>(Loadable.Loading)
    private val isMerging = MutableStateFlow(false)
    private val event = MutableStateFlow<DuplicatesEvent?>(null)

    val uiState: StateFlow<DuplicatesUiState> =
        combine(content, isMerging, event) { loadable, merging, eventValue ->
            DuplicatesUiState(content = loadable, isMerging = merging, event = eventValue)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(SUBSCRIBE_TIMEOUT_MILLIS),
            initialValue = DuplicatesUiState(),
        )

    // Catch-all осознанный: любая ошибка загрузки превращается в Error-стейт.
    @Suppress("TooGenericExceptionCaught")
    fun load() {
        viewModelScope.launch {
            try {
                val groups = findDuplicateGroups(getContactsUseCase())
                content.value = if (groups.isEmpty()) Loadable.Empty else Loadable.Content(groups)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logw(DUPLICATES_TAG, "load duplicates failed", e)
                content.value = Loadable.Error(e.message)
            }
        }
    }

    // Catch-all осознанный: ошибка слияния показывается тостом, список перезагружается.
    @Suppress("TooGenericExceptionCaught")
    fun onMerge(
        targetId: Long,
        sourceIds: List<Long>,
    ) {
        if (isMerging.value || sourceIds.isEmpty()) return
        viewModelScope.launch {
            isMerging.value = true
            try {
                mergeContactsUseCase(targetId, sourceIds)
                event.value = DuplicatesEvent.Merged
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logw(DUPLICATES_TAG, "merge contacts failed", e)
                event.value = DuplicatesEvent.MergeFailed
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
