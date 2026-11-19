package com.firstlimo.finance.data.repository

import android.content.Context
import com.firstlimo.finance.data.local.TransactionDao
import com.firstlimo.finance.data.local.TransactionEntity
import com.firstlimo.finance.data.model.FinancialSummary
import com.firstlimo.finance.data.model.FinancialTransaction
import com.firstlimo.finance.data.remote.FirestoreSyncService
import com.firstlimo.finance.utils.BackupManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

class FinancialRepository(
    private val transactionDao: TransactionDao,
    private val firestoreSyncService: FirestoreSyncService,
    private val context: Context? = null
) {

    /**
     * جلب كافة المعاملات المحلية للمستخدم
     */
    fun getTransactions(userId: String): Flow<List<FinancialTransaction>> {
        return transactionDao.getAllTransactions(userId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    /**
     * جلب الملخص المالي المحسوب لحظياً
     */
    fun getFinancialSummary(userId: String): Flow<FinancialSummary> {
        return getTransactions(userId).map { transactions ->
            FinancialSummary.calculate(transactions)
        }
    }

    /**
     * إضافة أو تعديل عملية مالية (Offline-First)
     * يتم حفظها محلياً فوراً ثم عمل نسخة احتياطية فورية على الذاكرة الخارجية لمنع فقدانها نهائياً
     */
    suspend fun saveTransaction(transaction: FinancialTransaction) {
        val entity = TransactionEntity.fromDomain(transaction.copy(isSynced = false))
        transactionDao.insertTransaction(entity)

        // حفظ نسخة احتياطية فورية على ذاكرة الهاتف العامة
        if (context != null) {
            val all = transactionDao.getAllTransactions(transaction.userId).firstOrNull()?.map { it.toDomain() } ?: emptyList()
            BackupManager.autoSaveBackup(context, all, transaction.userId)
        }

        try {
            firestoreSyncService.uploadTransaction(transaction.userId, transaction)
            transactionDao.markAsSynced(transaction.id)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * حذف عملية (Soft Delete محلياً وسحابياً وحفظ نسخة احتياطية)
     */
    suspend fun deleteTransaction(transaction: FinancialTransaction) {
        val now = System.currentTimeMillis()
        transactionDao.softDeleteTransaction(transaction.id, now)

        if (context != null) {
            val all = transactionDao.getAllTransactions(transaction.userId).firstOrNull()?.map { it.toDomain() } ?: emptyList()
            BackupManager.autoSaveBackup(context, all, transaction.userId)
        }

        try {
            firestoreSyncService.uploadTransaction(
                transaction.userId,
                transaction.copy(isDeleted = true, updatedAt = now)
            )
            transactionDao.markAsSynced(transaction.id)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * استرجاع نسخة احتياطية وحفظها في قاعدة البيانات المحلية
     */
    suspend fun restoreTransactionsFromBackup(transactions: List<FinancialTransaction>) {
        if (transactions.isEmpty()) return
        val entities = transactions.map { TransactionEntity.fromDomain(it) }
        transactionDao.insertAll(entities)
    }

    /**
     * مزامنة السجلات القادمة من السحابة وحفظها في قاعدة البيانات المحلية
     */
    suspend fun syncRemoteTransactionsToLocal(remoteTransactions: List<FinancialTransaction>) {
        if (remoteTransactions.isEmpty()) return
        val entities = remoteTransactions.map { TransactionEntity.fromDomain(it.copy(isSynced = true)) }
        transactionDao.insertAll(entities)

        if (context != null && remoteTransactions.isNotEmpty()) {
            val userId = remoteTransactions.first().userId
            val all = transactionDao.getAllTransactions(userId).firstOrNull()?.map { it.toDomain() } ?: emptyList()
            BackupManager.autoSaveBackup(context, all, userId)
        }
    }

    /**
     * رفع كافة المعاملات المعلقة التي تم إنشاؤها بدون إنترنت
     */
    suspend fun pushUnsyncedTransactionsToRemote(userId: String) {
        val unsyncedEntities = transactionDao.getUnsyncedTransactions()
        if (unsyncedEntities.isEmpty()) return

        val domainList = unsyncedEntities.map { it.toDomain() }
        firestoreSyncService.uploadBatchTransactions(userId, domainList)

        for (item in unsyncedEntities) {
            transactionDao.markAsSynced(item.id)
        }
    }
}
