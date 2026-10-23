package com.m4isper.contactsapp.data.datasource.write.contacts

import android.content.ContentValues
import android.provider.ContactsContract.CommonDataKinds.StructuredName
import com.m4isper.contactsapp.domain.model.contact.EditableName
import com.m4isper.contactsapp.domain.model.contact.NameMode

/**
 * Решение по строкам имени при обновлении контакта: при очистке удаляем все
 * существующие строки, иначе обновляем их все (чтобы не осталось старого имени
 * в другом raw contact); если строк нет — вставляем одну в целевой raw.
 */
internal data class NameActions(
    val deleteIds: List<Long>,
    val updateIds: List<Long>,
    val insertNew: Boolean,
)

internal fun decideNameActions(
    existingIds: List<Long>,
    nameIsEmpty: Boolean,
): NameActions =
    when {
        nameIsEmpty -> NameActions(deleteIds = existingIds, updateIds = emptyList(), insertNew = false)
        existingIds.isEmpty() -> NameActions(deleteIds = emptyList(), updateIds = emptyList(), insertNew = true)
        else -> NameActions(deleteIds = emptyList(), updateIds = existingIds, insertNew = false)
    }

/**
 * Значения строки StructuredName: в режиме DISPLAY пишем DISPLAY_NAME и очищаем
 * компоненты, в STRUCTURED — наоборот, display name провайдер соберёт сам.
 */
internal fun structuredNameValues(name: EditableName): ContentValues =
    ContentValues().apply {
        val components =
            listOf(
                StructuredName.GIVEN_NAME to name.given,
                StructuredName.FAMILY_NAME to name.family,
                StructuredName.MIDDLE_NAME to name.middle,
                StructuredName.PREFIX to name.prefix,
                StructuredName.SUFFIX to name.suffix,
                StructuredName.PHONETIC_GIVEN_NAME to name.phoneticGiven,
                StructuredName.PHONETIC_MIDDLE_NAME to name.phoneticMiddle,
                StructuredName.PHONETIC_FAMILY_NAME to name.phoneticFamily,
            )
        if (name.mode == NameMode.DISPLAY) {
            put(StructuredName.DISPLAY_NAME, name.display.trim())
            components.forEach { (column, _) -> putNull(column) }
        } else {
            putNull(StructuredName.DISPLAY_NAME)
            components.forEach { (column, value) -> put(column, value.trim().takeIf { it.isNotEmpty() }) }
        }
    }
