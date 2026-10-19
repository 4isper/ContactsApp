package com.m4isper.contactsapp.presentation.duplicates

import com.m4isper.contactsapp.domain.model.ContactDetails
import java.util.Locale

/** Минимальная длина номера (в цифрах): короче — совпадение считаем случайным. */
private const val MIN_PHONE_DIGITS = 5

/** Совпадение имён допускаем только для полных имён: минимум два слова и 5 символов. */
private const val MIN_NAME_LENGTH = 5

private val WHITESPACE = Regex("\\s+")

/**
 * Группа возможных дубликатов: [nameOnly] — внутри группы нет ни одного общего
 * телефона или адреса почты, контакты совпали только по полному имени.
 */
data class DuplicateGroup(
    val members: List<ContactDetails>,
    val nameOnly: Boolean,
)

/**
 * Ищет возможные дубликаты: контакты объединяются в группы по общим нормализованным
 * телефонам (цифры без форматирования) и адресам почты, а также по полному совпадению
 * имени. В группе первым идёт самый заполненный контакт — он же по умолчанию
 * остаётся основным при слиянии.
 */
internal fun findDuplicateGroups(contacts: List<ContactDetails>): List<DuplicateGroup> {
    val parents = IntArray(contacts.size) { it }

    fun rootOf(index: Int): Int {
        var root = index
        while (parents[root] != root) root = parents[root]
        var current = index
        while (parents[current] != current) {
            val next = parents[current]
            parents[current] = root
            current = next
        }
        return root
    }

    fun link(
        a: Int,
        b: Int,
    ) {
        val rootA = rootOf(a)
        val rootB = rootOf(b)
        if (rootA != rootB) parents[rootB] = rootA
    }

    val strongOwners = mutableMapOf<String, Int>()
    val nameOwners = mutableMapOf<String, Int>()
    contacts.forEachIndexed { index, contact ->
        strongKeys(contact).forEach { key ->
            strongOwners.putIfAbsent(key, index)?.let { owner -> link(owner, index) }
        }
        nameKey(contact)?.let { key ->
            nameOwners.putIfAbsent(key, index)?.let { owner -> link(owner, index) }
        }
    }

    return contacts.indices
        .groupBy { rootOf(it) }
        .values
        .mapNotNull { indices -> indices.takeIf { it.size > 1 }?.let { duplicateGroup(contacts, it) } }
        .sortedBy { group ->
            group.members
                .first()
                .name
                .lowercase(Locale.ROOT)
        }
}

private fun duplicateGroup(
    contacts: List<ContactDetails>,
    indices: List<Int>,
): DuplicateGroup {
    val members =
        indices
            .map { contacts[it] }
            .sortedWith(
                compareByDescending<ContactDetails> { completeness(it) }
                    .thenBy { it.name.lowercase(Locale.ROOT) }
                    .thenBy { it.contactId },
            )
    return DuplicateGroup(members = members, nameOnly = !hasSharedStrongKey(members))
}

/** Есть ли в группе общий телефон или адрес почты хотя бы у двух контактов. */
private fun hasSharedStrongKey(members: List<ContactDetails>): Boolean {
    val counts = mutableMapOf<String, Int>()
    members.forEach { contact ->
        strongKeys(contact).forEach { key -> counts[key] = (counts[key] ?: 0) + 1 }
    }
    return counts.values.any { it > 1 }
}

/** Ключи сильных совпадений: нормализованные телефоны и адреса почты (уникальные). */
private fun strongKeys(contact: ContactDetails): List<String> {
    val keys = mutableSetOf<String>()
    contact.phones.forEach { phone ->
        val digits = phone.number.filter(Char::isDigit)
        if (digits.length >= MIN_PHONE_DIGITS) keys += "p:$digits"
    }
    contact.emails.forEach { email ->
        normalizedText(email.address)?.let { keys += "e:$it" }
    }
    return keys.toList()
}

/** Полные имена для нестрогого сравнения: два и более слов, каждое непустое. */
private fun nameKey(contact: ContactDetails): String? {
    val normalized = normalizedText(contact.name) ?: return null
    return normalized.takeIf { it.length >= MIN_NAME_LENGTH && it.count(Char::isWhitespace) >= 1 }
}

/** Сколько полей заполнено: чем больше, тем вероятнее контакт останется основным. */
private fun completeness(contact: ContactDetails): Int =
    (if (contact.name.isNotBlank()) 1 else 0) +
        contact.phones.size +
        contact.emails.size +
        contact.addresses.size +
        contact.websites.size +
        contact.instantMessages.size +
        contact.relations.size +
        contact.events.size +
        (if (contact.organization != null) 1 else 0) +
        (if (!contact.note.isNullOrBlank()) 1 else 0) +
        (if (contact.birthday != null) 1 else 0) +
        (if (!contact.nickname.isNullOrBlank()) 1 else 0) +
        (if (contact.photoUri != null) 1 else 0)

private fun normalizedText(value: String): String? =
    value
        .trim()
        .lowercase(Locale.ROOT)
        .replace(WHITESPACE, " ")
        .takeIf(String::isNotEmpty)
