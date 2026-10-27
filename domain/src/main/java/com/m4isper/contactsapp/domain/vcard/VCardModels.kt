package com.m4isper.contactsapp.domain.vcard

import com.m4isper.contactsapp.domain.model.contact.ContactDetails
import com.m4isper.contactsapp.domain.model.contact.ContactForm

/**
 * Контакт, разобранный из .vcf: форма для редактора, фото и категории (названия групп).
 * Фото и категории лежат рядом с формой: провайдер хранит их отдельными строками,
 * а форма их не описывает.
 */
class VCardContact(
    val form: ContactForm,
    val photo: ByteArray? = null,
    val categories: List<String> = emptyList(),
    /** Фото было в файле, но не поместилось в лимит размера и не сохранено. */
    val photoSkipped: Boolean = false,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is VCardContact) return false
        return form == other.form &&
            photo.contentEquals(other.photo) &&
            categories == other.categories &&
            photoSkipped == other.photoSkipped
    }

    override fun hashCode(): Int {
        var result = form.hashCode()
        result = 31 * result + (photo?.contentHashCode() ?: 0)
        result = 31 * result + categories.hashCode()
        result = 31 * result + photoSkipped.hashCode()
        return result
    }
}

/** Контакт для экспорта: детали, фото и названия групп. */
class VCardExportContact(
    val details: ContactDetails,
    val photo: ByteArray? = null,
    val groups: List<String> = emptyList(),
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is VCardExportContact) return false
        return details == other.details &&
            photo.contentEquals(other.photo) &&
            groups == other.groups
    }

    override fun hashCode(): Int {
        var result = details.hashCode()
        result = 31 * result + (photo?.contentHashCode() ?: 0)
        result = 31 * result + groups.hashCode()
        return result
    }
}

/** Причина, по которой контакт из файла не удалось разобрать. */
enum class VCardIssue {
    /** В блоке нет ни одного поля. */
    EMPTY,

    /** В блоке нет ни одного поддерживаемого поля. */
    UNSUPPORTED,
}

/** Неразобранный контакт: имя (если было) и причина пропуска. */
data class VCardInvalidContact(
    val name: String,
    val issue: VCardIssue,
)

/** Результат разбора .vcf: разобранные контакты и пропущенные блоки. */
data class VCardParseResult(
    val contacts: List<VCardContact> = emptyList(),
    val invalid: List<VCardInvalidContact> = emptyList(),
)
