package com.m4isper.contactsapp.presentation.groups

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.m4isper.contactsapp.domain.model.ContactGroup
import com.m4isper.contactsapp.domain.usecase.CreateGroupUseCase
import com.m4isper.contactsapp.domain.usecase.DeleteGroupUseCase
import com.m4isper.contactsapp.domain.usecase.GetGroupMembershipUseCase
import com.m4isper.contactsapp.domain.usecase.GetGroupsUseCase
import com.m4isper.contactsapp.domain.usecase.RenameGroupUseCase
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

private const val GROUPS_TAG = "GroupsScreen"

/** Строка списка групп: сама метка и число контактов в ней. */
data class GroupItem(
    val group: ContactGroup,
    val contactCount: Int,
)

data class GroupsUiState(
    val content: Loadable<List<GroupItem>> = Loadable.Loading,
    val isBusy: Boolean = false,
    val errorMessage: String? = null,
)

@Inject
@ViewModelKey
@ContributesIntoMap(AppScope::class)
class GroupsViewModel(
    private val getGroupsUseCase: GetGroupsUseCase,
    private val getMembershipUseCase: GetGroupMembershipUseCase,
    private val createGroupUseCase: CreateGroupUseCase,
    private val renameGroupUseCase: RenameGroupUseCase,
    private val deleteGroupUseCase: DeleteGroupUseCase,
) : ViewModel() {
    private val content = MutableStateFlow<Loadable<List<GroupItem>>>(Loadable.Loading)
    private val isBusy = MutableStateFlow(false)
    private val errorMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<GroupsUiState> =
        combine(content, isBusy, errorMessage) { loadable, busy, error ->
            GroupsUiState(content = loadable, isBusy = busy, errorMessage = error)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(SUBSCRIBE_TIMEOUT_MILLIS),
            initialValue = GroupsUiState(),
        )

    // Catch-all осознанный: любая ошибка загрузки превращается в Error-стейт.
    @Suppress("TooGenericExceptionCaught")
    fun load() {
        viewModelScope.launch {
            try {
                val items = groupItems(getGroupsUseCase(), getMembershipUseCase())
                content.value = if (items.isEmpty()) Loadable.Empty else Loadable.Content(items)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logw(GROUPS_TAG, "load groups failed", e)
                content.value = Loadable.Error(e.message)
            }
        }
    }

    fun onCreate(title: String) = mutate { createGroupUseCase(title) }

    fun onRename(
        groupId: Long,
        title: String,
    ) = mutate { renameGroupUseCase(groupId, title) }

    fun onDelete(groupId: Long) = mutate { deleteGroupUseCase(groupId) }

    fun onErrorHandled() {
        errorMessage.value = null
    }

    // Catch-all осознанный: ошибка изменения показывается тостом, список перезагружается.
    @Suppress("TooGenericExceptionCaught")
    private fun mutate(block: suspend () -> Unit) {
        if (isBusy.value) return
        viewModelScope.launch {
            isBusy.value = true
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logw(GROUPS_TAG, "group mutation failed", e)
                errorMessage.value = e.message
            } finally {
                isBusy.value = false
            }
            load()
        }
    }
}

/** Число контактов на группу по карте членства contactId → группы. */
internal fun groupItems(
    groups: List<ContactGroup>,
    membership: Map<Long, Set<Long>>,
): List<GroupItem> {
    val counts = mutableMapOf<Long, Int>()
    membership.values.forEach { groupIds ->
        groupIds.forEach { groupId -> counts[groupId] = (counts[groupId] ?: 0) + 1 }
    }
    return groups.map { group -> GroupItem(group = group, contactCount = counts[group.id] ?: 0) }
}
