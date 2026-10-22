package com.m4isper.contactsapp.presentation.contacts

import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Выделенные контакты: набор id, который живёт отдельно от списка.
 * Отдельный кусок состояния — у выделения своя причина меняться.
 */
@Inject
internal class ContactsSelectionState {
    private val _selected = MutableStateFlow<Set<Long>>(emptySet())

    /** Выбранные контакты. */
    val selected: StateFlow<Set<Long>> = _selected.asStateFlow()

    /** Тап по карточке в режиме выделения и long-press в любом режиме переключают выделение. */
    fun onContactSelected(contactId: Long) {
        val current = _selected.value
        _selected.value = if (contactId in current) current - contactId else current + contactId
    }

    /** Снимает выделение. */
    fun clear() {
        _selected.value = emptySet()
    }

    /** Повторный вызов «выбрать все» снимает выделение. */
    fun selectAll(contactIds: List<Long>) {
        val all = contactIds.toSet()
        _selected.value = if (_selected.value.containsAll(all)) emptySet() else all
    }
}
