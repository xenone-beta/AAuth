package dev.aauth.app.data

import dev.aauth.app.core.Base32
import dev.aauth.app.core.OtpAccount
import dev.aauth.app.core.OtpAlgorithm
import dev.aauth.app.core.OtpType
import dev.aauth.app.core.SecretCipher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class AccountRepository(
    private val dao: AccountDao,
    private val cipher: SecretCipher,
) {
    val accounts: Flow<List<Account>> = dao.observeAll()
        .map { list -> list.map { it.toModel() } }
        .flowOn(Dispatchers.Default)

    /** Adds the accounts that are not stored yet and returns how many were actually added. */
    suspend fun addAll(list: List<OtpAccount>): Int = withContext(Dispatchers.IO) {
        val known = dao.getAll()
            .mapNotNull { e -> runCatching { keyOf(e.issuer, e.label, cipher.decrypt(e.secretEnc)) }.getOrNull() }
            .toHashSet()

        var added = 0
        for (item in list) {
            val bytes = runCatching { Base32.decode(item.secret) }.getOrNull() ?: continue
            val digitsOk = if (item.type == OtpType.STEAM) item.digits == 5 else item.digits in 6..8
            if (bytes.isEmpty() || !digitsOk || item.period < 1) continue
            if (!known.add(keyOf(item.issuer, item.label, bytes))) continue
            dao.insert(
                AccountEntity(
                    issuer = item.issuer,
                    label = item.label,
                    secretEnc = cipher.encrypt(bytes),
                    algorithm = item.algorithm.name,
                    digits = item.digits,
                    period = item.period,
                    createdAt = System.currentTimeMillis(),
                    type = item.type.name,
                    counter = item.counter,
                ),
            )
            added++
        }
        added
    }

    suspend fun delete(id: Long) = withContext(Dispatchers.IO) { dao.deleteById(id) }

    /** Erases every account. Used when too many wrong PINs were entered. */
    suspend fun deleteAll() = withContext(Dispatchers.IO) { dao.deleteAll() }

    /** HOTP: stores the counter for the next code. */
    suspend fun setCounter(id: Long, counter: Long) = withContext(Dispatchers.IO) { dao.setCounter(id, counter) }

    suspend fun rename(id: Long, issuer: String, label: String) =
        withContext(Dispatchers.IO) { dao.rename(id, issuer.trim(), label.trim()) }

    /** Returns every account that can still be decrypted, ready for export. */
    suspend fun exportAll(): List<OtpAccount> = withContext(Dispatchers.IO) {
        dao.getAll().mapNotNull { e ->
            val secret = runCatching { cipher.decrypt(e.secretEnc) }.getOrNull() ?: return@mapNotNull null
            OtpAccount(
                issuer = e.issuer,
                label = e.label,
                secret = Base32.encode(secret),
                algorithm = e.algorithm.toAlgorithm(),
                digits = e.digits,
                period = e.period,
                type = OtpType.fromName(e.type),
                counter = e.counter,
            )
        }
    }

    private fun AccountEntity.toModel() = Account(
        id = id,
        issuer = issuer,
        label = label,
        secret = runCatching { cipher.decrypt(secretEnc) }.getOrNull(),
        algorithm = algorithm.toAlgorithm(),
        digits = digits,
        period = period,
        type = OtpType.fromName(type),
        counter = counter,
    )

    private fun String.toAlgorithm(): OtpAlgorithm =
        runCatching { OtpAlgorithm.valueOf(this) }.getOrDefault(OtpAlgorithm.SHA1)

    private fun keyOf(issuer: String, label: String, secret: ByteArray): String =
        "$issuer\u0000$label\u0000${Base32.encode(secret)}"
}
