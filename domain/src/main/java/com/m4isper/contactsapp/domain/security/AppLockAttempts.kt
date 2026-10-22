package com.m4isper.contactsapp.domain.security

import java.time.Clock
import java.time.Duration
import java.time.Instant

/**
 * Защита от перебора PIN-кода: после серии неудачных попыток ввод блокируется
 * нарастающим временем. Счётчик живёт в памяти процесса — после перезапуска
 * приложения попытки обнуляются.
 */
class AppLockAttempts(
    private val clock: Clock = Clock.systemUTC(),
    private val maxAttempts: Int = DEFAULT_MAX_ATTEMPTS,
    private val baseDelay: Duration = DEFAULT_BASE_DELAY,
    private val maxDelay: Duration = DEFAULT_MAX_DELAY,
) {
    private var failures = 0
    private var lockedUntil: Instant? = null

    /** Заблокирован ли ввод PIN прямо сейчас. */
    fun isLocked(): Boolean = remainingLock() > Duration.ZERO

    /** Сколько осталось ждать до следующей попытки; нулевое — можно пробовать. */
    fun remainingLock(): Duration {
        val until = lockedUntil ?: return Duration.ZERO
        val remaining = Duration.between(clock.instant(), until)
        return if (remaining.isNegative) Duration.ZERO else remaining
    }

    /** Неудачная попытка: счётчик растёт, после порога ввод блокируется. */
    fun registerFailure() {
        failures += 1
        if (failures < maxAttempts) return
        val exponent = (failures - maxAttempts).coerceAtMost(MAX_BACKOFF_EXPONENT)
        val delay = baseDelay.multipliedBy(1L shl exponent).coerceAtMost(maxDelay)
        lockedUntil = clock.instant().plus(delay)
    }

    /** Успешный ввод сбрасывает счётчик и снимает блокировку. */
    fun registerSuccess() {
        failures = 0
        lockedUntil = null
    }

    companion object {
        /** Сколько неудачных попыток разрешено до блокировки. */
        const val DEFAULT_MAX_ATTEMPTS = 5

        /** Первая пауза после блокировки; дальше пауза удваивается. */
        val DEFAULT_BASE_DELAY: Duration = Duration.ofSeconds(30)

        /** Потолок паузы между попытками. */
        val DEFAULT_MAX_DELAY: Duration = Duration.ofMinutes(15)

        private const val MAX_BACKOFF_EXPONENT = 6
    }
}
