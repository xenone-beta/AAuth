package dev.aauth.app.core

import java.net.URLDecoder

/** A single TOTP account as it travels between QR codes, backups and the database. */
data class OtpAccount(
    val issuer: String,
    val label: String,
    /** Base32, upper case, no padding or spaces. */
    val secret: String,
    val algorithm: OtpAlgorithm = OtpAlgorithm.SHA1,
    val digits: Int = 6,
    val period: Int = 30,
)

/** Parser for the de-facto standard `otpauth://totp/...` key URI format. */
object OtpAuthUri {

    private const val PREFIX = "otpauth://"

    fun parse(raw: String): OtpAccount {
        val text = raw.trim()
        require(text.startsWith(PREFIX, ignoreCase = true)) { "Not an otpauth URI" }

        val rest = text.substring(PREFIX.length)
        val type = rest.substringBefore('/').lowercase()
        require(type == "totp") { "Only TOTP is supported" }

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

        return OtpAccount(
            issuer = issuer,
            label = account,
            secret = secret,
            algorithm = OtpAlgorithm.fromUri(params["algorithm"]),
            digits = params["digits"]?.toIntOrNull()?.takeIf { it in 6..8 } ?: 6,
            period = params["period"]?.toIntOrNull()?.takeIf { it in 1..300 } ?: 30,
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
