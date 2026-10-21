package com.m4isper.contactsapp.presentation.settings

import com.m4isper.contactsapp.domain.model.hashAppLockPin
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppLockViewModelTest {
    @Test
    fun `correct pin unlocks the app`() {
        val viewModel = AppLockViewModel()
        val hash = hashAppLockPin("1234")

        assertTrue(viewModel.unlock("1234", hash))
        assertTrue(viewModel.isUnlocked.value)
    }

    @Test
    fun `wrong pin keeps the app locked`() {
        val viewModel = AppLockViewModel()
        val hash = hashAppLockPin("1234")

        assertFalse(viewModel.unlock("9999", hash))
        assertFalse(viewModel.isUnlocked.value)
    }

    @Test
    fun `lock blocks the app again`() {
        val viewModel = AppLockViewModel()
        val hash = hashAppLockPin("1234")
        viewModel.unlock("1234", hash)

        viewModel.lock()

        assertFalse(viewModel.isUnlocked.value)
    }

    @Test
    fun `empty hash never unlocks`() {
        val viewModel = AppLockViewModel()

        assertFalse(viewModel.unlock("1234", ""))
        assertFalse(viewModel.isUnlocked.value)
        assertEquals(false, viewModel.isUnlocked.value)
    }
}
