package dev.aauth.app.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val issuer: String,
    val label: String,
    /** Secret bytes encrypted with the Android Keystore key (Base64: IV + ciphertext). */
    val secretEnc: String,
    val algorithm: String,
    val digits: Int,
    val period: Int,
    val createdAt: Long,
    /** "TOTP", "HOTP" or "STEAM". */
    @ColumnInfo(defaultValue = "TOTP") val type: String = "TOTP",
    /** Next counter value, only used by HOTP accounts. */
    @ColumnInfo(defaultValue = "0") val counter: Long = 0,
)
