package com.m4isper.contactsapp.presentation.contacts

import android.widget.Toast
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import com.m4isper.contactsapp.R

/**
 * Пока окно удаления открыто, показывает snackbar с действием «Отменить» на всё окно.
 * Таймер окна живёт в [PendingDeleteCoordinator]: когда он истёк ([DeleteWindow.closing]),
 * эффект гасит snackbar и сообщает об этом через [onWindowClosed] — единый источник времени,
 * поэтому окно UI и момент фактического удаления не расходятся.
 */
@Composable
internal fun DeleteUndoEffect(
    snackbarHostState: SnackbarHostState,
    pendingDeleteIds: Set<Long>,
    deleteClosing: Boolean,
    onUndoDelete: () -> Unit,
    onWindowClosed: () -> Unit,
) {
    val context = LocalContext.current
    // Ключ — размер очереди: второй батч в том же окне обновляет текст snackbar.
    LaunchedEffect(pendingDeleteIds.size) {
        if (pendingDeleteIds.isEmpty()) return@LaunchedEffect
        val count = pendingDeleteIds.size
        val result =
            snackbarHostState.showSnackbar(
                message = context.resources.getQuantityString(R.plurals.batch_delete_undo, count, count),
                actionLabel = context.getString(R.string.action_undo),
                withDismissAction = true,
                duration = SnackbarDuration.Indefinite,
            )
        // После коммита окно закрывает второй эффект ([deleteClosing]) — повторно
        // возвращать контакты в этом случае нельзя.
        if (result == SnackbarResult.ActionPerformed && !deleteClosing) {
            onUndoDelete()
            val message = context.resources.getQuantityString(R.plurals.batch_delete_undone, count, count)
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(deleteClosing) {
        if (deleteClosing) {
            snackbarHostState.currentSnackbarData?.dismiss()
            onWindowClosed()
        }
    }
}
