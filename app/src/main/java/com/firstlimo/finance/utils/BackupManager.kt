package com.firstlimo.finance.utils

import android.content.Context
import android.os.Environment
import com.firstlimo.finance.data.model.FinancialTransaction
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.io.FileReader
import java.io.FileWriter

object BackupManager {

    private val gson = Gson()

    /**
     * المجلد العام على ذاكرة الهاتف الخارجية المشتركة (Documents/FirstLimoFinance)
     * هذا المجلد لا يتم حذفه نهائياً عند مسح التطبيق أو إعادة تثبيته!
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
     * حفظ نسخة احتياطية تلقائية بصيغة JSON على ذاكرة الهاتف الدائمة
     */
    fun autoSaveBackup(context: Context, transactions: List<FinancialTransaction>, phoneNumber: String): File? {
        return try {
            val dir = getPublicBackupDirectory(context)
            val backupFile = File(dir, "backup_${phoneNumber.trim().ifEmpty { "default" }}.json")
            val generalBackupFile = File(dir, "first_limo_latest_backup.json")

            val jsonString = gson.toJson(transactions)

            // كتابة النسخة الخاصة برقم الهاتف
            val writer1 = FileWriter(backupFile)
            writer1.write(jsonString)
            writer1.flush()
            writer1.close()

            // كتابة النسخة العامة
            val writer2 = FileWriter(generalBackupFile)
            writer2.write(jsonString)
            writer2.flush()
            writer2.close()

            backupFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * فحص هل توجد نسخة احتياطية سابقة محفوظة على الهاتف لنفس رقم الهاتف
     */
    fun hasExistingBackup(context: Context, phoneNumber: String): Boolean {
        return try {
            val dir = getPublicBackupDirectory(context)
            val phoneFile = File(dir, "backup_${phoneNumber.trim()}.json")
            val generalFile = File(dir, "first_limo_latest_backup.json")
            (phoneFile.exists() && phoneFile.length() > 0) || (generalFile.exists() && generalFile.length() > 0)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * استرجاع كافة العمليات المحفوظة في النسخة الاحتياطية على الهاتف
     */
    fun restoreBackup(context: Context, phoneNumber: String): List<FinancialTransaction>? {
        return try {
            val dir = getPublicBackupDirectory(context)
            val phoneFile = File(dir, "backup_${phoneNumber.trim()}.json")
            val generalFile = File(dir, "first_limo_latest_backup.json")

            val targetFile = if (phoneFile.exists() && phoneFile.length() > 0) phoneFile else generalFile
            if (!targetFile.exists() || targetFile.length() == 0L) return null

            val reader = FileReader(targetFile)
            val type = object : TypeToken<List<FinancialTransaction>>() {}.type
            val list: List<FinancialTransaction>? = gson.fromJson(reader, type)
            reader.close()
            list
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
