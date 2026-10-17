package com.m4isper.contactsapp.domain.usecase

import com.m4isper.contactsapp.domain.model.ContactForm
import com.m4isper.contactsapp.domain.repository.ContactsRepository
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.CancellationException

@Inject
class ImportContactsUseCase(
    private val repository: ContactsRepository,
) {
    /** Создаёт контакты по одному, пропуская проблемные. @return число созданных контактов. */
    @Suppress("TooGenericExceptionCaught") // Осознанно: битый контакт не должен ронять весь импорт.
    suspend operator fun invoke(forms: List<ContactForm>): Int {
        var imported = 0
        forms.forEach { form ->
            try {
                repository.createContact(form)
                imported += 1
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Пропускаем проблемный контакт и продолжаем импорт.
            }
        }
        return imported
    }
}
