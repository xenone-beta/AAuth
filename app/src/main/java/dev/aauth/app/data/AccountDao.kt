package dev.aauth.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts ORDER BY issuer COLLATE NOCASE, label COLLATE NOCASE")
    fun observeAll(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts")
    suspend fun getAll(): List<AccountEntity>

    @Insert
    suspend fun insert(entity: AccountEntity): Long

    @Query("UPDATE accounts SET issuer = :issuer, label = :label WHERE id = :id")
    suspend fun rename(id: Long, issuer: String, label: String)

    @Query("DELETE FROM accounts WHERE id = :id")
    suspend fun deleteById(id: Long)
}
