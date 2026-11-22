package com.firstlimo.finance.data.repository

import android.content.Context
import com.firstlimo.finance.data.local.*
import com.firstlimo.finance.data.model.CustomerWithBalance
import com.firstlimo.finance.data.model.DebtsSummary
import com.firstlimo.finance.data.model.FinancialTransaction
import com.firstlimo.finance.data.model.TransactionType
import com.firstlimo.finance.data.remote.FirestoreSyncService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.util.UUID

class DebtsRepository(
    private val customerDao: CustomerDao,
    private val transactionDao: TransactionDao,
    private val context: Context? = null,
    private val syncService: FirestoreSyncService = FirestoreSyncService()
) {
    companion object {
        const val SYSTEM_CUSTOMER_NAME = "فرست ليموزين 🚖"
    }

    /**
     * التأكد من وجود العميل الأساسي "فرست ليموزين"
     */
    suspend fun ensureSystemCustomerExists(userId: String): CustomerEntity {
        val existing = customerDao.getSystemCustomer(userId)
        if (existing != null) return existing

        val systemCustomer = CustomerEntity(
            id = "sys_first_limo_$userId",
            userId = userId,
            name = SYSTEM_CUSTOMER_NAME,
            phone = null,
            address = "الشركة الرئيسية",
            notes = "حساب الشركة المالي المربوط بالمالية والمصاريف تلقائياً",
            isSystem = true
        )
        customerDao.insertCustomer(systemCustomer)
        return systemCustomer
    }

    /**
     * جلب قائمة العملاء مع أرصدتهم المحسوبة (أخذت، أعطيت، الصافي)
     */
    fun getCustomersWithBalance(userId: String): Flow<List<CustomerWithBalance>> {
        return combine(
            customerDao.getCustomers(userId),
            customerDao.getAllDebtTransactions(userId)
        ) { customers, transactions ->
            val transByCustomer = transactions.groupBy { it.customerId }
            customers.map { customer ->
                val list = transByCustomer[customer.id] ?: emptyList()
                val totalTook = list.filter { it.type == DebtType.TOOK.name }.sumOf { it.amount }
                val totalGave = list.filter { it.type == DebtType.GAVE.name }.sumOf { it.amount }
                val netBalance = totalTook - totalGave
                val lastDate = list.firstOrNull()?.dateString
                CustomerWithBalance(
                    customer = customer,
                    totalTook = totalTook,
                    totalGave = totalGave,
                    netBalance = netBalance,
                    lastTransactionDate = lastDate,
                    transactionsCount = list.size
                )
            }
        }
    }

    /**
     * جلب الملخص الإجمالي للديون
     */
    fun getDebtsSummary(userId: String): Flow<DebtsSummary> {
        return customerDao.getAllDebtTransactions(userId).combine(customerDao.getCustomers(userId)) { transactions, customers ->
            val totalTook = transactions.filter { it.type == DebtType.TOOK.name }.sumOf { it.amount }
            val totalGave = transactions.filter { it.type == DebtType.GAVE.name }.sumOf { it.amount }
            DebtsSummary(
                totalTookAll = totalTook,
                totalGaveAll = totalGave,
                netBalanceAll = totalTook - totalGave,
                customersCount = customers.size
            )
        }
    }

    /**
     * جلب معاملات عميل معين
     */
    fun getTransactionsForCustomer(customerId: String): Flow<List<DebtTransactionEntity>> {
        return customerDao.getDebtTransactionsForCustomer(customerId)
    }

    /**
     * حفظ أو تعديل عميل (الاسم فقط هو الإجباري)
     */
    suspend fun saveCustomer(customer: CustomerEntity) {
        val updated = customer.copy(updatedAt = System.currentTimeMillis())
        customerDao.insertCustomer(updated)
        try {
            syncService.uploadCustomer(customer.userId, updated)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * حذف عميل
     */
    suspend fun deleteCustomer(customerId: String, userId: String = "") {
        customerDao.softDeleteCustomer(customerId)
        if (userId.isNotBlank()) {
            try {
                syncService.uploadCustomer(userId, CustomerEntity(id = customerId, userId = userId, name = "", isDeleted = true))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * حفظ معاملة ديون مع الربط التلقائي بفرست ليموزين
     */
    suspend fun saveDebtTransaction(
        transaction: DebtTransactionEntity,
        isSystemCustomer: Boolean
    ) {
        var linkedFinId = transaction.linkedFinancialTransactionId

        // إذا كان العميل هو "فرست ليموزين"، نربط العملية تلقائياً بالمالية
        if (isSystemCustomer) {
            val finType = if (transaction.type == DebtType.TOOK.name) {
                TransactionType.COLLECTION // أخذت من ليموزين = تحصيل
            } else {
                TransactionType.EXPENSE    // أعطيت لـ ليموزين = مصروف
            }

            val finId = linkedFinId ?: UUID.randomUUID().toString()
            linkedFinId = finId

            val finEntity = TransactionEntity(
                id = finId,
                userId = transaction.userId,
                type = finType.name,
                amount = transaction.amount,
                timestamp = System.currentTimeMillis(),
                dateString = transaction.dateString,
                timeString = transaction.timeString,
                notes = transaction.notes,
                receiptImagePath = transaction.receiptImagePath,
                createdAt = transaction.createdAt,
                updatedAt = System.currentTimeMillis(),
                isDeleted = false,
                isSynced = false
            )
            transactionDao.insertTransaction(finEntity)
        }

        val updatedTx = transaction.copy(
            linkedFinancialTransactionId = linkedFinId,
            updatedAt = System.currentTimeMillis()
        )
        customerDao.insertDebtTransaction(updatedTx)

        try {
            syncService.uploadDebtTransaction(transaction.userId, updatedTx)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * حذف معاملة ديون (وإلغاء المعاملة المالية المربوطة بها إن وجدت)
     */
    suspend fun deleteDebtTransaction(transaction: DebtTransactionEntity) {
        customerDao.softDeleteDebtTransaction(transaction.id)
        if (!transaction.linkedFinancialTransactionId.isNullOrBlank()) {
            transactionDao.softDeleteTransaction(transaction.linkedFinancialTransactionId)
        }
        try {
            syncService.uploadDebtTransaction(transaction.userId, transaction.copy(isDeleted = true, updatedAt = System.currentTimeMillis()))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * استرجاع العملاء ومعاملات الديون من النسخة الاحتياطية
     */
    suspend fun restoreFromBackup(
        customers: List<CustomerEntity>,
        debtTransactions: List<DebtTransactionEntity>
    ) {
        if (customers.isNotEmpty()) {
            customerDao.insertAllCustomers(customers)
        }
        if (debtTransactions.isNotEmpty()) {
            customerDao.insertAllDebtTransactions(debtTransactions)
        }
    }

    /**
     * جلب كافة العملاء والديون من سحابة Firebase وحفظها محلياً فوراً
     */
    suspend fun syncFromCloud(userId: String) {
        if (userId.isBlank()) return
        try {
            val remoteCustomers = syncService.fetchAllCustomers(userId)
            if (remoteCustomers.isNotEmpty()) {
                customerDao.insertAllCustomers(remoteCustomers)
            }
            val remoteDebts = syncService.fetchAllDebtTransactions(userId)
            if (remoteDebts.isNotEmpty()) {
                customerDao.insertAllDebtTransactions(remoteDebts)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * الاستماع الحي واللحظي لتحديثات العملاء والديون في السحابة
     */
    fun startCloudListener(userId: String, scope: kotlinx.coroutines.CoroutineScope) {
        if (userId.isBlank()) return
        scope.launch {
            syncService.listenToUserCustomers(userId)
                .catch { e -> e.printStackTrace() }
                .collect { list ->
                    if (list.isNotEmpty()) {
                        customerDao.insertAllCustomers(list)
                    }
                }
        }
        scope.launch {
            syncService.listenToUserDebtTransactions(userId)
                .catch { e -> e.printStackTrace() }
                .collect { list ->
                    if (list.isNotEmpty()) {
                        customerDao.insertAllDebtTransactions(list)
                    }
                }
        }
    }
}
