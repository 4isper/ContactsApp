package com.m4isper.contactsapp.presentation.common.permissions

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.core.net.toUri

internal fun hasPermission(
    context: Context,
    permission: String,
): Boolean =
    ContextCompat.checkSelfPermission(context, permission) ==
        PackageManager.PERMISSION_GRANTED

internal fun openAppSettings(context: Context) {
    val intent =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = "package:${context.packageName}".toUri()
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    context.startActivity(intent)
}

/**
 * Постоянный отказ: проверяется в колбэке запроса сразу после отказа —
 * система больше не покажет диалог, маршрут только через настройки.
 */
internal fun isPermanentlyDenied(
    context: Context,
    permission: String,
): Boolean {
    val activity = context as? Activity ?: return false
    return !activity.shouldShowRequestPermissionRationale(permission)
}

/** После отказа в колбэке: постоянный отказ открывает настройки, обычный — молчит. */
internal fun openSettingsIfPermanentlyDenied(
    context: Context,
    permission: String,
) {
    if (isPermanentlyDenied(context, permission)) {
        openAppSettings(context)
    }
}
