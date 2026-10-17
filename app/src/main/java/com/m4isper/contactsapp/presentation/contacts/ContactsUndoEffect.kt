package com.m4isper.contactsapp.presentation.contacts

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import com.m4isper.contactsapp.R

/**
 * Пока удаление отложено (список отложенных контактов не пуст), показывает snackbar с действием
 * «Отменить»: нажатие возвращает контакты в список, тайм-аут ничего не делает — удаление выполнит
 * ViewModel по истечении окна отмены.
 */
@Composable
internal fun DeleteUndoEffect(
    snackbarHostState: SnackbarHostState,
    pendingDeleteIds: Set<Long>,
    onUndoDelete: () -> Unit,
) {
    val context = LocalContext.current
    LaunchedEffect(pendingDeleteIds) {
        if (pendingDeleteIds.isEmpty()) return@LaunchedEffect
        val count = pendingDeleteIds.size
        val result =
            snackbarHostState.showSnackbar(
                message = context.resources.getQuantityString(R.plurals.batch_delete_undo, count, count),
                actionLabel = context.getString(R.string.action_undo),
                duration = SnackbarDuration.Short,
            )
        if (result == SnackbarResult.ActionPerformed) {
            onUndoDelete()
        }
    }
}
