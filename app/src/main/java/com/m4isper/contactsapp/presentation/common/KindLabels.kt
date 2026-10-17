package com.m4isper.contactsapp.presentation.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.m4isper.contactsapp.R
import com.m4isper.contactsapp.domain.model.AddressKind
import com.m4isper.contactsapp.domain.model.EmailKind
import com.m4isper.contactsapp.domain.model.ImProtocol
import com.m4isper.contactsapp.domain.model.InstantMessage
import com.m4isper.contactsapp.domain.model.PhoneKind

@Composable
internal fun phoneKindLabel(kind: PhoneKind): String =
    stringResource(
        when (kind) {
            PhoneKind.MOBILE -> R.string.phone_type_mobile
            PhoneKind.HOME -> R.string.phone_type_home
            PhoneKind.WORK -> R.string.phone_type_work
            PhoneKind.OTHER -> R.string.phone_type_other
        },
    )

@Composable
internal fun emailKindLabel(kind: EmailKind): String =
    stringResource(
        when (kind) {
            EmailKind.HOME -> R.string.email_type_home
            EmailKind.WORK -> R.string.email_type_work
            EmailKind.OTHER -> R.string.email_type_other
        },
    )

@Composable
internal fun addressKindLabel(kind: AddressKind): String =
    stringResource(
        when (kind) {
            AddressKind.HOME -> R.string.address_type_home
            AddressKind.WORK -> R.string.address_type_work
            AddressKind.OTHER -> R.string.address_type_other
        },
    )

@Composable
internal fun imProtocolLabel(protocol: ImProtocol): String =
    stringResource(
        when (protocol) {
            ImProtocol.AIM -> R.string.im_protocol_aim
            ImProtocol.MSN -> R.string.im_protocol_msn
            ImProtocol.YAHOO -> R.string.im_protocol_yahoo
            ImProtocol.SKYPE -> R.string.im_protocol_skype
            ImProtocol.QQ -> R.string.im_protocol_qq
            ImProtocol.GOOGLE_TALK -> R.string.im_protocol_google_talk
            ImProtocol.ICQ -> R.string.im_protocol_icq
            ImProtocol.JABBER -> R.string.im_protocol_jabber
            ImProtocol.CUSTOM -> R.string.im_protocol_other
        },
    )

/** Подпись мессенджера: произвольный сервис, если он задан, иначе имя протокола. */
@Composable
internal fun instantMessageLabel(im: InstantMessage): String =
    im.customProtocol
        ?.takeIf { im.protocol == ImProtocol.CUSTOM && it.isNotBlank() }
        ?: imProtocolLabel(im.protocol)
