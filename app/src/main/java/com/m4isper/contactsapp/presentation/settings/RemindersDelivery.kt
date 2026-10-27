package com.m4isper.contactsapp.presentation.settings

import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationManagerCompat

/** Идентификатор канала напоминаний: общий для приёмника и настроек. */
internal const val REMINDERS_CHANNEL_ID = "reminders"

/** Итоговое состояние переключателя: настройка включена и уведомления реально доставляются. */
internal fun remindersEffectivelyOn(
    enabled: Boolean,
    appNotificationsEnabled: Boolean,
    channelBlocked: Boolean,
): Boolean = enabled && appNotificationsEnabled && !channelBlocked

/** Уведомления приложения включены в системе. */
internal fun appNotificationsEnabled(context: Context): Boolean =
    NotificationManagerCompat.from(context).areNotificationsEnabled()

/** Канал напоминаний существует и заглушён пользователем. */
internal fun remindersChannelBlocked(context: Context): Boolean {
    val channel =
        NotificationManagerCompat.from(context).getNotificationChannel(REMINDERS_CHANNEL_ID)
            ?: return false
    return channel.importance == NotificationManager.IMPORTANCE_NONE
}
