package com.firstlimo.finance.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.firstlimo.finance.data.local.AppDatabase
import com.firstlimo.finance.data.local.CustomerEntity
import com.firstlimo.finance.data.local.DebtTransactionEntity
import com.firstlimo.finance.data.model.CustomerWithBalance
import com.firstlimo.finance.data.model.DebtsSummary
import com.firstlimo.finance.data.repository.DebtsRepository
import com.firstlimo.finance.utils.BackupManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class DebtsViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val debtsRepository = DebtsRepository(
        customerDao = database.customerDao(),
        transactionDao = database.transactionDao(),
        context = application
    )

    private val _currentUserId = MutableStateFlow("")
    val currentUserId: StateFlow<String> = _currentUserId

    val searchQuery = MutableStateFlow("")

    // قائمة العملاء مع أرصدتهم مع دعم البحث الفوري
    val customersWithBalance: StateFlow<List<CustomerWithBalance>> = combine(
        _currentUserId.flatMapLatest { userId ->
            if (userId.isEmpty()) flowOf(emptyList())
            else debtsRepository.getCustomersWithBalance(userId)
        },
        searchQuery
    ) { list, query ->
        val cleanQuery = query.trim().lowercase()
        if (cleanQuery.isEmpty()) {
            list
        } else {
            list.filter { item ->
                item.customer.name.lowercase().contains(cleanQuery) ||
                        (item.customer.phone?.contains(cleanQuery) == true) ||
                        (item.customer.notes?.lowercase()?.contains(cleanQuery) == true)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // الملخص الإجمالي للديون
    val debtsSummary: StateFlow<DebtsSummary> = _currentUserId
        .flatMapLatest { userId ->
            if (userId.isEmpty()) flowOf(DebtsSummary())
            else debtsRepository.getDebtsSummary(userId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DebtsSummary())

    // العميل المحدد حالياً لعرض تفاصيله
    private val _selectedCustomerId = MutableStateFlow<String?>(null)
    val selectedCustomerId: StateFlow<String?> = _selectedCustomerId

    val selectedCustomerTransactions: StateFlow<List<DebtTransactionEntity>> = _selectedCustomerId
        .flatMapLatest { customerId ->
            if (customerId.isNullOrEmpty()) flowOf(emptyList())
            else debtsRepository.getTransactionsForCustomer(customerId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setUserId(userId: String) {
        _currentUserId.value = userId
        if (userId.isNotEmpty()) {
            viewModelScope.launch {
                // 1. ترحيل أي عملاء وديون محلية مسجلة سابقاً إلى حساب Google الجديد
                debtsRepository.migrateLocalDataToUser(userId)
                debtsRepository.ensureSystemCustomerExists(userId)
                // 2. مزامنة فورية وجلب لكافة العملاء والديون من السحابة
                debtsRepository.syncFromCloud(userId)
                // 3. رفع كافة البيانات المحلية والمرحلة إلى سحابة Google Cloud فوراً
                debtsRepository.uploadAllLocalDataToCloud(userId)
                // 4. بدء الاستماع المباشر للتعديلات اللحظية
                debtsRepository.startCloudListener(userId, viewModelScope)
                // فحص النسخ الاحتياطية كخطة طوارئ
                checkAndRestoreDebtsBackup(userId)
            }
        }
    }

    fun selectCustomer(customerId: String?) {
        _selectedCustomerId.value = customerId
    }

    private fun checkAndRestoreDebtsBackup(userId: String) {
        viewModelScope.launch {
            val currentList = debtsRepository.getCustomersWithBalance(userId).firstOrNull() ?: emptyList()
            if (currentList.size <= 1) { // فقط النظام أو لا يوجد
                val backup = BackupManager.restoreComprehensiveBackup(getApplication(), userId)
                if (backup != null && (backup.customers.isNotEmpty() || backup.debtTransactions.isNotEmpty())) {
                    debtsRepository.restoreFromBackup(backup.customers, backup.debtTransactions)
                }
            }
        }
    }

    fun addCustomer(name: String, phone: String?, address: String?, notes: String?) {
        viewModelScope.launch {
            val customer = CustomerEntity(
                userId = _currentUserId.value,
                name = name.trim(),
                phone = phone?.trim()?.ifBlank { null },
                address = address?.trim()?.ifBlank { null },
                notes = notes?.trim()?.ifBlank { null },
                isSystem = false
            )
            debtsRepository.saveCustomer(customer)
        }
    }

    fun updateCustomer(
        customer: CustomerEntity,
        name: String,
        phone: String?,
        address: String?,
        notes: String?
    ) {
        viewModelScope.launch {
            val updated = customer.copy(
                name = name.trim(),
                phone = phone?.trim()?.ifBlank { null },
                address = address?.trim()?.ifBlank { null },
                notes = notes?.trim()?.ifBlank { null },
                updatedAt = System.currentTimeMillis()
            )
            debtsRepository.saveCustomer(updated)
        }
    }

    fun deleteCustomer(customerId: String) {
        viewModelScope.launch {
            debtsRepository.deleteCustomer(customerId, _currentUserId.value)
        }
    }

    fun deleteCustomer(customer: CustomerEntity) {
        viewModelScope.launch {
            debtsRepository.deleteCustomer(customer.id, customer.userId)
        }
    }

    fun togglePinCustomer(customer: CustomerEntity) {
        viewModelScope.launch {
            debtsRepository.togglePinCustomer(customer)
        }
    }

    fun addDebtTransaction(
        customerId: String,
        isSystemCustomer: Boolean,
        type: String, // "TOOK" or "GAVE"
        amount: Double,
        notes: String,
        date: String,
        time: String,
        receiptPath: String?,
        editingId: String? = null,
        dueDate: String? = null
    ) {
        viewModelScope.launch {
            val transaction = DebtTransactionEntity(
                id = editingId ?: java.util.UUID.randomUUID().toString(),
                userId = _currentUserId.value,
                customerId = customerId,
                type = type,
                amount = amount,
                dateString = date,
                timeString = time,
                notes = notes,
                receiptImagePath = receiptPath,
                dueDateString = dueDate
            )
            debtsRepository.saveDebtTransaction(transaction, isSystemCustomer)
        }
    }

    fun deleteDebtTransaction(transaction: DebtTransactionEntity) {
        viewModelScope.launch {
            debtsRepository.deleteDebtTransaction(transaction)
        }
    }
}
