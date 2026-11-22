package com.firstlimo.finance.ui.viewmodel

import android.app.Application
import android.content.Context
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
import com.firstlimo.finance.utils.BackupManager
import com.firstlimo.finance.utils.InstallerUtils
import com.firstlimo.finance.utils.NetworkMonitor
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class FinancialViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val firestoreSyncService = FirestoreSyncService()
    private val repository = FinancialRepository(
        transactionDao = database.transactionDao(),
        firestoreSyncService = firestoreSyncService,
        context = application,
        customerDao = database.customerDao()
    )
    private val networkMonitor = NetworkMonitor(application)
    private val updateService = GitHubUpdateService()
    private val prefs = application.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    val currentAppVersion: String
        get() = try {
            val pInfo = getApplication<Application>().packageManager.getPackageInfo(getApplication<Application>().packageName, 0)
            pInfo.versionName ?: "2.0.0"
        } catch (e: Exception) {
            "2.0.5"
        }

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

    private val _showUpToDateDialog = MutableStateFlow(false)
    val showUpToDateDialog: StateFlow<Boolean> = _showUpToDateDialog

    private val _downloadProgress = MutableStateFlow<Int?>(null)
    val downloadProgress: StateFlow<Int?> = _downloadProgress

    // حالة النسخ الاحتياطي
    private val _backupStatusMessage = MutableStateFlow<String?>(null)
    val backupStatusMessage: StateFlow<String?> = _backupStatusMessage

    fun setUserId(userId: String) {
        _currentUserId.value = userId
        if (userId.isNotEmpty()) {
            viewModelScope.launch {
                repository.syncFromCloud(userId)
            }
            listenToRemoteSync(userId)
            syncUnsynced(userId)
            // فحص هل توجد نسخة احتياطية محلية لاستعادتها تلقائياً إذا كانت قاعدة البيانات فارغة
            checkAndRestoreInitialBackup(userId)
        }
    }

    private fun checkAndRestoreInitialBackup(userId: String) {
        viewModelScope.launch {
            val currentList = repository.getTransactions(userId).firstOrNull() ?: emptyList()
            if (currentList.isEmpty()) {
                val restored = BackupManager.restoreBackup(getApplication(), userId)
                if (!restored.isNullOrEmpty()) {
                    repository.restoreTransactionsFromBackup(restored)
                }
            }
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
        time: String,
        receiptImagePath: String? = null
    ) {
        val userId = _currentUserId.value
        if (userId.isEmpty()) return

        val newTx = FinancialTransaction(
            userId = userId,
            type = type,
            amount = amount,
            notes = notes,
            dateString = date,
            timeString = time,
            receiptImagePath = receiptImagePath
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

    // استرجاع النسخة الاحتياطية يدوياً
    fun restoreFromBackup() {
        viewModelScope.launch {
            val restored = BackupManager.restoreBackup(getApplication(), _currentUserId.value)
            if (!restored.isNullOrEmpty()) {
                repository.restoreTransactionsFromBackup(restored)
                _backupStatusMessage.value = "تم استرجاع ${restored.size} عملية بنجاح! ✅"
            } else {
                _backupStatusMessage.value = "لم يتم العثور على نسخة احتياطية سابقة."
            }
        }
    }

    // إنشاء نسخة احتياطية يدوية
    fun createBackup() {
        viewModelScope.launch {
            val list = transactions.value
            val file = BackupManager.autoSaveBackup(getApplication(), list, _currentUserId.value)
            if (file != null) {
                _backupStatusMessage.value = "تم حفظ النسخة الاحتياطية بنجاح على الهاتف! 📁"
            } else {
                _backupStatusMessage.value = "تعذر حفظ النسخة الاحتياطية."
            }
        }
    }

    fun clearBackupMessage() {
        _backupStatusMessage.value = null
    }

    // فحص التحديثات من GitHub
    fun checkForAppUpdates(isManualCheck: Boolean = false) {
        viewModelScope.launch {
            val release = updateService.checkForUpdate(currentAppVersion)
            if (release != null) {
                val dismissedVersion = prefs.getString("dismissed_update_version", null)
                if (!isManualCheck && dismissedVersion == release.getCleanVersion()) {
                    return@launch
                }
                _availableUpdate.value = release
                _showUpToDateDialog.value = false
            } else {
                _availableUpdate.value = null
                if (isManualCheck) {
                    _showUpToDateDialog.value = true
                }
            }
        }
    }

    fun dismissUpdateDialog() {
        val release = _availableUpdate.value
        if (release != null) {
            prefs.edit().putString("dismissed_update_version", release.getCleanVersion()).apply()
        }
        _availableUpdate.value = null
        _downloadProgress.value = null
    }

    fun dismissUpToDateDialog() {
        _showUpToDateDialog.value = false
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
