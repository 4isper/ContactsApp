package com.m4isper.contactsapp.domain.model.contact

/** Статус синхронизации аккаунта контактов. */
enum class AccountSyncStatus {
    /** Аккаунт синхронизирован, данные актуальны. */
    SYNCED,
    /** Идёт синхронизация прямо сейчас. */
    SYNCING,
    /** Данные устарели, нужна синхронизация. */
    STALE,
    /** Ошибка синхронизации. */
    ERROR,
}

/** Аккаунт с информацией о состоянии синхронизации. */
data class AccountSyncInfo(
    val account: ContactAccount,
    /** Текущий статус синхронизации. */
    val syncStatus: AccountSyncStatus = AccountSyncStatus.SYNCED,
    /** Время последней успешной синхронизации в миллисекундах (epoch). */
    val lastSyncTimeMs: Long?,
)
