package com.m4isper.contactsapp.presentation.common

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.m4isper.contactsapp.R
import com.m4isper.contactsapp.presentation.contacts.ContactsViewModel

private const val TAG = "ContactsCall"

internal fun hasPermission(context: Context, permission: String): Boolean =
    ContextCompat.checkSelfPermission(context, permission) ==
        PackageManager.PERMISSION_GRANTED

internal fun openAppSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = "package:${context.packageName}".toUri()
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(intent)
}

private fun telUri(phone: String): Uri = Uri.fromParts("tel", phone, null)

/** Возвращает true, если интент звонка запущен. */
internal fun placeCall(context: Context, phone: String): Boolean {
    Log.d(TAG, "placeCall: phone='$phone'")
    return runCatching {
        val callIntent = Intent(Intent.ACTION_CALL).apply {
            data = telUri(phone)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(callIntent)
    }.onFailure { e ->
        Log.w(TAG, "placeCall failed", e)
        Toast.makeText(context, context.getString(R.string.call_failed), Toast.LENGTH_SHORT).show()
    }.isSuccess
}

internal fun openDialer(context: Context, phone: String) {
    runCatching {
        val dialIntent = Intent(Intent.ACTION_DIAL).apply {
            data = telUri(phone)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(dialIntent)
    }
}

internal fun openSms(context: Context, phone: String) {
    runCatching {
        val smsIntent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.fromParts("sms", phone, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(smsIntent)
    }.onFailure { e ->
        Log.w(TAG, "openSms failed", e)
        Toast.makeText(context, context.getString(R.string.sms_failed), Toast.LENGTH_SHORT).show()
    }
}

internal fun copyNumber(context: Context, phone: String) {
    val clipboard = context.getSystemService(ClipboardManager::class.java)
    clipboard?.setPrimaryClip(ClipData.newPlainText("phone", phone))
    Toast.makeText(
        context,
        context.getString(R.string.copied_to_clipboard),
        Toast.LENGTH_SHORT,
    ).show()
}

internal fun tryPlaceCall(
    context: Context,
    phone: String,
    onPermissionNeeded: (String) -> Unit,
) {
    val granted = hasPermission(context, Manifest.permission.CALL_PHONE)
    Log.d(TAG, "tryPlaceCall: phone='$phone', callPermissionGranted=$granted")
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
    Log.d(TAG, "onCallPermissionResult: isGranted=$isGranted, phone=$phone")
    if (phone == null) return
    if (isGranted) {
        placeCall(context, phone)
    } else {
        // Fallback без разрешения: открываем дайлер, чтобы звонок всё равно работал.
        Toast.makeText(
            context,
            context.getString(R.string.call_no_permission_dialer),
            Toast.LENGTH_SHORT,
        ).show()
        openDialer(context, phone)
    }
}

internal fun syncContactsPermission(
    context: Context,
    viewModel: ContactsViewModel,
    onPermissionChanged: (Boolean) -> Unit,
) {
    val granted = hasPermission(context, Manifest.permission.READ_CONTACTS)
    onPermissionChanged(granted)
    if (granted) {
        viewModel.refresh()
    } else {
        viewModel.clear()
    }
}
