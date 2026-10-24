package com.m4isper.contactsapp.presentation.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.Manifest
import androidx.core.content.ContextCompat
import com.m4isper.contactsapp.ContactsApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Восстанавливает будильник напоминаний после перезагрузки устройства. */
class ReminderBootReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent?,
    ) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return
        val pending = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            try {
                if (ContextCompat.checkSelfPermission(appContext, Manifest.permission.READ_CONTACTS) !=
                    PackageManager.PERMISSION_GRANTED
                ) {
                    return@launch
                }
                val graph = (appContext as? ContactsApp)?.appGraph ?: return@launch
                val settings = graph.observeSettings().first()
                if (settings.remindersEnabled) {
                    BirthdayReminderScheduler(appContext).schedule()
                }
            } finally {
                pending.finish()
            }
        }
    }
}
