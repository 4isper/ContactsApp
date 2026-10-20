package com.m4isper.contactsapp.presentation.editor

import com.m4isper.contactsapp.domain.model.ContactAccount
import com.m4isper.contactsapp.domain.model.ContactForm
import com.m4isper.contactsapp.domain.model.ContactGroup
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
import com.m4isper.contactsapp.domain.model.FormError
import com.m4isper.contactsapp.domain.model.NameMode
import com.m4isper.contactsapp.domain.model.composeDisplayName

/**
 * Строка списка в редакторе: доменная модель плюс стабильный [rowId] для Compose-ключа.
 * rowId нужен там, где у модели ещё нет dataId (новая, не сохранённая строка).
 */
internal data class EditorRow<T>(
    val rowId: Long,
    val value: T,
)

internal data class EditorUiState(
    val contactId: Long? = null,
    /** Аккаунты, доступные для нового контакта; у существующего список пуст. */
    val accounts: List<ContactAccount> = emptyList(),
    val selectedAccount: ContactAccount? = null,
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
    val allGroups: List<ContactGroup> = emptyList(),
    val selectedGroupIds: Set<Long> = emptySet(),
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

    /** Выбранные группы в порядке списка всех групп. */
    val selectedGroups: List<ContactGroup>
        get() = allGroups.filter { it.id in selectedGroupIds }

    /** Группы, доступные для выбранного аккаунта; у существующего контакта — все. */
    val availableGroups: List<ContactGroup>
        get() {
            val account = selectedAccount
            return if (isNew && account != null) {
                allGroups.filter { it.belongsTo(account) }
            } else {
                allGroups
            }
        }

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
            groups = selectedGroupIds.toList(),
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

/** Состояние формы по загруженной форме; [idGenerator] выдаёт стабильные ключи строк. */
internal fun editorStateFromForm(
    contactId: Long,
    form: ContactForm,
    idGenerator: () -> Long,
): EditorUiState {
    fun <T> row(value: T) = EditorRow(rowId = idGenerator(), value = value)
    return EditorUiState(
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
        phones = form.phones.map(::row).ifEmpty { listOf(row(EditablePhone(number = ""))) },
        emails = form.emails.map(::row),
        addresses = form.addresses.map(::row),
        websites = form.websites.map(::row),
        instantMessages = form.instantMessages.map(::row),
        relations = form.relations.map(::row),
        events = form.events.map(::row),
        organization = form.organization,
        note = form.note,
        birthday = form.birthday,
        nickname = form.nickname,
        selectedGroupIds = form.groups.toSet(),
        photoUri = form.photoUri,
        photoChanged = false,
        isBusy = false,
        initialForm = form,
    )
}

/** Список строк, в котором строка [rowId] заменяется результатом [transform]. */
internal fun <T> List<EditorRow<T>>.withRow(
    rowId: Long,
    transform: (T) -> T,
): List<EditorRow<T>> =
    map { row ->
        if (row.rowId == rowId) row.copy(value = transform(row.value)) else row
    }

/**
 * DISPLAY → STRUCTURED: если компоненты пустые, переносим полное имя в «Имя»,
 * чтобы данные не потерялись. STRUCTURED → DISPLAY: собираем имя из компонентов.
 */
internal fun EditorUiState.toggledNameMode(): EditorUiState =
    if (nameMode == NameMode.DISPLAY) {
        val given = if (hasNameComponents) givenName else displayName.trim()
        copy(nameMode = NameMode.STRUCTURED, givenName = given)
    } else {
        copy(nameMode = NameMode.DISPLAY, displayName = composedName)
    }

/** Пустые строки — плейсхолдеры для ввода, в форму сохранения они не попадают. */
internal fun ContactForm.withoutBlankRows(): ContactForm =
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
        groups = groups.sorted(),
    )

private fun String?.stripped(): String? = this?.takeIf { it.isNotBlank() }
