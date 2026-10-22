package com.m4isper.contactsapp.presentation.contacts

import com.m4isper.contactsapp.domain.usecase.DeleteContactsUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.coroutines.coroutineContext

/** Окно отмены: контакты скрыты из списка, snackbar предлагает отменить удаление. */
internal data class DeleteWindow(
    val contactIds: Set<Long> = emptySet(),
    /** Таймер истёк, удаление выполняется: snackbar нужно закрыть. */
    val closing: Boolean = false,
)

/** Итог удаления: [failedCount] = 0 — удалено всё. */
internal data class DeleteFinished(
    val deletedIds: Set<Long>,
    val failedIds: Set<Long>,
) {
    val allDeleted: Boolean
        get() = failedIds.isEmpty()
}

/**
 * Сценарий отложенного удаления. Единственный таймер окна отмены живёт здесь: UI лишь
 * показывает snackbar, пока окно открыто, и закрывает его по сигналу «closing».
 * Ошибка удаления докладывается отдельным флагом [failure], чтобы повторные удаления
 * не «съедались» дедупликацией StateFlow.
 */
internal class PendingDeleteCoordinator(
    private val scope: CoroutineScope,
    private val deleteContacts: DeleteContactsUseCase,
) {
    private val _window = MutableStateFlow<DeleteWindow?>(null)
    private val _failure = MutableStateFlow(false)
    private val _finished = MutableStateFlow<DeleteFinished?>(null)

    val window: StateFlow<DeleteWindow?> = _window.asStateFlow()
    val failure: StateFlow<Boolean> = _failure.asStateFlow()
    val finished: StateFlow<DeleteFinished?> = _finished.asStateFlow()

    private var job: Job? = null

    /** Ставит выбранные контакты в очередь удаления; повторный вызов перезапускает окно отмены. */
    fun stage(contactIds: Set<Long>) {
        if (contactIds.isEmpty()) return
        val previous = job
        previous?.cancel()
        _window.value = DeleteWindow(contactIds = _window.value?.contactIds.orEmpty() + contactIds)
        job =
            scope.launch {
                var paused = true
                try {
                    delay(DELETE_UNDO_TIMEOUT_MILLIS)
                    paused = false
                    val ids = _window.value?.contactIds.orEmpty()
                    try {
                        val result = deleteContacts(ids)
                        _finished.value =
                            DeleteFinished(
                                deletedIds = result.deletedIds,
                                failedIds = result.failedIds,
                            )
                    } catch (e: CancellationException) {
                        throw e
                    } catch (_: Exception) {
                        _failure.value = true
                    }
                } finally {
                    // Запоздалое завершение отменённого job не должно трогать состояние новой сцены.
                    if (job === coroutineContext[Job]) {
                        job = null
                        if (!paused) {
                            // Окно закрылось по таймеру: UI гасит snackbar и сообщает через [windowClosed].
                            _window.value = DeleteWindow(closing = true)
                        }
                    }
                }
            }
    }

    /** Отменяет удаление: контакты возвращаются в список. После коммита — no-op. */
    fun undo() {
        val current = job ?: return
        current.cancel()
        job = null
        _window.value = null
    }

    /** Итог удаления закончился — сбрасываем флаг закрытия окна. */
    fun windowClosed() {
        _window.value = null
    }

    /** Ошибку уже показали — сбрасываем, чтобы повторить её при следующей неудаче. */
    fun failureHandled() {
        _failure.value = false
    }

    /** Итог обработан списком — сбрасываем. */
    fun finishedHandled() {
        _finished.value = null
    }
}

internal const val DELETE_UNDO_TIMEOUT_MILLIS = 4_000L
