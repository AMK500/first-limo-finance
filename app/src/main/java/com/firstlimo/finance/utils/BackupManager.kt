package com.firstlimo.finance.utils

import android.content.Context
import android.os.Environment
import com.firstlimo.finance.data.local.CustomerEntity
import com.firstlimo.finance.data.local.DebtTransactionEntity
import com.firstlimo.finance.data.model.FinancialTransaction
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.io.FileReader
import java.io.FileWriter

data class ComprehensiveBackup(
    val version: Int = 2,
    val phoneNumber: String = "",
    val financialTransactions: List<FinancialTransaction> = emptyList(),
    val customers: List<CustomerEntity> = emptyList(),
    val debtTransactions: List<DebtTransactionEntity> = emptyList(),
    val backupTimestamp: Long = System.currentTimeMillis()
)

object BackupManager {

    private val gson = Gson()

    /**
     * المجلد العام على ذاكرة الهاتف الخارجية المشتركة (Documents/FirstLimoFinance)
     */
    private fun getPublicBackupDirectory(context: Context): File {
        val docsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        val backupDir = File(docsDir, "FirstLimoFinance")
        if (!backupDir.exists()) {
            backupDir.mkdirs()
        }
        return backupDir
    }

    /**
     * حفظ نسخة احتياطية شاملة بصيغة JSON على ذاكرة الهاتف الدائمة
     */
    fun autoSaveBackup(
        context: Context,
        transactions: List<FinancialTransaction>,
        phoneNumber: String,
        customers: List<CustomerEntity> = emptyList(),
        debtTransactions: List<DebtTransactionEntity> = emptyList()
    ): File? {
        return try {
            val dir = getPublicBackupDirectory(context)
            val backupFile = File(dir, "backup_${phoneNumber.trim().ifEmpty { "default" }}.json")
            val generalBackupFile = File(dir, "first_limo_latest_backup.json")

            val backupData = ComprehensiveBackup(
                version = 2,
                phoneNumber = phoneNumber,
                financialTransactions = transactions,
                customers = customers,
                debtTransactions = debtTransactions,
                backupTimestamp = System.currentTimeMillis()
            )

            val jsonString = gson.toJson(backupData)

            // كتابة النسخة الخاصة برقم الهاتف
            FileWriter(backupFile).use { it.write(jsonString) }

            // كتابة النسخة العامة
            FileWriter(generalBackupFile).use { it.write(jsonString) }

            backupFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * استرجاع النسخة الاحتياطية الشاملة مع التوافق التام مع النسخ القديمة
     */
    fun restoreComprehensiveBackup(context: Context, phoneNumber: String): ComprehensiveBackup? {
        return try {
            val dir = getPublicBackupDirectory(context)
            val phoneFile = File(dir, "backup_${phoneNumber.trim()}.json")
            val generalFile = File(dir, "first_limo_latest_backup.json")

            val fileToRead = when {
                phoneFile.exists() && phoneFile.length() > 0 -> phoneFile
                generalFile.exists() && generalFile.length() > 0 -> generalFile
                else -> null
            } ?: return null

            val jsonString = FileReader(fileToRead).use { it.readText() }

            // أولاً: محاولة قراءة التنسيق الشامل الجديد
            try {
                val backup = gson.fromJson(jsonString, ComprehensiveBackup::class.java)
                if (backup != null && (backup.financialTransactions.isNotEmpty() || backup.customers.isNotEmpty())) {
                    return backup
                }
            } catch (e: Exception) {
                // ليس التنسيق الجديد
            }

            // ثانياً: محاولة قراءة التنسيق القديم (قائمة معاملات مالية مباشرة)
            val type = object : TypeToken<List<FinancialTransaction>>() {}.type
            val legacyList: List<FinancialTransaction>? = gson.fromJson(jsonString, type)
            if (!legacyList.isNullOrEmpty()) {
                return ComprehensiveBackup(
                    version = 1,
                    phoneNumber = phoneNumber,
                    financialTransactions = legacyList,
                    customers = emptyList(),
                    debtTransactions = emptyList()
                )
            }
            null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * استرجاع كافة العمليات المحفوظة في النسخة الاحتياطية (للتوافق مع الاستدعاءات الحالية)
     */
    fun restoreBackup(context: Context, phoneNumber: String): List<FinancialTransaction>? {
        return restoreComprehensiveBackup(context, phoneNumber)?.financialTransactions
    }
}
