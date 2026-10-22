package com.m4isper.contactsapp.data.vcard

import com.m4isper.contactsapp.domain.vcard.VCardCodec
import com.m4isper.contactsapp.domain.vcard.VCardExportContact
import com.m4isper.contactsapp.domain.vcard.VCardParseResult
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Разбор и сборка .vcf уходят с вызывающего потока на [Dispatchers.Default]. */
@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class VCardCodecImpl : VCardCodec {
    override suspend fun parse(text: String): VCardParseResult = withContext(Dispatchers.Default) { parseVCards(text) }

    override suspend fun encode(contacts: List<VCardExportContact>): String =
        withContext(Dispatchers.Default) { buildVCards(contacts) }
}
