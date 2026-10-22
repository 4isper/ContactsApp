package com.m4isper.contactsapp.presentation.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.m4isper.contactsapp.domain.model.ContactAccount
import com.m4isper.contactsapp.domain.model.ContactGroup
import com.m4isper.contactsapp.domain.model.EditablePhone
import com.m4isper.contactsapp.domain.model.FormField
import com.m4isper.contactsapp.domain.model.findSimilarContacts
import com.m4isper.contactsapp.domain.model.validateContactFields
import com.m4isper.contactsapp.domain.usecase.CreateContactUseCase
import com.m4isper.contactsapp.domain.usecase.CreateGroupUseCase
import com.m4isper.contactsapp.domain.usecase.DeleteContactsUseCase
import com.m4isper.contactsapp.domain.usecase.GetContactAccountUseCase
import com.m4isper.contactsapp.domain.usecase.GetContactAccountsUseCase
import com.m4isper.contactsapp.domain.usecase.GetContactFormUseCase
import com.m4isper.contactsapp.domain.usecase.GetContactsUseCase
import com.m4isper.contactsapp.domain.usecase.GetDefaultContactAccountUseCase
import com.m4isper.contactsapp.domain.usecase.GetGroupsUseCase
import com.m4isper.contactsapp.domain.usecase.SetLastContactAccountUseCase
import com.m4isper.contactsapp.domain.usecase.UpdateContactUseCase
import com.m4isper.contactsapp.presentation.common.logw
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val EDITOR_TAG = "ContactEditor"

@Inject
@ViewModelKey
@ContributesIntoMap(AppScope::class)
class ContactEditorViewModel(
    private val getContactFormUseCase: GetContactFormUseCase,
    private val createContactUseCase: CreateContactUseCase,
    private val updateContactUseCase: UpdateContactUseCase,
    private val deleteContactsUseCase: DeleteContactsUseCase,
    private val getGroupsUseCase: GetGroupsUseCase,
    private val createGroupUseCase: CreateGroupUseCase,
    private val getContactAccountsUseCase: GetContactAccountsUseCase,
    private val getContactAccountUseCase: GetContactAccountUseCase,
    private val getDefaultContactAccountUseCase: GetDefaultContactAccountUseCase,
    private val getContactsUseCase: GetContactsUseCase,
    private val setLastContactAccountUseCase: SetLastContactAccountUseCase,
) : ViewModel() {
    private val state = MutableStateFlow(EditorUiState())
    private var nextRowId = 0L

    /** Повторный load того же контакта (например, после выдачи разрешения) не перезагружает форму. */
    private var loadStarted = false
    private var loadStartedFor: Long? = null
    private var loadSession: Long = 0L

    internal val uiState: StateFlow<EditorUiState> = state.asStateFlow()

    // Catch-all осознанный: любая ошибка превращается в Error-стейт.
    @Suppress("TooGenericExceptionCaught") // Осознанно: любая ошибка загрузки превращается в Error-стейт.
    fun load(
        id: Long?,
        sessionId: Long = 0L,
    ) {
        // Тот же сеанс — та же форма: перечитывать её нельзя, иначе пропали бы правки.
        if (loadStarted && loadStartedFor == id && loadSession == sessionId) return
        loadStarted = true
        loadStartedFor = id
        loadSession = sessionId
        if (id == null) {
            startNewContact()
            return
        }
        state.value =
            EditorUiState(
                contactId = id,
                phones = listOf(newRow(EditablePhone(number = ""))),
                isBusy = true,
            )
        viewModelScope.launch {
            try {
                val form = getContactFormUseCase(id)
                if (form == null) {
                    state.update { it.copy(closeScreen = true) }
                } else {
                    state.value =
                        editorStateFromForm(id, form) { ++nextRowId }
                            .copy(
                                allGroups = loadGroups(getGroupsUseCase),
                                selectedAccount = loadContactAccount(getContactAccountUseCase, id),
                            )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logw(EDITOR_TAG, "load contact form failed", e)
                state.update { it.copy(errorMessage = e.message, isBusy = false) }
            }
        }
    }

    /** Новый контакт: сразу показываем локальный аккаунт, затем подгружаем доступные. */
    private fun startNewContact() {
        state.value =
            EditorUiState(
                phones = listOf(newRow(EditablePhone(number = ""))),
                accounts = listOf(ContactAccount.Local),
                selectedAccount = ContactAccount.Local,
            )
        viewModelScope.launch {
            val accounts = loadAccounts(getContactAccountsUseCase)
            val groups = loadGroups(getGroupsUseCase)
            val defaultAccount = loadDefaultAccount(getDefaultContactAccountUseCase)
            state.update { current ->
                val selected =
                    defaultAccount.takeIf { it in accounts }
                        ?: current.selectedAccount.takeIf { it in accounts }
                        ?: accounts.firstOrNull { it.isLocal }
                        ?: accounts.firstOrNull()
                val availableGroupIds =
                    groups
                        .filter { group -> selected?.let(group::belongsTo) == true }
                        .mapTo(mutableSetOf(), ContactGroup::id)
                current.copy(
                    accounts = accounts,
                    selectedAccount = selected,
                    allGroups = groups,
                    selectedGroupIds = current.selectedGroupIds.intersect(availableGroupIds),
                )
            }
        }
    }

    fun onAccountSelected(account: ContactAccount) {
        if (!state.value.isNew) return
        update { current ->
            val availableGroupIds =
                current.allGroups
                    .filter { it.belongsTo(account) }
                    .mapTo(mutableSetOf(), ContactGroup::id)
            current.copy(
                selectedAccount = account,
                selectedGroupIds = current.selectedGroupIds.intersect(availableGroupIds),
            )
        }
        viewModelScope.launch { persistLastAccount(setLastContactAccountUseCase, account) }
    }

    /** Создаёт группу прямо из пикера и сразу отмечает её: метку создают, чтобы назначить контакту. */
    @Suppress("TooGenericExceptionCaught") // Осознанно: сбой создания группы показывается в форме.
    fun onCreateGroup(title: String) {
        if (title.isBlank()) return
        val account = state.value.selectedAccount ?: ContactAccount.Local
        viewModelScope.launch {
            try {
                val group = createGroupUseCase(title, account)
                state.update { current ->
                    val currentAccount = current.selectedAccount ?: ContactAccount.Local
                    current.copy(
                        allGroups = (current.allGroups + group).distinctBy(ContactGroup::id),
                        selectedGroupIds =
                            if (group.belongsTo(currentAccount)) {
                                current.selectedGroupIds + group.id
                            } else {
                                current.selectedGroupIds
                            },
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logw(EDITOR_TAG, "create group failed", e)
                state.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    fun isDirty(): Boolean = state.value.isDirty()

    // Catch-all осознанный: любая ошибка превращается в Error-стейт.
    @Suppress("TooGenericExceptionCaught") // Осознанно: ошибка сохранения показывается в поле формы.
    fun onSave() {
        val id = state.value.contactId
        val form = state.value.toForm()
        val fieldErrors = validateContactFields(form)
        // Неразобранная дата (из провайдера) — подсказка, а не повод отказать в сохранении.
        val blocking = fieldErrors.filterKeys { it != FormField.EVENT }
        if (blocking.isNotEmpty()) {
            // Показываем проблему у конкретного поля и не идём в провайдер.
            state.update { it.copy(fieldErrors = fieldErrors) }
            return
        }
        if (fieldErrors.isNotEmpty()) {
            state.update { it.copy(fieldErrors = fieldErrors) }
        }
        viewModelScope.launch {
            if (id == null) {
                // Новый контакт: предупреждаем, если такой уже есть.
                val similar = findSimilarContacts(form, loadContacts(getContactsUseCase))
                if (similar.isNotEmpty()) {
                    state.update { it.copy(similarContacts = similar) }
                    return@launch
                }
            }
            save(state, createContactUseCase, updateContactUseCase, id, form)
        }
    }

    /** Пользователь подтвердил сохранение несмотря на похожие контакты. */
    fun onSaveConfirmed() {
        val id = state.value.contactId
        val form = state.value.toForm()
        state.update { it.copy(similarContacts = emptyList()) }
        viewModelScope.launch { save(state, createContactUseCase, updateContactUseCase, id, form) }
    }

    /** Пользователь отказался от сохранения. */
    fun onSaveDismissed() {
        state.update { it.copy(similarContacts = emptyList()) }
    }

    // Catch-all осознанный: любая ошибка превращается в Error-стейт.
    @Suppress("TooGenericExceptionCaught") // Осознанно: сбой удаления показывается тостом.
    fun onDelete() {
        val id = state.value.contactId ?: return
        viewModelScope.launch {
            state.update { it.copy(isBusy = true, deleteFailed = false) }
            try {
                val result = deleteContactsUseCase(setOf(id))
                state.update {
                    it.copy(
                        closeScreen = result.allDeleted,
                        deleteFailed = !result.allDeleted,
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logw(EDITOR_TAG, "delete contact failed", e)
                state.update { it.copy(errorMessage = e.message) }
            } finally {
                state.update { it.copy(isBusy = false) }
            }
        }
    }

    /** Меняет состояние формы и снимает прежние ошибки. */
    internal fun update(transform: (EditorUiState) -> EditorUiState) {
        state.update { transform(it).copy(formError = null, errorMessage = null, deleteFailed = false) }
    }

    /** Новая строка со стабильным ключом: счётчик живёт в пределах экрана. */
    private fun <T> newRow(value: T) = EditorRow(rowId = ++nextRowId, value = value)
}

/** Перемещает строку на [offset] позиций; выход за границы списка игнорируется. */
internal fun <T> List<EditorRow<T>>.moved(
    rowId: Long,
    offset: Int,
): List<EditorRow<T>> {
    val from = indexOfFirst { it.rowId == rowId }
    val to = from + offset
    return if (offset == 0 || from < 0 || to !in indices) {
        this
    } else {
        toMutableList().apply {
            add(to, removeAt(from))
        }
    }
}
