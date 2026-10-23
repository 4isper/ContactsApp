package com.m4isper.contactsapp.presentation.contacts.effects

import com.m4isper.contactsapp.domain.model.contact.ContactDetails
import com.m4isper.contactsapp.domain.usecase.contacts.ExportContactsUseCase
import com.m4isper.contactsapp.presentation.common.safeCall
import com.m4isper.contactsapp.presentation.contacts.ContactsEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

private const val FILES_TAG = "ContactsFiles"

/**
 * Экспорт .vcf: всех контактов или только выделенных. Сборку файла делает UI.
 * Импорт живёт отдельно: [com.m4isper.contactsapp.presentation.import.ImportPreviewViewModel].
 */
internal class ContactsFileActions(
    private val scope: CoroutineScope,
    private val events: MutableStateFlow<ContactsEvent?>,
    private val exportContacts: ExportContactsUseCase,
    private val getContacts: suspend () -> List<ContactDetails>,
) {
    /** Собирает .vcf всех контактов; запись в файл — на стороне UI. */
    fun onExportRequested() {
        scope.launch {
            val ids =
                safeCall(FILES_TAG, "export contacts load failed") {
                    getContacts().map { it.contactId }
                }.getOrElse { null }

            events.value =
                when {
                    ids == null -> ContactsEvent.ExportFailed
                    ids.isEmpty() -> ContactsEvent.ExportEmpty
                    else -> exportReady(ids)
                }
        }
    }

    /** Собирает .vcf выделенных контактов. */
    fun onExportSelection(contactIds: List<Long>) {
        scope.launch {
            events.value =
                when {
                    contactIds.isEmpty() -> ContactsEvent.ExportEmpty
                    else -> exportReady(contactIds)
                }
        }
    }

    /**
     * Содержимое .vcf для контактов: null, если сборка не удалась.
     * Экран делит этот текст с пользователем напрямую.
     */
    suspend fun exportContent(contactIds: List<Long>): String? =
        safeCall(FILES_TAG, "export vcard encode failed") {
            exportContacts(contactIds).takeIf { it.isNotEmpty() }
        }.getOrElse { null }

    private suspend fun exportReady(contactIds: List<Long>): ContactsEvent {
        val content = exportContent(contactIds)
        return if (content == null) {
            ContactsEvent.ExportFailed
        } else {
            ContactsEvent.ExportReady(content = content, count = contactIds.size)
        }
    }
}
