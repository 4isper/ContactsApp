package com.m4isper.contactsapp.presentation.common

import kotlinx.coroutines.CancellationException

/**
 * Безопасный вызов: отмена корутины пробрасывается дальше, ошибка логируется
 * и возвращается вызывающему коду. Широкий catch живёт только здесь.
 */
@Suppress("TooGenericExceptionCaught") // Осознанно: ошибка приходит от провайдера, вид ошибки выбирает вызывающий.
internal inline fun <T> safeCall(
    tag: String,
    message: String,
    block: () -> T,
): Result<T> =
    try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        loge(tag, message, e)
        Result.failure(e)
    }
