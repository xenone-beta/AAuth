package dev.aauth.app.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PinHasherTest {

    @Test
    fun correctPinVerifies() {
        val salt = PinHasher.newSalt()
        val hash = PinHasher.hash("4821", salt)
        assertTrue(PinHasher.verify("4821", salt, hash))
    }

    @Test
    fun wrongPinFails() {
        val salt = PinHasher.newSalt()
        val hash = PinHasher.hash("4821", salt)
        assertFalse(PinHasher.verify("4822", salt, hash))
    }

    @Test
    fun saltChangesTheHash() {
        val a = PinHasher.hash("4821", PinHasher.newSalt())
        val b = PinHasher.hash("4821", PinHasher.newSalt())
        assertFalse(a.contentEquals(b))
    }

    @Test
    fun validatesFormat() {
        assertTrue(PinHasher.isValidPin("1234"))
        assertTrue(PinHasher.isValidPin("12345678"))
        assertFalse(PinHasher.isValidPin("123"))
        assertFalse(PinHasher.isValidPin("123456789"))
        assertFalse(PinHasher.isValidPin("12a4"))
        assertFalse(PinHasher.isValidPin(""))
    }

    @Test
    fun lockoutGrowsAndIsCapped() {
        assertEquals(0L, PinLockout.delayMillis(0))
        assertEquals(0L, PinLockout.delayMillis(4))
        assertEquals(30_000L, PinLockout.delayMillis(5))
        assertEquals(60_000L, PinLockout.delayMillis(6))
        assertEquals(120_000L, PinLockout.delayMillis(7))
        assertEquals(30L * 60L * 1000L, PinLockout.delayMillis(50))
    }

    @Test
    fun attemptsLeftCountsDown() {
        assertEquals(5, PinLockout.attemptsLeftBeforeDelay(0))
        assertEquals(1, PinLockout.attemptsLeftBeforeDelay(4))
        assertEquals(0, PinLockout.attemptsLeftBeforeDelay(5))
    }
}
