package com.m4isper.contactsapp.domain.model

import com.m4isper.contactsapp.domain.model.contact.AddressKind
import com.m4isper.contactsapp.domain.model.contact.ContactDetails
import com.m4isper.contactsapp.domain.model.contact.EmailAddress
import com.m4isper.contactsapp.domain.model.contact.EmailKind
import com.m4isper.contactsapp.domain.model.contact.PhoneKind
import com.m4isper.contactsapp.domain.model.contact.PhoneNumber
import com.m4isper.contactsapp.domain.model.contact.PostalAddress
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ContactDetailsTest {
    @Test
    fun `primary phone prefers an explicit flag`() {
        val details =
            ContactDetails(
                contactId = 1,
                name = "Anna",
                phones =
                    listOf(
                        PhoneNumber("111", PhoneKind.MOBILE),
                        PhoneNumber("222", PhoneKind.WORK, isPrimary = true),
                    ),
            )

        assertEquals("222", details.primaryPhone?.number)
    }

    @Test
    fun `primary phone falls back to the first item`() {
        val details =
            ContactDetails(
                contactId = 1,
                name = "Anna",
                phones = listOf(PhoneNumber("111", PhoneKind.MOBILE)),
            )

        assertEquals("111", details.primaryPhone?.number)
    }

    @Test
    fun `primary email prefers an explicit flag`() {
        val details =
            ContactDetails(
                contactId = 1,
                name = "Anna",
                emails =
                    listOf(
                        EmailAddress("home@example.com", EmailKind.HOME),
                        EmailAddress("work@example.com", EmailKind.WORK, isPrimary = true),
                    ),
            )

        assertEquals("work@example.com", details.primaryEmail?.address)
    }

    @Test
    fun `structured address display includes all non-blank parts`() {
        val address =
            PostalAddress(
                address = "legacy",
                kind = AddressKind.HOME,
                poBox = "PO 5",
                street = "Lenina 1",
                neighborhood = "Center",
                city = "Moscow",
                region = "MO",
                postcode = "101000",
                country = "Russia",
            )

        assertEquals("PO 5, Lenina 1, Center, Moscow, MO, 101000, Russia", address.displayAddress)
    }

    @Test
    fun `legacy address remains the display fallback`() {
        val address = PostalAddress(address = "Moscow, Lenina 1", kind = AddressKind.HOME)

        assertEquals("Moscow, Lenina 1", address.displayAddress)
        assertEquals("Moscow, Lenina 1", address.streetLine)
    }

    @Test
    fun `po box only address still has a display value`() {
        val address = PostalAddress(address = "", kind = AddressKind.OTHER, poBox = "PO 5")

        assertEquals("PO 5", address.displayAddress)
    }
}
