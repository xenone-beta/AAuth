package dev.aauth.app.core

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Salted PBKDF2 hashing for the app PIN. Only the hash and salt are stored, never the PIN.
 * A short numeric PIN is weak on its own, so [PinLockout] adds attempt throttling on top.
 */
object PinHasher {
    const val MIN_LENGTH = 4
    const val MAX_LENGTH = 8
    private const val SALT_SIZE = 16
    private const val ITERATIONS = 120_000
    private val random = SecureRandom()

    fun isValidPin(pin: String): Boolean =
        pin.length in MIN_LENGTH..MAX_LENGTH && pin.all { it in '0'..'9' }

    fun newSalt(): ByteArray = ByteArray(SALT_SIZE).also { random.nextBytes(it) }

    fun hash(pin: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), salt, ITERATIONS, 256)
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    fun verify(pin: String, salt: ByteArray, expected: ByteArray): Boolean =
        MessageDigest.isEqual(hash(pin, salt), expected)
}

/** Escalating delay after repeated wrong PIN entries. */
object PinLockout {
    private const val FREE_ATTEMPTS = 4

    /** Milliseconds the user must wait after [failedAttempts] wrong tries in a row (0 = no wait). */
    fun delayMillis(failedAttempts: Int): Long {
        if (failedAttempts <= FREE_ATTEMPTS) return 0L
        // 5th wrong try: 30 s, then 1 min, 2 min, 4 min ... capped at 30 min.
        val step = failedAttempts - FREE_ATTEMPTS - 1
        val seconds = 30L shl step.coerceAtMost(6)
        return (seconds * 1000L).coerceAtMost(30L * 60L * 1000L)
    }

    fun attemptsLeftBeforeDelay(failedAttempts: Int): Int = (FREE_ATTEMPTS - failedAttempts + 1).coerceAtLeast(0)
}
