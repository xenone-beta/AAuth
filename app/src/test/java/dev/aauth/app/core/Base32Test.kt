package dev.aauth.app.core

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class Base32Test {

    // Test vectors from RFC 4648 section 10 (padding omitted).
    private val vectors = listOf(
        "" to "",
        "f" to "MY",
        "fo" to "MZXQ",
        "foo" to "MZXW6",
        "foob" to "MZXW6YQ",
        "fooba" to "MZXW6YTB",
        "foobar" to "MZXW6YTBOI",
    )

    @Test
    fun encodeMatchesRfcVectors() {
        for ((plain, encoded) in vectors) {
            assertEquals(encoded, Base32.encode(plain.toByteArray()))
        }
    }

    @Test
    fun decodeMatchesRfcVectors() {
        for ((plain, encoded) in vectors) {
            if (encoded.isEmpty()) continue
            assertArrayEquals(plain.toByteArray(), Base32.decode(encoded))
        }
    }

    @Test
    fun decodeIgnoresPaddingCaseAndSeparators() {
        val expected = "foobar".toByteArray()
        assertArrayEquals(expected, Base32.decode("mzxw6ytboi======"))
        assertArrayEquals(expected, Base32.decode("MZXW 6YTB OI"))
        assertArrayEquals(expected, Base32.decode("mzxw-6ytb-oi"))
    }

    @Test
    fun roundTripsRandomLookingBytes() {
        val data = ByteArray(37) { (it * 31 + 7).toByte() }
        assertArrayEquals(data, Base32.decode(Base32.encode(data)))
    }

    @Test(expected = IllegalArgumentException::class)
    fun decodeRejectsInvalidCharacters() {
        Base32.decode("MZXW1YTB")
    }

    @Test(expected = IllegalArgumentException::class)
    fun decodeRejectsEmptyInput() {
        Base32.decode("  ")
    }
}
