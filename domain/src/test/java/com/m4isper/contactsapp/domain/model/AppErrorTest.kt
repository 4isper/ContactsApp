package com.m4isper.contactsapp.domain.model

import com.m4isper.contactsapp.domain.model.common.AppError
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
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

    @Test
    fun `a local-or-SIM account rejection means the local account is blocked`() {
        assertEquals(
            AppError.LOCAL_ACCOUNT_BLOCKED,
            AppError.from(
                IllegalArgumentException(
                    "Cannot add contacts to local or SIM accounts when default account is set to cloud",
                ),
            ),
        )
    }

    @Test
    fun `an unrelated illegal argument stays unknown`() {
        assertEquals(AppError.UNKNOWN, AppError.from(IllegalArgumentException("bad argument")))
    }
}
