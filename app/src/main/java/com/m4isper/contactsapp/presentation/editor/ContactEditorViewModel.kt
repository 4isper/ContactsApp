package com.m4isper.contactsapp.presentation.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.m4isper.contactsapp.domain.model.AddressKind
import com.m4isper.contactsapp.domain.model.Birthday
import com.m4isper.contactsapp.domain.model.ContactForm
import com.m4isper.contactsapp.domain.model.EditableAddress
import com.m4isper.contactsapp.domain.model.EditableBirthday
import com.m4isper.contactsapp.domain.model.EditableEmail
import com.m4isper.contactsapp.domain.model.EditableEvent
import com.m4isper.contactsapp.domain.model.EditableInstantMessage
import com.m4isper.contactsapp.domain.model.EditableName
import com.m4isper.contactsapp.domain.model.EditableNickname
import com.m4isper.contactsapp.domain.model.EditableNote
import com.m4isper.contactsapp.domain.model.EditableOrganization
import com.m4isper.contactsapp.domain.model.EditablePhone
import com.m4isper.contactsapp.domain.model.EditableRelation
import com.m4isper.contactsapp.domain.model.EditableWebsite
import com.m4isper.contactsapp.domain.model.EmailKind
import com.m4isper.contactsapp.domain.model.EventKind
import com.m4isper.contactsapp.domain.model.FormError
import com.m4isper.contactsapp.domain.model.ImProtocol
import com.m4isper.contactsapp.domain.model.NameMode
import com.m4isper.contactsapp.domain.model.PhoneKind
import com.m4isper.contactsapp.domain.model.RelationKind
import com.m4isper.contactsapp.domain.model.composeDisplayName
import com.m4isper.contactsapp.domain.usecase.CreateContactUseCase
import com.m4isper.contactsapp.domain.usecase.DeleteContactUseCase
import com.m4isper.contactsapp.domain.usecase.GetContactFormUseCase
import com.m4isper.contactsapp.domain.usecase.SaveResult
import com.m4isper.contactsapp.domain.usecase.UpdateContactUseCase
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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicLong

data class EditorPhoneRow(
    val rowId: Long,
    val dataId: Long? = null,
    val number: String = "",
    val kind: PhoneKind = PhoneKind.MOBILE,
    val customLabel: String = "",
)

data class EditorEmailRow(
    val rowId: Long,
    val dataId: Long? = null,
    val address: String = "",
    val kind: EmailKind = EmailKind.HOME,
    val customLabel: String = "",
)

data class EditorAddressRow(
    val rowId: Long,
    val dataId: Long? = null,
    val address: String = "",
    val kind: AddressKind = AddressKind.HOME,
    val customLabel: String = "",
)

data class EditorWebsiteRow(
    val rowId: Long,
    val dataId: Long? = null,
    val url: String = "",
)

data class EditorImRow(
    val rowId: Long,
    val dataId: Long? = null,
    val address: String = "",
    val protocol: ImProtocol = ImProtocol.CUSTOM,
    val customProtocol: String = "",
)

data class EditorRelationRow(
    val rowId: Long,
    val dataId: Long? = null,
    val name: String = "",
    val kind: RelationKind = RelationKind.OTHER,
    val customLabel: String = "",
)

data class EditorEventRow(
    val rowId: Long,
    val dataId: Long? = null,
    val date: Birthday? = null,
    val kind: EventKind = EventKind.ANNIVERSARY,
    val customLabel: String = "",
)

data class EditorUiState(
    val nameMode: NameMode = NameMode.DISPLAY,
    val displayName: String = "",
    val givenName: String = "",
    val familyName: String = "",
    val middleName: String = "",
    val namePrefix: String = "",
    val nameSuffix: String = "",
    val phoneticGivenName: String = "",
    val phoneticMiddleName: String = "",
    val phoneticFamilyName: String = "",
    val phones: List<EditorPhoneRow> = emptyList(),
    val emails: List<EditorEmailRow> = emptyList(),
    val addresses: List<EditorAddressRow> = emptyList(),
    val websites: List<EditorWebsiteRow> = emptyList(),
    val instantMessages: List<EditorImRow> = emptyList(),
    val organization: EditableOrganization = EditableOrganization(),
    val note: EditableNote = EditableNote(),
    val birthday: EditableBirthday = EditableBirthday(),
    val nickname: EditableNickname = EditableNickname(),
    val relations: List<EditorRelationRow> = emptyList(),
    val events: List<EditorEventRow> = emptyList(),
    val photoUri: String? = null,
    val isNew: Boolean = true,
    val isBusy: Boolean = false,
    val formError: FormError? = null,
    val errorMessage: String? = null,
    val closeScreen: Boolean = false,
) {
    /** Имя для превью аватара и заголовка: поле или собранные компоненты. */
    val effectiveName: String
        get() =
            if (nameMode == NameMode.DISPLAY) {
                displayName
            } else {
                composeDisplayName(namePrefix, givenName, middleName, familyName, nameSuffix)
            }
}

private val rowIds = AtomicLong(0)

private fun newPhoneRow(
    dataId: Long? = null,
    number: String = "",
    kind: PhoneKind = PhoneKind.MOBILE,
    customLabel: String = "",
) = EditorPhoneRow(
    rowId = rowIds.incrementAndGet(),
    dataId = dataId,
    number = number,
    kind = kind,
    customLabel = customLabel,
)

private fun newEmailRow(
    dataId: Long? = null,
    address: String = "",
    kind: EmailKind = EmailKind.HOME,
    customLabel: String = "",
) = EditorEmailRow(
    rowId = rowIds.incrementAndGet(),
    dataId = dataId,
    address = address,
    kind = kind,
    customLabel = customLabel,
)

private fun newAddressRow(
    dataId: Long? = null,
    address: String = "",
    kind: AddressKind = AddressKind.HOME,
    customLabel: String = "",
) = EditorAddressRow(
    rowId = rowIds.incrementAndGet(),
    dataId = dataId,
    address = address,
    kind = kind,
    customLabel = customLabel,
)

private fun newWebsiteRow(
    dataId: Long? = null,
    url: String = "",
) = EditorWebsiteRow(
    rowId = rowIds.incrementAndGet(),
    dataId = dataId,
    url = url,
)

private fun newImRow(
    dataId: Long? = null,
    address: String = "",
    protocol: ImProtocol = ImProtocol.CUSTOM,
    customProtocol: String = "",
) = EditorImRow(
    rowId = rowIds.incrementAndGet(),
    dataId = dataId,
    address = address,
    protocol = protocol,
    customProtocol = customProtocol,
)

private fun newRelationRow(
    dataId: Long? = null,
    name: String = "",
    kind: RelationKind = RelationKind.OTHER,
    customLabel: String = "",
) = EditorRelationRow(
    rowId = rowIds.incrementAndGet(),
    dataId = dataId,
    name = name,
    kind = kind,
    customLabel = customLabel,
)

private fun newEventRow(
    dataId: Long? = null,
    date: Birthday? = null,
    kind: EventKind = EventKind.ANNIVERSARY,
    customLabel: String = "",
) = EditorEventRow(
    rowId = rowIds.incrementAndGet(),
    dataId = dataId,
    date = date,
    kind = kind,
    customLabel = customLabel,
)

private data class EditorData(
    val contactId: Long? = null,
    val nameMode: NameMode = NameMode.DISPLAY,
    val displayName: String = "",
    val givenName: String = "",
    val familyName: String = "",
    val middleName: String = "",
    val namePrefix: String = "",
    val nameSuffix: String = "",
    val phoneticGivenName: String = "",
    val phoneticMiddleName: String = "",
    val phoneticFamilyName: String = "",
    val phones: List<EditorPhoneRow> = emptyList(),
    val emails: List<EditorEmailRow> = emptyList(),
    val addresses: List<EditorAddressRow> = emptyList(),
    val websites: List<EditorWebsiteRow> = emptyList(),
    val instantMessages: List<EditorImRow> = emptyList(),
    val organization: EditableOrganization = EditableOrganization(),
    val note: EditableNote = EditableNote(),
    val birthday: EditableBirthday = EditableBirthday(),
    val nickname: EditableNickname = EditableNickname(),
    val relations: List<EditorRelationRow> = emptyList(),
    val events: List<EditorEventRow> = emptyList(),
    val photoUri: String? = null,
    val photoChanged: Boolean = false,
    val isBusy: Boolean = false,
    val initialForm: ContactForm = ContactForm(),
) {
    val hasNameComponents: Boolean
        get() =
            givenName.isNotBlank() ||
                familyName.isNotBlank() ||
                middleName.isNotBlank() ||
                namePrefix.isNotBlank() ||
                nameSuffix.isNotBlank() ||
                phoneticGivenName.isNotBlank() ||
                phoneticMiddleName.isNotBlank() ||
                phoneticFamilyName.isNotBlank()

    val composedName: String
        get() = composeDisplayName(namePrefix, givenName, middleName, familyName, nameSuffix)

    fun toForm(): ContactForm =
        ContactForm(
            name = editableName(),
            phones = phones.map { it.toEditable() },
            emails = emails.map { it.toEditable() },
            addresses = addresses.map { it.toEditable() },
            websites = websites.map { it.toEditable() },
            instantMessages = instantMessages.map { it.toEditable() },
            relations = relations.map { it.toEditable() },
            events = events.map { it.toEditable() },
            nickname = nickname,
            organization = organization,
            note = note,
            birthday = birthday,
            photoUri = photoUri,
            photoChanged = photoChanged,
        )

    private fun editableName(): EditableName =
        EditableName(
            mode = nameMode,
            display = displayName,
            given = givenName,
            family = familyName,
            middle = middleName,
            prefix = namePrefix,
            suffix = nameSuffix,
            phoneticGiven = phoneticGivenName,
            phoneticMiddle = phoneticMiddleName,
            phoneticFamily = phoneticFamilyName,
        )

    private fun EditorPhoneRow.toEditable(): EditablePhone =
        EditablePhone(
            dataId = dataId,
            number = number,
            kind = kind,
            customLabel = customLabel.takeIf { it.isNotBlank() },
        )

    private fun EditorEmailRow.toEditable(): EditableEmail =
        EditableEmail(
            dataId = dataId,
            address = address,
            kind = kind,
            customLabel = customLabel.takeIf { it.isNotBlank() },
        )

    private fun EditorAddressRow.toEditable(): EditableAddress =
        EditableAddress(
            dataId = dataId,
            address = address,
            kind = kind,
            customLabel = customLabel.takeIf { it.isNotBlank() },
        )

    private fun EditorWebsiteRow.toEditable(): EditableWebsite = EditableWebsite(dataId = dataId, url = url)

    private fun EditorImRow.toEditable(): EditableInstantMessage =
        EditableInstantMessage(
            dataId = dataId,
            address = address,
            protocol = protocol,
            customProtocol = customProtocol.takeIf { it.isNotBlank() },
        )

    private fun EditorRelationRow.toEditable(): EditableRelation =
        EditableRelation(
            dataId = dataId,
            name = name,
            kind = kind,
            customLabel = customLabel.takeIf { it.isNotBlank() },
        )

    private fun EditorEventRow.toEditable(): EditableEvent =
        EditableEvent(
            dataId = dataId,
            date = date,
            kind = kind,
            customLabel = customLabel.takeIf { it.isNotBlank() },
        )
}

/** Пустые строки — плейсхолдеры для ввода, в форму сохранения они не попадают. */
private fun ContactForm.withoutBlankRows(): ContactForm =
    copy(
        phones = phones.filter { it.number.isNotBlank() },
        emails = emails.filter { it.address.isNotBlank() },
        addresses = addresses.filter { it.address.isNotBlank() },
        websites = websites.filter { it.url.isNotBlank() },
        instantMessages = instantMessages.filter { it.address.isNotBlank() },
        relations = relations.filter { it.name.isNotBlank() },
        events = events.filter { it.date != null },
    )

@Inject
@ViewModelKey
@ContributesIntoMap(AppScope::class)
// Подавление осознанное: фасад формы, тонкие сеттеры на каждое поле; декомпозиция не даёт выгоды.
@Suppress("TooManyFunctions")
class ContactEditorViewModel(
    private val getContactFormUseCase: GetContactFormUseCase,
    private val createContactUseCase: CreateContactUseCase,
    private val updateContactUseCase: UpdateContactUseCase,
    private val deleteContactUseCase: DeleteContactUseCase,
) : ViewModel() {
    private val data = MutableStateFlow(EditorData())
    private val formError = MutableStateFlow<FormError?>(null)
    private val errorMessage = MutableStateFlow<String?>(null)
    private val closeScreen = MutableStateFlow(false)

    val uiState: StateFlow<EditorUiState> =
        combine(data, formError, errorMessage, closeScreen) { editor, error, message, close ->
            EditorUiState(
                nameMode = editor.nameMode,
                displayName = editor.displayName,
                givenName = editor.givenName,
                familyName = editor.familyName,
                middleName = editor.middleName,
                namePrefix = editor.namePrefix,
                nameSuffix = editor.nameSuffix,
                phoneticGivenName = editor.phoneticGivenName,
                phoneticMiddleName = editor.phoneticMiddleName,
                phoneticFamilyName = editor.phoneticFamilyName,
                phones = editor.phones,
                emails = editor.emails,
                addresses = editor.addresses,
                websites = editor.websites,
                instantMessages = editor.instantMessages,
                organization = editor.organization,
                note = editor.note,
                birthday = editor.birthday,
                nickname = editor.nickname,
                relations = editor.relations,
                events = editor.events,
                photoUri = editor.photoUri,
                isNew = editor.contactId == null,
                isBusy = editor.isBusy,
                formError = error,
                errorMessage = message,
                closeScreen = close,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(SUBSCRIBE_TIMEOUT_MILLIS),
            initialValue = EditorUiState(),
        )

    // Catch-all осознанный: любая ошибка превращается в Error-стейт.
    @Suppress("TooGenericExceptionCaught")
    fun load(id: Long?) {
        formError.value = null
        errorMessage.value = null
        closeScreen.value = false
        if (id == null) {
            data.value = EditorData(phones = listOf(newPhoneRow()))
            return
        }
        data.value = EditorData(contactId = id, phones = listOf(newPhoneRow()), isBusy = true)
        viewModelScope.launch {
            try {
                val form = getContactFormUseCase(id)
                if (form == null) {
                    closeScreen.value = true
                } else {
                    data.value = editorDataFromForm(id, form)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                errorMessage.value = e.message
                data.update { it.copy(isBusy = false) }
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

    /**
     * DISPLAY → STRUCTURED: если компоненты пустые, переносим полное имя в «Имя»,
     * чтобы данные не потерялись. STRUCTURED → DISPLAY: собираем имя из компонентов.
     */
    fun onToggleNameMode() =
        updateData { editor ->
            if (editor.nameMode == NameMode.DISPLAY) {
                val given = if (editor.hasNameComponents) editor.givenName else editor.displayName.trim()
                editor.copy(nameMode = NameMode.STRUCTURED, givenName = given)
            } else {
                editor.copy(
                    nameMode = NameMode.DISPLAY,
                    displayName = editor.composedName,
                )
            }
        }

    fun onPhoneChange(
        rowId: Long,
        value: String,
    ) = updatePhones(rowId) { it.copy(number = value) }

    fun onKindChange(
        rowId: Long,
        kind: PhoneKind,
    ) = updatePhones(rowId) { it.copy(kind = kind) }

    fun onCustomLabelChange(
        rowId: Long,
        value: String,
    ) = updatePhones(rowId) { it.copy(customLabel = value) }

    fun onAddPhone() = updateData { it.copy(phones = it.phones + newPhoneRow()) }

    fun onRemovePhone(rowId: Long) =
        updateData {
            val remaining = it.phones.filterNot { row -> row.rowId == rowId }
            it.copy(phones = remaining.ifEmpty { listOf(newPhoneRow()) })
        }

    fun onEmailChange(
        rowId: Long,
        value: String,
    ) = updateEmails(rowId) { it.copy(address = value) }

    fun onEmailKindChange(
        rowId: Long,
        kind: EmailKind,
    ) = updateEmails(rowId) { it.copy(kind = kind) }

    fun onEmailCustomLabelChange(
        rowId: Long,
        value: String,
    ) = updateEmails(rowId) { it.copy(customLabel = value) }

    fun onAddEmail() = updateData { it.copy(emails = it.emails + newEmailRow()) }

    fun onRemoveEmail(rowId: Long) =
        updateData { editor ->
            editor.copy(emails = editor.emails.filterNot { row -> row.rowId == rowId })
        }

    fun onAddressChange(
        rowId: Long,
        value: String,
    ) = updateAddresses(rowId) { it.copy(address = value) }

    fun onAddressKindChange(
        rowId: Long,
        kind: AddressKind,
    ) = updateAddresses(rowId) { it.copy(kind = kind) }

    fun onAddressCustomLabelChange(
        rowId: Long,
        value: String,
    ) = updateAddresses(rowId) { it.copy(customLabel = value) }

    fun onAddAddress() = updateData { it.copy(addresses = it.addresses + newAddressRow()) }

    fun onRemoveAddress(rowId: Long) =
        updateData { editor ->
            editor.copy(addresses = editor.addresses.filterNot { row -> row.rowId == rowId })
        }

    fun onWebsiteChange(
        rowId: Long,
        value: String,
    ) = updateWebsites(rowId) { it.copy(url = value) }

    fun onAddWebsite() = updateData { it.copy(websites = it.websites + newWebsiteRow()) }

    fun onRemoveWebsite(rowId: Long) =
        updateData { editor ->
            editor.copy(websites = editor.websites.filterNot { row -> row.rowId == rowId })
        }

    fun onImChange(
        rowId: Long,
        value: String,
    ) = updateInstantMessages(rowId) { it.copy(address = value) }

    fun onImProtocolChange(
        rowId: Long,
        protocol: ImProtocol,
    ) = updateInstantMessages(rowId) { it.copy(protocol = protocol) }

    fun onImCustomProtocolChange(
        rowId: Long,
        value: String,
    ) = updateInstantMessages(rowId) { it.copy(customProtocol = value) }

    fun onAddIm() = updateData { it.copy(instantMessages = it.instantMessages + newImRow()) }

    fun onRemoveIm(rowId: Long) =
        updateData { editor ->
            editor.copy(instantMessages = editor.instantMessages.filterNot { row -> row.rowId == rowId })
        }

    fun onNicknameChange(value: String) = updateData { it.copy(nickname = it.nickname.copy(nickname = value)) }

    fun onRelationNameChange(
        rowId: Long,
        value: String,
    ) = updateRelations(rowId) { it.copy(name = value) }

    fun onRelationKindChange(
        rowId: Long,
        kind: RelationKind,
    ) = updateRelations(rowId) { it.copy(kind = kind) }

    fun onRelationCustomLabelChange(
        rowId: Long,
        value: String,
    ) = updateRelations(rowId) { it.copy(customLabel = value) }

    fun onAddRelation() = updateData { it.copy(relations = it.relations + newRelationRow()) }

    fun onRemoveRelation(rowId: Long) =
        updateData { editor ->
            editor.copy(relations = editor.relations.filterNot { row -> row.rowId == rowId })
        }

    fun onEventDateChange(
        rowId: Long,
        date: Birthday?,
    ) = updateEvents(rowId) { it.copy(date = date) }

    fun onEventKindChange(
        rowId: Long,
        kind: EventKind,
    ) = updateEvents(rowId) { it.copy(kind = kind) }

    fun onEventCustomLabelChange(
        rowId: Long,
        value: String,
    ) = updateEvents(rowId) { it.copy(customLabel = value) }

    fun onAddEvent() = updateData { it.copy(events = it.events + newEventRow()) }

    fun onRemoveEvent(rowId: Long) =
        updateData { editor ->
            editor.copy(events = editor.events.filterNot { row -> row.rowId == rowId })
        }

    fun onOrganizationCompanyChange(value: String) =
        updateData { it.copy(organization = it.organization.copy(company = value)) }

    fun onOrganizationTitleChange(value: String) =
        updateData { it.copy(organization = it.organization.copy(title = value)) }

    fun onNoteChange(value: String) = updateData { it.copy(note = it.note.copy(note = value)) }

    fun onBirthdayChange(value: Birthday?) = updateData { it.copy(birthday = it.birthday.copy(value = value)) }

    fun onPhotoPicked(uri: String) = updateData { it.copy(photoUri = uri, photoChanged = true) }

    fun onPhotoRemoved() = updateData { it.copy(photoUri = null, photoChanged = true) }

    fun isDirty(): Boolean = data.value.toForm().withoutBlankRows() != data.value.initialForm.withoutBlankRows()

    // Catch-all осознанный: любая ошибка превращается в Error-стейт.
    @Suppress("TooGenericExceptionCaught")
    fun onSave() {
        val id = data.value.contactId
        val form = data.value.toForm().withoutBlankRows()
        viewModelScope.launch {
            data.update { it.copy(isBusy = true) }
            errorMessage.value = null
            try {
                val result =
                    if (id == null) {
                        createContactUseCase(form)
                    } else {
                        updateContactUseCase(id, form)
                    }
                when (result) {
                    is SaveResult.Saved -> closeScreen.value = true
                    is SaveResult.Invalid -> formError.value = result.error
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                errorMessage.value = e.message
            } finally {
                data.update { it.copy(isBusy = false) }
            }
        }
    }

    // Catch-all осознанный: любая ошибка превращается в Error-стейт.
    @Suppress("TooGenericExceptionCaught")
    fun onDelete() {
        val id = data.value.contactId ?: return
        viewModelScope.launch {
            data.update { it.copy(isBusy = true) }
            try {
                deleteContactUseCase(id)
                closeScreen.value = true
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                errorMessage.value = e.message
            } finally {
                data.update { it.copy(isBusy = false) }
            }
        }
    }

    private fun updateData(transform: (EditorData) -> EditorData) {
        data.update(transform)
        formError.value = null
    }

    private fun updatePhones(
        rowId: Long,
        transform: (EditorPhoneRow) -> EditorPhoneRow,
    ) = updateData { editor ->
        editor.copy(
            phones =
                editor.phones.map { row ->
                    if (row.rowId == rowId) transform(row) else row
                },
        )
    }

    private fun updateEmails(
        rowId: Long,
        transform: (EditorEmailRow) -> EditorEmailRow,
    ) = updateData { editor ->
        editor.copy(
            emails =
                editor.emails.map { row ->
                    if (row.rowId == rowId) transform(row) else row
                },
        )
    }

    private fun updateAddresses(
        rowId: Long,
        transform: (EditorAddressRow) -> EditorAddressRow,
    ) = updateData { editor ->
        editor.copy(
            addresses =
                editor.addresses.map { row ->
                    if (row.rowId == rowId) transform(row) else row
                },
        )
    }

    private fun updateWebsites(
        rowId: Long,
        transform: (EditorWebsiteRow) -> EditorWebsiteRow,
    ) = updateData { editor ->
        editor.copy(
            websites =
                editor.websites.map { row ->
                    if (row.rowId == rowId) transform(row) else row
                },
        )
    }

    private fun updateInstantMessages(
        rowId: Long,
        transform: (EditorImRow) -> EditorImRow,
    ) = updateData { editor ->
        editor.copy(
            instantMessages =
                editor.instantMessages.map { row ->
                    if (row.rowId == rowId) transform(row) else row
                },
        )
    }

    private fun updateRelations(
        rowId: Long,
        transform: (EditorRelationRow) -> EditorRelationRow,
    ) = updateData { editor ->
        editor.copy(
            relations =
                editor.relations.map { row ->
                    if (row.rowId == rowId) transform(row) else row
                },
        )
    }

    private fun updateEvents(
        rowId: Long,
        transform: (EditorEventRow) -> EditorEventRow,
    ) = updateData { editor ->
        editor.copy(
            events =
                editor.events.map { row ->
                    if (row.rowId == rowId) transform(row) else row
                },
        )
    }

    private fun editorDataFromForm(
        contactId: Long,
        form: ContactForm,
    ): EditorData =
        EditorData(
            contactId = contactId,
            nameMode = form.name.mode,
            displayName = form.name.display,
            givenName = form.name.given,
            familyName = form.name.family,
            middleName = form.name.middle,
            namePrefix = form.name.prefix,
            nameSuffix = form.name.suffix,
            phoneticGivenName = form.name.phoneticGiven,
            phoneticMiddleName = form.name.phoneticMiddle,
            phoneticFamilyName = form.name.phoneticFamily,
            phones = form.phones.map(::phoneRowOf).ifEmpty { listOf(newPhoneRow()) },
            emails = form.emails.map(::emailRowOf),
            addresses = form.addresses.map(::addressRowOf),
            websites = form.websites.map(::websiteRowOf),
            instantMessages = form.instantMessages.map(::imRowOf),
            relations = form.relations.map(::relationRowOf),
            events = form.events.map(::eventRowOf),
            nickname = form.nickname,
            organization = form.organization,
            note = form.note,
            birthday = form.birthday,
            photoUri = form.photoUri,
            photoChanged = false,
            isBusy = false,
            initialForm = form,
        )

    private fun phoneRowOf(phone: EditablePhone) =
        newPhoneRow(
            dataId = phone.dataId,
            number = phone.number,
            kind = phone.kind,
            customLabel = phone.customLabel.orEmpty(),
        )

    private fun emailRowOf(email: EditableEmail) =
        newEmailRow(
            dataId = email.dataId,
            address = email.address,
            kind = email.kind,
            customLabel = email.customLabel.orEmpty(),
        )

    private fun addressRowOf(address: EditableAddress) =
        newAddressRow(
            dataId = address.dataId,
            address = address.address,
            kind = address.kind,
            customLabel = address.customLabel.orEmpty(),
        )

    private fun websiteRowOf(website: EditableWebsite) = newWebsiteRow(dataId = website.dataId, url = website.url)

    private fun imRowOf(message: EditableInstantMessage) =
        newImRow(
            dataId = message.dataId,
            address = message.address,
            protocol = message.protocol,
            customProtocol = message.customProtocol.orEmpty(),
        )

    private fun relationRowOf(relation: EditableRelation) =
        newRelationRow(
            dataId = relation.dataId,
            name = relation.name,
            kind = relation.kind,
            customLabel = relation.customLabel.orEmpty(),
        )

    private fun eventRowOf(event: EditableEvent) =
        newEventRow(
            dataId = event.dataId,
            date = event.date,
            kind = event.kind,
            customLabel = event.customLabel.orEmpty(),
        )
}
