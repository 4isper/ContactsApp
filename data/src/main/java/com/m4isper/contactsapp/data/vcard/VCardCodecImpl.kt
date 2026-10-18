package com.m4isper.contactsapp.data.vcard

import com.m4isper.contactsapp.domain.model.ContactDetails
import com.m4isper.contactsapp.domain.model.ContactForm
import com.m4isper.contactsapp.domain.vcard.VCardCodec
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class VCardCodecImpl : VCardCodec {
    override fun parse(text: String): List<ContactForm> = parseVCards(text)

    override fun encode(contacts: List<ContactDetails>): String = buildVCards(contacts)
}
