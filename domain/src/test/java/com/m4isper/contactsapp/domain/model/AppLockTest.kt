package com.m4isper.contactsapp.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppLockTest {
    @Test
    fun `hash is stable for the same pin`() {
        assertEquals(hashAppLockPin("1234"), hashAppLockPin("1234"))
    }

    @Test
    fun `hash differs for different pins`() {
        assertNotEquals(hashAppLockPin("1234"), hashAppLockPin("4321"))
    }

    @Test
    fun `pin is not stored in clear text`() {
        assertFalse(hashAppLockPin("1234").contains("1234"))
    }

    @Test
    fun `correct pin passes verification`() {
        val hash = hashAppLockPin("1234")

        assertTrue(verifyAppLockPin("1234", hash))
    }

    @Test
    fun `wrong pin fails verification`() {
        val hash = hashAppLockPin("1234")

        assertFalse(verifyAppLockPin("9999", hash))
    }

    @Test
    fun `empty hash never passes`() {
        assertFalse(verifyAppLockPin("1234", ""))
    }
}
