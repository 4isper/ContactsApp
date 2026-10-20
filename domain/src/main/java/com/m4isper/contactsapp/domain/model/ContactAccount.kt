package com.m4isper.contactsapp.domain.model

/**
 * Аккаунт, в который можно сохранить контакт.
 * [Local] означает, что контакт хранится только на устройстве и не синхронизируется.
 */
data class ContactAccount(
    val name: String? = null,
    val type: String? = null,
) {
    val isLocal: Boolean
        get() = name.isNullOrBlank() && type.isNullOrBlank()

    companion object {
        val Local = ContactAccount()
    }
}
