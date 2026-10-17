package com.m4isper.contactsapp.data.datasource

import android.content.ContentValues
import android.provider.ContactsContract.CommonDataKinds.StructuredName
import com.m4isper.contactsapp.domain.model.EditableName
import com.m4isper.contactsapp.domain.model.NameMode
import com.m4isper.contactsapp.domain.model.NameParts

// Чтение и запись компонентов StructuredName. Раскладка колонок: data1 — display name,
// data2 — given, data3 — family, data4 — prefix, data5 — middle, data6 — suffix,
// data7 — phonetic given, data8 — phonetic middle, data9 — phonetic family.

/** Компоненты структурированного имени; null, если все они пустые. */
internal fun namePartsOf(rows: List<DataRow>): NameParts? =
    rows
        .firstOrNull { it.mimetype == StructuredName.CONTENT_ITEM_TYPE }
        ?.let { row ->
            NameParts(
                given = row.data2.orEmpty().trim(),
                family = row.data3.orEmpty().trim(),
                prefix = row.data4.orEmpty().trim(),
                middle = row.data5.orEmpty().trim(),
                suffix = row.data6.orEmpty().trim(),
                phoneticGiven = row.data7.orEmpty().trim(),
                phoneticMiddle = row.data8.orEmpty().trim(),
                phoneticFamily = row.data9.orEmpty().trim(),
            )
        }?.takeIf { !it.isEmpty }

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
