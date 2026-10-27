package com.m4isper.contactsapp.data.vcard

import com.m4isper.contactsapp.domain.model.contact.AddressKind
import com.m4isper.contactsapp.domain.model.contact.EmailKind
import com.m4isper.contactsapp.domain.model.contact.EventKind
import com.m4isper.contactsapp.domain.model.contact.PhoneKind
import com.m4isper.contactsapp.domain.model.contact.RelationKind
import com.m4isper.contactsapp.domain.model.contact.SipKind
import com.m4isper.contactsapp.domain.model.contact.WebsiteKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VCardExtraValuesFullTest {
    @Test
    fun vcardRelationType_returnsName() {
        assertEquals("MOTHER", vcardRelationType(RelationKind.MOTHER))
    }

    @Test
    fun relationKindOfVCard_mapsKnownAndUnknown() {
        assertEquals(RelationKind.MOTHER, relationKindOfVCard(setOf("mother")))
        assertEquals(RelationKind.OTHER, relationKindOfVCard(setOf("unknown")))
    }

    @Test
    fun vcardLabelOf_returnsTrimmedOrNull() {
        assertEquals("label", vcardLabelOf(mapOf("X-ABLABEL" to "  label  ")))
        assertNull(vcardLabelOf(mapOf("X-ABLABEL" to "  ")))
        assertNull(vcardLabelOf(emptyMap()))
    }

    @Test
    fun eventKindOfVCardLabel_mapsAnniversary() {
        assertEquals(EventKind.ANNIVERSARY, eventKindOfVCardLabel("Anniversary"))
        assertNull(eventKindOfVCardLabel("other"))
    }

    @Test
    fun phoneKindOfVCardLabel_mapsKnown() {
        assertEquals(PhoneKind.MOBILE, phoneKindOfVCardLabel("mobile"))
        assertEquals(PhoneKind.WORK_MOBILE, phoneKindOfVCardLabel("work cell"))
        assertNull(phoneKindOfVCardLabel("unknown"))
    }

    @Test
    fun emailKindOfVCardLabel_mapsKnown() {
        assertEquals(EmailKind.HOME, emailKindOfVCardLabel("home"))
        assertNull(emailKindOfVCardLabel("unknown"))
    }

    @Test
    fun addressKindOfVCardLabel_mapsKnown() {
        assertEquals(AddressKind.HOME, addressKindOfVCardLabel("home"))
        assertNull(addressKindOfVCardLabel("unknown"))
    }

    @Test
    fun websiteKindOfVCardLabel_mapsKnown() {
        assertEquals(WebsiteKind.HOMEPAGE, websiteKindOfVCardLabel("homepage"))
        assertNull(websiteKindOfVCardLabel("unknown"))
    }

    @Test
    fun sipKindOfVCardLabel_mapsKnown() {
        assertEquals(SipKind.HOME, sipKindOfVCardLabel("home"))
        assertNull(sipKindOfVCardLabel("unknown"))
    }

    @Test
    fun cleanVCardLabel_stripsAndroidWrapper() {
        assertEquals("Home", cleanVCardLabel("_\$!<Home>!\$_"))
        assertEquals("Home", cleanVCardLabel("  Home  "))
    }
}
