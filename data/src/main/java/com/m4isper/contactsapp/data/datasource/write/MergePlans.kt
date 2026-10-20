package com.m4isper.contactsapp.data.datasource.write

import com.m4isper.contactsapp.domain.model.ContactForm
import com.m4isper.contactsapp.domain.model.EditableName

/**
 * Сливает формы дубликатов в одну: поля [target] остаются основными, недостающие
 * значения источников добавляются в концы списков. Строки источников получают
 * dataId = null — план обновления вставит их в целевой raw contact как новые,
 * поэтому ссылки на строки удаляемых контактов не сохраняются. Имя, одиночные
 * поля и фото берутся из target, если он их заполнил; метки объединяются.
 * События источников с неразобранной датой не переносятся: план записи умеет
 * вставлять только события с датой.
 */
internal fun mergeContactForms(
    target: ContactForm,
    sources: List<ContactForm>,
): ContactForm {
    val sourcePhoto = sources.firstNotNullOfOrNull { it.photoUri }
    return ContactForm(
        name = mergedName(target, sources),
        phones = mergeValues(target, sources, ContactForm::phones, ::phoneKey) { it.copy(rowId = null) },
        emails = mergeValues(target, sources, ContactForm::emails, ::emailKey) { it.copy(rowId = null) },
        addresses = mergeValues(target, sources, ContactForm::addresses, ::addressKey) { it.copy(rowId = null) },
        websites = mergeValues(target, sources, ContactForm::websites, ::websiteKey) { it.copy(rowId = null) },
        instantMessages =
            mergeValues(target, sources, ContactForm::instantMessages, ::imKey) { it.copy(rowId = null) },
        events =
            mergeValues(target, sources, { form -> form.events.filter { it.date != null } }, ::eventKey) {
                it.copy(rowId = null)
            },
        organization =
            firstFilled(target.organization, sources.map { it.organization }, { !it.isEmpty }) {
                it.copy(rowId = null)
            },
        note =
            firstFilled(target.note, sources.map { it.note }, { it.note.isNotBlank() }) {
                it.copy(rowId = null)
            },
        birthday =
            firstFilled(target.birthday, sources.map { it.birthday }, { it.value != null }) {
                it.copy(rowId = null)
            },
        nickname =
            firstFilled(target.nickname, sources.map { it.nickname }, { it.nickname.isNotBlank() }) {
                it.copy(rowId = null)
            },
        relations = mergeValues(target, sources, ContactForm::relations, ::relationKey) { it.copy(rowId = null) },
        groups = (listOf(target.groups) + sources.map { it.groups }).flatten().distinct(),
        photoUri = target.photoUri ?: sourcePhoto,
        photoChanged = target.photoChanged || (target.photoUri == null && sourcePhoto != null),
    )
}

/** Имя: значение target, иначе первое заполненное имя источника. */
private fun mergedName(
    target: ContactForm,
    sources: List<ContactForm>,
): EditableName =
    if (!target.name.isEmpty) {
        target.name
    } else {
        sources.firstOrNull { !it.name.isEmpty }?.name ?: target.name
    }

/**
 * Списки: значения target сохраняют свои dataId, значения источников добавляются,
 * если такого же значения ещё нет. Пустые значения пропускаются.
 */
private fun <T> mergeValues(
    target: ContactForm,
    sources: List<ContactForm>,
    values: (ContactForm) -> List<T>,
    key: (T) -> String?,
    fresh: (T) -> T,
): List<T> {
    val merged = values(target).toMutableList()
    val seen = merged.mapNotNullTo(mutableSetOf(), key)
    sources.forEach { source ->
        values(source).forEach { item ->
            val itemKey = key(item)
            if (itemKey != null && seen.add(itemKey)) merged += fresh(item)
        }
    }
    return merged
}

/** Одиночное поле: значение target, иначе первое заполненное значение источника. */
private fun <T> firstFilled(
    primary: T,
    alternatives: List<T>,
    isFilled: (T) -> Boolean,
    fresh: (T) -> T,
): T =
    if (isFilled(primary)) {
        primary
    } else {
        alternatives.firstOrNull(isFilled)?.let(fresh) ?: primary
    }
