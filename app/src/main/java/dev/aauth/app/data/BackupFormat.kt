package dev.aauth.app.data

import dev.aauth.app.core.OtpAccount
import dev.aauth.app.core.OtpAlgorithm
import dev.aauth.app.core.OtpType
import org.json.JSONArray
import org.json.JSONObject

/** Plain JSON that gets encrypted by BackupCrypto before it is written to a file. */
object BackupFormat {

    fun toBytes(accounts: List<OtpAccount>): ByteArray {
        val array = JSONArray()
        for (a in accounts) {
            array.put(
                JSONObject()
                    .put("issuer", a.issuer)
                    .put("label", a.label)
                    .put("secret", a.secret)
                    .put("algorithm", a.algorithm.name)
                    .put("digits", a.digits)
                    .put("period", a.period)
                    .put("type", a.type.name)
                    .put("counter", a.counter),
            )
        }
        val root = JSONObject().put("version", 2).put("accounts", array)
        return root.toString().toByteArray(Charsets.UTF_8)
    }

    fun fromBytes(bytes: ByteArray): List<OtpAccount> {
        val root = JSONObject(String(bytes, Charsets.UTF_8))
        val array = root.getJSONArray("accounts")
        val result = ArrayList<OtpAccount>(array.length())
        for (i in 0 until array.length()) {
            val o = array.getJSONObject(i)
            result.add(
                OtpAccount(
                    issuer = o.optString("issuer"),
                    label = o.optString("label"),
                    secret = o.getString("secret"),
                    algorithm = runCatching { OtpAlgorithm.valueOf(o.optString("algorithm", "SHA1")) }
                        .getOrDefault(OtpAlgorithm.SHA1),
                    digits = o.optInt("digits", 6),
                    period = o.optInt("period", 30),
                    type = OtpType.fromName(o.optString("type", "TOTP")),
                    counter = o.optLong("counter", 0L),
                ),
            )
        }
        return result
    }
}
