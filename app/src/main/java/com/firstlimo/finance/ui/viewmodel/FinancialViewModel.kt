package com.firstlimo.finance.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.firstlimo.finance.data.local.AppDatabase
import com.firstlimo.finance.data.model.FinancialSummary
import com.firstlimo.finance.data.model.FinancialTransaction
import com.firstlimo.finance.data.model.GitHubRelease
import com.firstlimo.finance.data.model.TransactionType
import com.firstlimo.finance.data.remote.FirestoreSyncService
import com.firstlimo.finance.data.remote.GitHubUpdateService
import com.firstlimo.finance.data.repository.FinancialRepository
import com.firstlimo.finance.utils.InstallerUtils
import com.firstlimo.finance.utils.NetworkMonitor
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class FinancialViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val firestoreSyncService = FirestoreSyncService()
    private val repository = FinancialRepository(database.transactionDao(), firestoreSyncService)
    private val networkMonitor = NetworkMonitor(application)
    private val updateService = GitHubUpdateService()

    // هوية المستخدم الحالي
    private val _currentUserId = MutableStateFlow<String>("")
    val currentUserId: StateFlow<String> = _currentUserId

    // المعاملات والملخص المالي
    val transactions: StateFlow<List<FinancialTransaction>> = _currentUserId
        .flatMapLatest { userId ->
            if (userId.isEmpty()) flowOf(emptyList())
            else repository.getTransactions(userId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val financialSummary: StateFlow<FinancialSummary> = _currentUserId
        .flatMapLatest { userId ->
            if (userId.isEmpty()) flowOf(FinancialSummary())
            else repository.getFinancialSummary(userId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FinancialSummary())

    // حالة الاتصال والمزامنة
    val isOnline: StateFlow<Boolean> = networkMonitor.isOnline
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing

    // حالة التحديثات الذاتية من داخل التطبيق
    private val _availableUpdate = MutableStateFlow<GitHubRelease?>(null)
    val availableUpdate: StateFlow<GitHubRelease?> = _availableUpdate

    private val _downloadProgress = MutableStateFlow<Int?>(null)
    val downloadProgress: StateFlow<Int?> = _downloadProgress

    fun setUserId(userId: String) {
        _currentUserId.value = userId
        if (userId.isNotEmpty()) {
            listenToRemoteSync(userId)
            syncUnsynced(userId)
        }
    }

    private fun listenToRemoteSync(userId: String) {
        viewModelScope.launch {
            firestoreSyncService.listenToUserTransactions(userId)
                .catch { e -> e.printStackTrace() }
                .collect { remoteList ->
                    repository.syncRemoteTransactionsToLocal(remoteList)
                }
        }
    }

    fun syncUnsynced(userId: String = _currentUserId.value) {
        if (userId.isEmpty()) return
        viewModelScope.launch {
            _isSyncing.value = true
            try {
                repository.pushUnsyncedTransactionsToRemote(userId)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isSyncing.value = false
            }
        }
    }

    fun addTransaction(
        type: TransactionType,
        amount: Double,
        notes: String,
        date: String,
        time: String
    ) {
        val userId = _currentUserId.value
        if (userId.isEmpty()) return

        val newTx = FinancialTransaction(
            userId = userId,
            type = type,
            amount = amount,
            notes = notes,
            dateString = date,
            timeString = time
        )

        viewModelScope.launch {
            repository.saveTransaction(newTx)
        }
    }

    fun updateTransaction(transaction: FinancialTransaction) {
        viewModelScope.launch {
            repository.saveTransaction(transaction.copy(updatedAt = System.currentTimeMillis()))
        }
    }

    fun deleteTransaction(transaction: FinancialTransaction) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
        }
    }

    // فحص التحديثات من GitHub
    fun checkForAppUpdates(currentVersion: String = "1.0.0") {
        viewModelScope.launch {
            val release = updateService.checkForUpdate(currentVersion)
            _availableUpdate.value = release
        }
    }

    fun dismissUpdateDialog() {
        _availableUpdate.value = null
        _downloadProgress.value = null
    }

    fun startInAppUpdate() {
        val release = _availableUpdate.value ?: return
        val downloadUrl = release.getApkDownloadUrl() ?: return

        viewModelScope.launch {
            _downloadProgress.value = 0
            val apkFile = updateService.downloadApk(
                context = getApplication(),
                downloadUrl = downloadUrl,
                onProgress = { progress ->
                    _downloadProgress.value = progress
                }
            )

            if (apkFile != null) {
                _downloadProgress.value = 100
                InstallerUtils.installApk(getApplication(), apkFile)
                _availableUpdate.value = null
                _downloadProgress.value = null
            }
        }
    }
}
