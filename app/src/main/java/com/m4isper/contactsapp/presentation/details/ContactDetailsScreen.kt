package com.m4isper.contactsapp.presentation.details

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.m4isper.contactsapp.R
import com.m4isper.contactsapp.domain.model.ContactDetails
import com.m4isper.contactsapp.domain.model.PhoneKind
import com.m4isper.contactsapp.domain.model.PhoneNumber
import com.m4isper.contactsapp.presentation.common.ConfirmCallDialog
import com.m4isper.contactsapp.presentation.common.ContactAvatar
import com.m4isper.contactsapp.presentation.common.ErrorState
import com.m4isper.contactsapp.presentation.common.LoadingState
import com.m4isper.contactsapp.presentation.common.copyNumber
import com.m4isper.contactsapp.presentation.common.onCallPermissionResult
import com.m4isper.contactsapp.presentation.common.openSms
import com.m4isper.contactsapp.presentation.common.shareContact
import com.m4isper.contactsapp.presentation.common.tryPlaceCall
import dev.zacsweers.metrox.viewmodel.metroViewModel

private data class CallTarget(
    val name: String,
    val phone: String,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactDetailsScreen(
    contactId: Long,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ContactDetailsViewModel = metroViewModel(),
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var pendingCall by remember { mutableStateOf<String?>(null) }
    var pendingConfirm by remember { mutableStateOf<CallTarget?>(null) }

    val callPermissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
        ) { isGranted ->
            val phone = pendingCall
            pendingCall = null
            onCallPermissionResult(context, isGranted, phone)
        }

    fun requestCall(phone: String) {
        tryPlaceCall(context, phone) { needed ->
            pendingCall = needed
            callPermissionLauncher.launch(Manifest.permission.CALL_PHONE)
        }
    }

    LaunchedEffect(contactId) {
        viewModel.load(contactId)
    }

    LifecycleResumeEffect(contactId) {
        viewModel.refresh()
        onPauseOrDispose { /* no-op */ }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            DetailsTopBar(
                title = (uiState.content as? DetailsContent.Content)?.details?.name,
                shareable = (uiState.content as? DetailsContent.Content)?.details,
                onBack = onBack,
                onShare = { shareContact(context, it) },
            )
        },
    ) { innerPadding ->
        val screenModifier =
            Modifier
                .fillMaxSize()
                .padding(innerPadding)

        when (val content = uiState.content) {
            is DetailsContent.Loading -> LoadingState()
            is DetailsContent.NotFound -> NotFoundState()
            is DetailsContent.Error ->
                ErrorState(
                    message = content.message,
                    onRetry = viewModel::refresh,
                )
            is DetailsContent.Content ->
                DetailsContent(
                    details = content.details,
                    onCallClick = {
                        pendingConfirm = CallTarget(content.details.name, it.number)
                    },
                    onSmsClick = { openSms(context, it.number) },
                    onCopyClick = { copyNumber(context, it.number) },
                    modifier = screenModifier,
                )
        }
    }

    pendingConfirm?.let { target ->
        ConfirmCallDialog(
            contactName = target.name,
            contactPhone = target.phone,
            onConfirm = {
                pendingConfirm = null
                requestCall(target.phone)
            },
            onDismiss = { pendingConfirm = null },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DetailsTopBar(
    title: String?,
    shareable: ContactDetails?,
    onBack: () -> Unit,
    onShare: (ContactDetails) -> Unit,
) {
    TopAppBar(
        title = { Text(title ?: stringResource(R.string.action_back)) },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.action_back),
                )
            }
        },
        actions = {
            if (shareable != null) {
                IconButton(onClick = { onShare(shareable) }) {
                    Icon(
                        Icons.Filled.Share,
                        contentDescription = stringResource(R.string.share_contact),
                    )
                }
            }
        },
    )
}

@Composable
private fun NotFoundState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.contact_not_found),
            style = MaterialTheme.typography.bodyLarge,
            color = Color.Gray,
        )
    }
}

@Composable
private fun DetailsContent(
    details: ContactDetails,
    onCallClick: (PhoneNumber) -> Unit,
    onSmsClick: (PhoneNumber) -> Unit,
    onCopyClick: (PhoneNumber) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier.fillMaxSize()) {
        item(key = "header") {
            DetailsHeader(name = details.name, photoUri = details.photoUri)
        }
        items(
            items = details.phones,
            key = { it.number },
        ) { phone ->
            PhoneRow(
                phone = phone,
                onClick = { onCallClick(phone) },
                onSmsClick = { onSmsClick(phone) },
                onCopyClick = { onCopyClick(phone) },
            )
        }
    }
}

@Composable
private fun DetailsHeader(
    name: String,
    photoUri: String?,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp),
    ) {
        ContactAvatar(
            name = name,
            photoUri = photoUri,
            size = 72.dp,
            textStyle = MaterialTheme.typography.headlineMedium,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = name,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun PhoneRow(
    phone: PhoneNumber,
    onClick: () -> Unit,
    onSmsClick: () -> Unit,
    onCopyClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = phone.number,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = phoneKindLabel(phone),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
            )
        }
        IconButton(onClick = onSmsClick) {
            Icon(
                Icons.Filled.Message,
                contentDescription = stringResource(R.string.action_send_sms),
            )
        }
        IconButton(onClick = onCopyClick) {
            Icon(
                Icons.Filled.ContentCopy,
                contentDescription = stringResource(R.string.action_copy_number),
            )
        }
        IconButton(onClick = onClick) {
            Icon(
                Icons.Filled.Call,
                contentDescription = stringResource(R.string.action_call),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun phoneKindLabel(phone: PhoneNumber): String =
    phone.customLabel ?: stringResource(
        when (phone.kind) {
            PhoneKind.MOBILE -> R.string.phone_type_mobile
            PhoneKind.HOME -> R.string.phone_type_home
            PhoneKind.WORK -> R.string.phone_type_work
            PhoneKind.OTHER -> R.string.phone_type_other
        },
    )
