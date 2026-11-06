package dev.aauth.app.core

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class BackupCryptoTest {

    private val plain = """{"version":1,"accounts":[]}""".toByteArray()

    @Test
    fun roundTrips() {
        val encrypted = BackupCrypto.encrypt(plain, "correct horse".toCharArray())
        assertArrayEquals(plain, BackupCrypto.decrypt(encrypted, "correct horse".toCharArray()))
    }

    @Test
    fun usesFreshSaltAndIvEachTime() {
        val a = BackupCrypto.encrypt(plain, "password1".toCharArray())
        val b = BackupCrypto.encrypt(plain, "password1".toCharArray())
        assertFalse(a.contentEquals(b))
    }

    @Test(expected = WrongPasswordException::class)
    fun wrongPasswordIsRejected() {
        val encrypted = BackupCrypto.encrypt(plain, "right-password".toCharArray())
        BackupCrypto.decrypt(encrypted, "wrong-password".toCharArray())
    }

    @Test(expected = WrongPasswordException::class)
    fun tamperedCiphertextIsRejected() {
        val encrypted = BackupCrypto.encrypt(plain, "password1".toCharArray())
        encrypted[encrypted.size - 1] = (encrypted[encrypted.size - 1].toInt() xor 0x01).toByte()
        BackupCrypto.decrypt(encrypted, "password1".toCharArray())
    }

    @Test(expected = InvalidBackupException::class)
    fun foreignFileIsRejected() {
        BackupCrypto.decrypt("this is definitely not a backup file at all".toByteArray(), "x".toCharArray())
    }

    @Test(expected = InvalidBackupException::class)
    fun truncatedFileIsRejected() {
        val encrypted = BackupCrypto.encrypt(plain, "password1".toCharArray())
        BackupCrypto.decrypt(encrypted.copyOf(10), "password1".toCharArray())
    }
}
