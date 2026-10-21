package com.m4isper.contactsapp.presentation.groups

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.m4isper.contactsapp.domain.model.ContactAccount
import com.m4isper.contactsapp.domain.model.ContactDetails
import com.m4isper.contactsapp.domain.model.ContactGroup
import com.m4isper.contactsapp.domain.usecase.AddContactsToGroupUseCase
import com.m4isper.contactsapp.domain.usecase.CreateGroupUseCase
import com.m4isper.contactsapp.domain.usecase.DeleteGroupUseCase
import com.m4isper.contactsapp.domain.usecase.GetContactsUseCase
import com.m4isper.contactsapp.domain.usecase.GetGroupMembershipUseCase
import com.m4isper.contactsapp.domain.usecase.GetGroupsUseCase
import com.m4isper.contactsapp.domain.usecase.MergeGroupsUseCase
import com.m4isper.contactsapp.domain.usecase.RemoveContactsFromGroupUseCase
import com.m4isper.contactsapp.domain.usecase.RenameGroupUseCase
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

private const val GROUP_DETAIL_TAG = "GroupDetail"

/** Удалённая группа: её можно восстановить отменой удаления. */
data class DeletedGroup(
    val title: String,
    val account: ContactAccount,
    val memberIds: List<Long>,
)

/** События экрана группы: изменения и результат действий. */
sealed interface GroupDetailEvent {
    data object MemberRemoved : GroupDetailEvent

    data object MembersAdded : GroupDetailEvent

    data object GroupRenamed : GroupDetailEvent

    data object GroupMerged : GroupDetailEvent

    data object GroupDeleted : GroupDetailEvent

    data object GroupRestored : GroupDetailEvent

    data object ActionFailed : GroupDetailEvent
}

data class GroupDetailUiState(
    val group: ContactGroup? = null,
    val members: List<ContactDetails> = emptyList(),
    /** Все контакты: из них выбирают участников группы. */
    val allContacts: List<ContactDetails> = emptyList(),
    val groups: List<ContactGroup> = emptyList(),
    val isLoading: Boolean = true,
    val isBusy: Boolean = false,
    val event: GroupDetailEvent? = null,
    /** Удалённая группа для отмены удаления. */
    val deletedGroup: DeletedGroup? = null,
)

/** Данные группы: сама метка, участники и список остальных групп. */
private data class GroupData(
    val group: ContactGroup?,
    val members: List<ContactDetails>,
    val allContacts: List<ContactDetails>,
    val groups: List<ContactGroup>,
)

/** Состояние экрана: загрузка, занятость, событие и удалённая группа. */
private data class GroupState(
    val loading: Boolean,
    val busy: Boolean,
    val event: GroupDetailEvent?,
    val deleted: DeletedGroup?,
)

/** Экран группы: участники, добавление, удаление, переименование, слияние. */
@Inject
@ViewModelKey
@ContributesIntoMap(AppScope::class)
class GroupDetailViewModel(
    private val getGroupsUseCase: GetGroupsUseCase,
    private val getMembershipUseCase: GetGroupMembershipUseCase,
    private val getContactsUseCase: GetContactsUseCase,
    private val addContactsToGroupUseCase: AddContactsToGroupUseCase,
    private val removeContactsFromGroupUseCase: RemoveContactsFromGroupUseCase,
    private val renameGroupUseCase: RenameGroupUseCase,
    private val deleteGroupUseCase: DeleteGroupUseCase,
    private val createGroupUseCase: CreateGroupUseCase,
    private val mergeGroupsUseCase: MergeGroupsUseCase,
) : ViewModel() {
    private val groupIdFlow = MutableStateFlow<Long?>(null)
    private val groupFlow = MutableStateFlow<ContactGroup?>(null)
    private val membersFlow = MutableStateFlow<List<ContactDetails>>(emptyList())
    private val allContactsFlow = MutableStateFlow<List<ContactDetails>>(emptyList())
    private val groupsFlow = MutableStateFlow<List<ContactGroup>>(emptyList())
    private val isLoadingFlow = MutableStateFlow(true)
    private val isBusyFlow = MutableStateFlow(false)
    private val eventFlow = MutableStateFlow<GroupDetailEvent?>(null)
    private val deletedGroupFlow = MutableStateFlow<DeletedGroup?>(null)

    val uiState: StateFlow<GroupDetailUiState> =
        combine(
            combine(groupFlow, membersFlow, allContactsFlow, groupsFlow, ::GroupData),
            combine(isLoadingFlow, isBusyFlow, eventFlow, deletedGroupFlow, ::GroupState),
        ) { data, state ->
            GroupDetailUiState(
                group = data.group,
                members = data.members,
                allContacts = data.allContacts,
                groups = data.groups,
                isLoading = state.loading,
                isBusy = state.busy,
                event = state.event,
                deletedGroup = state.deleted,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(SUBSCRIBE_TIMEOUT_MILLIS),
            initialValue = GroupDetailUiState(),
        )

    /** Открывает группу и её состав. */
    fun load(groupId: Long) {
        if (groupIdFlow.value == groupId && !isLoadingFlow.value) return
        groupIdFlow.value = groupId
        refresh()
    }

    /** Перечитывает группу, состав и список групп. */
    @Suppress("TooGenericExceptionCaught") // Осознанный: ошибка загрузки показывается пустым экраном.
    private fun refresh() {
        val groupId = groupIdFlow.value ?: return
        viewModelScope.launch {
            isLoadingFlow.value = true
            try {
                val groups = getGroupsUseCase()
                val membership = getMembershipUseCase()
                val memberIds = membership.filterValues { groupId in it }.keys
                val contacts = getContactsUseCase()
                groupFlow.value = groups.firstOrNull { it.id == groupId }
                groupsFlow.value = groups.filter { it.id != groupId }
                allContactsFlow.value = contacts
                membersFlow.value = contacts.filter { it.contactId in memberIds }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logw(GROUP_DETAIL_TAG, "load group failed", e)
            } finally {
                isLoadingFlow.value = false
            }
        }
    }

    /** Убирает контакт из группы. */
    fun onRemoveMember(contactId: Long) =
        mutate(GroupDetailEvent.MemberRemoved) { groupId ->
            removeContactsFromGroupUseCase(groupId, listOf(contactId))
        }

    /** Добавляет контакты в группу. */
    fun onAddMembers(contactIds: List<Long>) =
        mutate(GroupDetailEvent.MembersAdded) { groupId ->
            addContactsToGroupUseCase(groupId, contactIds)
        }

    /** Переименовывает группу. */
    fun onRename(title: String) =
        mutate(GroupDetailEvent.GroupRenamed) { groupId ->
            renameGroupUseCase(groupId, title)
        }

    /** Удаляет группу; участники запоминаются, чтобы удаление можно было отменить. */
    fun onDelete() {
        val groupId = groupIdFlow.value ?: return
        val group = groupFlow.value ?: return
        val members = membersFlow.value.map { it.contactId }
        mutate(GroupDetailEvent.GroupDeleted) { id ->
            deleteGroupUseCase(id)
            deletedGroupFlow.value =
                DeletedGroup(
                    title = group.title,
                    account = group.account,
                    memberIds = members,
                )
        }
    }

    /** Отменяет удаление: группа создаётся заново вместе с участниками. */
    fun onUndoDelete() {
        val deleted = deletedGroupFlow.value ?: return
        mutate(GroupDetailEvent.GroupRestored) {
            val group = createGroupUseCase(deleted.title, deleted.account)
            if (deleted.memberIds.isNotEmpty()) {
                addContactsToGroupUseCase(group.id, deleted.memberIds)
            }
        }
    }

    /** Переносит участников в другую группу. */
    fun onMerge(targetGroupId: Long) =
        mutate(GroupDetailEvent.GroupMerged) { groupId ->
            mergeGroupsUseCase(groupId, targetGroupId)
        }

    fun onEventHandled() {
        eventFlow.value = null
    }

    /** Изменение группы: событие об успехе или об ошибке. */
    @Suppress("TooGenericExceptionCaught") // Осознанный: ошибка изменения показывается тостом.
    private fun mutate(
        success: GroupDetailEvent,
        block: suspend (Long) -> Unit,
    ) {
        val groupId = groupIdFlow.value ?: return
        if (isBusyFlow.value) return
        viewModelScope.launch {
            isBusyFlow.value = true
            try {
                block(groupId)
                eventFlow.value = success
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logw(GROUP_DETAIL_TAG, "group mutation failed", e)
                eventFlow.value = GroupDetailEvent.ActionFailed
            } finally {
                isBusyFlow.value = false
            }
            refresh()
        }
    }
}
