package com.m4isper.contactsapp.presentation.common

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

/**
 * Единая точка звонков для любого экрана: проверка разрешения,
 * запрос через системный диалог, fallback в дайлер. Использовать так:
 * `val requestCall = rememberCallHandler()`.
 */
@Composable
internal fun rememberCallHandler(): (String) -> Unit {
    val context = LocalContext.current
    var pendingCall by remember { mutableStateOf<String?>(null) }

    val callPermissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
        ) { isGranted ->
            val phone = pendingCall
            pendingCall = null
            onCallPermissionResult(context, isGranted, phone)
        }

    return { phone ->
        tryPlaceCall(context, phone) { needed ->
            pendingCall = needed
            callPermissionLauncher.launch(Manifest.permission.CALL_PHONE)
        }
    }
}
