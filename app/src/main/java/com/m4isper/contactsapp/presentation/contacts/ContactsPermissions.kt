package com.m4isper.contactsapp.presentation.contacts

import android.Manifest
import android.content.Context
import com.m4isper.contactsapp.presentation.common.hasPermission

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
