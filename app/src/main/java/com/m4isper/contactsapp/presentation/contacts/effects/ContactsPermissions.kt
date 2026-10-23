package com.m4isper.contactsapp.presentation.contacts.effects

import android.Manifest
import android.content.Context
import com.m4isper.contactsapp.presentation.common.permissions.hasPermission
import com.m4isper.contactsapp.presentation.contacts.ContactsViewModel

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
