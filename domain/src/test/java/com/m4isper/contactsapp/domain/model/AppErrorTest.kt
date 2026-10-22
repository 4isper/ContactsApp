package com.m4isper.contactsapp.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.IOException

/** Классификация ошибок: пользователю показывают вид ошибки, а не текст провайдера. */
class AppErrorTest {
    @Test
    fun `a security failure means no permission`() {
        assertEquals(AppError.PERMISSION_DENIED, AppError.from(SecurityException("denied")))
    }

    @Test
    fun `an io failure means the provider is unavailable`() {
        assertEquals(AppError.PROVIDER_FAILED, AppError.from(IOException("broken pipe")))
    }

    @Test
    fun `an unknown failure stays unknown`() {
        assertEquals(AppError.UNKNOWN, AppError.from(RuntimeException("boom")))
    }
}
