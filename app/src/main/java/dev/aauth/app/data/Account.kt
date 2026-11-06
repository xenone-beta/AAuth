package dev.aauth.app.data

import dev.aauth.app.core.OtpAlgorithm

/** UI-facing account. [secret] is null if the Keystore key could not decrypt the stored value. */
class Account(
    val id: Long,
    val issuer: String,
    val label: String,
    val secret: ByteArray?,
    val algorithm: OtpAlgorithm,
    val digits: Int,
    val period: Int,
) {
    /** Main line shown on the card: the service name, or the account if no service is set. */
    val title: String get() = issuer.ifBlank { label }

    /** Secondary line: the account name, only if it is not already the title. */
    val subtitle: String get() = if (issuer.isBlank()) "" else label
}
