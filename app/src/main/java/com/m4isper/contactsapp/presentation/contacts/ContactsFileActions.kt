package com.m4isper.contactsapp.presentation.contacts

import com.m4isper.contactsapp.domain.model.ContactDetails
import com.m4isper.contactsapp.domain.usecase.ImportContactsUseCase
import com.m4isper.contactsapp.domain.vcard.VCardCodec
import com.m4isper.contactsapp.presentation.common.logw
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

private const val FILES_TAG = "ContactsFiles"

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
    private val vCardCodec: VCardCodec,
    private val refresh: () -> Unit,
) {
    /** Разбирает текст .vcf, создаёт контакты и сообщает результат одноразовым событием. */
    @Suppress("TooGenericExceptionCaught") // Осознанно: битый .vcf не должен ронять экран.
    fun onImportText(text: String) {
        scope.launch {
            val forms =
                try {
                    vCardCodec.parse(text)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    logw(FILES_TAG, "vCard parse failed", e)
                    null
                }
            if (forms == null) {
                events.value = ContactsEvent.ImportFailed
                return@launch
            }
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
                } catch (e: Exception) {
                    logw(FILES_TAG, "export contacts load failed", e)
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
            } catch (e: Exception) {
                logw(FILES_TAG, "export contact details failed", e)
                emptyList()
            }
        return if (details.isEmpty()) {
            ContactsEvent.ExportFailed
        } else {
            ContactsEvent.ExportReady(content = vCardCodec.encode(details), count = details.size)
        }
    }
}
