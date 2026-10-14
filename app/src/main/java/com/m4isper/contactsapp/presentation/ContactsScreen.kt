package com.m4isper.contactsapp.presentation

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.m4isper.contactsapp.domain.model.Contact
import dev.zacsweers.metrox.viewmodel.metroViewModel

@Composable
fun ContactsScreen(
    modifier: Modifier = Modifier,
    viewModel: ContactsViewModel = metroViewModel(),
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var hasReadPermission by remember {
        mutableStateOf(hasPermission(context, Manifest.permission.READ_CONTACTS))
    }
    var pendingCall by remember { mutableStateOf<Contact?>(null) }
    var pendingConfirm by remember { mutableStateOf<Contact?>(null) }

    val readPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { isGranted ->
        hasReadPermission = isGranted
        if (isGranted) {
            viewModel.refresh()
        }
    }

    val callPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { isGranted ->
        val contact = pendingCall
        pendingCall = null
        onCallPermissionResult(context, isGranted, contact)
    }

    LifecycleResumeEffect(context) {
        syncContactsPermission(context, viewModel) { hasReadPermission = it }
        onPauseOrDispose { /* no-op */ }
    }

    if (!hasReadPermission) {
        PermissionRequest(
            modifier = modifier,
            onGrantClick = {
                readPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
            },
            onSettingsClick = { openAppSettings(context) },
        )
    } else {
        ContactList(
            uiState = uiState,
            onSearchQueryChange = viewModel::onSearchQueryChange,
            onContactClick = { pendingConfirm = it },
            onRetry = viewModel::refresh,
            modifier = modifier,
        )
    }

    pendingConfirm?.let { contact ->
        ConfirmCallDialog(
            contact = contact,
            onConfirm = {
                pendingConfirm = null
                tryPlaceCall(context, contact) { pending ->
                    pendingCall = pending
                    callPermissionLauncher.launch(Manifest.permission.CALL_PHONE)
                }
            },
            onDismiss = { pendingConfirm = null },
        )
    }
}
