package com.m4isper.contactsapp.presentation.recents

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.m4isper.contactsapp.R
import com.m4isper.contactsapp.domain.model.contact.PhoneNumber
import com.m4isper.contactsapp.presentation.common.Loadable
import com.m4isper.contactsapp.presentation.common.call.ConfirmCallDialog
import com.m4isper.contactsapp.presentation.common.call.rememberCallHandler
import com.m4isper.contactsapp.presentation.common.format.phoneText
import com.m4isper.contactsapp.presentation.common.permissions.PermissionRequest
import com.m4isper.contactsapp.presentation.common.permissions.hasPermission
import com.m4isper.contactsapp.presentation.common.permissions.openAppSettings
import com.m4isper.contactsapp.presentation.common.share.openSms
import com.m4isper.contactsapp.presentation.contacts.components.ContactCard
import com.m4isper.contactsapp.presentation.contacts.components.rememberShowFavoriteQuickAction
import com.m4isper.contactsapp.ui.components.EmptyState
import com.m4isper.contactsapp.ui.components.ErrorState
import com.m4isper.contactsapp.ui.components.LoadingState
import dev.zacsweers.metrox.viewmodel.metroViewModel

private data class RecentsCallTarget(
    val name: String,
    val phone: PhoneNumber,
)

/** Недавние контакты по данным всего устройства; доступны те же быстрые действия. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecentsScreen(
    onBack: () -> Unit,
    onContactClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RecentsViewModel = metroViewModel(),
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val requestCall = rememberCallHandler()
    var pendingCall by remember { mutableStateOf<RecentsCallTarget?>(null) }
    val showFavoriteQuickAction = rememberShowFavoriteQuickAction()
    var hasReadPermission by remember {
        mutableStateOf(hasPermission(context, Manifest.permission.READ_CONTACTS))
    }

    val readPermissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
        ) { isGranted ->
            hasReadPermission = isGranted
            if (isGranted) {
                viewModel.load()
            } else {
                viewModel.clear()
            }
        }

    LifecycleResumeEffect(context) {
        val granted = hasPermission(context, Manifest.permission.READ_CONTACTS)
        hasReadPermission = granted
        if (granted) {
            viewModel.load()
        } else {
            viewModel.clear()
        }
        onPauseOrDispose { /* no-op */ }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.recents_title))
                        Text(
                            text = stringResource(R.string.recents_device_wide),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("recents_back_button"),
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        val screenModifier =
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
        if (!hasReadPermission) {
            PermissionRequest(
                modifier = screenModifier,
                onGrantClick = { readPermissionLauncher.launch(Manifest.permission.READ_CONTACTS) },
                onSettingsClick = { openAppSettings(context) },
            )
        } else {
            when (val content = uiState.content) {
                is Loadable.Loading -> LoadingState(screenModifier)
                is Loadable.Empty ->
                    EmptyState(
                        icon = Icons.Filled.History,
                        title = stringResource(R.string.recents_empty),
                        description = stringResource(R.string.recents_empty_desc),
                        primaryLabel = stringResource(R.string.action_browse_contacts),
                        onPrimaryClick = onBack,
                        modifier = screenModifier,
                    )
                is Loadable.Error ->
                    ErrorState(
                        error = content.error,
                        onRetry = viewModel::load,
                        modifier = screenModifier,
                    )
                is Loadable.Content ->
                    LazyColumn(
                        modifier = screenModifier.testTag("recents_list"),
                    ) {
                        items(
                            items = content.data,
                            key = { it.contactId },
                        ) { contact ->
                            ContactCard(
                                contact = contact,
                                isFavorite = contact.contactId in uiState.favorites,
                                selected = false,
                                isSelectionMode = false,
                                onClick = { onContactClick(contact.contactId) },
                                onLongClick = {},
                                onCall = {
                                    contact.primaryPhone?.let {
                                        pendingCall = RecentsCallTarget(contact.name, it)
                                    }
                                },
                                onSms = {
                                    contact.primaryPhone?.let { openSms(context, it.number) }
                                },
                                onToggleFavorite = { viewModel.toggleFavorite(contact.contactId) },
                                showFavoriteQuickAction = showFavoriteQuickAction,
                            )
                        }
                    }
            }
        }
    }

    pendingCall?.let { target ->
        ConfirmCallDialog(
            contactName = target.name,
            phoneText = phoneText(target.phone),
            onConfirm = {
                pendingCall = null
                requestCall(target.phone.number)
            },
            onDismiss = { pendingCall = null },
        )
    }
}
