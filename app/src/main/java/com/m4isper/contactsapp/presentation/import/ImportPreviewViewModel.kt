package com.m4isper.contactsapp.presentation.import

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.m4isper.contactsapp.domain.duplicates.findImportDuplicates
import com.m4isper.contactsapp.domain.model.contact.ContactAccount
import com.m4isper.contactsapp.domain.model.contact.ContactDetails
import com.m4isper.contactsapp.domain.model.contact.ImportDecision
import com.m4isper.contactsapp.domain.model.contact.ImportEntry
import com.m4isper.contactsapp.domain.model.contact.ImportOutcome
import com.m4isper.contactsapp.domain.usecase.contacts.GetContactAccountsUseCase
import com.m4isper.contactsapp.domain.usecase.contacts.GetContactsUseCase
import com.m4isper.contactsapp.domain.usecase.contacts.ImportContactsUseCase
import com.m4isper.contactsapp.domain.vcard.VCardCodec
import com.m4isper.contactsapp.domain.vcard.VCardContact
import com.m4isper.contactsapp.domain.vcard.VCardInvalidContact
import com.m4isper.contactsapp.presentation.common.safeCall
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val IMPORT_TAG = "ImportPreview"

/** Один контакт из файла в предпросмотре импорта. */
data class ImportPreviewEntry(
    val index: Int,
    val name: String,
    val summary: String,
    val decision: ImportDecision,
    val duplicates: List<ContactDetails>,
    val hasPhoto: Boolean,
    val categories: List<String>,
)

/** Состояние экрана предпросмотра импорта. */
data class ImportPreviewUiState(
    val isPreparing: Boolean = false,
    val entries: List<ImportPreviewEntry> = emptyList(),
    val invalid: List<VCardInvalidContact> = emptyList(),
    val accounts: List<ContactAccount> = emptyList(),
    val selectedAccount: ContactAccount? = null,
    val isImporting: Boolean = false,
    val outcome: ImportOutcome? = null,
    /** Файл не удалось разобрать целиком. */
    val failed: Boolean = false,
) {
    val duplicateCount: Int
        get() = entries.count { it.duplicates.isNotEmpty() }

    val canImport: Boolean
        get() = entries.isNotEmpty() && !isImporting && outcome == null
}

/**
 * Предпросмотр импорта .vcf: сколько контактов в файле, какие из них уже есть в контактах
 * и что будет сделано с каждым — создать, влить в существующий или пропустить.
 */
@Inject
@ViewModelKey
@ContributesIntoMap(AppScope::class)
class ImportPreviewViewModel(
    private val codec: VCardCodec,
    private val getContacts: GetContactsUseCase,
    private val getContactAccounts: GetContactAccountsUseCase,
    private val importContacts: ImportContactsUseCase,
) : ViewModel() {
    private val state = MutableStateFlow(ImportPreviewUiState())
    val uiState: StateFlow<ImportPreviewUiState> = state.asStateFlow()

    private var parsed: List<VCardContact> = emptyList()

    /** Разбор файла и поиск дублей среди существующих контактов. */
    fun prepare(text: String) {
        viewModelScope.launch {
            state.value = ImportPreviewUiState(isPreparing = true)
            val result =
                safeCall(IMPORT_TAG, "vCard parse failed") {
                    codec.parse(text)
                }.getOrElse { null }

            if (result == null) {
                state.value = ImportPreviewUiState(failed = true)
                return@launch
            }
            val contacts = loadContacts()
            val accounts = loadAccounts()
            parsed = result.contacts
            state.value =
                ImportPreviewUiState(
                    entries = result.contacts.mapIndexed { index, contact -> entryOf(index, contact, contacts) },
                    invalid = result.invalid,
                    accounts = accounts,
                    selectedAccount = accounts.firstOrNull(),
                )
        }
    }

    /** Решение пользователя по одному контакту. */
    fun onDecisionChange(
        index: Int,
        decision: ImportDecision,
    ) {
        state.value =
            state.value.copy(
                entries =
                    state.value.entries.map { entry ->
                        if (entry.index == index) entry.copy(decision = decision) else entry
                    },
            )
    }

    /** Аккаунт, в который попадут новые контакты. */
    fun onAccountChange(account: ContactAccount) {
        state.value = state.value.copy(selectedAccount = account)
    }

    /** Импорт выбранных контактов. */
    fun onImport() {
        val current = state.value
        if (!current.canImport) return
        viewModelScope.launch {
            state.value = current.copy(isImporting = true)
            val entries =
                current.entries.mapNotNull { entry ->
                    parsed.getOrNull(entry.index)?.let { ImportEntry(contact = it, decision = entry.decision) }
                }
            val outcome =
                safeCall(IMPORT_TAG, "import failed") {
                    importContacts(entries, current.selectedAccount ?: ContactAccount.Local)
                }.getOrElse { null }

            state.value =
                state.value.copy(
                    isImporting = false,
                    outcome = outcome ?: ImportOutcome(),
                    failed = outcome == null,
                )
        }
    }

    private fun entryOf(
        index: Int,
        contact: VCardContact,
        contacts: List<ContactDetails>,
    ): ImportPreviewEntry =
        ImportPreviewEntry(
            index = index,
            name =
                contact.form.name.display
                    .ifBlank { contact.form.name.composed },
            summary =
                contact.form.phones
                    .firstOrNull()
                    ?.number ?: contact.form.emails
                    .firstOrNull()
                    ?.address
                    .orEmpty(),
            decision = ImportDecision.Create,
            duplicates = findImportDuplicates(contact, contacts),
            hasPhoto = contact.photo != null,
            categories = contact.categories,
        )

    private suspend fun loadContacts(): List<ContactDetails> =
        safeCall(IMPORT_TAG, "load contacts failed") {
            getContacts()
        }.getOrElse { emptyList() }

    private suspend fun loadAccounts(): List<ContactAccount> =
        safeCall(IMPORT_TAG, "load accounts failed") {
            getContactAccounts()
        }.getOrElse { listOf(ContactAccount.Local) }
}
