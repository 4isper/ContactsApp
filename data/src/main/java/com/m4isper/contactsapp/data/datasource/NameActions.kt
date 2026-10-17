package com.m4isper.contactsapp.data.datasource

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
