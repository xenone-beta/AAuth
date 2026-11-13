package dev.aauth.app.data

import dev.aauth.app.core.OtpAlgorithm
import dev.aauth.app.core.OtpType
import dev.aauth.app.core.Totp

/** UI-facing account. [secret] is null if the Keystore key could not decrypt the stored value. */
class Account(
    val id: Long,
    val issuer: String,
    val label: String,
    val secret: ByteArray?,
    val algorithm: OtpAlgorithm,
    val digits: Int,
    val period: Int,
    val type: OtpType = OtpType.TOTP,
    val counter: Long = 0,
) {
    /** Codes that change on their own with the clock (everything except HOTP). */
    val isTimeBased: Boolean get() = type != OtpType.HOTP

    /** The code to show at [nowMillis], or null if the secret could not be decrypted. */
    fun codeAt(nowMillis: Long): String? {
        val key = secret ?: return null
        return when (type) {
            OtpType.TOTP -> Totp.generate(key, nowMillis, algorithm, digits, period)
            OtpType.HOTP -> Totp.hotp(key, counter, algorithm, digits)
            OtpType.STEAM -> Totp.steam(key, nowMillis)
        }
    }

    /** Length of the window a code stays valid, in seconds (HOTP codes do not expire; 30 is used for the widget). */
    val windowSeconds: Int get() = if (type == OtpType.TOTP) period else 30

    /** Main line shown on the card: the service name, or the account if no service is set. */
    val title: String get() = issuer.ifBlank { label }

    /** Secondary line: the account name, only if it is not already the title. */
    val subtitle: String get() = if (issuer.isBlank()) "" else label
}
