package com.m4isper.contactsapp.domain.model

import com.m4isper.contactsapp.domain.model.contact.ContactAccount
import com.m4isper.contactsapp.domain.model.contact.ContactGroup
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ContactGroupTest {
    private val google = ContactAccount(name = "user@gmail.com", type = "com.google")

    @Test
    fun `local group belongs to local account`() {
        val group = ContactGroup(id = 1, title = "Семья")

        assertTrue(group.belongsTo(ContactAccount.Local))
        assertFalse(group.belongsTo(google))
    }

    @Test
    fun `synced group belongs only to its account`() {
        val group =
            ContactGroup(
                id = 2,
                title = "Работа",
                accountName = google.name,
                accountType = google.type,
            )

        assertTrue(group.belongsTo(google))
        assertFalse(group.belongsTo(ContactAccount.Local))
        assertFalse(group.belongsTo(ContactAccount(name = "other@gmail.com", type = "com.google")))
    }
}
