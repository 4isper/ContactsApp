package com.m4isper.contactsapp.presentation.common

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.m4isper.contactsapp.R

private const val INTENTS_TAG = "ContactIntents"

internal fun openSms(
    context: Context,
    phone: String,
) {
    runCatching {
        val smsIntent =
            Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.fromParts("sms", phone, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        context.startActivity(smsIntent)
    }.onFailure { e ->
        logw(INTENTS_TAG, "openSms failed", e)
        Toast.makeText(context, context.getString(R.string.sms_failed), Toast.LENGTH_SHORT).show()
    }
}

internal fun openEmail(
    context: Context,
    address: String,
) {
    runCatching {
        val emailIntent =
            Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.fromParts("mailto", address, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        context.startActivity(emailIntent)
    }.onFailure { e ->
        logw(INTENTS_TAG, "openEmail failed", e)
        Toast.makeText(context, context.getString(R.string.email_failed), Toast.LENGTH_SHORT).show()
    }
}

internal fun openMap(
    context: Context,
    address: String,
) {
    runCatching {
        val mapIntent =
            Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=${Uri.encode(address)}")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        context.startActivity(mapIntent)
    }.onFailure { e ->
        logw(INTENTS_TAG, "openMap failed", e)
        Toast.makeText(context, context.getString(R.string.map_failed), Toast.LENGTH_SHORT).show()
    }
}

internal fun openWebsite(
    context: Context,
    url: String,
) {
    runCatching {
        val normalized = if (url.contains("://")) url else "https://$url"
        val webIntent =
            Intent(Intent.ACTION_VIEW, Uri.parse(normalized)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        context.startActivity(webIntent)
    }.onFailure { e ->
        logw(INTENTS_TAG, "openWebsite failed", e)
        Toast.makeText(context, context.getString(R.string.website_failed), Toast.LENGTH_SHORT).show()
    }
}

internal fun copyToClipboard(
    context: Context,
    text: String,
) {
    val clipboard = context.getSystemService(ClipboardManager::class.java)
    clipboard?.setPrimaryClip(ClipData.newPlainText("contact", text))
    Toast
        .makeText(
            context,
            context.getString(R.string.copied_to_clipboard),
            Toast.LENGTH_SHORT,
        ).show()
}
