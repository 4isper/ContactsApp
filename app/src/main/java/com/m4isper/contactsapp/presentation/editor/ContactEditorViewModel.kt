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

/**
 * Строка списка в редакторе: доменная модель плюс стабильный [rowId] для Compose-ключа.
 * rowId нужен там, где у модели ещё нет dataId (новая, не сохранённая строка).
 */
internal data class EditorRow<T>(
    val rowId: Long,
    val value: T,
)

/** Список строк, в котором строка [rowId] заменяется результатом [transform]. */
private fun <T> List<EditorRow<T>>.withRow(
    rowId: Long,
    transform: (T) -> T,
): List<EditorRow<T>> =
    map { row ->
        if (row.rowId == rowId) row.copy(value = transform(row.value)) else row
    }

internal data class EditorUiState(
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
    val phones: List<EditorRow<EditablePhone>> = emptyList(),
    val emails: List<EditorRow<EditableEmail>> = emptyList(),
    val addresses: List<EditorRow<EditableAddress>> = emptyList(),
    val websites: List<EditorRow<EditableWebsite>> = emptyList(),
    val instantMessages: List<EditorRow<EditableInstantMessage>> = emptyList(),
    val organization: EditableOrganization = EditableOrganization(),
    val note: EditableNote = EditableNote(),
    val birthday: EditableBirthday = EditableBirthday(),
    val nickname: EditableNickname = EditableNickname(),
    val relations: List<EditorRow<EditableRelation>> = emptyList(),
    val events: List<EditorRow<EditableEvent>> = emptyList(),
    val photoUri: String? = null,
    val photoChanged: Boolean = false,
    val isBusy: Boolean = false,
    val formError: FormError? = null,
    val errorMessage: String? = null,
    val closeScreen: Boolean = false,
    /** Форма на момент загрузки: база для сравнения в [isDirty]. */
    val initialForm: ContactForm = ContactForm(),
) {
    val isNew: Boolean
        get() = contactId == null

    /** Имя для превью аватара и заголовка: поле или собранные компоненты. */
    val effectiveName: String
        get() =
            if (nameMode == NameMode.DISPLAY) {
                displayName
            } else {
                composedName
            }

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

    /** Текущее состояние формы; пустые строки-плейсхолдеры убирает [withoutBlankRows]. */
    fun toForm(): ContactForm =
        ContactForm(
            name = editableName(),
            phones = phones.map { it.value },
            emails = emails.map { it.value },
            addresses = addresses.map { it.value },
            websites = websites.map { it.value },
            instantMessages = instantMessages.map { it.value },
            relations = relations.map { it.value },
            events = events.map { it.value },
            nickname = nickname,
            organization = organization,
            note = note,
            birthday = birthday,
            photoUri = photoUri,
            photoChanged = photoChanged,
        )

    fun isDirty(): Boolean = toForm().withoutBlankRows() != initialForm.withoutBlankRows()

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
}

/** Пустые строки — плейсхолдеры для ввода, в форму сохранения они не попадают. */
private fun ContactForm.withoutBlankRows(): ContactForm =
    copy(
        phones =
            phones
                .filter { it.number.isNotBlank() }
                .map { it.copy(customLabel = it.customLabel.stripped()) },
        emails =
            emails
                .filter { it.address.isNotBlank() }
                .map { it.copy(customLabel = it.customLabel.stripped()) },
        addresses =
            addresses
                .filter { it.address.isNotBlank() }
                .map { it.copy(customLabel = it.customLabel.stripped()) },
        websites = websites.filter { it.url.isNotBlank() },
        instantMessages =
            instantMessages
                .filter { it.address.isNotBlank() }
                .map { it.copy(customProtocol = it.customProtocol.stripped()) },
        relations =
            relations
                .filter { it.name.isNotBlank() }
                .map { it.copy(customLabel = it.customLabel.stripped()) },
        events =
            events
                .filter { it.date != null || it.unparsedDate != null }
                .map { it.copy(customLabel = it.customLabel.stripped()) },
    )

private fun String?.stripped(): String? = this?.takeIf { it.isNotBlank() }

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
            state.value = EditorUiState(phones = listOf(newRow(EditablePhone(number = ""))))
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
                    state.value = stateFromForm(id, form)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logw(EDITOR_TAG, "load contact form failed", e)
                state.update { it.copy(errorMessage = e.message, isBusy = false) }
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

    fun onNicknameChange(value: String) = updateData { it.copy(nickname = it.nickname.copy(nickname = value)) }

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

    fun onNoteChange(value: String) = updateData { it.copy(note = it.note.copy(note = value)) }

    fun onBirthdayChange(value: Birthday?) = updateData { it.copy(birthday = it.birthday.copy(value = value)) }

    fun onPhotoPicked(uri: String) = updateData { it.copy(photoUri = uri, photoChanged = true) }

    fun onPhotoRemoved() = updateData { it.copy(photoUri = null, photoChanged = true) }

    fun isDirty(): Boolean = state.value.isDirty()

    // Catch-all осознанный: любая ошибка превращается в Error-стейт.
    @Suppress("TooGenericExceptionCaught")
    fun onSave() {
        val id = state.value.contactId
        val form = state.value.toForm().withoutBlankRows()
        viewModelScope.launch {
            state.update { it.copy(isBusy = true, errorMessage = null) }
            try {
                val result =
                    if (id == null) {
                        createContactUseCase(form)
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
            state.update { it.copy(isBusy = true) }
            try {
                deleteContactUseCase(id)
                state.update { it.copy(closeScreen = true) }
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
        state.update { transform(it).copy(formError = null, errorMessage = null) }
    }

    /** Новая строка со стабильным ключом: счётчик живёт в пределах экрана. */
    private fun <T> newRow(value: T) = EditorRow(rowId = ++nextRowId, value = value)

    private fun stateFromForm(
        contactId: Long,
        form: ContactForm,
    ): EditorUiState =
        EditorUiState(
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
            phones = form.phones.map(::newRow).ifEmpty { listOf(newRow(EditablePhone(number = ""))) },
            emails = form.emails.map(::newRow),
            addresses = form.addresses.map(::newRow),
            websites = form.websites.map(::newRow),
            instantMessages = form.instantMessages.map(::newRow),
            relations = form.relations.map(::newRow),
            events = form.events.map(::newRow),
            organization = form.organization,
            note = form.note,
            birthday = form.birthday,
            nickname = form.nickname,
            photoUri = form.photoUri,
            photoChanged = false,
            isBusy = false,
            initialForm = form,
        )
}
