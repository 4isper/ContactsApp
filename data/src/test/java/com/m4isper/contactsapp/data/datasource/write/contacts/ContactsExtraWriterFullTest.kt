package com.m4isper.contactsapp.data.datasource.write.contacts

import android.provider.ContactsContract
import com.m4isper.contactsapp.domain.model.contact.EditableOrganization
import com.m4isper.contactsapp.domain.model.contact.EditableRelation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ContactsExtraWriterFullTest {
    @Test
    fun relationValues_mapsCustomAndKnownKind() {
        val custom =
            relationValues(
                EditableRelation(
                    name = "n",
                    kind = com.m4isper.contactsapp.domain.model.contact.RelationKind.OTHER,
                    customLabel = "lbl",
                ),
            )
        assertEquals("n", custom.getAsString(android.provider.ContactsContract.CommonDataKinds.Relation.NAME))
        assertEquals(
            android.provider.ContactsContract.CommonDataKinds.Relation.TYPE_CUSTOM,
            custom.getAsInteger(android.provider.ContactsContract.CommonDataKinds.Relation.TYPE),
        )

        val known =
            relationValues(
                EditableRelation(name = "n", kind = com.m4isper.contactsapp.domain.model.contact.RelationKind.MOTHER),
            )
        assertEquals(
            android.provider.ContactsContract.CommonDataKinds.Relation.TYPE_MOTHER,
            known.getAsInteger(android.provider.ContactsContract.CommonDataKinds.Relation.TYPE),
        )
    }

    @Test
    fun nicknameValues_containsName() {
        val cv = nicknameValues("nick")
        assertEquals("nick", cv.getAsString(android.provider.ContactsContract.CommonDataKinds.Nickname.NAME))
        assertEquals(
            android.provider.ContactsContract.CommonDataKinds.Nickname.TYPE_DEFAULT,
            cv.getAsInteger(android.provider.ContactsContract.CommonDataKinds.Nickname.TYPE),
        )
    }

    @Test
    fun relationOperations_emptyWhenNoItems() {
        assertTrue(
            relationOperations(
                1L,
                com.m4isper.contactsapp.data.datasource.read
                    .ContactRows(),
                com.m4isper.contactsapp.domain.model.contact
                    .ContactForm(),
            ).isEmpty(),
        )
    }

    @Test
    fun organizationValues_isEmpty() {
        val org =
            com.m4isper.contactsapp.domain.model.contact
                .EditableOrganization()
        assertTrue(org.isEmpty)
    }
}
