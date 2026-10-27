package com.m4isper.contactsapp.domain.usecase

import com.m4isper.contactsapp.domain.model.contact.ContactDetails
import com.m4isper.contactsapp.domain.model.contact.ContactForm
import com.m4isper.contactsapp.domain.model.contact.ContactGroup
import com.m4isper.contactsapp.domain.repository.ContactsReader
import com.m4isper.contactsapp.domain.repository.GroupsRepository
import com.m4isper.contactsapp.domain.usecase.contacts.ExportContactsUseCase
import com.m4isper.contactsapp.domain.usecase.contacts.GetContactDetailsUseCase
import com.m4isper.contactsapp.domain.usecase.contacts.LoadContactPhotoUseCase
import com.m4isper.contactsapp.domain.usecase.groups.GetGroupMembershipUseCase
import com.m4isper.contactsapp.domain.usecase.groups.GetGroupsUseCase
import com.m4isper.contactsapp.domain.vcard.VCardCodec
import com.m4isper.contactsapp.domain.vcard.VCardExportContact
import com.m4isper.contactsapp.domain.vcard.VCardParseResult
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

private class ExportContactsRepository(
    private val details: Map<Long, ContactDetails>,
    private val photos: Map<Long, ByteArray> = emptyMap(),
) : ContactsReader {
    override suspend fun getContacts(): List<ContactDetails> = details.values.toList()

    override suspend fun getContactsPage(
        offset: Int,
        limit: Int,
    ): List<ContactDetails> = emptyList()

    override suspend fun getProfileContactId(): Long? = null

    override suspend fun getContactDetails(contactId: Long): ContactDetails? = details[contactId]

    override suspend fun getContactForm(contactId: Long): ContactForm? = null

    override suspend fun loadContactPhoto(contactId: Long): ByteArray? = photos[contactId]
}

private class ExportGroupsRepository(
    private val groups: List<ContactGroup>,
    private val membership: Map<Long, Set<Long>>,
) : GroupsRepository {
    override suspend fun getGroups(): List<ContactGroup> = groups

    override suspend fun getMembership(): Map<Long, Set<Long>> = membership

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
    ) = Unit
}

private class RecordingCodec : VCardCodec {
    val encoded = mutableListOf<List<VCardExportContact>>()

    override suspend fun parse(text: String): VCardParseResult = VCardParseResult()

    override suspend fun encode(contacts: List<VCardExportContact>): String {
        encoded += contacts
        return "vcf"
    }
}

class ExportContactsUseCaseTest {
    @Test
    fun `export includes details photo and group titles`() =
        runTest {
            val codec = RecordingCodec()
            val repository =
                ExportContactsRepository(
                    details =
                        mapOf(
                            7L to ContactDetails(contactId = 7, name = "Anna"),
                            8L to ContactDetails(contactId = 8, name = "Bob"),
                        ),
                    photos = mapOf(7L to byteArrayOf(1, 2, 3)),
                )
            val groups =
                ExportGroupsRepository(
                    groups = listOf(ContactGroup(id = 1, title = "Friends")),
                    membership = mapOf(7L to setOf(1L)),
                )
            val useCase =
                ExportContactsUseCase(
                    getContactDetails = GetContactDetailsUseCase(repository),
                    loadContactPhoto = LoadContactPhotoUseCase(repository),
                    getGroups = GetGroupsUseCase(groups),
                    getGroupMembership = GetGroupMembershipUseCase(groups),
                    codec = codec,
                )

            useCase(listOf(7L, 8L))

            val exported = codec.encoded.single()
            assertEquals(listOf(7L, 8L), exported.map { it.details.contactId })
            assertEquals(listOf("Friends"), exported.first().groups)
            assertEquals(listOf<Byte>(1, 2, 3), exported.first().photo?.toList())
            assertNull(exported.last().photo)
        }

    @Test
    fun `export skips contacts without details`() =
        runTest {
            val codec = RecordingCodec()
            val repository =
                ExportContactsRepository(
                    details = mapOf(7L to ContactDetails(contactId = 7, name = "Anna")),
                )
            val groups = ExportGroupsRepository(groups = emptyList(), membership = emptyMap())
            val useCase =
                ExportContactsUseCase(
                    getContactDetails = GetContactDetailsUseCase(repository),
                    loadContactPhoto = LoadContactPhotoUseCase(repository),
                    getGroups = GetGroupsUseCase(groups),
                    getGroupMembership = GetGroupMembershipUseCase(groups),
                    codec = codec,
                )

            useCase(listOf(7L, 8L))

            assertEquals(listOf(7L), codec.encoded.single().map { it.details.contactId })
        }
}
