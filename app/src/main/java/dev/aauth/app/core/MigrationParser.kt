package dev.aauth.app.core

import java.net.URLDecoder
import java.util.Base64

/**
 * Parses the `otpauth-migration://offline?data=...` links produced by
 * "Export accounts" in Google Authenticator. The payload is a small protobuf message:
 *
 *   MigrationPayload { repeated OtpParameters otp_parameters = 1; ... }
 *   OtpParameters { bytes secret = 1; string name = 2; string issuer = 3;
 *                   Algorithm algorithm = 4; DigitCount digits = 5; OtpType type = 6; ... }
 *
 * Only TOTP entries with SHA1/SHA256/SHA512 are imported.
 */
object MigrationParser {

    private const val PREFIX = "otpauth-migration://"

    fun isMigrationUri(raw: String): Boolean = raw.trim().startsWith(PREFIX, ignoreCase = true)

    fun parse(raw: String): List<OtpAccount> {
        require(isMigrationUri(raw)) { "Not a migration URI" }
        val query = raw.trim().substringAfter('?', "")
        val data = query.split('&')
            .map { it.split('=', limit = 2) }
            .firstOrNull { it[0] == "data" }
            ?.getOrNull(1)
        requireNotNull(data) { "Missing data parameter" }

        // A raw '+' in a URL turns into a space after URL decoding; restore it.
        val base64 = URLDecoder.decode(data, "UTF-8").replace(' ', '+')
        val bytes = Base64.getMimeDecoder().decode(base64)

        val reader = Reader(bytes)
        val result = ArrayList<OtpAccount>()
        while (reader.hasMore()) {
            val tag = reader.varint().toInt()
            val field = tag ushr 3
            val wire = tag and 7
            if (field == 1 && wire == 2) {
                parseOtp(reader.bytes())?.let { result.add(it) }
            } else {
                reader.skip(wire)
            }
        }
        return result
    }

    private fun parseOtp(bytes: ByteArray): OtpAccount? {
        val reader = Reader(bytes)
        var secret = ByteArray(0)
        var name = ""
        var issuer = ""
        var algorithm = 1
        var digits = 1
        var type = 2

        while (reader.hasMore()) {
            val tag = reader.varint().toInt()
            val field = tag ushr 3
            val wire = tag and 7
            when {
                field == 1 && wire == 2 -> secret = reader.bytes()
                field == 2 && wire == 2 -> name = String(reader.bytes(), Charsets.UTF_8)
                field == 3 && wire == 2 -> issuer = String(reader.bytes(), Charsets.UTF_8)
                field == 4 && wire == 0 -> algorithm = reader.varint().toInt()
                field == 5 && wire == 0 -> digits = reader.varint().toInt()
                field == 6 && wire == 0 -> type = reader.varint().toInt()
                else -> reader.skip(wire)
            }
        }

        if (type == 1) return null // HOTP is not supported
        if (secret.isEmpty()) return null
        val algo = when (algorithm) {
            0, 1 -> OtpAlgorithm.SHA1
            2 -> OtpAlgorithm.SHA256
            3 -> OtpAlgorithm.SHA512
            else -> return null // MD5 and unknown values
        }

        val issuerFromName = if (name.contains(':')) name.substringBefore(':').trim() else ""
        return OtpAccount(
            issuer = issuer.ifBlank { issuerFromName },
            label = name.substringAfter(':').trim(),
            secret = Base32.encode(secret),
            algorithm = algo,
            digits = if (digits == 2) 8 else 6,
            period = 30,
        )
    }

    private class Reader(private val buf: ByteArray) {
        private var pos = 0

        fun hasMore(): Boolean = pos < buf.size

        fun varint(): Long {
            var result = 0L
            var shift = 0
            while (true) {
                require(pos < buf.size) { "Truncated varint" }
                val b = buf[pos++].toInt()
                result = result or ((b and 0x7F).toLong() shl shift)
                if ((b and 0x80) == 0) break
                shift += 7
                require(shift < 64) { "Varint too long" }
            }
            return result
        }

        fun bytes(): ByteArray {
            val length = varint().toInt()
            require(length >= 0 && pos + length <= buf.size) { "Truncated field" }
            val out = buf.copyOfRange(pos, pos + length)
            pos += length
            return out
        }

        fun skip(wireType: Int) {
            when (wireType) {
                0 -> varint()
                1 -> pos += 8
                2 -> bytes()
                5 -> pos += 4
                else -> throw IllegalArgumentException("Unsupported wire type $wireType")
            }
        }
    }
}
