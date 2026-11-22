package com.firstlimo.finance.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomerDao {

    // ================= العملاء =================
    @Query("SELECT * FROM customers WHERE userId = :userId AND isDeleted = 0 ORDER BY isSystem DESC, updatedAt DESC")
    fun getCustomers(userId: String): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM customers WHERE userId = :userId AND isDeleted = 0")
    suspend fun getCustomersList(userId: String): List<CustomerEntity>

    @Query("SELECT * FROM customers WHERE userId = :userId AND id = :customerId AND isDeleted = 0 LIMIT 1")
    suspend fun getCustomerById(userId: String, customerId: String): CustomerEntity?

    @Query("SELECT * FROM customers WHERE userId = :userId AND isSystem = 1 AND isDeleted = 0 LIMIT 1")
    suspend fun getSystemCustomer(userId: String): CustomerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: CustomerEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllCustomers(customers: List<CustomerEntity>)

    @Update
    suspend fun updateCustomer(customer: CustomerEntity)

    @Query("UPDATE customers SET isDeleted = 1, updatedAt = :updatedAt WHERE id = :customerId")
    suspend fun softDeleteCustomer(customerId: String, updatedAt: Long = System.currentTimeMillis())

    // ================= معاملات الديون =================
    @Query("SELECT * FROM debt_transactions WHERE customerId = :customerId AND isDeleted = 0 ORDER BY createdAt DESC")
    fun getDebtTransactionsForCustomer(customerId: String): Flow<List<DebtTransactionEntity>>

    @Query("SELECT * FROM debt_transactions WHERE userId = :userId AND isDeleted = 0 ORDER BY createdAt DESC")
    fun getAllDebtTransactions(userId: String): Flow<List<DebtTransactionEntity>>

    @Query("SELECT * FROM debt_transactions WHERE userId = :userId AND isDeleted = 0")
    suspend fun getAllDebtTransactionsList(userId: String): List<DebtTransactionEntity>

    @Query("SELECT * FROM debt_transactions WHERE linkedFinancialTransactionId = :linkedId AND isDeleted = 0 LIMIT 1")
    suspend fun getDebtTransactionByLinkedId(linkedId: String): DebtTransactionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDebtTransaction(transaction: DebtTransactionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllDebtTransactions(transactions: List<DebtTransactionEntity>)

    @Update
    suspend fun updateDebtTransaction(transaction: DebtTransactionEntity)

    @Query("UPDATE debt_transactions SET isDeleted = 1, updatedAt = :updatedAt WHERE id = :transactionId")
    suspend fun softDeleteDebtTransaction(transactionId: String, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE debt_transactions SET isDeleted = 1, updatedAt = :updatedAt WHERE linkedFinancialTransactionId = :linkedId")
    suspend fun softDeleteByLinkedId(linkedId: String, updatedAt: Long = System.currentTimeMillis())

    // ================= ترحيل البيانات المحلية إلى حساب Google الجديد =================
    @Query("UPDATE customers SET userId = :newUserId WHERE userId != :newUserId AND isSystem = 0")
    suspend fun migrateCustomersToUser(newUserId: String): Int

    @Query("UPDATE customers SET id = 'sys_first_limo_' || :newUserId, userId = :newUserId WHERE isSystem = 1 AND userId != :newUserId")
    suspend fun migrateSystemCustomerToUser(newUserId: String): Int

    @Query("UPDATE debt_transactions SET userId = :newUserId WHERE userId != :newUserId")
    suspend fun migrateDebtTransactionsToUser(newUserId: String): Int

    @Query("UPDATE debt_transactions SET customerId = 'sys_first_limo_' || :newUserId WHERE customerId LIKE 'sys_first_limo_%' AND customerId != ('sys_first_limo_' || :newUserId)")
    suspend fun updateLinkedSystemCustomerId(newUserId: String): Int
}
