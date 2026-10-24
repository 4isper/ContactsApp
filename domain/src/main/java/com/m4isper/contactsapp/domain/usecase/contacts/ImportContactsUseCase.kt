package com.m4isper.contactsapp.domain.usecase.contacts

import com.m4isper.contactsapp.domain.model.contact.ContactAccount
import com.m4isper.contactsapp.domain.model.contact.ContactGroup
import com.m4isper.contactsapp.domain.model.contact.ImportDecision
import com.m4isper.contactsapp.domain.model.contact.ImportEntry
import com.m4isper.contactsapp.domain.model.contact.ImportFailure
import com.m4isper.contactsapp.domain.model.contact.ImportFailureReason
import com.m4isper.contactsapp.domain.model.contact.ImportOutcome
import com.m4isper.contactsapp.domain.usecase.groups.AddContactsToGroupUseCase
import com.m4isper.contactsapp.domain.usecase.groups.GetGroupsUseCase
import com.m4isper.contactsapp.domain.vcard.VCardContact
import kotlinx.coroutines.CancellationException
import java.util.Locale

/**
 * Импорт контактов из .vcf: создание, слияние с существующими и пропуск — по решению пользователя.
 * Импорт идёт через [CreateContactUseCase], поэтому форма нормализуется и валидируется
 * так же, как при обычном создании. Проблемный контакт не роняет остальные:
 * по нему пишется причина в [ImportOutcome.failures].
 */
class ImportContactsUseCase(
    private val createContact: CreateContactUseCase,
    private val mergeContacts: MergeContactsUseCase,
    private val setContactPhoto: SetContactPhotoUseCase,
    private val addContactsToGroup: AddContactsToGroupUseCase,
    private val getGroups: GetGroupsUseCase,
    private val deleteContacts: DeleteContactsUseCase,
) {
    suspend operator fun invoke(
        entries: List<ImportEntry>,
        account: ContactAccount = ContactAccount.Local,
    ): ImportOutcome {
        val groupsByTitle =
            getGroups()
                .filter { it.belongsTo(account) }
                .groupBy { it.title.trim().lowercase(Locale.ROOT) }
        val failures = mutableListOf<ImportFailure>()
        val skippedCategories = mutableSetOf<String>()
        var created = 0
        var merged = 0
        var skipped = 0

        entries.forEach { entry ->
            when (val decision = entry.decision) {
                ImportDecision.Skip -> skipped += 1
                ImportDecision.Create ->
                    created += importCreated(entry, account, groupsByTitle, failures, skippedCategories)
                is ImportDecision.Merge ->
                    merged +=
                        importMerged(
                            decision,
                            entry,
                            account,
                            groupsByTitle,
                            failures,
                            skippedCategories,
                        )
            }
        }
        return ImportOutcome(
            created = created,
            merged = merged,
            skipped = skipped,
            failures = failures,
            skippedCategories = skippedCategories.toList(),
        )
    }

    /** Создание контакта: 1, если контакт создан, иначе 0 с записанной причиной. */
    private suspend fun importCreated(
        entry: ImportEntry,
        account: ContactAccount,
        groupsByTitle: Map<String, List<ContactGroup>>,
        failures: MutableList<ImportFailure>,
        skippedCategories: MutableSet<String>,
    ): Int =
        when (val result = createSafely(entry, account)) {
            null -> {
                failures += entry.failure(ImportFailureReason.FAILED)
                0
            }
            is SaveResult.Invalid -> {
                failures += entry.failure(ImportFailureReason.INVALID)
                0
            }
            is SaveResult.Saved -> {
                applyExtras(result.contactId, entry.contact, groupsByTitle, failures, skippedCategories)
                1
            }
        }

    /** Влив в существующий контакт: 1, если влито, иначе 0 с записанной причиной. */
    private suspend fun importMerged(
        decision: ImportDecision.Merge,
        entry: ImportEntry,
        account: ContactAccount,
        groupsByTitle: Map<String, List<ContactGroup>>,
        failures: MutableList<ImportFailure>,
        skippedCategories: MutableSet<String>,
    ): Int =
        when (val result = createSafely(entry, account)) {
            null -> {
                failures += entry.failure(ImportFailureReason.FAILED)
                0
            }
            is SaveResult.Invalid -> {
                failures += entry.failure(ImportFailureReason.INVALID)
                0
            }
            is SaveResult.Saved ->
                if (mergeInto(decision.targetId, result.contactId)) {
                    applyExtras(decision.targetId, entry.contact, groupsByTitle, failures, skippedCategories)
                    1
                } else {
                    failures += entry.failure(ImportFailureReason.MERGE_FAILED)
                    deleteContacts(setOf(result.contactId))
                    0
                }
        }

    /** Создание контакта: сбой провайдера не отменяет импорт остальных. */
    private suspend fun createSafely(
        entry: ImportEntry,
        account: ContactAccount,
    ): SaveResult? =
        try {
            createContact(entry.contact.form, account)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }

    /** Слияние импортированного контакта с существующим: поля цели остаются, недостающие добавляются. */
    private suspend fun mergeInto(
        targetId: Long,
        sourceId: Long,
    ): Boolean =
        try {
            mergeContacts(targetId, listOf(sourceId)) != null
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            false
        }

    /** Фото и группы из файла: ошибка здесь не отменяет импорт самого контакта. */
    private suspend fun applyExtras(
        contactId: Long,
        contact: VCardContact,
        groupsByTitle: Map<String, List<ContactGroup>>,
        failures: MutableList<ImportFailure>,
        skippedCategories: MutableSet<String>,
    ) {
        contact.photo?.let { photo ->
            try {
                setContactPhoto(contactId, photo)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                failures += ImportFailure(contact.form.name.display, ImportFailureReason.PHOTO_FAILED)
            }
        }
        contact.categories.forEach { category ->
            val group = groupsByTitle[category.trim().lowercase(Locale.ROOT)]?.firstOrNull()
            if (group == null) {
                skippedCategories += category
            } else {
                try {
                    addContactsToGroup(group.id, listOf(contactId))
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    failures += ImportFailure(contact.form.name.display, ImportFailureReason.GROUP_FAILED)
                }
            }
        }
    }

    private fun ImportEntry.failure(reason: ImportFailureReason): ImportFailure =
        ImportFailure(name = contact.form.name.display, reason = reason)
}
