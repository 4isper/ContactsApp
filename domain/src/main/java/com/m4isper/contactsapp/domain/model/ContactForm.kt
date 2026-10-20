package com.m4isper.contactsapp.domain.model

/**
 * Строки формы, загруженные из репозитория, несут [rowId] — непрозрачный идентификатор
 * существующей записи. Его назначает репозиторий, домен его не интерпретирует;
 * у новых строк rowId == null.
 */
data class EditablePhone(
    val rowId: Long? = null,
    val number: String,
    val kind: PhoneKind = PhoneKind.MOBILE,
    val customLabel: String? = null,
)

data class EditableEmail(
    val rowId: Long? = null,
    val address: String,
    val kind: EmailKind = EmailKind.HOME,
    val customLabel: String? = null,
)

data class EditableAddress(
    val rowId: Long? = null,
    val address: String,
    val kind: AddressKind = AddressKind.HOME,
    val customLabel: String? = null,
)

data class EditableWebsite(
    val rowId: Long? = null,
    val url: String,
)

data class EditableInstantMessage(
    val rowId: Long? = null,
    val address: String,
    val protocol: ImProtocol = ImProtocol.CUSTOM,
    val customProtocol: String? = null,
)

data class EditableOrganization(
    val rowId: Long? = null,
    val company: String = "",
    val title: String = "",
) {
    val isEmpty: Boolean
        get() = company.isBlank() && title.isBlank()
}

data class EditableNote(
    val rowId: Long? = null,
    val note: String = "",
)

data class EditableBirthday(
    val rowId: Long? = null,
    val value: Birthday? = null,
)

data class EditableNickname(
    val rowId: Long? = null,
    val nickname: String = "",
)

data class EditableRelation(
    val rowId: Long? = null,
    val name: String,
    val kind: RelationKind = RelationKind.OTHER,
    val customLabel: String? = null,
)

data class EditableEvent(
    val rowId: Long? = null,
    val date: Birthday? = null,
    val kind: EventKind = EventKind.ANNIVERSARY,
    val customLabel: String? = null,
    /**
     * Дата в формате провайдера, которую не удалось разобрать: строка события не теряется,
     * сохраняется как есть, пока пользователь не задаст дату.
     */
    val unparsedDate: String? = null,
)

enum class NameMode {
    /** Одно поле «Полное имя»: пишется в DISPLAY_NAME. */
    DISPLAY,

    /** Компоненты Имя/Фамилия/Отчество/... : провайдер сам собирает DISPLAY_NAME. */
    STRUCTURED,
}

/**
 * Имя в редакторе: единое поле или компоненты. Собранное имя —
 * «приставка имя отчество фамилия суффикс», пустые части пропускаются.
 */
data class EditableName(
    val mode: NameMode = NameMode.DISPLAY,
    val display: String = "",
    val given: String = "",
    val family: String = "",
    val middle: String = "",
    val prefix: String = "",
    val suffix: String = "",
    val phoneticGiven: String = "",
    val phoneticMiddle: String = "",
    val phoneticFamily: String = "",
) {
    val hasComponents: Boolean
        get() =
            given.isNotBlank() ||
                family.isNotBlank() ||
                middle.isNotBlank() ||
                prefix.isNotBlank() ||
                suffix.isNotBlank() ||
                phoneticGiven.isNotBlank() ||
                phoneticMiddle.isNotBlank() ||
                phoneticFamily.isNotBlank()

    /** Пусто ли имя в текущем режиме. */
    val isEmpty: Boolean
        get() = if (mode == NameMode.DISPLAY) display.isBlank() else !hasComponents

    val composed: String
        get() = composeDisplayName(prefix, given, middle, family, suffix)
}

/** Склейка частей имени через пробел; пустые и пробельные части пропускаются. */
fun composeDisplayName(vararg parts: String): String =
    parts
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .joinToString(" ")

data class ContactForm(
    val name: EditableName = EditableName(),
    val phones: List<EditablePhone> = emptyList(),
    val emails: List<EditableEmail> = emptyList(),
    val addresses: List<EditableAddress> = emptyList(),
    val websites: List<EditableWebsite> = emptyList(),
    val instantMessages: List<EditableInstantMessage> = emptyList(),
    val organization: EditableOrganization = EditableOrganization(),
    val note: EditableNote = EditableNote(),
    val birthday: EditableBirthday = EditableBirthday(),
    val nickname: EditableNickname = EditableNickname(),
    val relations: List<EditableRelation> = emptyList(),
    val events: List<EditableEvent> = emptyList(),
    /** Выбранные группы (метки) контакта. */
    val groups: List<Long> = emptyList(),
    /** Текущее фото (thumbnail существующего контакта или выбранный пользователем URI). */
    val photoUri: String? = null,
    /** true, если пользователь сменил или удалил фото — только тогда фото трогаем. */
    val photoChanged: Boolean = false,
)

/**
 * Нормализует форму перед сохранением: убирает пустые строки-плейсхолдеры редактора,
 * пустые подписи и сортирует группы. Единая точка нормализации для всех сценариев:
 * редактор, импорт и любые будущие точки входа.
 */
fun ContactForm.normalized(): ContactForm =
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

enum class FormError {
    EMPTY_FORM,
}

fun validateContactForm(form: ContactForm): FormError? = FormError.EMPTY_FORM.takeIf { form.isEffectivelyEmpty() }

private fun ContactForm.isEffectivelyEmpty(): Boolean =
    name.isEmpty &&
        hasNoListFields() &&
        hasNoSingleFields()

/** Многозначные поля (списки строк) не заполнены. */
private fun ContactForm.hasNoListFields(): Boolean =
    phones.none { it.number.isNotBlank() } &&
        emails.none { it.address.isNotBlank() } &&
        addresses.none { it.address.isNotBlank() } &&
        websites.none { it.url.isNotBlank() } &&
        instantMessages.none { it.address.isNotBlank() } &&
        relations.none { it.name.isNotBlank() } &&
        events.none { it.date != null || it.unparsedDate != null }

/** Одиночные поля (организация, заметка, ДР, никнейм) не заполнены. */
private fun ContactForm.hasNoSingleFields(): Boolean =
    organization.isEmpty &&
        note.note.isBlank() &&
        birthday.value == null &&
        nickname.nickname.isBlank()
