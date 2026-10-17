package com.m4isper.contactsapp.presentation.contacts

import com.m4isper.contactsapp.domain.model.ContactDetails
import com.m4isper.contactsapp.domain.usecase.ImportContactsUseCase
import com.m4isper.contactsapp.presentation.common.buildVCards
import com.m4isper.contactsapp.presentation.common.parseVCards
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

/**
 * Сценарии импорта и экспорта .vcf. Вынесены из ViewModel отдельным объектом,
 * чтобы её API и лимит функций на файл оставались компактными.
 */
internal class ContactsFileActions(
    private val scope: CoroutineScope,
    private val events: MutableStateFlow<ContactsEvent?>,
    private val getContacts: suspend () -> List<ContactDetails>,
    private val getContactDetails: suspend (Long) -> ContactDetails?,
    private val importContacts: ImportContactsUseCase,
    private val refresh: () -> Unit,
) {
    /** Разбирает текст .vcf, создаёт контакты и сообщает результат одноразовым событием. */
    fun onImportText(text: String) {
        scope.launch {
            val forms = parseVCards(text)
            val imported = importContacts(forms)
            events.value =
                when {
                    imported > 0 -> ContactsEvent.ImportFinished(imported)
                    forms.isEmpty() -> ContactsEvent.ImportEmpty
                    else -> ContactsEvent.ImportFailed
                }
            if (imported > 0) refresh()
        }
    }

    /** Собирает .vcf всех контактов; запись в файл — на стороне UI. */
    @Suppress("TooGenericExceptionCaught") // Осознанно: ошибки сбора превращаем в одноразовые события.
    fun onExportRequested() {
        scope.launch {
            val contacts =
                try {
                    getContacts()
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    null
                }
            events.value =
                when {
                    contacts == null -> ContactsEvent.ExportFailed
                    contacts.isEmpty() -> ContactsEvent.ExportEmpty
                    else -> exportReady(contacts)
                }
        }
    }

    @Suppress("TooGenericExceptionCaught") // Осознанно: любую ошибку детализации показываем как ExportFailed.
    private suspend fun exportReady(contacts: List<ContactDetails>): ContactsEvent {
        val details =
            try {
                contacts.mapNotNull { getContactDetails(it.contactId) }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                emptyList()
            }
        return if (details.isEmpty()) {
            ContactsEvent.ExportFailed
        } else {
            ContactsEvent.ExportReady(content = buildVCards(details), count = details.size)
        }
    }
}
