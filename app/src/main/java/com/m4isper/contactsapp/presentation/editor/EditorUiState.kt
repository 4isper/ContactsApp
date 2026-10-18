package com.m4isper.contactsapp.presentation.editor

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
    )

private fun String?.stripped(): String? = this?.takeIf { it.isNotBlank() }
