package dev.aauth.app.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MigrationParserTest {

    // Hand-built protobuf payload with three entries:
    //   1. GitHub / alice@example.com, SHA1, 6 digits, TOTP
    //   2. "Example:bob" with no issuer field, SHA256, 8 digits, TOTP
    //   3. Legacy / hotp-user, HOTP, counter not set (0)
    private val uri = "otpauth-migration://offline?data=" +
        "CjcKFDEyMzQ1Njc4OTAxMjM0NTY3ODkwEhFhbGljZUBleGFtcGxlLmNvbRoGR2l0SHViIAEoATACCiEKCkhlbGxvId6tvu8S" +
        "C0V4YW1wbGU6Ym9iGgAgAigCMAIKJQoKYWJjZGVmZ2hpahIJaG90cC11c2VyGgZMZWdhY3kgASgBMAEQARgBIAA%3D"

    @Test
    fun recognizesMigrationUris() {
        assertTrue(MigrationParser.isMigrationUri(uri))
        assertFalse(MigrationParser.isMigrationUri("otpauth://totp/x?secret=ABCDEFGH"))
    }

    @Test
    fun importsTotpAndHotpEntries() {
        val accounts = MigrationParser.parse(uri)
        assertEquals(3, accounts.size)

        val first = accounts[0]
        assertEquals("GitHub", first.issuer)
        assertEquals("alice@example.com", first.label)
        assertEquals("GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ", first.secret)
        assertEquals(OtpAlgorithm.SHA1, first.algorithm)
        assertEquals(6, first.digits)
        assertEquals(30, first.period)

        val second = accounts[1]
        assertEquals("Example", second.issuer)
        assertEquals("bob", second.label)
        assertEquals("JBSWY3DPEHPK3PXP", second.secret)
        assertEquals(OtpAlgorithm.SHA256, second.algorithm)
        assertEquals(8, second.digits)
        assertEquals(OtpType.TOTP, second.type)

        val third = accounts[2]
        assertEquals("Legacy", third.issuer)
        assertEquals("hotp-user", third.label)
        assertEquals(OtpType.HOTP, third.type)
        assertEquals(0L, third.counter)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsMissingData() {
        MigrationParser.parse("otpauth-migration://offline")
    }
}
