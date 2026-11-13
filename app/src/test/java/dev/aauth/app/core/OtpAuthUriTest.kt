package dev.aauth.app.core

import org.junit.Assert.assertEquals
import org.junit.Test

class OtpAuthUriTest {

    @Test
    fun parsesFullUri() {
        val account = OtpAuthUri.parse(
            "otpauth://totp/GitHub:alice@example.com?secret=JBSWY3DPEHPK3PXP&issuer=GitHub" +
                "&algorithm=SHA256&digits=8&period=60",
        )
        assertEquals("GitHub", account.issuer)
        assertEquals("alice@example.com", account.label)
        assertEquals("JBSWY3DPEHPK3PXP", account.secret)
        assertEquals(OtpAlgorithm.SHA256, account.algorithm)
        assertEquals(8, account.digits)
        assertEquals(60, account.period)
    }

    @Test
    fun appliesDefaults() {
        val account = OtpAuthUri.parse("otpauth://totp/alice?secret=JBSWY3DPEHPK3PXP")
        assertEquals("", account.issuer)
        assertEquals("alice", account.label)
        assertEquals(OtpAlgorithm.SHA1, account.algorithm)
        assertEquals(6, account.digits)
        assertEquals(30, account.period)
    }

    @Test
    fun decodesPercentEncodedLabelAndNormalizesSecret() {
        val account = OtpAuthUri.parse("otpauth://totp/Example%3Aalice%40google.com?secret=jbsw%20y3dp")
        assertEquals("Example", account.issuer)
        assertEquals("alice@google.com", account.label)
        assertEquals("JBSWY3DP", account.secret)
    }

    @Test
    fun issuerParameterWinsOverLabelPrefix() {
        val account = OtpAuthUri.parse("otpauth://totp/Old:bob?secret=JBSWY3DPEHPK3PXP&issuer=New")
        assertEquals("New", account.issuer)
        assertEquals("bob", account.label)
    }

    @Test
    fun ignoresOutOfRangeDigitsAndPeriod() {
        val account = OtpAuthUri.parse("otpauth://totp/x?secret=JBSWY3DPEHPK3PXP&digits=12&period=0")
        assertEquals(6, account.digits)
        assertEquals(30, account.period)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsMissingSecret() {
        OtpAuthUri.parse("otpauth://totp/alice?issuer=GitHub")
    }

    @Test
    fun parsesHotpWithCounter() {
        val account = OtpAuthUri.parse("otpauth://hotp/Example:alice?secret=JBSWY3DPEHPK3PXP&counter=7")
        assertEquals(OtpType.HOTP, account.type)
        assertEquals(7L, account.counter)
        assertEquals("Example", account.issuer)
        assertEquals("alice", account.label)
    }

    @Test
    fun hotpCounterDefaultsToZero() {
        assertEquals(0L, OtpAuthUri.parse("otpauth://hotp/alice?secret=JBSWY3DPEHPK3PXP").counter)
    }

    @Test
    fun parsesSteam() {
        val account = OtpAuthUri.parse("otpauth://steam/alice?secret=JBSWY3DPEHPK3PXP")
        assertEquals(OtpType.STEAM, account.type)
        assertEquals("Steam", account.issuer)
        assertEquals(5, account.digits)
        assertEquals(OtpAlgorithm.SHA1, account.algorithm)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsUnknownType() {
        OtpAuthUri.parse("otpauth://mystery/alice?secret=JBSWY3DPEHPK3PXP")
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsOtherSchemes() {
        OtpAuthUri.parse("https://example.com/?secret=JBSWY3DPEHPK3PXP")
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsInvalidBase32Secret() {
        OtpAuthUri.parse("otpauth://totp/alice?secret=NOT-VALID-1")
    }
}
