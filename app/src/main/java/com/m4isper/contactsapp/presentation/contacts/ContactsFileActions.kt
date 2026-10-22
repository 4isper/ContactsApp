package com.m4isper.contactsapp.presentation.contacts

import com.m4isper.contactsapp.domain.model.ContactDetails
import com.m4isper.contactsapp.domain.usecase.ExportContactsUseCase
import com.m4isper.contactsapp.presentation.common.logw
import kotlinx.coroutines.CancellationException
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
    @Suppress("TooGenericExceptionCaught") // Осознанно: любая ошибка загрузки превращается в ExportFailed.
    fun onExportRequested() {
        scope.launch {
            val ids =
                try {
                    getContacts().map { it.contactId }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    logw(FILES_TAG, "export contacts load failed", e)
                    null
                }
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
    @Suppress("TooGenericExceptionCaught") // Осознанно: любую ошибку сборки показываем как ExportFailed.
    suspend fun exportContent(contactIds: List<Long>): String? =
        try {
            exportContacts(contactIds).takeIf { it.isNotEmpty() }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logw(FILES_TAG, "export vcard encode failed", e)
            null
        }

    @Suppress("TooGenericExceptionCaught") // Осознанно: любую ошибку сборки показываем как ExportFailed.
    private suspend fun exportReady(contactIds: List<Long>): ContactsEvent {
        val content = exportContent(contactIds)
        return if (content == null) {
            ContactsEvent.ExportFailed
        } else {
            ContactsEvent.ExportReady(content = content, count = contactIds.size)
        }
    }
}
