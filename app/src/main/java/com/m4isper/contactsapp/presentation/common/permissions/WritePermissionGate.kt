package com.m4isper.contactsapp.presentation.common.permissions

import android.Manifest
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.m4isper.contactsapp.R

/**
 * Контекстная выдача WRITE_CONTACTS: запрос происходит в момент действия,
 * а не при открытии экрана. После выдачи отложенное действие выполняется ровно
 * один раз; состояние перепроверяется при каждом возврате на экран, чтобы отказ
 * из настроек не кэшировался.
 */
internal class WritePermissionGate {
    internal var granted by mutableStateOf(false)

    /** Действие, отложенное до ответа системы на запрос права. */
    private var pending: (() -> Unit)? = null

    /** Присваивается эффектом после создания лончера. */
    private var request: (() -> Unit)? = null

    val isGranted: Boolean
        get() = granted

    /** Выполняет [action] сразу; без права запрашивает его и повторит один раз после выдачи. */
    fun run(action: () -> Unit) {
        if (granted) {
            action()
        } else {
            pending = action
            request?.invoke()
        }
    }

    internal fun onPermissionResult(
        result: Boolean,
        onDenied: () -> Unit,
    ) {
        granted = result
        val action = pending
        pending = null
        if (result) {
            action?.invoke()
        } else {
            onDenied()
        }
    }

    internal fun refresh(context: Context) {
        granted = hasPermission(context, Manifest.permission.WRITE_CONTACTS)
    }

    internal fun setRequest(value: () -> Unit) {
        request = value
    }
}

@Composable
internal fun rememberWritePermissionGate(onDenied: () -> Unit = {}): WritePermissionGate {
    val context = LocalContext.current
    val currentOnDenied by rememberUpdatedState(onDenied)
    val gate = remember { WritePermissionGate() }
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { result ->
            // Постоянный отказ: снекбара недостаточно — сразу открываем настройки.
            if (!result) openSettingsIfPermanentlyDenied(context, Manifest.permission.WRITE_CONTACTS)
            gate.onPermissionResult(result, currentOnDenied)
        }
    LaunchedEffect(launcher) {
        gate.setRequest { launcher.launch(Manifest.permission.WRITE_CONTACTS) }
    }
    LifecycleResumeEffect(context) {
        gate.refresh(context)
        onPauseOrDispose { /* no-op */ }
    }
    return gate
}

/** Снекбар отказа: короткое объяснение и переход в настройки права. */
internal suspend fun showWriteDeniedSnackbar(
    context: Context,
    snackbarHostState: SnackbarHostState,
) {
    val openSettings = context.getString(R.string.permission_open_settings)
    val result =
        snackbarHostState.showSnackbar(
            message = context.getString(R.string.permission_write_rationale),
            actionLabel = openSettings,
        )
    if (result == SnackbarResult.ActionPerformed) {
        openAppSettings(context)
    }
}
