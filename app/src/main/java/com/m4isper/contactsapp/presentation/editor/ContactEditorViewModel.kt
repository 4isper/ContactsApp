package com.m4isper.contactsapp.presentation.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.m4isper.contactsapp.domain.model.AddressKind
import com.m4isper.contactsapp.domain.model.Birthday
import com.m4isper.contactsapp.domain.model.ContactAccount
import com.m4isper.contactsapp.domain.model.ContactGroup
import com.m4isper.contactsapp.domain.model.EditableAddress
import com.m4isper.contactsapp.domain.model.EditableEmail
import com.m4isper.contactsapp.domain.model.EditableEvent
import com.m4isper.contactsapp.domain.model.EditableInstantMessage
import com.m4isper.contactsapp.domain.model.EditablePhone
import com.m4isper.contactsapp.domain.model.EditableRelation
import com.m4isper.contactsapp.domain.model.EditableSipAddress
import com.m4isper.contactsapp.domain.model.EditableWebsite
import com.m4isper.contactsapp.domain.model.EmailKind
import com.m4isper.contactsapp.domain.model.EventKind
import com.m4isper.contactsapp.domain.model.ImProtocol
import com.m4isper.contactsapp.domain.model.PhoneKind
import com.m4isper.contactsapp.domain.model.RelationKind
import com.m4isper.contactsapp.domain.model.SipKind
import com.m4isper.contactsapp.domain.model.WebsiteKind
import com.m4isper.contactsapp.domain.usecase.CreateContactUseCase
import com.m4isper.contactsapp.domain.usecase.CreateGroupUseCase
import com.m4isper.contactsapp.domain.usecase.DeleteContactsUseCase
import com.m4isper.contactsapp.domain.usecase.GetContactAccountUseCase
import com.m4isper.contactsapp.domain.usecase.GetContactAccountsUseCase
import com.m4isper.contactsapp.domain.usecase.GetContactFormUseCase
import com.m4isper.contactsapp.domain.usecase.GetGroupsUseCase
import com.m4isper.contactsapp.domain.usecase.GetLastContactAccountUseCase
import com.m4isper.contactsapp.domain.usecase.SaveResult
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
// Подавление осознанное: фасад формы, тонкие сеттеры на каждое поле; декомпозиция не даёт выгоды.
@Suppress("TooManyFunctions", "LargeClass")
class ContactEditorViewModel(
    private val getContactFormUseCase: GetContactFormUseCase,
    private val createContactUseCase: CreateContactUseCase,
    private val updateContactUseCase: UpdateContactUseCase,
    private val deleteContactsUseCase: DeleteContactsUseCase,
    private val getGroupsUseCase: GetGroupsUseCase,
    private val createGroupUseCase: CreateGroupUseCase,
    private val getContactAccountsUseCase: GetContactAccountsUseCase,
    private val getContactAccountUseCase: GetContactAccountUseCase,
    private val getLastContactAccountUseCase: GetLastContactAccountUseCase,
    private val setLastContactAccountUseCase: SetLastContactAccountUseCase,
) : ViewModel() {
    private val state = MutableStateFlow(EditorUiState())
    private var nextRowId = 0L

    /** Повторный load того же контакта (например, после выдачи разрешения) не перезагружает форму. */
    private var loadStarted = false
    private var loadStartedFor: Long? = null

    internal val uiState: StateFlow<EditorUiState> = state.asStateFlow()

    // Catch-all осознанный: любая ошибка превращается в Error-стейт.
    @Suppress("TooGenericExceptionCaught")
    fun load(id: Long?) {
        if (loadStarted && loadStartedFor == id) return
        loadStarted = true
        loadStartedFor = id
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
                                allGroups = loadGroups(),
                                selectedAccount = loadContactAccount(id),
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
            val accounts = loadAccounts()
            val groups = loadGroups()
            val lastAccount = loadLastContactAccount()
            state.update { current ->
                val selected =
                    lastAccount.takeIf { it in accounts }
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

    fun onDisplayNameChange(value: String) = updateData { it.copy(displayName = value) }

    fun onGivenNameChange(value: String) = updateData { it.copy(givenName = value) }

    fun onFamilyNameChange(value: String) = updateData { it.copy(familyName = value) }

    fun onMiddleNameChange(value: String) = updateData { it.copy(middleName = value) }

    fun onNamePrefixChange(value: String) = updateData { it.copy(namePrefix = value) }

    fun onNameSuffixChange(value: String) = updateData { it.copy(nameSuffix = value) }

    fun onPhoneticGivenNameChange(value: String) = updateData { it.copy(phoneticGivenName = value) }

    fun onPhoneticMiddleNameChange(value: String) = updateData { it.copy(phoneticMiddleName = value) }

    fun onPhoneticFamilyNameChange(value: String) = updateData { it.copy(phoneticFamilyName = value) }

    fun onToggleNameMode() = updateData { it.toggledNameMode() }

    fun onPhoneChange(
        rowId: Long,
        value: String,
    ) = updateData { it.copy(phones = it.phones.withRow(rowId) { phone -> phone.copy(number = value) }) }

    fun onKindChange(
        rowId: Long,
        kind: PhoneKind,
    ) = updateData { it.copy(phones = it.phones.withRow(rowId) { phone -> phone.copy(kind = kind) }) }

    fun onCustomLabelChange(
        rowId: Long,
        value: String,
    ) = updateData { it.copy(phones = it.phones.withRow(rowId) { phone -> phone.copy(customLabel = value) }) }

    fun onAddPhone() = updateData { it.copy(phones = it.phones + newRow(EditablePhone(number = ""))) }

    fun onSetPrimaryPhone(rowId: Long) =
        updateData {
            it.copy(
                phones =
                    it.phones.map { row ->
                        row.copy(value = row.value.copy(isPrimary = row.rowId == rowId))
                    },
            )
        }

    fun onMovePhone(
        rowId: Long,
        offset: Int,
    ) = updateData { it.copy(phones = it.phones.moved(rowId, offset)) }

    fun onRemovePhone(rowId: Long) =
        updateData {
            val remaining = it.phones.filterNot { row -> row.rowId == rowId }
            it.copy(phones = remaining.ifEmpty { listOf(newRow(EditablePhone(number = ""))) })
        }

    fun onEmailChange(
        rowId: Long,
        value: String,
    ) = updateData { it.copy(emails = it.emails.withRow(rowId) { email -> email.copy(address = value) }) }

    fun onEmailKindChange(
        rowId: Long,
        kind: EmailKind,
    ) = updateData { it.copy(emails = it.emails.withRow(rowId) { email -> email.copy(kind = kind) }) }

    fun onEmailCustomLabelChange(
        rowId: Long,
        value: String,
    ) = updateData { it.copy(emails = it.emails.withRow(rowId) { email -> email.copy(customLabel = value) }) }

    fun onAddEmail() = updateData { it.copy(emails = it.emails + newRow(EditableEmail(address = ""))) }

    fun onSetPrimaryEmail(rowId: Long) =
        updateData {
            it.copy(
                emails =
                    it.emails.map { row ->
                        row.copy(value = row.value.copy(isPrimary = row.rowId == rowId))
                    },
            )
        }

    fun onMoveEmail(
        rowId: Long,
        offset: Int,
    ) = updateData { it.copy(emails = it.emails.moved(rowId, offset)) }

    fun onRemoveEmail(rowId: Long) =
        updateData { editor ->
            editor.copy(emails = editor.emails.filterNot { row -> row.rowId == rowId })
        }

    fun onAddressChange(
        rowId: Long,
        value: String,
    ) = updateData {
        it.copy(addresses = it.addresses.withRow(rowId) { address -> address.copy(address = value) })
    }

    fun onAddressStreetChange(
        rowId: Long,
        value: String,
    ) = updateData {
        it.copy(
            addresses =
                it.addresses.withRow(rowId) { address ->
                    address.copy(street = value, address = "")
                },
        )
    }

    fun onAddressPoBoxChange(
        rowId: Long,
        value: String,
    ) = updateData {
        it.copy(addresses = it.addresses.withRow(rowId) { address -> address.copy(poBox = value) })
    }

    fun onAddressNeighborhoodChange(
        rowId: Long,
        value: String,
    ) = updateData {
        it.copy(addresses = it.addresses.withRow(rowId) { address -> address.copy(neighborhood = value) })
    }

    fun onAddressCityChange(
        rowId: Long,
        value: String,
    ) = updateData {
        it.copy(addresses = it.addresses.withRow(rowId) { address -> address.copy(city = value) })
    }

    fun onAddressRegionChange(
        rowId: Long,
        value: String,
    ) = updateData {
        it.copy(addresses = it.addresses.withRow(rowId) { address -> address.copy(region = value) })
    }

    fun onAddressPostcodeChange(
        rowId: Long,
        value: String,
    ) = updateData {
        it.copy(addresses = it.addresses.withRow(rowId) { address -> address.copy(postcode = value) })
    }

    fun onAddressCountryChange(
        rowId: Long,
        value: String,
    ) = updateData {
        it.copy(addresses = it.addresses.withRow(rowId) { address -> address.copy(country = value) })
    }

    fun onAddressKindChange(
        rowId: Long,
        kind: AddressKind,
    ) = updateData { it.copy(addresses = it.addresses.withRow(rowId) { address -> address.copy(kind = kind) }) }

    fun onAddressCustomLabelChange(
        rowId: Long,
        value: String,
    ) = updateData {
        it.copy(addresses = it.addresses.withRow(rowId) { address -> address.copy(customLabel = value) })
    }

    fun onAddAddress() = updateData { it.copy(addresses = it.addresses + newRow(EditableAddress(address = ""))) }

    fun onRemoveAddress(rowId: Long) =
        updateData { editor ->
            editor.copy(addresses = editor.addresses.filterNot { row -> row.rowId == rowId })
        }

    fun onWebsiteChange(
        rowId: Long,
        value: String,
    ) = updateData { it.copy(websites = it.websites.withRow(rowId) { website -> website.copy(url = value) }) }

    fun onWebsiteKindChange(
        rowId: Long,
        kind: WebsiteKind,
    ) = updateData {
        it.copy(websites = it.websites.withRow(rowId) { website -> website.copy(kind = kind) })
    }

    fun onWebsiteCustomLabelChange(
        rowId: Long,
        value: String,
    ) = updateData {
        it.copy(websites = it.websites.withRow(rowId) { website -> website.copy(customLabel = value) })
    }

    fun onAddWebsite() = updateData { it.copy(websites = it.websites + newRow(EditableWebsite(url = ""))) }

    fun onRemoveWebsite(rowId: Long) =
        updateData { editor ->
            editor.copy(websites = editor.websites.filterNot { row -> row.rowId == rowId })
        }

    fun onImChange(
        rowId: Long,
        value: String,
    ) = updateData {
        it.copy(instantMessages = it.instantMessages.withRow(rowId) { message -> message.copy(address = value) })
    }

    fun onImProtocolChange(
        rowId: Long,
        protocol: ImProtocol,
    ) = updateData {
        it.copy(instantMessages = it.instantMessages.withRow(rowId) { message -> message.copy(protocol = protocol) })
    }

    fun onImCustomProtocolChange(
        rowId: Long,
        value: String,
    ) = updateData {
        it.copy(instantMessages = it.instantMessages.withRow(rowId) { message -> message.copy(customProtocol = value) })
    }

    fun onAddIm() =
        updateData {
            it.copy(instantMessages = it.instantMessages + newRow(EditableInstantMessage(address = "")))
        }

    fun onRemoveIm(rowId: Long) =
        updateData { editor ->
            editor.copy(instantMessages = editor.instantMessages.filterNot { row -> row.rowId == rowId })
        }

    fun onSipChange(
        rowId: Long,
        value: String,
    ) = updateData {
        it.copy(sipAddresses = it.sipAddresses.withRow(rowId) { sip -> sip.copy(address = value) })
    }

    fun onSipKindChange(
        rowId: Long,
        kind: SipKind,
    ) = updateData {
        it.copy(sipAddresses = it.sipAddresses.withRow(rowId) { sip -> sip.copy(kind = kind) })
    }

    fun onSipCustomLabelChange(
        rowId: Long,
        value: String,
    ) = updateData {
        it.copy(sipAddresses = it.sipAddresses.withRow(rowId) { sip -> sip.copy(customLabel = value) })
    }

    fun onAddSip() =
        updateData {
            it.copy(sipAddresses = it.sipAddresses + newRow(EditableSipAddress(address = "")))
        }

    fun onRemoveSip(rowId: Long) =
        updateData { editor ->
            editor.copy(sipAddresses = editor.sipAddresses.filterNot { row -> row.rowId == rowId })
        }

    fun onNicknameChange(value: String) = updateData { it.copy(nickname = it.nickname.copy(nickname = value)) }

    fun onAccountSelected(account: ContactAccount) {
        if (!state.value.isNew) return
        updateData { current ->
            val availableGroupIds =
                current.allGroups
                    .filter { it.belongsTo(account) }
                    .mapTo(mutableSetOf(), ContactGroup::id)
            current.copy(
                selectedAccount = account,
                selectedGroupIds = current.selectedGroupIds.intersect(availableGroupIds),
            )
        }
        viewModelScope.launch { persistLastAccount(account) }
    }

    fun onGroupToggled(groupId: Long) =
        updateData {
            it.copy(
                selectedGroupIds =
                    if (groupId in it.selectedGroupIds) {
                        it.selectedGroupIds - groupId
                    } else {
                        it.selectedGroupIds + groupId
                    },
            )
        }

    /** Создаёт группу прямо из пикера и сразу отмечает её: метку создают, чтобы назначить контакту. */
    @Suppress("TooGenericExceptionCaught")
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

    fun onRelationNameChange(
        rowId: Long,
        value: String,
    ) = updateData { it.copy(relations = it.relations.withRow(rowId) { relation -> relation.copy(name = value) }) }

    fun onRelationKindChange(
        rowId: Long,
        kind: RelationKind,
    ) = updateData { it.copy(relations = it.relations.withRow(rowId) { relation -> relation.copy(kind = kind) }) }

    fun onRelationCustomLabelChange(
        rowId: Long,
        value: String,
    ) = updateData {
        it.copy(relations = it.relations.withRow(rowId) { relation -> relation.copy(customLabel = value) })
    }

    fun onAddRelation() = updateData { it.copy(relations = it.relations + newRow(EditableRelation(name = ""))) }

    fun onRemoveRelation(rowId: Long) =
        updateData { editor ->
            editor.copy(relations = editor.relations.filterNot { row -> row.rowId == rowId })
        }

    fun onEventDateChange(
        rowId: Long,
        date: Birthday?,
    ) = updateData { it.copy(events = it.events.withRow(rowId) { event -> event.copy(date = date) }) }

    fun onEventKindChange(
        rowId: Long,
        kind: EventKind,
    ) = updateData { it.copy(events = it.events.withRow(rowId) { event -> event.copy(kind = kind) }) }

    fun onEventCustomLabelChange(
        rowId: Long,
        value: String,
    ) = updateData { it.copy(events = it.events.withRow(rowId) { event -> event.copy(customLabel = value) }) }

    fun onAddEvent() = updateData { it.copy(events = it.events + newRow(EditableEvent())) }

    fun onRemoveEvent(rowId: Long) =
        updateData { editor ->
            editor.copy(events = editor.events.filterNot { row -> row.rowId == rowId })
        }

    fun onOrganizationCompanyChange(value: String) =
        updateData { it.copy(organization = it.organization.copy(company = value)) }

    fun onOrganizationTitleChange(value: String) =
        updateData { it.copy(organization = it.organization.copy(title = value)) }

    fun onOrganizationDepartmentChange(value: String) =
        updateData { it.copy(organization = it.organization.copy(department = value)) }

    fun onOrganizationJobDescriptionChange(value: String) =
        updateData { it.copy(organization = it.organization.copy(jobDescription = value)) }

    fun onOrganizationOfficeLocationChange(value: String) =
        updateData { it.copy(organization = it.organization.copy(officeLocation = value)) }

    fun onNoteChange(value: String) = updateData { it.copy(note = it.note.copy(note = value)) }

    fun onBirthdayChange(value: Birthday?) = updateData { it.copy(birthday = it.birthday.copy(value = value)) }

    fun onPhotoPicked(uri: String) = updateData { it.copy(photoUri = uri, photoChanged = true) }

    fun onPhotoRemoved() = updateData { it.copy(photoUri = null, photoChanged = true) }

    fun isDirty(): Boolean = state.value.isDirty()

    // Catch-all осознанный: любая ошибка превращается в Error-стейт.
    @Suppress("TooGenericExceptionCaught")
    fun onSave() {
        val id = state.value.contactId
        val form = state.value.toForm()
        viewModelScope.launch {
            state.update { it.copy(isBusy = true, errorMessage = null) }
            try {
                val result =
                    if (id == null) {
                        createContactUseCase(form, state.value.selectedAccount ?: ContactAccount.Local)
                    } else {
                        updateContactUseCase(id, form)
                    }
                when (result) {
                    is SaveResult.Saved -> state.update { it.copy(closeScreen = true) }
                    is SaveResult.Invalid -> state.update { it.copy(formError = result.error) }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logw(EDITOR_TAG, "save contact failed", e)
                state.update { it.copy(errorMessage = e.message) }
            } finally {
                state.update { it.copy(isBusy = false) }
            }
        }
    }

    // Catch-all осознанный: любая ошибка превращается в Error-стейт.
    @Suppress("TooGenericExceptionCaught")
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

    private fun updateData(transform: (EditorUiState) -> EditorUiState) {
        state.update { transform(it).copy(formError = null, errorMessage = null, deleteFailed = false) }
    }

    /** Список групп для пикера: сбой чтения не должен ломать редактирование контакта. */
    @Suppress("TooGenericExceptionCaught")
    private suspend fun loadGroups(): List<ContactGroup> =
        try {
            getGroupsUseCase()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logw(EDITOR_TAG, "load groups failed", e)
            emptyList()
        }

    /** Аккаунты для нового контакта: сбой чтения оставляет только локальный. */
    @Suppress("TooGenericExceptionCaught")
    private suspend fun loadAccounts(): List<ContactAccount> {
        val loaded =
            try {
                getContactAccountsUseCase()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logw(EDITOR_TAG, "load accounts failed", e)
                emptyList()
            }
        return (listOf(ContactAccount.Local) + loaded.filterNot { it.isLocal }).distinct()
    }

    /** Аккаунт существующего контакта: сбой чтения не мешает редактированию. */
    @Suppress("TooGenericExceptionCaught")
    private suspend fun loadContactAccount(contactId: Long): ContactAccount =
        try {
            getContactAccountUseCase(contactId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logw(EDITOR_TAG, "load contact account failed", e)
            ContactAccount.Local
        }

    /** Последний выбранный аккаунт: сбой настроек не мешает созданию контакта. */
    @Suppress("TooGenericExceptionCaught")
    private suspend fun loadLastContactAccount(): ContactAccount =
        try {
            getLastContactAccountUseCase()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logw(EDITOR_TAG, "load last contact account failed", e)
            ContactAccount.Local
        }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun persistLastAccount(account: ContactAccount) {
        try {
            setLastContactAccountUseCase(account)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logw(EDITOR_TAG, "save last contact account failed", e)
        }
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
