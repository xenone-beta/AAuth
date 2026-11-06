package dev.aauth.app.core

import java.security.SecureRandom
import javax.crypto.BadPaddingException
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

class WrongPasswordException : Exception("Wrong password or corrupted backup")

class InvalidBackupException : Exception("Not an AAuth backup file")

/**
 * Password-based encryption for backup files.
 *
 * File layout: MAGIC (6) | salt (16) | IV (12) | AES-256-GCM ciphertext + tag.
 * The key is derived with PBKDF2-HMAC-SHA256; the magic header is authenticated as AAD.
 */
object BackupCrypto {
    private val MAGIC = "AAUTH1".toByteArray(Charsets.US_ASCII)
    private const val SALT_SIZE = 16
    private const val IV_SIZE = 12
    private const val TAG_SIZE = 16
    private const val ITERATIONS = 210_000
    private val random = SecureRandom()

    fun encrypt(plain: ByteArray, password: CharArray): ByteArray {
        val salt = ByteArray(SALT_SIZE).also { random.nextBytes(it) }
        val iv = ByteArray(IV_SIZE).also { random.nextBytes(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, deriveKey(password, salt), GCMParameterSpec(TAG_SIZE * 8, iv))
        cipher.updateAAD(MAGIC)
        return MAGIC + salt + iv + cipher.doFinal(plain)
    }

    fun decrypt(data: ByteArray, password: CharArray): ByteArray {
        val headerSize = MAGIC.size + SALT_SIZE + IV_SIZE
        if (data.size < headerSize + TAG_SIZE || !data.copyOfRange(0, MAGIC.size).contentEquals(MAGIC)) {
            throw InvalidBackupException()
        }
        val salt = data.copyOfRange(MAGIC.size, MAGIC.size + SALT_SIZE)
        val iv = data.copyOfRange(MAGIC.size + SALT_SIZE, headerSize)
        val body = data.copyOfRange(headerSize, data.size)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, deriveKey(password, salt), GCMParameterSpec(TAG_SIZE * 8, iv))
        cipher.updateAAD(MAGIC)
        try {
            return cipher.doFinal(body)
        } catch (e: BadPaddingException) {
            throw WrongPasswordException()
        }
    }

    private fun deriveKey(password: CharArray, salt: ByteArray): SecretKeySpec {
        val spec = PBEKeySpec(password, salt, ITERATIONS, 256)
        try {
            val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            return SecretKeySpec(factory.generateSecret(spec).encoded, "AES")
        } finally {
            spec.clearPassword()
        }
    }
}
