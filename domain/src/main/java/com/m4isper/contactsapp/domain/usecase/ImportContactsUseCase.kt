package com.m4isper.contactsapp.domain.usecase

import com.m4isper.contactsapp.domain.model.ContactForm
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.CancellationException

/**
 * Импорт идёт через [CreateContactUseCase], поэтому форма нормализуется и валидируется
 * так же, как при обычном создании. Невалидные и проблемные контакты пропускаются.
 */
@Inject
class ImportContactsUseCase(
    private val createContact: CreateContactUseCase,
) {
    /** Создаёт контакты по одному, пропуская проблемные. @return число созданных контактов. */
    @Suppress("TooGenericExceptionCaught") // Осознанно: битый контакт не должен ронять весь импорт.
    suspend operator fun invoke(forms: List<ContactForm>): Int {
        var imported = 0
        forms.forEach { form ->
            try {
                if (createContact(form) is SaveResult.Saved) imported += 1
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Пропускаем проблемный контакт и продолжаем импорт.
            }
        }
        return imported
    }
}
