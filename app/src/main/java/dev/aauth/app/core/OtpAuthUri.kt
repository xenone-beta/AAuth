package dev.aauth.app.core

import java.net.URLDecoder

/** A single account as it travels between QR codes, backups and the database. */
data class OtpAccount(
    val issuer: String,
    val label: String,
    /** Base32, upper case, no padding or spaces. */
    val secret: String,
    val algorithm: OtpAlgorithm = OtpAlgorithm.SHA1,
    val digits: Int = 6,
    val period: Int = 30,
    val type: OtpType = OtpType.TOTP,
    /** Next counter value; only used by [OtpType.HOTP]. */
    val counter: Long = 0,
)

/** Parser for the de-facto standard `otpauth://totp/...` key URI format (also `hotp` and `steam`). */
object OtpAuthUri {

    private const val PREFIX = "otpauth://"

    fun parse(raw: String): OtpAccount {
        val text = raw.trim()
        require(text.startsWith(PREFIX, ignoreCase = true)) { "Not an otpauth URI" }

        val rest = text.substring(PREFIX.length)
        val type = rest.substringBefore('/').lowercase()
        val otpType = when (type) {
            "totp" -> OtpType.TOTP
            "hotp" -> OtpType.HOTP
            "steam" -> OtpType.STEAM
            else -> throw IllegalArgumentException("Unsupported type: $type")
        }

        val afterType = rest.substringAfter('/', "")
        val labelPart = afterType.substringBefore('?')
        val params = parseQuery(afterType.substringAfter('?', ""))

        val label = decode(labelPart)
        val issuerFromLabel = if (label.contains(':')) label.substringBefore(':').trim() else ""
        val account = label.substringAfter(':').trim()
        val issuer = (params["issuer"] ?: issuerFromLabel).trim()

        val secret = params["secret"]
            ?.filter { it.isLetterOrDigit() }
            ?.uppercase()
            .orEmpty()
        require(secret.isNotEmpty()) { "Missing secret" }
        require(Base32.decode(secret).isNotEmpty()) { "Secret is too short" }

        if (otpType == OtpType.STEAM) {
            // Steam Guard is always HMAC-SHA1, 5 characters, 30 seconds.
            return OtpAccount(
                issuer = issuer.ifBlank { "Steam" },
                label = account,
                secret = secret,
                algorithm = OtpAlgorithm.SHA1,
                digits = Totp.STEAM_DIGITS,
                period = 30,
                type = OtpType.STEAM,
            )
        }

        return OtpAccount(
            issuer = issuer,
            label = account,
            secret = secret,
            algorithm = OtpAlgorithm.fromUri(params["algorithm"]),
            digits = params["digits"]?.toIntOrNull()?.takeIf { it in 6..8 } ?: 6,
            period = params["period"]?.toIntOrNull()?.takeIf { it in 1..300 } ?: 30,
            type = otpType,
            counter = if (otpType == OtpType.HOTP) params["counter"]?.toLongOrNull()?.takeIf { it >= 0 } ?: 0L else 0L,
        )
    }

    private fun parseQuery(query: String): Map<String, String> {
        if (query.isEmpty()) return emptyMap()
        val map = HashMap<String, String>()
        for (pair in query.split('&')) {
            if (pair.isEmpty()) continue
            val key = decode(pair.substringBefore('=')).lowercase()
            val value = decode(pair.substringAfter('=', ""))
            map[key] = value
        }
        return map
    }

    private fun decode(value: String): String = URLDecoder.decode(value, "UTF-8")
}
