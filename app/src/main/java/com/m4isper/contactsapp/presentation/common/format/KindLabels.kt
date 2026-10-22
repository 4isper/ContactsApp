package com.m4isper.contactsapp.presentation.common.format

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.m4isper.contactsapp.R
import com.m4isper.contactsapp.domain.model.AddressKind
import com.m4isper.contactsapp.domain.model.ContactEvent
import com.m4isper.contactsapp.domain.model.ContactRelation
import com.m4isper.contactsapp.domain.model.EmailKind
import com.m4isper.contactsapp.domain.model.EventKind
import com.m4isper.contactsapp.domain.model.ImProtocol
import com.m4isper.contactsapp.domain.model.InstantMessage
import com.m4isper.contactsapp.domain.model.PhoneKind
import com.m4isper.contactsapp.domain.model.RelationKind
import com.m4isper.contactsapp.domain.model.SipKind
import com.m4isper.contactsapp.domain.model.WebsiteKind

@Composable
internal fun phoneKindLabel(kind: PhoneKind): String =
    stringResource(
        when (kind) {
            PhoneKind.MOBILE -> R.string.phone_type_mobile
            PhoneKind.HOME -> R.string.phone_type_home
            PhoneKind.WORK -> R.string.phone_type_work
            PhoneKind.WORK_MOBILE -> R.string.phone_type_work_mobile
            PhoneKind.FAX -> R.string.phone_type_fax
            PhoneKind.PAGER -> R.string.phone_type_pager
            PhoneKind.CALLBACK -> R.string.phone_type_callback
            PhoneKind.COMPANY_MAIN -> R.string.phone_type_company_main
            PhoneKind.OTHER -> R.string.phone_type_other
        },
    )

@Composable
internal fun emailKindLabel(kind: EmailKind): String =
    stringResource(
        when (kind) {
            EmailKind.HOME -> R.string.email_type_home
            EmailKind.WORK -> R.string.email_type_work
            EmailKind.MOBILE -> R.string.email_type_mobile
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
internal fun websiteKindLabel(kind: WebsiteKind): String =
    stringResource(
        when (kind) {
            WebsiteKind.HOMEPAGE -> R.string.website_type_homepage
            WebsiteKind.HOME -> R.string.website_type_home
            WebsiteKind.WORK -> R.string.website_type_work
            WebsiteKind.BLOG -> R.string.website_type_blog
            WebsiteKind.PROFILE -> R.string.website_type_profile
            WebsiteKind.FTP -> R.string.website_type_ftp
            WebsiteKind.OTHER -> R.string.website_type_other
        },
    )

@Composable
internal fun sipKindLabel(kind: SipKind): String =
    stringResource(
        when (kind) {
            SipKind.HOME -> R.string.sip_type_home
            SipKind.WORK -> R.string.sip_type_work
            SipKind.OTHER -> R.string.sip_type_other
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

/**
 * Метки связей лежат в карте, а не в when: ветвей больше лимита
 * CyclomaticComplexMethod.
 */
private val RELATION_KIND_LABELS =
    mapOf(
        RelationKind.ASSISTANT to R.string.relation_type_assistant,
        RelationKind.BROTHER to R.string.relation_type_brother,
        RelationKind.CHILD to R.string.relation_type_child,
        RelationKind.DOMESTIC_PARTNER to R.string.relation_type_domestic_partner,
        RelationKind.FATHER to R.string.relation_type_father,
        RelationKind.FRIEND to R.string.relation_type_friend,
        RelationKind.MANAGER to R.string.relation_type_manager,
        RelationKind.MOTHER to R.string.relation_type_mother,
        RelationKind.PARENT to R.string.relation_type_parent,
        RelationKind.PARTNER to R.string.relation_type_partner,
        RelationKind.REFERRED_BY to R.string.relation_type_referred_by,
        RelationKind.RELATIVE to R.string.relation_type_relative,
        RelationKind.SISTER to R.string.relation_type_sister,
        RelationKind.SPOUSE to R.string.relation_type_spouse,
        RelationKind.OTHER to R.string.relation_type_other,
    )

@Composable
internal fun relationKindLabel(kind: RelationKind): String = stringResource(RELATION_KIND_LABELS.getValue(kind))

/** Подпись связи: произвольная метка, если она задана, иначе имя вида. */
@Composable
internal fun relationLabel(relation: ContactRelation): String =
    relation.customLabel
        ?.takeIf { relation.kind == RelationKind.OTHER && it.isNotBlank() }
        ?: relationKindLabel(relation.kind)

@Composable
internal fun eventKindLabel(kind: EventKind): String =
    stringResource(
        when (kind) {
            EventKind.ANNIVERSARY -> R.string.event_type_anniversary
            EventKind.OTHER -> R.string.event_type_other
        },
    )

/** Подпись события: произвольная метка, если она задана, иначе имя вида. */
@Composable
internal fun contactEventLabel(event: ContactEvent): String =
    event.customLabel
        ?.takeIf { event.kind == EventKind.OTHER && it.isNotBlank() }
        ?: eventKindLabel(event.kind)
