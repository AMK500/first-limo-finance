package com.firstlimo.finance.data.repository

import com.firstlimo.finance.data.local.TransactionDao
import com.firstlimo.finance.data.local.TransactionEntity
import com.firstlimo.finance.data.model.FinancialSummary
import com.firstlimo.finance.data.model.FinancialTransaction
import com.firstlimo.finance.data.remote.FirestoreSyncService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class FinancialRepository(
    private val transactionDao: TransactionDao,
    private val firestoreSyncService: FirestoreSyncService
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
     * يتم حفظها محلياً فوراً في Room ثم رفعها سحابياً لـ Firestore
     */
    suspend fun saveTransaction(transaction: FinancialTransaction) {
        val entity = TransactionEntity.fromDomain(transaction.copy(isSynced = false))
        transactionDao.insertTransaction(entity)

        try {
            firestoreSyncService.uploadTransaction(transaction.userId, transaction)
            transactionDao.markAsSynced(transaction.id)
        } catch (e: Exception) {
            // ستبقى isSynced = 0 وسيتم رفعها تلقائياً عند عودة الإنترنت
            e.printStackTrace()
        }
    }

    /**
     * حذف عملية (Soft Delete محلياً وسحابياً)
     */
    suspend fun deleteTransaction(transaction: FinancialTransaction) {
        val now = System.currentTimeMillis()
        transactionDao.softDeleteTransaction(transaction.id, now)

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
     * مزامنة السجلات القادمة من السحابة وحفظها في قاعدة البيانات المحلية
     */
    suspend fun syncRemoteTransactionsToLocal(remoteTransactions: List<FinancialTransaction>) {
        if (remoteTransactions.isEmpty()) return
        val entities = remoteTransactions.map { TransactionEntity.fromDomain(it.copy(isSynced = true)) }
        transactionDao.insertAll(entities)
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
