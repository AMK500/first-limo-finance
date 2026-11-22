package com.firstlimo.finance.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Query("SELECT * FROM transactions WHERE userId = :userId AND isDeleted = 0 ORDER BY timestamp DESC")
    fun getAllTransactions(userId: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE isSynced = 0")
    suspend fun getUnsyncedTransactions(): List<TransactionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(transactions: List<TransactionEntity>)

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Query("UPDATE transactions SET isDeleted = 1, updatedAt = :updatedAt, isSynced = 0 WHERE id = :id")
    suspend fun softDeleteTransaction(id: String, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE transactions SET isSynced = 1 WHERE id = :id")
    suspend fun markAsSynced(id: String)

    @Query("DELETE FROM transactions WHERE userId = :userId")
    suspend fun clearAllForUser(userId: String)

    @Query("UPDATE transactions SET userId = :newUserId, isSynced = 0 WHERE userId != :newUserId")
    suspend fun migrateTransactionsToUser(newUserId: String): Int

    @Query("SELECT * FROM transactions WHERE userId = :userId AND isDeleted = 0")
    suspend fun getTransactionsListForUser(userId: String): List<TransactionEntity>
}
