package com.m4isper.contactsapp.domain.repository

import com.m4isper.contactsapp.domain.model.ContactAccount

/** Аккаунты контактов: последний использованный и аккаунт по умолчанию. */
interface AccountSettings {
    suspend fun setLastContactAccount(account: ContactAccount)

    /** Аккаунт по умолчанию для новых контактов. */
    suspend fun setDefaultContactAccount(account: ContactAccount)
}
