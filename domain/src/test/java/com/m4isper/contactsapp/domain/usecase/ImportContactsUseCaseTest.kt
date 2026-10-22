package com.m4isper.contactsapp.domain.usecase

import com.m4isper.contactsapp.domain.model.ContactAccount
import com.m4isper.contactsapp.domain.model.ContactDetails
import com.m4isper.contactsapp.domain.model.ContactForm
import com.m4isper.contactsapp.domain.model.ContactGroup
import com.m4isper.contactsapp.domain.model.EditableName
import com.m4isper.contactsapp.domain.model.ImportDecision
import com.m4isper.contactsapp.domain.model.ImportEntry
import com.m4isper.contactsapp.domain.repository.ContactsMerger
import com.m4isper.contactsapp.domain.repository.ContactsReader
import com.m4isper.contactsapp.domain.repository.ContactsWriter
import com.m4isper.contactsapp.domain.repository.FavoritesRepository
import com.m4isper.contactsapp.domain.repository.GroupsRepository
import com.m4isper.contactsapp.domain.vcard.VCardContact
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private class ImportFakeRepository(
    private val failAt: Int? = null,
) : ContactsReader,
    ContactsWriter,
    ContactsMerger {
    val created = mutableListOf<ContactForm>()

    override suspend fun updateContact(
        contactId: Long,
        form: ContactForm,
    ) = Unit

    override suspend fun createContact(
        form: ContactForm,
        account: ContactAccount,
    ): Long {
        val index = attempts
        attempts += 1
        if (failAt == index) error("import failed")
        created += form
        nextId += 1
        return nextId
    }

    override suspend fun getContactsPage(
        offset: Int,
        limit: Int,
    ): List<ContactDetails> = emptyList()

    override suspend fun loadContactPhoto(contactId: Long): ByteArray? = null

    override suspend fun getProfileContactId(): Long? = null

    override suspend fun unmergeContacts(link: com.m4isper.contactsapp.domain.model.ContactLink) = Unit

    val photos = mutableListOf<Pair<Long, ByteArray>>()
    val merged = mutableListOf<Pair<Long, List<Long>>>()
    val deleted = mutableListOf<Long>()
    private var attempts = 0
    private var nextId = 100L

    override suspend fun getContacts(): List<ContactDetails> = emptyList()

    override suspend fun getContactDetails(contactId: Long): ContactDetails? = null

    override suspend fun getContactForm(contactId: Long): ContactForm? = null

    override suspend fun deleteContact(contactId: Long) {
        deleted += contactId
    }

    override suspend fun mergeContacts(
        targetId: Long,
        sourceIds: List<Long>,
    ): com.m4isper.contactsapp.domain.model.ContactLink? {
        merged += targetId to sourceIds
        return com.m4isper.contactsapp.domain.model.ContactLink(
            target =
                com.m4isper.contactsapp.domain.model
                    .RawContactRef(targetId),
            sources =
                sourceIds.map {
                    com.m4isper.contactsapp.domain.model
                        .RawContactRef(it)
                },
        )
    }

    override suspend fun setContactPhoto(
        contactId: Long,
        photo: ByteArray,
    ) {
        photos += contactId to photo
    }
}

private class ImportFakeGroupsRepository(
    private val groups: List<ContactGroup> = emptyList(),
) : GroupsRepository {
    val added = mutableListOf<Pair<Long, List<Long>>>()

    override suspend fun getGroups(): List<ContactGroup> = groups

    override suspend fun getMembership(): Map<Long, Set<Long>> = emptyMap()

    override suspend fun getContactGroups(contactId: Long): List<ContactGroup> = emptyList()

    override suspend fun createGroup(title: String): ContactGroup = error("not used")

    override suspend fun renameGroup(
        groupId: Long,
        title: String,
    ) = Unit

    override suspend fun deleteGroup(groupId: Long) = Unit

    override suspend fun removeContactsFromGroup(
        groupId: Long,
        contactIds: List<Long>,
    ) = Unit

    override suspend fun mergeGroups(
        sourceGroupId: Long,
        targetGroupId: Long,
    ) = Unit

    override suspend fun addContactsToGroup(
        groupId: Long,
        contactIds: List<Long>,
    ) {
        added += groupId to contactIds
    }
}

private class ImportFakeFavoritesRepository : FavoritesRepository {
    override fun observeFavorites(): Flow<Set<Long>> = flowOf(emptySet())

    override suspend fun toggleFavorite(contactId: Long) = Unit

    override suspend fun removeFavorite(contactId: Long) = Unit
}

private fun form(name: String) = ContactForm(name = EditableName(display = name))

private fun entry(
    name: String,
    decision: ImportDecision = ImportDecision.Create,
    photo: ByteArray? = null,
    categories: List<String> = emptyList(),
) = ImportEntry(
    contact = VCardContact(form = form(name), photo = photo, categories = categories),
    decision = decision,
)

private fun useCase(
    repository: ImportFakeRepository,
    groups: ImportFakeGroupsRepository = ImportFakeGroupsRepository(),
) = ImportContactsUseCase(
    createContact = CreateContactUseCase(repository),
    mergeContacts = MergeContactsUseCase(repository),
    setContactPhoto = SetContactPhotoUseCase(repository),
    addContactsToGroup = AddContactsToGroupUseCase(groups),
    getGroups = GetGroupsUseCase(groups),
    deleteContacts = DeleteContactsUseCase(repository, ImportFakeFavoritesRepository()),
)

class ImportContactsUseCaseTest {
    @Test
    fun `creates every parsed contact`() =
        runTest {
            val repository = ImportFakeRepository()

            val outcome = useCase(repository)(listOf(entry("Anna"), entry("Bob")))

            assertEquals(2, outcome.created)
            assertEquals(0, outcome.merged)
            assertEquals(listOf("Anna", "Bob"), repository.created.map { it.name.display })
        }

    @Test
    fun `skips failing contact and keeps importing`() =
        runTest {
            val repository = ImportFakeRepository(failAt = 1)

            val outcome =
                useCase(repository)(
                    listOf(entry("Anna"), entry("Bob"), entry("Cara")),
                )

            assertEquals(2, outcome.created)
            assertEquals(1, outcome.failures.size)
            assertEquals("Bob", outcome.failures.single().name)
            assertEquals(listOf("Anna", "Cara"), repository.created.map { it.name.display })
        }

    @Test
    fun `empty list reports zero`() =
        runTest {
            val outcome = useCase(ImportFakeRepository())(emptyList())

            assertEquals(0, outcome.created)
        }

    @Test
    fun `skips invalid form without reaching repository`() =
        runTest {
            val repository = ImportFakeRepository()
            val invalid =
                ImportEntry(
                    contact = VCardContact(form = ContactForm()),
                    decision = ImportDecision.Create,
                )

            val outcome = useCase(repository)(listOf(entry("Anna"), invalid))

            assertEquals(1, outcome.created)
            assertEquals(1, outcome.failures.size)
            assertEquals(listOf("Anna"), repository.created.map { it.name.display })
        }

    @Test
    fun `skipped entries are not created`() =
        runTest {
            val repository = ImportFakeRepository()

            val outcome = useCase(repository)(listOf(entry("Anna", ImportDecision.Skip)))

            assertEquals(0, outcome.created)
            assertEquals(1, outcome.skipped)
            assertTrue(repository.created.isEmpty())
        }

    @Test
    fun `merged entry is created and merged into the target`() =
        runTest {
            val repository = ImportFakeRepository()

            val outcome = useCase(repository)(listOf(entry("Anna", ImportDecision.Merge(targetId = 7))))

            assertEquals(0, outcome.created)
            assertEquals(1, outcome.merged)
            assertEquals(listOf(7L), repository.merged.map { it.first })
            assertEquals(listOf(listOf(101L)), repository.merged.map { it.second })
        }

    @Test
    fun `photo and categories are applied to the created contact`() =
        runTest {
            val repository = ImportFakeRepository()
            val groups = ImportFakeGroupsRepository(listOf(ContactGroup(id = 5, title = "Friends")))

            val outcome =
                useCase(repository, groups)(
                    listOf(entry("Anna", photo = byteArrayOf(1, 2, 3), categories = listOf("Friends"))),
                )

            assertEquals(1, outcome.created)
            assertEquals(1, repository.photos.size)
            assertEquals(listOf(5L to listOf(101L)), groups.added)
            assertTrue(outcome.skippedCategories.isEmpty())
        }

    @Test
    fun `category without group is reported as skipped`() =
        runTest {
            val repository = ImportFakeRepository()

            val outcome = useCase(repository)(listOf(entry("Anna", categories = listOf("Unknown"))))

            assertEquals(1, outcome.created)
            assertEquals(listOf("Unknown"), outcome.skippedCategories)
        }
}
