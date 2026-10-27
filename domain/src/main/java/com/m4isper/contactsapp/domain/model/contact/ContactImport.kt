package com.m4isper.contactsapp.domain.model.contact

import com.m4isper.contactsapp.domain.vcard.VCardContact

/** Решение по одному контакту из файла: создать, слить с существующим или пропустить. */
sealed interface ImportDecision {
    /** Создать новый контакт. */
    data object Create : ImportDecision

    /** Влить данные файла в существующий контакт: его поля остаются, недостающие добавляются. */
    data class Merge(
        val targetId: Long,
    ) : ImportDecision

    /** Не импортировать контакт. */
    data object Skip : ImportDecision
}

/** Контакт из файла и решение по нему. */
data class ImportEntry(
    val contact: VCardContact,
    val decision: ImportDecision,
)

/** Что не удалось сделать с контактом из файла. */
enum class ImportFailureReason {
    /** Контакт не прошёл проверку формы. */
    INVALID,

    /** Провайдер отклонил запись. */
    FAILED,

    /** Не удалось влить контакт в существующий. */
    MERGE_FAILED,

    /** Не удалось записать фото. */
    PHOTO_FAILED,

    /** Не удалось добавить контакт в группу. */
    GROUP_FAILED,
}

/** Ошибка импорта одного контакта. */
data class ImportFailure(
    val name: String,
    val reason: ImportFailureReason,
)

/** Итог импорта: сколько создано, слито, пропущено и что не получилось. */
data class ImportOutcome(
    val created: Int = 0,
    val merged: Int = 0,
    val skipped: Int = 0,
    val failures: List<ImportFailure> = emptyList(),
    /** Категории из файла, для которых в контактах нет подходящей группы. */
    val skippedCategories: List<String> = emptyList(),
)
