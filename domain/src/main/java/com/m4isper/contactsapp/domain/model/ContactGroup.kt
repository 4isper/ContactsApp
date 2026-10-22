package com.m4isper.contactsapp.domain.model

/**
 * Группа (метка) контактов из таблицы [android.provider.ContactsContract.Groups].
 * [accountName] и [accountType] пусты у локальных групп.
 */
data class ContactGroup(
    val id: Long,
    val title: String,
    val accountName: String? = null,
    val accountType: String? = null,
) {
    val isLocal: Boolean
        get() = accountName.isNullOrBlank() && accountType.isNullOrBlank()

    val account: ContactAccount
        get() = ContactAccount(name = accountName, type = accountType)

    /** Принадлежит ли группа аккаунту [account]; локальная группа соответствует [ContactAccount.Local]. */
    fun belongsTo(account: ContactAccount): Boolean =
        if (account.isLocal) {
            isLocal
        } else {
            accountName == account.name && accountType == account.type
        }
}
