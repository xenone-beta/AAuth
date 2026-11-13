package dev.aauth.app.core

import dev.aauth.app.data.BackupFormat
import org.junit.Assert.assertEquals
import org.junit.Test

class BackupFormatTest {

    @Test
    fun roundTripKeepsTypeAndCounter() {
        val accounts = listOf(
            OtpAccount("GitHub", "alice", "JBSWY3DPEHPK3PXP"),
            OtpAccount("Bank", "token", "JBSWY3DPEHPK3PXP", type = OtpType.HOTP, counter = 42),
            OtpAccount("Steam", "gabe", "JBSWY3DPEHPK3PXP", digits = 5, type = OtpType.STEAM),
        )
        val restored = BackupFormat.fromBytes(BackupFormat.toBytes(accounts))
        assertEquals(accounts, restored)
    }

    @Test
    fun oldBackupsWithoutTypeAreTotp() {
        val json = """{"version":1,"accounts":[{"issuer":"A","label":"b","secret":"JBSWY3DPEHPK3PXP"}]}"""
        val restored = BackupFormat.fromBytes(json.toByteArray())
        assertEquals(OtpType.TOTP, restored.single().type)
        assertEquals(0L, restored.single().counter)
    }
}
