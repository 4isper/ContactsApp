package com.m4isper.contactsapp.domain.usecase

import com.m4isper.contactsapp.domain.vcard.VCardCodec
import com.m4isper.contactsapp.domain.vcard.VCardExportContact
import dev.zacsweers.metro.Inject

/**
 * Экспорт контактов в .vcf: детали, фото и названия групп.
 * Контакты без данных (удалённые между чтением списка и сборкой) пропускаются.
 */
@Inject
class ExportContactsUseCase(
    private val getContactDetails: GetContactDetailsUseCase,
    private val loadContactPhoto: LoadContactPhotoUseCase,
    private val getGroups: GetGroupsUseCase,
    private val getGroupMembership: GetGroupMembershipUseCase,
    private val codec: VCardCodec,
) {
    suspend operator fun invoke(contactIds: List<Long>): String {
        val membership = getGroupMembership()
        val groups = getGroups().associateBy { it.id }
        val contacts =
            contactIds.mapNotNull { contactId ->
                val details = getContactDetails(contactId) ?: return@mapNotNull null
                VCardExportContact(
                    details = details,
                    photo = loadContactPhoto(contactId),
                    groups =
                        membership[contactId].orEmpty().mapNotNull { groupId ->
                            groups[groupId]?.title
                        },
                )
            }
        return codec.encode(contacts)
    }
}
