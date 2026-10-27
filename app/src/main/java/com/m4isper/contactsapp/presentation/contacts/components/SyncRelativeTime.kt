package com.m4isper.contactsapp.presentation.contacts.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.m4isper.contactsapp.R
import com.m4isper.contactsapp.domain.model.contact.AccountSyncInfo
import com.m4isper.contactsapp.domain.model.contact.AccountSyncStatus
import com.m4isper.contactsapp.domain.model.contact.ContactAccount
import com.m4isper.contactsapp.presentation.common.account.contactAccountSubtitle
import kotlinx.coroutines.delay

/**
 * Относительное время последней синхронизации. Отдельная модель позволяет
 * проверить границы без Android runtime и без строк, зашитых в Kotlin.
 */
internal sealed interface SyncRelativeTime {
    data object JustNow : SyncRelativeTime

    data class Minutes(
        val count: Long,
    ) : SyncRelativeTime

    data class Hours(
        val count: Long,
    ) : SyncRelativeTime

    data class Days(
        val count: Long,
    ) : SyncRelativeTime
}

/**
 * Переводит epoch-время в локализуемый относительный интервал.
 * Будущие и отрицательные значения считаются «только что», чтобы
 * расхождение часов не показывало «-1 мин назад».
 */
internal fun syncRelativeTime(
    lastSyncTimeMs: Long,
    nowMs: Long,
): SyncRelativeTime {
    val elapsedMs = (nowMs - lastSyncTimeMs).coerceAtLeast(0L)
    val minutes = elapsedMs / MINUTE_MS
    return when {
        minutes < 1 -> SyncRelativeTime.JustNow
        minutes < MINUTES_PER_HOUR -> SyncRelativeTime.Minutes(minutes)
        minutes < MINUTES_PER_DAY -> SyncRelativeTime.Hours(minutes / MINUTES_PER_HOUR)
        else -> SyncRelativeTime.Days(minutes / MINUTES_PER_DAY)
    }
}

/** Время «сейчас» для относительных подписей: обновляется раз в минуту. */
@Composable
internal fun rememberSyncNow(infos: List<AccountSyncInfo>): Long? {
    val timestamps = infos.mapNotNull { it.lastSyncTimeMs }
    return produceState<Long?>(initialValue = null, key1 = timestamps) {
        if (timestamps.isEmpty()) {
            value = null
            return@produceState
        }
        while (true) {
            value = System.currentTimeMillis()
            delay(SYNC_CLOCK_INTERVAL_MS)
        }
    }.value
}

/** Текст статуса синхронизации: локализованная подпись и относительное время. */
@Composable
internal fun syncStatusText(
    info: AccountSyncInfo,
    nowMs: Long?,
): String {
    val status = stringResource(info.syncStatus.labelRes())
    val relativeTime = info.lastSyncTimeMs?.let { lastSync -> nowMs?.let { syncRelativeTimeText(lastSync, it) } }
    return when {
        relativeTime == null -> status
        info.syncStatus == AccountSyncStatus.SYNCED ->
            stringResource(R.string.account_sync_synced_ago, relativeTime)
        else -> stringResource(R.string.account_sync_account_line, status, relativeTime)
    }
}

/** Подпись аккаунта в меню: тип аккаунта и статус синхронизации. */
@Composable
internal fun accountDetailText(
    account: ContactAccount,
    status: String?,
): String {
    val subtitle = contactAccountSubtitle(account)
    return status?.let { stringResource(R.string.account_sync_account_line, subtitle, it) } ?: subtitle
}

@Composable
private fun syncRelativeTimeText(
    lastSyncTimeMs: Long,
    nowMs: Long,
): String =
    when (val relative = syncRelativeTime(lastSyncTimeMs, nowMs)) {
        SyncRelativeTime.JustNow -> stringResource(R.string.account_sync_just_now)
        is SyncRelativeTime.Minutes ->
            pluralStringResource(
                R.plurals.account_sync_minutes_ago,
                relative.count.toPluralCount(),
                relative.count,
            )
        is SyncRelativeTime.Hours ->
            pluralStringResource(
                R.plurals.account_sync_hours_ago,
                relative.count.toPluralCount(),
                relative.count,
            )
        is SyncRelativeTime.Days ->
            pluralStringResource(
                R.plurals.account_sync_days_ago,
                relative.count.toPluralCount(),
                relative.count,
            )
    }

private fun Long.toPluralCount(): Int = coerceAtMost(Int.MAX_VALUE.toLong()).toInt()

private fun AccountSyncStatus.labelRes(): Int =
    when (this) {
        AccountSyncStatus.SYNCED -> R.string.account_sync_synced
        AccountSyncStatus.SYNCING -> R.string.account_sync_syncing
        AccountSyncStatus.STALE -> R.string.account_sync_stale
        AccountSyncStatus.ERROR -> R.string.account_sync_error
    }

private const val MINUTE_MS = 60_000L
private const val MINUTES_PER_HOUR = 60L
private const val MINUTES_PER_DAY = 24L * MINUTES_PER_HOUR
private const val SYNC_CLOCK_INTERVAL_MS = 60_000L
