package dev.aauth.app.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TotpTest {

    // RFC 6238 appendix B: the shared secret differs per algorithm.
    private val sha1Secret = "12345678901234567890".toByteArray()
    private val sha256Secret = "12345678901234567890123456789012".toByteArray()
    private val sha512Secret = "1234567890123456789012345678901234567890123456789012345678901234".toByteArray()

    private fun code(secret: ByteArray, seconds: Long, algorithm: OtpAlgorithm) =
        Totp.generate(secret, seconds * 1000, algorithm, digits = 8, periodSeconds = 30)

    @Test
    fun sha1MatchesRfc6238() {
        assertEquals("94287082", code(sha1Secret, 59, OtpAlgorithm.SHA1))
        assertEquals("07081804", code(sha1Secret, 1111111109, OtpAlgorithm.SHA1))
        assertEquals("14050471", code(sha1Secret, 1111111111, OtpAlgorithm.SHA1))
        assertEquals("89005924", code(sha1Secret, 1234567890, OtpAlgorithm.SHA1))
        assertEquals("69279037", code(sha1Secret, 2000000000, OtpAlgorithm.SHA1))
        assertEquals("65353130", code(sha1Secret, 20000000000, OtpAlgorithm.SHA1))
    }

    @Test
    fun sha256MatchesRfc6238() {
        assertEquals("46119246", code(sha256Secret, 59, OtpAlgorithm.SHA256))
        assertEquals("68084774", code(sha256Secret, 1111111109, OtpAlgorithm.SHA256))
        assertEquals("67062674", code(sha256Secret, 1111111111, OtpAlgorithm.SHA256))
        assertEquals("91819424", code(sha256Secret, 1234567890, OtpAlgorithm.SHA256))
        assertEquals("90698825", code(sha256Secret, 2000000000, OtpAlgorithm.SHA256))
        assertEquals("77737706", code(sha256Secret, 20000000000, OtpAlgorithm.SHA256))
    }

    @Test
    fun sha512MatchesRfc6238() {
        assertEquals("90693936", code(sha512Secret, 59, OtpAlgorithm.SHA512))
        assertEquals("25091201", code(sha512Secret, 1111111109, OtpAlgorithm.SHA512))
        assertEquals("99943326", code(sha512Secret, 1111111111, OtpAlgorithm.SHA512))
        assertEquals("93441116", code(sha512Secret, 1234567890, OtpAlgorithm.SHA512))
        assertEquals("38618901", code(sha512Secret, 2000000000, OtpAlgorithm.SHA512))
        assertEquals("47863826", code(sha512Secret, 20000000000, OtpAlgorithm.SHA512))
    }

    @Test
    fun sixDigitCodeIsTheTailOfTheEightDigitCode() {
        assertEquals("287082", Totp.generate(sha1Secret, 59_000, OtpAlgorithm.SHA1, digits = 6))
    }

    @Test
    fun hotpMatchesRfc4226() {
        val expected = listOf("755224", "287082", "359152", "969429", "338314")
        expected.forEachIndexed { counter, value ->
            assertEquals(value, Totp.hotp(sha1Secret, counter.toLong()))
        }
    }

    @Test
    fun keepsLeadingZeros() {
        // 07081804 from the RFC vector must not turn into 7081804.
        assertEquals("07081804", code(sha1Secret, 1111111109, OtpAlgorithm.SHA1))
    }

    @Test
    fun secondsRemainingCountsDownFromThePeriod() {
        assertEquals(30, Totp.secondsRemaining(0, 30))
        assertEquals(30, Totp.secondsRemaining(60_000, 30))
        assertEquals(1, Totp.secondsRemaining(59_000, 30))
        assertEquals(25, Totp.secondsRemaining(5_500, 30))
    }

    @Test
    fun remainingFractionGoesFromOneToZero() {
        assertEquals(1f, Totp.remainingFraction(0, 30), 0.0001f)
        assertEquals(0.5f, Totp.remainingFraction(15_000, 30), 0.0001f)
        assertEquals(0.0333f, Totp.remainingFraction(29_000, 30), 0.001f)
    }

    @Test
    fun steamCodeHasFiveCharactersFromTheSteamAlphabet() {
        val alphabet = "23456789BCDFGHJKMNPQRTVWXY"
        for (seconds in listOf(0L, 59L, 1_111_111_109L, 2_000_000_000L)) {
            val code = Totp.steam(sha1Secret, seconds * 1000)
            assertEquals(5, code.length)
            assertTrue(code.all { it in alphabet })
        }
    }

    @Test
    fun steamCodeIsStableInsideAWindowAndChangesAfterIt() {
        val a = Totp.steam(sha1Secret, 30_000)
        assertEquals(a, Totp.steam(sha1Secret, 59_999))
        assertNotEquals(a, Totp.steam(sha1Secret, 60_000))
    }

    @Test
    fun steamMatchesReferenceValues() {
        // Computed with an independent implementation of the Steam Guard algorithm.
        assertEquals("PV9M4", Totp.steam(sha1Secret, 59L * 1000))
        assertEquals("PY4YB", Totp.steam(sha1Secret, 1111111109L * 1000))
        assertEquals("9N776", Totp.steam(sha1Secret, 2000000000L * 1000))
    }
}
