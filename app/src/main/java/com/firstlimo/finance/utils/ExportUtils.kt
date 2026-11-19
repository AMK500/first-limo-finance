package com.firstlimo.finance.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.firstlimo.finance.data.model.FinancialSummary
import com.firstlimo.finance.data.model.FinancialTransaction
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets

object ExportUtils {

    /**
     * تصدير سجل العمليات إلى ملف Excel بصيغة CSV مدعوم بالـ UTF-8 مع BOM للعربية
     */
    fun exportToCsv(
        context: Context,
        transactions: List<FinancialTransaction>,
        summary: FinancialSummary
    ): File? {
        return try {
            val exportDir = File(context.cacheDir, "exports")
            if (!exportDir.exists()) exportDir.mkdirs()

            val file = File(exportDir, "FirstLimo_Report_${System.currentTimeMillis()}.csv")
            val fos = FileOutputStream(file)
            val writer = OutputStreamWriter(fos, StandardCharsets.UTF_8)

            // كتابة UTF-8 BOM لفتح الأحرف العربية بشكل صحيح في برنامج Excel
            fos.write(0xEF)
            fos.write(0xBB)
            fos.write(0xBF)

            // ترويسة التقرير والملخص
            writer.write("تقرير الحسابات المالية - فرست ليموزين\n")
            writer.write("إجمالي التحصيل,${summary.totalCollection} ج.م\n")
            writer.write("إجمالي المصاريف,${summary.totalExpense} ج.م\n")
            writer.write("إجمالي الكوميسيون,${summary.totalCommission} ج.م\n")
            writer.write("صافي رصيد فرست ليموزين,${summary.netLimoBalance} ج.م\n")
            writer.write("إجمالي النقدية الشخصية,${summary.totalPersonalCash} ج.م\n")
            writer.write("إجمالي السيولة (الحسابين معاً),${summary.totalOverallLiquidity} ج.م\n\n")

            // عناوين الأعمدة
            writer.write("المعرف,نوع العملية,المبلغ (ج.م),التاريخ,الوقت,الملاحظات والتفاصيل\n")

            // صفوف العمليات
            for (tx in transactions) {
                val cleanNotes = tx.notes.replace(",", " - ").replace("\n", " ")
                writer.write("${tx.id},${tx.type.titleArabic},${tx.amount},${tx.dateString},${tx.timeString},\"$cleanNotes\"\n")
            }

            writer.flush()
            writer.close()
            fos.close()
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * مشاركة التقرير عبر تطبيقات المراسلة مثل واتساب
     */
    fun shareFile(context: Context, file: File, mimeType: String = "text/csv") {
        try {
            val contentUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, contentUri)
                putExtra(Intent.EXTRA_SUBJECT, "كشف حساب فرست ليموزين")
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            }

            val chooser = Intent.createChooser(intent, "مشاركة كشف الحساب")
            chooser.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
