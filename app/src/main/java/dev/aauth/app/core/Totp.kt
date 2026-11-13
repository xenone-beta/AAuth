package dev.aauth.app.core

import java.nio.ByteBuffer
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

enum class OtpAlgorithm(val macName: String, val uriName: String) {
    SHA1("HmacSHA1", "SHA1"),
    SHA256("HmacSHA256", "SHA256"),
    SHA512("HmacSHA512", "SHA512");

    companion object {
        fun fromUri(name: String?): OtpAlgorithm =
            entries.firstOrNull { it.uriName.equals(name, ignoreCase = true) } ?: SHA1
    }
}

/** How an account's codes are produced. */
enum class OtpType {
    /** Time-based codes (RFC 6238). */
    TOTP,

    /** Counter-based codes (RFC 4226): a new code only when the user asks for the next one. */
    HOTP,

    /** Steam Guard: time-based, HMAC-SHA1, five characters from a special alphabet. */
    STEAM;

    companion object {
        fun fromName(name: String?): OtpType =
            entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: TOTP
    }
}

/** RFC 6238 (TOTP) on top of RFC 4226 (HOTP). */
object Totp {

    fun generate(
        secret: ByteArray,
        timeMillis: Long,
        algorithm: OtpAlgorithm = OtpAlgorithm.SHA1,
        digits: Int = 6,
        periodSeconds: Int = 30,
    ): String {
        val counter = timeMillis / 1000L / periodSeconds
        return hotp(secret, counter, algorithm, digits)
    }

    fun hotp(
        secret: ByteArray,
        counter: Long,
        algorithm: OtpAlgorithm = OtpAlgorithm.SHA1,
        digits: Int = 6,
    ): String {
        require(digits in 6..8) { "Digits must be between 6 and 8" }
        val message = ByteBuffer.allocate(8).putLong(counter).array()
        val mac = Mac.getInstance(algorithm.macName)
        mac.init(SecretKeySpec(secret, algorithm.macName))
        val hash = mac.doFinal(message)

        val offset = hash[hash.size - 1].toInt() and 0x0F
        val binary = ((hash[offset].toInt() and 0x7F) shl 24) or
            ((hash[offset + 1].toInt() and 0xFF) shl 16) or
            ((hash[offset + 2].toInt() and 0xFF) shl 8) or
            (hash[offset + 3].toInt() and 0xFF)

        var modulus = 1
        repeat(digits) { modulus *= 10 }
        return (binary % modulus).toString().padStart(digits, '0')
    }

    private const val STEAM_ALPHABET = "23456789BCDFGHJKMNPQRTVWXY"
    const val STEAM_DIGITS = 5

    /** Steam Guard code: the HOTP truncation of HMAC-SHA1 over the 30 s counter, written in base 26. */
    fun steam(secret: ByteArray, timeMillis: Long): String {
        val counter = timeMillis / 1000L / 30L
        val message = ByteBuffer.allocate(8).putLong(counter).array()
        val mac = Mac.getInstance(OtpAlgorithm.SHA1.macName)
        mac.init(SecretKeySpec(secret, OtpAlgorithm.SHA1.macName))
        val hash = mac.doFinal(message)
        val offset = hash[hash.size - 1].toInt() and 0x0F
        var value = ((hash[offset].toInt() and 0x7F) shl 24) or
            ((hash[offset + 1].toInt() and 0xFF) shl 16) or
            ((hash[offset + 2].toInt() and 0xFF) shl 8) or
            (hash[offset + 3].toInt() and 0xFF)
        val out = StringBuilder(STEAM_DIGITS)
        repeat(STEAM_DIGITS) {
            out.append(STEAM_ALPHABET[value % STEAM_ALPHABET.length])
            value /= STEAM_ALPHABET.length
        }
        return out.toString()
    }

    /** Whole seconds left until the current code changes (1..period). */
    fun secondsRemaining(timeMillis: Long, periodSeconds: Int): Int {
        val elapsed = ((timeMillis / 1000L) % periodSeconds).toInt()
        return periodSeconds - elapsed
    }

    /** Fraction of the current period that is still left, from 1.0 (fresh) to 0.0 (about to change). */
    fun remainingFraction(timeMillis: Long, periodSeconds: Int): Float {
        val periodMillis = periodSeconds * 1000L
        val elapsed = timeMillis % periodMillis
        return 1f - elapsed.toFloat() / periodMillis.toFloat()
    }
}
