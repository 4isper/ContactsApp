package com.m4isper.contactsapp.presentation.details

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.m4isper.contactsapp.R
import com.m4isper.contactsapp.domain.model.contact.ContactAccount
import com.m4isper.contactsapp.domain.model.contact.ContactDetails
import com.m4isper.contactsapp.domain.model.contact.PhoneNumber
import com.m4isper.contactsapp.presentation.common.Loadable
import com.m4isper.contactsapp.presentation.common.account.contactAccountTitle
import com.m4isper.contactsapp.presentation.common.call.ConfirmCallDialog
import com.m4isper.contactsapp.presentation.common.call.rememberCallHandler
import com.m4isper.contactsapp.presentation.common.format.phoneText
import com.m4isper.contactsapp.presentation.common.share.contactAsText
import com.m4isper.contactsapp.presentation.common.share.copyToClipboard
import com.m4isper.contactsapp.presentation.common.share.openSms
import com.m4isper.contactsapp.presentation.common.share.shareContact
import com.m4isper.contactsapp.presentation.common.share.shareContactAsQr
import com.m4isper.contactsapp.presentation.common.share.shareContactAsText
import com.m4isper.contactsapp.presentation.editor.components.AccountPickerDialog
import com.m4isper.contactsapp.presentation.widget.ContactsWidgetProvider
import com.m4isper.contactsapp.ui.components.ConfirmDialog
import com.m4isper.contactsapp.ui.components.EmptyState
import com.m4isper.contactsapp.ui.components.ErrorState
import com.m4isper.contactsapp.ui.components.LoadingState
import dev.zacsweers.metrox.viewmodel.metroViewModel
import kotlinx.coroutines.launch

private data class CallTarget(
    val name: String,
    val phone: PhoneNumber,
)

/** Что пользователь хочет сделать с аккаунтом контакта. */
private enum class AccountAction {
    COPY,
    MOVE,
}

@Composable
fun ContactDetailsScreen(
    contactId: Long,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ContactDetailsViewModel = metroViewModel(),
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val requestCall = rememberCallHandler()
    val scope = rememberCoroutineScope()

    var pendingConfirm by remember { mutableStateOf<CallTarget?>(null) }
    var showAccountPicker by remember { mutableStateOf(false) }
    var pendingAction by remember { mutableStateOf<AccountAction?>(null) }
    var pendingMoveAccount by remember { mutableStateOf<ContactAccount?>(null) }

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
                title =
                    (uiState.content as? Loadable.Content)?.data?.name
                        ?: stringResource(R.string.app_name),
                shareable = (uiState.content as? Loadable.Content)?.data,
                isFavorite = uiState.isFavorite,
                onBack = onBack,
                onEdit = onEdit,
                onShare = {
                    scope.launch {
                        val vCard = viewModel.vCardText()
                        if (vCard == null) {
                            Toast.makeText(context, R.string.share_failed, Toast.LENGTH_SHORT).show()
                        } else {
                            shareContact(context, vCard)
                        }
                    }
                },
                onShareAsText = {
                    scope.launch {
                        shareDetailsAsText(context, (uiState.content as? Loadable.Content)?.data)
                    }
                },
                onShareAsQr = {
                    scope.launch {
                        shareDetailsAsQr(context, viewModel.vCardText())
                    }
                },
                onToggleFavorite = {
                    viewModel.toggleFavorite()
                    ContactsWidgetProvider.refresh(context)
                },
                onCopyToAccount = {
                    pendingAction = AccountAction.COPY
                    showAccountPicker = true
                },
                onMoveToAccount = {
                    pendingAction = AccountAction.MOVE
                    showAccountPicker = true
                },
            )
        },
    ) { innerPadding ->
        val screenModifier =
            Modifier
                .fillMaxSize()
                .padding(innerPadding)

        when (val content = uiState.content) {
            is Loadable.Loading -> LoadingState(screenModifier)
            is Loadable.Empty ->
                EmptyState(
                    icon = Icons.Filled.PersonSearch,
                    title = stringResource(R.string.contact_not_found),
                    modifier = screenModifier,
                )
            is Loadable.Error ->
                ErrorState(
                    error = content.error,
                    onRetry = viewModel::refresh,
                    modifier = screenModifier,
                )
            is Loadable.Content ->
                DetailsContent(
                    details = content.data,
                    onCallClick = { phone ->
                        pendingConfirm = CallTarget(content.data.name, phone)
                    },
                    onSmsClick = { openSms(context, it.number) },
                    onCopyClick = { copyToClipboard(context, it) },
                    modifier = screenModifier,
                    groups = uiState.groups,
                )
        }
    }

    pendingConfirm?.let { target ->
        ConfirmCallDialog(
            contactName = target.name,
            phoneText = phoneText(target.phone),
            onConfirm = {
                pendingConfirm = null
                requestCall(target.phone.number)
            },
            onDismiss = { pendingConfirm = null },
        )
    }

    // Смена аккаунта: выбор аккаунта и подтверждение переноса с удалением исходника.
    DetailsEventToast(uiState.event, viewModel::onEventHandled)

    AccountChangeDialogs(
        uiState = uiState,
        showAccountPicker = showAccountPicker,
        pendingMoveAccount = pendingMoveAccount,
        onAccountPicked = { account ->
            showAccountPicker = false
            when (pendingAction) {
                AccountAction.COPY -> {
                    pendingAction = null
                    viewModel.onCopyToAccount(account)
                }
                AccountAction.MOVE -> pendingMoveAccount = account
                null -> Unit
            }
        },
        onPickerDismissed = { showAccountPicker = false },
        onMoveConfirmed = { account ->
            pendingMoveAccount = null
            viewModel.onMoveToAccount(account)
        },
        onMoveDismissed = { pendingMoveAccount = null },
    )
}

/** Одноразовые события карточки показываем тостом. */
@Composable
private fun DetailsEventToast(
    event: DetailsEvent?,
    onHandled: () -> Unit,
) {
    val context = LocalContext.current
    LaunchedEffect(event) {
        val messageRes =
            when (event) {
                is DetailsEvent.ContactCopied -> R.string.details_copy_done
                is DetailsEvent.ContactMoved -> R.string.details_move_done
                DetailsEvent.AccountActionFailed -> R.string.details_account_failed
                null -> return@LaunchedEffect
            }
        Toast.makeText(context, messageRes, Toast.LENGTH_SHORT).show()
        onHandled()
    }
}

/** Диалоги смены аккаунта: выбор аккаунта и подтверждение переноса. */
@Composable
private fun AccountChangeDialogs(
    uiState: DetailsUiState,
    showAccountPicker: Boolean,
    pendingMoveAccount: ContactAccount?,
    onAccountPicked: (ContactAccount) -> Unit,
    onPickerDismissed: () -> Unit,
    onMoveConfirmed: (ContactAccount) -> Unit,
    onMoveDismissed: () -> Unit,
) {
    if (showAccountPicker) {
        AccountPickerDialog(
            accounts = uiState.accounts,
            selected = null,
            onSelect = onAccountPicked,
            onDismiss = onPickerDismissed,
        )
    }
    pendingMoveAccount?.let { account ->
        ConfirmDialog(
            title = stringResource(R.string.details_move_confirm_title),
            text = stringResource(R.string.details_move_confirm_text, contactAccountTitle(account)),
            confirmLabel = stringResource(R.string.details_move_to_account),
            onConfirm = { onMoveConfirmed(account) },
            onDismiss = onMoveDismissed,
        )
    }
}

/** Делится контактом текстом: без деталей показываем ошибку, а не падаем. */
private suspend fun shareDetailsAsText(
    context: Context,
    details: ContactDetails?,
) {
    if (details == null) {
        shareOrFail(context, false) { }
    } else {
        shareOrFail(context, true) { shareContactAsText(context, contactAsText(details)) }
    }
}

/** Делится контактом QR-кодом: без карточки показываем ошибку, а не падаем. */
private suspend fun shareDetailsAsQr(
    context: Context,
    vCard: String?,
) {
    if (vCard == null) {
        shareOrFail(context, false) { }
    } else {
        shareOrFail(context, true) { shareContactAsQr(context, vCard) }
    }
}

private suspend fun shareOrFail(
    context: Context,
    hasContent: Boolean,
    action: suspend () -> Unit,
) {
    if (hasContent) {
        action()
    } else {
        Toast.makeText(context, R.string.share_failed, Toast.LENGTH_SHORT).show()
    }
}
