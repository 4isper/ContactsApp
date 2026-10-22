package com.m4isper.contactsapp.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppLockTest {
    @Test
    fun `hash is salted for the same pin`() {
        assertNotEquals(hashAppLockPin("1234"), hashAppLockPin("1234"))
    }

    @Test
    fun `hash uses the versioned PBKDF2 format`() {
        val hash = hashAppLockPin("1234")

        assertTrue(hash.startsWith("pbkdf2-sha256$"))
        assertEquals(4, hash.split('$').size)
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
    fun `legacy SHA-256 hash still verifies`() {
        assertTrue(verifyAppLockPin("1234", "078e8919d760dd638c367b6b18b351300915f5fd23689f15ce1ba4d598cdea04"))
    }

    @Test
    fun `malformed hash never passes`() {
        assertFalse(verifyAppLockPin("1234", "pbkdf2-sha256\$not-a-number\$salt\$hash"))
    }

    @Test
    fun `empty hash never passes`() {
        assertFalse(verifyAppLockPin("1234", ""))
    }

    @Test
    fun `a new hash does not need migration`() {
        assertFalse(needsAppLockHashMigration(hashAppLockPin("1234")))
    }

    @Test
    fun `a legacy hash needs migration`() {
        assertTrue(needsAppLockHashMigration("078e8919d760dd638c367b6b18b351300915f5fd23689f15ce1ba4d598cdea04"))
    }

    @Test
    fun `an empty hash needs no migration`() {
        assertFalse(needsAppLockHashMigration(""))
    }
}
