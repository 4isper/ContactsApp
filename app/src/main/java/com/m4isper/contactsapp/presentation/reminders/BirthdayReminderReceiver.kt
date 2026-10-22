package com.m4isper.contactsapp.presentation.reminders

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.ContactsContract
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.m4isper.contactsapp.ContactsApp
import com.m4isper.contactsapp.R
import com.m4isper.contactsapp.domain.reminders.OccasionKind
import com.m4isper.contactsapp.domain.reminders.UpcomingOccasion
import com.m4isper.contactsapp.domain.reminders.upcomingOccasions
import com.m4isper.contactsapp.domain.usecase.GetContactsUseCase
import com.m4isper.contactsapp.domain.usecase.ObserveSettingsUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Приёмник будильника: собирает праздники и показывает уведомление. */
class BirthdayReminderReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent?,
    ) {
        val pending = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            try {
                showReminder(appContext)
            } finally {
                pending.finish()
            }
        }
    }

    private suspend fun showReminder(context: Context) {
        val graph = (context.applicationContext as? ContactsApp)?.appGraph ?: return
        val settings = ObserveSettingsUseCase(graph.settingsReader)().first()
        val upcoming =
            if (settings.remindersEnabled) {
                upcomingOccasions(
                    contacts = GetContactsUseCase(graph.contactsReader)(),
                    leadDays = settings.reminderLeadDays,
                    today = LocalDate.now(),
                )
            } else {
                emptyList()
            }
        // Уведомление показываем только с разрешения: на Android 13+ его нужно запросить.
        if (upcoming.isNotEmpty()) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
            ) {
                createChannel(context)
                val manager = NotificationManagerCompat.from(context)
                manager.notify(NOTIFICATION_ID, buildNotification(context, upcoming))
            }
        }
    }

    private fun buildNotification(
        context: Context,
        upcoming: List<UpcomingOccasion>,
    ): android.app.Notification {
        val title =
            context.getString(
                R.string.reminders_notification_title,
                upcoming.size,
            )
        val text = upcoming.joinToString("\n") { occasion -> occasion.line(context) }
        // Нажатие открывает карточку контакта из уведомления.
        val open =
            PendingIntent.getActivity(
                context,
                0,
                openContactIntent(context, upcoming.first().contactId),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        return NotificationCompat
            .Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
    }

    /** Запрос на открытие карточки контакта. */
    private fun openContactIntent(
        context: Context,
        contactId: Long,
    ): Intent =
        Intent(
            Intent.ACTION_VIEW,
            Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, contactId.toString()),
        ).apply {
            setPackage(context.packageName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

    private fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val channel =
            NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.reminders_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT,
            )
        manager.createNotificationChannel(channel)
    }

    companion object {
        private const val CHANNEL_ID = "reminders"
        private const val NOTIFICATION_ID = 4201
    }
}

/** Строка уведомления для одного праздника. */
private fun UpcomingOccasion.line(context: Context): String {
    val kind =
        when (kind) {
            OccasionKind.BIRTHDAY -> context.getString(R.string.reminders_birthday)
            OccasionKind.EVENT -> context.getString(R.string.reminders_event)
        }
    return "$contactName — $kind, $date"
}
