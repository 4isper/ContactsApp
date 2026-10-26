package com.m4isper.contactsapp

import com.m4isper.contactsapp.domain.model.contact.AccountSyncInfo
import com.m4isper.contactsapp.domain.model.contact.ContactAccount
import com.m4isper.contactsapp.domain.model.contact.ContactDetails
import com.m4isper.contactsapp.domain.model.contact.ContactEvent
import com.m4isper.contactsapp.domain.model.contact.ContactForm
import com.m4isper.contactsapp.domain.model.contact.ContactGroup
import com.m4isper.contactsapp.domain.model.contact.ContactLink
import com.m4isper.contactsapp.domain.model.contact.ContactRelation
import com.m4isper.contactsapp.domain.model.contact.EditableEvent
import com.m4isper.contactsapp.domain.model.contact.EditableName
import com.m4isper.contactsapp.domain.model.contact.EditableNickname
import com.m4isper.contactsapp.domain.model.contact.EditablePhone
import com.m4isper.contactsapp.domain.model.contact.EditableRelation
import com.m4isper.contactsapp.domain.model.contact.InstantMessage
import com.m4isper.contactsapp.domain.model.contact.PhoneKind
import com.m4isper.contactsapp.domain.model.contact.PhoneNumber
import com.m4isper.contactsapp.domain.model.contact.RawContactRef
import com.m4isper.contactsapp.domain.model.settings.AppSettings
import com.m4isper.contactsapp.domain.model.settings.ContactSortOrder
import com.m4isper.contactsapp.domain.model.settings.ListDensity
import com.m4isper.contactsapp.domain.model.settings.ThemeMode
import com.m4isper.contactsapp.domain.repository.ContactsAccounts
import com.m4isper.contactsapp.domain.repository.ContactsChangeSource
import com.m4isper.contactsapp.domain.repository.ContactsMerger
import com.m4isper.contactsapp.domain.repository.ContactsReader
import com.m4isper.contactsapp.domain.repository.ContactsWriter
import com.m4isper.contactsapp.domain.repository.FavoritesRepository
import com.m4isper.contactsapp.domain.repository.GroupsRepository
import com.m4isper.contactsapp.domain.repository.settings.AccountSettings
import com.m4isper.contactsapp.domain.repository.settings.AppearanceSettings
import com.m4isper.contactsapp.domain.repository.settings.LanguageSettings
import com.m4isper.contactsapp.domain.repository.settings.ListSettings
import com.m4isper.contactsapp.domain.repository.settings.PrivacySettings
import com.m4isper.contactsapp.domain.repository.settings.ReminderSettings
import com.m4isper.contactsapp.domain.repository.settings.SettingsMaintenance
import com.m4isper.contactsapp.domain.repository.settings.SettingsReader
import com.m4isper.contactsapp.domain.vcard.VCardCodec
import com.m4isper.contactsapp.domain.vcard.VCardContact
import com.m4isper.contactsapp.domain.vcard.VCardExportContact
import com.m4isper.contactsapp.domain.vcard.VCardParseResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

internal fun testDetails(
    id: Long,
    name: String,
    vararg phones: String,
    instantMessages: List<InstantMessage> = emptyList(),
    nickname: String? = null,
    relations: List<ContactRelation> = emptyList(),
    events: List<ContactEvent> = emptyList(),
) = ContactDetails(
    contactId = id,
    name = name,
    phones = phones.map { PhoneNumber(it, PhoneKind.MOBILE) },
    instantMessages = instantMessages,
    nickname = nickname,
    relations = relations,
    events = events,
)

internal class FakeAndroidRepository(
    var contacts: List<ContactDetails> = emptyList(),
    var failure: Throwable? = null,
    initialFavorites: Set<Long> = emptySet(),
) : ContactsReader,
    ContactsAccounts,
    ContactsWriter,
    ContactsMerger,
    FavoritesRepository {
    // Список экрана грузится постранично: фейк режет contacts тем же способом,
    // что и реальный провайдер, иначе все ViewModel-тесты видят Loadable.Empty.
    override suspend fun getContactsPage(
        offset: Int,
        limit: Int,
    ): List<ContactDetails> {
        failure?.let { throw it }
        return contacts.drop(offset).take(limit)
    }

    override suspend fun loadContactPhoto(contactId: Long): ByteArray? = null

    override suspend fun getProfileContactId(): Long? = null

    override suspend fun unmergeContacts(link: ContactLink) = Unit

    private val favoritesFlow = MutableStateFlow(initialFavorites)
    val created = mutableListOf<ContactForm>()
    val createdAccounts = mutableListOf<ContactAccount>()
    var accounts: List<ContactAccount> = listOf(ContactAccount.Local)
    val updated = mutableListOf<Pair<Long, ContactForm>>()
    val deleted = mutableListOf<Long>()
    val merged = mutableListOf<Pair<Long, List<Long>>>()

    override suspend fun getContacts(): List<ContactDetails> {
        failure?.let { throw it }
        return contacts
    }

    override suspend fun getContactForm(contactId: Long): ContactForm? {
        failure?.let { throw it }
        val details = contacts.firstOrNull { it.contactId == contactId } ?: return null
        return ContactForm(
            name = EditableName(display = details.name),
            phones = details.phones.map { EditablePhone(number = it.number, kind = it.kind) },
            nickname = EditableNickname(nickname = details.nickname.orEmpty()),
            relations =
                details.relations.map { relation ->
                    EditableRelation(
                        name = relation.name,
                        kind = relation.kind,
                        customLabel = relation.customLabel,
                    )
                },
            events =
                details.events.map { event ->
                    EditableEvent(date = event.date, kind = event.kind, customLabel = event.customLabel)
                },
        )
    }

    override suspend fun getContactDetails(contactId: Long): ContactDetails? {
        failure?.let { throw it }
        return contacts.firstOrNull { it.contactId == contactId }
    }

    override suspend fun getContactAccount(contactId: Long): ContactAccount =
        contacts.firstOrNull { it.contactId == contactId }?.account ?: ContactAccount.Local

    override suspend fun createContact(
        form: ContactForm,
        account: ContactAccount,
    ): Long {
        failure?.let { throw it }
        created += form
        createdAccounts += account
        return created.size.toLong()
    }

    override suspend fun getContactAccounts(): List<ContactAccount> = accounts

    override suspend fun getAccountSyncInfos(): List<AccountSyncInfo> = emptyList()

    override suspend fun updateContact(
        contactId: Long,
        form: ContactForm,
    ) {
        failure?.let { throw it }
        updated += contactId to form
    }

    override suspend fun setContactPhoto(
        contactId: Long,
        photo: ByteArray,
    ) = Unit

    override suspend fun deleteContact(contactId: Long) {
        failure?.let { throw it }
        deleted += contactId
    }

    override suspend fun mergeContacts(
        targetId: Long,
        sourceIds: List<Long>,
    ): com.m4isper.contactsapp.domain.model.contact.ContactLink? {
        failure?.let { throw it }
        merged += targetId to sourceIds
        contacts = contacts.filterNot { it.contactId in sourceIds }
        return com.m4isper.contactsapp.domain.model.contact.ContactLink(
            target =
                com.m4isper.contactsapp.domain.model.contact
                    .RawContactRef(targetId),
            sources =
                sourceIds.map {
                    com.m4isper.contactsapp.domain.model.contact
                        .RawContactRef(it)
                },
        )
    }

    override fun observeFavorites(): Flow<Set<Long>> = favoritesFlow.asStateFlow()

    override suspend fun toggleFavorite(contactId: Long) {
        val current = favoritesFlow.value
        favoritesFlow.value =
            if (contactId in current) {
                current - contactId
            } else {
                current + contactId
            }
    }

    override suspend fun removeFavorite(contactId: Long) {
        favoritesFlow.value = favoritesFlow.value - contactId
    }
}

/** Фейк источника изменений провайдера: тест сам решает, когда «пришло» событие. */
internal class FakeContactsChangeSource : ContactsChangeSource {
    private val changes = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    override fun observeChanges(): Flow<Unit> = changes

    fun notifyChange() {
        changes.tryEmit(Unit)
    }
}

/** Фейк репозитория групп: in-memory состояние и запись вызовов для проверок. */
internal class FakeGroupsRepository(
    var groups: List<ContactGroup> = emptyList(),
    var membership: Map<Long, Set<Long>> = emptyMap(),
    var failure: Throwable? = null,
) : GroupsRepository {
    val addedToGroup = mutableListOf<Pair<Long, List<Long>>>()
    val removed = mutableListOf<Pair<Long, List<Long>>>()
    val merged = mutableListOf<Pair<Long, Long>>()
    val createdGroupAccounts = mutableListOf<ContactAccount>()

    override suspend fun getGroups(): List<ContactGroup> {
        failure?.let { throw it }
        return groups
    }

    override suspend fun getMembership(): Map<Long, Set<Long>> {
        failure?.let { throw it }
        return membership
    }

    override suspend fun getContactGroups(contactId: Long): List<ContactGroup> {
        failure?.let { throw it }
        return groups.filter { it.id in membership[contactId].orEmpty() }
    }

    override suspend fun createGroup(title: String): ContactGroup = createGroup(title, ContactAccount.Local)

    override suspend fun createGroup(
        title: String,
        account: ContactAccount,
    ): ContactGroup {
        failure?.let { throw it }
        createdGroupAccounts += account
        val group =
            ContactGroup(
                id = (groups.maxOfOrNull { it.id } ?: 0L) + 1,
                title = title,
                accountName = account.name,
                accountType = account.type,
            )
        groups = groups + group
        return group
    }

    override suspend fun renameGroup(
        groupId: Long,
        title: String,
    ) {
        failure?.let { throw it }
        groups = groups.map { if (it.id == groupId) it.copy(title = title) else it }
    }

    override suspend fun deleteGroup(groupId: Long) {
        failure?.let { throw it }
        groups = groups.filterNot { it.id == groupId }
    }

    override suspend fun removeContactsFromGroup(
        groupId: Long,
        contactIds: List<Long>,
    ) {
        removed += groupId to contactIds
        membership = membership.mapValues { (_, ids) -> ids - groupId }
    }

    override suspend fun mergeGroups(
        sourceGroupId: Long,
        targetGroupId: Long,
    ) {
        merged += sourceGroupId to targetGroupId
    }

    override suspend fun addContactsToGroup(
        groupId: Long,
        contactIds: List<Long>,
    ) {
        failure?.let { throw it }
        addedToGroup += groupId to contactIds
        val updated = membership.toMutableMap()
        contactIds.forEach { contactId ->
            updated[contactId] = updated[contactId].orEmpty() + groupId
        }
        membership = updated
    }
}

/**
 * Фейк порта vCard: UI-тесты не зависят от формата .vcf — разбор отдаёт заранее
 * заданные формы, сборка возвращает заданный текст, вызовы записываются для проверок.
 */
internal class FakeVCardCodec(
    var forms: List<ContactForm> = emptyList(),
    var encoded: String = "VCARD",
) : VCardCodec {
    val parsedTexts = mutableListOf<String>()
    val encodedContacts = mutableListOf<List<VCardExportContact>>()

    override suspend fun parse(text: String): VCardParseResult {
        parsedTexts += text
        return VCardParseResult(contacts = forms.map { VCardContact(form = it) })
    }

    override suspend fun encode(contacts: List<VCardExportContact>): String {
        encodedContacts += contacts
        return encoded
    }
}

/** Фейк репозитория настроек: in-memory состояние и запись вызовов для проверок. */
internal class FakeSettingsRepository(
    initial: AppSettings = AppSettings(),
) : SettingsReader,
    AppearanceSettings,
    ListSettings,
    AccountSettings,
    LanguageSettings,
    PrivacySettings,
    ReminderSettings,
    SettingsMaintenance {
    private val state = MutableStateFlow(initial)

    /** Текущие настройки; тесты могут менять их отсюда, чтобы проверить реакцию UI. */
    val settings: MutableStateFlow<AppSettings> = state

    val themeModes = mutableListOf<ThemeMode>()
    val sortOrders = mutableListOf<ContactSortOrder>()
    val densities = mutableListOf<ListDensity>()
    val favoritesFirstValues = mutableListOf<Boolean>()
    val lastContactAccounts = mutableListOf<ContactAccount>()
    val defaultContactAccounts = mutableListOf<ContactAccount>()
    val languages = mutableListOf<String>()
    val dynamicColorValues = mutableListOf<Boolean>()
    val showPhotosValues = mutableListOf<Boolean>()
    val appLocks = mutableListOf<Pair<Boolean, String>>()
    val secureWindowValues = mutableListOf<Boolean>()
    var resetCalled = false

    override fun observeSettings(): Flow<AppSettings> = state

    override suspend fun setThemeMode(mode: ThemeMode) {
        themeModes += mode
        state.value = state.value.copy(themeMode = mode)
    }

    override suspend fun setSortOrder(order: ContactSortOrder) {
        sortOrders += order
        state.value = state.value.copy(sortOrder = order)
    }

    override suspend fun setListDensity(density: ListDensity) {
        densities += density
        state.value = state.value.copy(listDensity = density)
    }

    override suspend fun setFavoritesFirst(enabled: Boolean) {
        favoritesFirstValues += enabled
        state.value = state.value.copy(favoritesFirst = enabled)
    }

    override suspend fun setLastContactAccount(account: ContactAccount) {
        lastContactAccounts += account
        state.value = state.value.copy(lastContactAccount = account)
    }

    override suspend fun setDefaultContactAccount(account: ContactAccount) {
        defaultContactAccounts += account
        state.value = state.value.copy(defaultContactAccount = account)
    }

    override suspend fun setAppLanguage(language: String) {
        languages += language
        state.value = state.value.copy(appLanguage = language)
    }

    override suspend fun setDynamicColor(enabled: Boolean) {
        dynamicColorValues += enabled
        state.value = state.value.copy(dynamicColor = enabled)
    }

    override suspend fun setShowPhotos(enabled: Boolean) {
        showPhotosValues += enabled
        state.value = state.value.copy(showPhotos = enabled)
    }

    override suspend fun setAppLock(
        enabled: Boolean,
        pinHash: String,
    ) {
        appLocks += enabled to pinHash
        state.value = state.value.copy(appLockEnabled = enabled, appLockPinHash = pinHash)
    }

    override suspend fun setSecureWindow(enabled: Boolean) {
        secureWindowValues += enabled
        state.value = state.value.copy(secureWindow = enabled)
    }

    override suspend fun setReminders(
        enabled: Boolean,
        leadDays: Int,
    ) = Unit

    override suspend fun resetSettings() {
        resetCalled = true
        state.value = AppSettings()
    }
}
