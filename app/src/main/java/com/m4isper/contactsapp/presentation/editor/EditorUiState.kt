package com.m4isper.contactsapp.presentation.editor

import com.m4isper.contactsapp.domain.model.common.AppError
import com.m4isper.contactsapp.domain.model.contact.ContactAccount
import com.m4isper.contactsapp.domain.model.contact.ContactDetails
import com.m4isper.contactsapp.domain.model.contact.ContactForm
import com.m4isper.contactsapp.domain.model.contact.ContactGroup
import com.m4isper.contactsapp.domain.model.contact.EditableAddress
import com.m4isper.contactsapp.domain.model.contact.EditableBirthday
import com.m4isper.contactsapp.domain.model.contact.EditableEmail
import com.m4isper.contactsapp.domain.model.contact.EditableEvent
import com.m4isper.contactsapp.domain.model.contact.EditableInstantMessage
import com.m4isper.contactsapp.domain.model.contact.EditableName
import com.m4isper.contactsapp.domain.model.contact.EditableNickname
import com.m4isper.contactsapp.domain.model.contact.EditableNote
import com.m4isper.contactsapp.domain.model.contact.EditableOrganization
import com.m4isper.contactsapp.domain.model.contact.EditablePhone
import com.m4isper.contactsapp.domain.model.contact.EditableRelation
import com.m4isper.contactsapp.domain.model.contact.EditableSipAddress
import com.m4isper.contactsapp.domain.model.contact.EditableWebsite
import com.m4isper.contactsapp.domain.model.contact.FormError
import com.m4isper.contactsapp.domain.model.contact.FormField
import com.m4isper.contactsapp.domain.model.contact.NameMode
import com.m4isper.contactsapp.domain.model.contact.PhotoRef
import com.m4isper.contactsapp.domain.model.contact.composeDisplayName
import com.m4isper.contactsapp.domain.model.contact.normalized

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
    val sipAddresses: List<EditorRow<EditableSipAddress>> = emptyList(),
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
    /** Ошибки конкретных полей: показываем проблему рядом с полем. */
    val fieldErrors: Map<FormField, FormError> = emptyMap(),
    /** Строка-владелец первой ошибки каждого поля: подсвечиваем только её. */
    val firstErrorRowIds: Map<FormField, Long> = emptyMap(),
    /** Похожие контакты: предупреждаем о вероятном дубле перед сохранением. */
    val similarContacts: List<ContactDetails> = emptyList(),
    val error: AppError? = null,
    /** Удаление не удалось: экран не закрывается, форма остаётся доступной. */
    val deleteFailed: Boolean = false,
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

    /** Группы, доступные для аккаунта контакта. */
    val availableGroups: List<ContactGroup>
        get() {
            val account = selectedAccount ?: return allGroups
            return allGroups.filter { it.belongsTo(account) }
        }

    /** Текущее состояние формы; пустые строки-плейсхолдеры убирает доменная нормализация. */
    fun toForm(): ContactForm =
        ContactForm(
            name = editableName(),
            phones = phones.map { it.value },
            emails = emails.map { it.value },
            addresses = addresses.map { it.value },
            websites = websites.map { it.value },
            instantMessages = instantMessages.map { it.value },
            sipAddresses = sipAddresses.map { it.value },
            relations = relations.map { it.value },
            events = events.map { it.value },
            nickname = nickname,
            organization = organization,
            note = note,
            birthday = birthday,
            groups = selectedGroupIds.toList(),
            photo = PhotoRef.of(photoUri),
            photoChanged = photoChanged,
        )

    fun isDirty(): Boolean = toForm().normalized() != initialForm.normalized()

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
        sipAddresses = form.sipAddresses.map(::row),
        relations = form.relations.map(::row),
        events = form.events.map(::row),
        organization = form.organization,
        note = form.note,
        birthday = form.birthday,
        nickname = form.nickname,
        selectedGroupIds = form.groups.toSet(),
        photoUri = form.photo?.uri,
        photoChanged = false,
        isBusy = false,
        initialForm = form,
    )
}

/**
 * Следующий id строки формы: счётчик общий для всех видов строк, чтобы ключи
 * списков не совпадали.
 */
internal fun EditorUiState.nextRowId(): Long {
    val ids =
        listOf(
            phones.maxOfOrNull { it.rowId } ?: 0L,
            emails.maxOfOrNull { it.rowId } ?: 0L,
            addresses.maxOfOrNull { it.rowId } ?: 0L,
            websites.maxOfOrNull { it.rowId } ?: 0L,
            instantMessages.maxOfOrNull { it.rowId } ?: 0L,
            sipAddresses.maxOfOrNull { it.rowId } ?: 0L,
            relations.maxOfOrNull { it.rowId } ?: 0L,
            events.maxOfOrNull { it.rowId } ?: 0L,
        )
    return ids.max() + 1L
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
