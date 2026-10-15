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

internal fun copyNumber(
    context: Context,
    phone: String,
) {
    val clipboard = context.getSystemService(ClipboardManager::class.java)
    clipboard?.setPrimaryClip(ClipData.newPlainText("phone", phone))
    Toast
        .makeText(
            context,
            context.getString(R.string.copied_to_clipboard),
            Toast.LENGTH_SHORT,
        ).show()
}
