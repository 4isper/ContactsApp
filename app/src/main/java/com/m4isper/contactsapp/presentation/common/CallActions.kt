package com.m4isper.contactsapp.presentation.common

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.m4isper.contactsapp.R

private const val TAG = "ContactsCall"

private fun telUri(phone: String): Uri = Uri.fromParts("tel", phone, null)

/** Возвращает true, если интент звонка запущен. */
internal fun placeCall(
    context: Context,
    phone: String,
): Boolean {
    logd(TAG) { "placeCall" }
    return runCatching {
        val callIntent =
            Intent(Intent.ACTION_CALL).apply {
                data = telUri(phone)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        context.startActivity(callIntent)
    }.onFailure { e ->
        logw(TAG, "placeCall failed", e)
        Toast.makeText(context, context.getString(R.string.call_failed), Toast.LENGTH_SHORT).show()
    }.isSuccess
}

internal fun openDialer(
    context: Context,
    phone: String,
) {
    runCatching {
        val dialIntent =
            Intent(Intent.ACTION_DIAL).apply {
                data = telUri(phone)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        context.startActivity(dialIntent)
    }.onFailure { e ->
        logw(TAG, "openDialer failed", e)
        Toast.makeText(context, context.getString(R.string.dialer_failed), Toast.LENGTH_SHORT).show()
    }
}

internal fun tryPlaceCall(
    context: Context,
    phone: String,
    onPermissionNeeded: (String) -> Unit,
) {
    val granted = hasPermission(context, Manifest.permission.CALL_PHONE)
    logd(TAG) { "tryPlaceCall: callPermissionGranted=$granted" }
    if (granted) {
        placeCall(context, phone)
    } else {
        onPermissionNeeded(phone)
    }
}

internal fun onCallPermissionResult(
    context: Context,
    isGranted: Boolean,
    phone: String?,
) {
    logd(TAG) { "onCallPermissionResult: isGranted=$isGranted" }
    if (phone == null) return
    if (isGranted) {
        placeCall(context, phone)
    } else {
        // Fallback без разрешения: открываем дайлер, чтобы звонок всё равно работал.
        Toast
            .makeText(
                context,
                context.getString(R.string.call_no_permission_dialer),
                Toast.LENGTH_SHORT,
            ).show()
        openDialer(context, phone)
    }
}
