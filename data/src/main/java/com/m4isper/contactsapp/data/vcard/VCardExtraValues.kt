package com.m4isper.contactsapp.data.vcard

import com.m4isper.contactsapp.domain.model.AddressKind
import com.m4isper.contactsapp.domain.model.EmailKind
import com.m4isper.contactsapp.domain.model.EventKind
import com.m4isper.contactsapp.domain.model.PhoneKind
import com.m4isper.contactsapp.domain.model.RelationKind
import java.util.Locale

/** TYPE связи: отдельного стандарта нет, используем имена RelationKind. */
internal fun vcardRelationType(kind: RelationKind): String = kind.name

/** Вид связи: неизвестный или произвольный токен трактуем как OTHER. */
internal fun relationKindOfVCard(types: Set<String>): RelationKind =
    types
        .map { it.trim().replace(' ', '_').uppercase(Locale.ROOT) }
        .firstNotNullOfOrNull { token -> RelationKind.entries.firstOrNull { it.name == token } }
        ?: RelationKind.OTHER

/** Произвольная метка из параметра X-ABLabel (формат Apple/Android). */
internal fun vcardLabelOf(params: Map<String, String>): String? =
    params["X-ABLABEL"]?.trim()?.takeIf { it.isNotEmpty() }

/** Подпись "Anniversary" — это вид события, а не произвольная подпись. */
internal fun eventKindOfVCardLabel(label: String): EventKind? =
    if (label.trim().lowercase(Locale.ROOT) == "anniversary") EventKind.ANNIVERSARY else null

/** Подпись X-ABLabel может совпадать со стандартным типом: тогда это тип, а не CUSTOM. */
internal fun phoneKindOfVCardLabel(label: String): PhoneKind? =
    when (label.trim().lowercase(Locale.ROOT)) {
        "mobile", "cell" -> PhoneKind.MOBILE
        "home" -> PhoneKind.HOME
        "work" -> PhoneKind.WORK
        else -> null
    }

internal fun emailKindOfVCardLabel(label: String): EmailKind? =
    when (label.trim().lowercase(Locale.ROOT)) {
        "home" -> EmailKind.HOME
        "work" -> EmailKind.WORK
        else -> null
    }

internal fun addressKindOfVCardLabel(label: String): AddressKind? =
    when (label.trim().lowercase(Locale.ROOT)) {
        "home" -> AddressKind.HOME
        "work" -> AddressKind.WORK
        else -> null
    }

private const val ANDROID_LABEL_PREFIX = "_\$!<"
private const val ANDROID_LABEL_SUFFIX = ">!\$_"

/** Подпись группы itemN.X-ABLabel: Android оборачивает стандартные имена в _$!<...>!$_. */
internal fun cleanVCardLabel(raw: String): String {
    val trimmed = raw.trim()
    return if (trimmed.startsWith(ANDROID_LABEL_PREFIX) && trimmed.endsWith(ANDROID_LABEL_SUFFIX)) {
        trimmed.substring(ANDROID_LABEL_PREFIX.length, trimmed.length - ANDROID_LABEL_SUFFIX.length)
    } else {
        trimmed
    }
}
